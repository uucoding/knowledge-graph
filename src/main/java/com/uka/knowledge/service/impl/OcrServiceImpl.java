package com.uka.knowledge.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uka.knowledge.config.FileConfig;
import com.uka.knowledge.config.OcrConfig;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import com.uka.knowledge.mapper.OcrRecordMapper;
import com.uka.knowledge.model.entity.OcrRecord;
import com.uka.knowledge.model.vo.OcrRecordVO;
import com.uka.knowledge.service.KnowledgeExtractService;
import com.uka.knowledge.service.OcrService;
import com.uka.knowledge.service.OllamaService;
import com.uka.knowledge.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * OCR服务实现类
 * <p>
 * 实现图片OCR识别相关的业务逻辑
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OcrServiceImpl extends ServiceImpl<OcrRecordMapper, OcrRecord> implements OcrService {

    private final FileConfig fileConfig;
    private final OcrConfig ocrConfig;
    private final OllamaService ollamaService;
    private final VectorService vectorService;
    private final KnowledgeExtractService knowledgeExtractService;
    private final AsyncService asyncService;

    /**
     * 支持的图片类型
     */
    private static final List<String> IMAGE_TYPES = Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "tiff");

    /**
     * 上传图片并进行OCR识别
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OcrRecord uploadAndRecognize(MultipartFile file) {
        // 验证文件
        validateImage(file);

        // 保存图片
        String imagePath = saveImage(file);

        // 获取图片信息
        String originalName = file.getOriginalFilename();
        String imageType = FileUtil.getSuffix(originalName);

        // 创建OCR记录
        OcrRecord record = new OcrRecord();
        record.setImageName(FileUtil.mainName(originalName));
        record.setOriginalName(originalName);
        record.setImagePath(imagePath);
        record.setImageType(imageType);
        record.setImageSize(file.getSize());
        record.setLanguage(ocrConfig.getLanguage());
        record.setStatus(OcrRecord.STATUS_PENDING);

        // 保存到数据库
        this.save(record);
        // 事务之后执行
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            // 事务提交后执行

                            // 异步进行OCR识别
                            asyncService.asyncRecognize(record.getId());
                        }
                    }
            );
        }
        return record;
    }


    /**
     * 对指定图片进行OCR识别
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OcrRecord recognizeImage(Long recordId) {
        OcrRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 更新状态为处理中
        record.setStatus(OcrRecord.STATUS_PROCESSING);
        this.updateById(record);

        try {
            // 执行OCR识别
            String ocrText = performOcr(record.getImagePath());
            record.setOcrText(ocrText);

            // 生成向量并存储
            if (StrUtil.isNotBlank(ocrText)) {
                float[] vector = ollamaService.generateEmbedding(ocrText);
                String vectorId = vectorService.insertVector(new VectorService.VectorInsertData(record.getId(), vector, "ocr"));
                record.setVectorId(vectorId);

                // 自动抽取知识并构建图谱
                try {
                    KnowledgeExtractService.ExtractResult extractResult =
                            knowledgeExtractService.extractFromOcr(record.getId(), ocrText);
                    log.info("OCR知识抽取完成, recordId={}, 节点数={}, 关系数={}",
                            recordId, extractResult.nodeCount(), extractResult.relationCount());
                } catch (Exception e) {
                    log.warn("OCR知识抽取失败, recordId={}, error={}", recordId, e.getMessage());
                    // 知识抽取失败不影响OCR识别的整体结果
                }
            }

            // 更新状态为已完成
            record.setStatus(OcrRecord.STATUS_COMPLETED);
            record.setErrorMsg(null);

            log.info("OCR识别成功, recordId={}, textLength={}", recordId,
                    ocrText != null ? ocrText.length() : 0);

        } catch (Exception e) {
            log.error("OCR识别失败, recordId={}", recordId, e);
            record.setStatus(OcrRecord.STATUS_FAILED);
            record.setErrorMsg(e.getMessage());
        }

        this.updateById(record);
        return record;
    }

    /**
     * 分页查询OCR记录
     */
    @Override
    public Page<OcrRecordVO> pageRecords(Integer pageNum, Integer pageSize,
                                          String keyword, String imageType, Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<OcrRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), OcrRecord::getOcrText, keyword)
                .or()
                .like(StrUtil.isNotBlank(keyword), OcrRecord::getImageName, keyword);
        wrapper.eq(StrUtil.isNotBlank(imageType), OcrRecord::getImageType, imageType);
        wrapper.eq(status != null, OcrRecord::getStatus, status);
        wrapper.orderByDesc(OcrRecord::getCreateTime);

        // 执行分页查询
        Page<OcrRecord> page = this.page(new Page<>(pageNum, pageSize), wrapper);

        // 转换为VO
        Page<OcrRecordVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).toList());

        return voPage;
    }

    /**
     * 获取OCR记录详情
     */
    @Override
    public OcrRecordVO getRecordDetail(Long id) {
        OcrRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return convertToVO(record);
    }

    /**
     * 删除OCR记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRecord(Long id) {
        OcrRecord record = this.getById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 删除向量
        if (StrUtil.isNotBlank(record.getVectorId())) {
            vectorService.deleteVector(record.getVectorId());
        }

        // 删除图片文件
        FileUtil.del(record.getImagePath());

        // 逻辑删除记录
        return this.removeById(id);
    }

    /**
     * 重新识别图片
     */
    @Override
    public OcrRecord reRecognize(Long id) {
        return recognizeImage(id);
    }

    /**
     * 验证上传的图片
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图片不能为空");
        }

        String originalName = file.getOriginalFilename();
        String imageType = FileUtil.getSuffix(originalName);

        if (!IMAGE_TYPES.contains(imageType.toLowerCase())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT,
                    "不支持的图片类型: " + imageType);
        }
    }

    /**
     * 保存图片到本地
     */
    private String saveImage(MultipartFile file) {
        try {
            // 生成存储路径：uploads/ocr/2024/01/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

            // 使用绝对路径
            Path basePath = Paths.get(fileConfig.getUploadPath()).toAbsolutePath().normalize();
            Path uploadDir = basePath.resolve("ocr").resolve(datePath);
            Files.createDirectories(uploadDir);

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String extension = FileUtil.getSuffix(originalName);
            String newFileName = IdUtil.fastSimpleUUID() + "." + extension;

            // 保存文件 - 使用字节流方式，避免transferTo的路径问题
            Path filePath = uploadDir.resolve(newFileName);
            Files.write(filePath, file.getBytes());

            return filePath.toString();

        } catch (IOException e) {
            log.error("图片保存失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 执行OCR识别
     *
     * @param imagePath 图片路径
     * @return 识别结果文本
     */
    private String performOcr(String imagePath) {
        try {
            // 加载库文件
            System.setProperty("jna.library.path", ocrConfig.getLibraryFile());
            // 创建Tesseract实例
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(ocrConfig.getDataPath());
            tesseract.setLanguage(ocrConfig.getLanguage());

            // 读取图片
            File imageFile = new File(imagePath);
            BufferedImage image = ImageIO.read(imageFile);

            if (image == null) {
                throw new BusinessException(ResultCode.OCR_FAILED, "无法读取图片文件");
            }

            // 2. 预处理
            image = preprocessImage(image);

            // 执行OCR
            String result = tesseract.doOCR(image);

            return StrUtil.trim(result);

        } catch (TesseractException e) {
            log.error("Tesseract OCR失败", e);
            throw new BusinessException(ResultCode.OCR_FAILED, "OCR识别失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("读取图片失败", e);
            throw new BusinessException(ResultCode.OCR_FAILED, "读取图片失败: " + e.getMessage());
        }
    }

    /**
     * 实体转VO
     */
    private OcrRecordVO convertToVO(OcrRecord record) {
        OcrRecordVO vo = new OcrRecordVO();
        BeanUtils.copyProperties(record, vo);

        // 图片大小可读格式
        vo.setImageSizeReadable(FileUtil.readableFileSize(record.getImageSize()));

        // 图片访问URL
        vo.setImageUrl("/uploads/" + record.getImagePath().replace("\\", "/"));

        return vo;
    }

    private BufferedImage preprocessImage(BufferedImage image) {
        // 转换为灰度图
        BufferedImage grayImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );
        grayImage.getGraphics().drawImage(image, 0, 0, null);

        // 二值化（黑白）
        BufferedImage binaryImage = new BufferedImage(
                grayImage.getWidth(),
                grayImage.getHeight(),
                BufferedImage.TYPE_BYTE_BINARY
        );
        binaryImage.getGraphics().drawImage(grayImage, 0, 0, null);

        // 调整对比度
        BufferedImage enhancedImage = enhanceContrast(binaryImage);

        return enhancedImage;
    }

    private BufferedImage enhanceContrast(BufferedImage image) {
        // 简单对比度增强
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int gray = (rgb >> 16) & 0xFF;
                // 阈值处理
                int newGray = gray > 128 ? 255 : 0;
                int newRgb = (newGray << 16) | (newGray << 8) | newGray;
                result.setRGB(x, y, newRgb);
            }
        }
        return result;
    }
}
