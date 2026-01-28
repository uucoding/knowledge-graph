package com.uka.knowledge.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uka.knowledge.config.FileConfig;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import com.uka.knowledge.mapper.DocumentChunkMapper;
import com.uka.knowledge.mapper.DocumentMapper;
import com.uka.knowledge.mapper.KnowledgeNodeMapper;
import com.uka.knowledge.model.entity.Document;
import com.uka.knowledge.model.entity.DocumentChunk;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.vo.DocumentVO;
import com.uka.knowledge.service.DocumentService;
import com.uka.knowledge.service.KnowledgeExtractService;
import com.uka.knowledge.service.KnowledgeNodeService;
import com.uka.knowledge.service.OllamaService;
import com.uka.knowledge.service.VectorService;
import com.uka.knowledge.util.DocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 文档服务实现类
 * <p>
 * 实现文档上传、解析、查询等业务逻辑
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements DocumentService {

    private final FileConfig fileConfig;
    private final DocumentParser documentParser;
    private final OllamaService ollamaService;
    private final VectorService vectorService;
    private final KnowledgeExtractService knowledgeExtractService;
    private final KnowledgeNodeMapper knowledgeNodeMapper;
    private final KnowledgeNodeService knowledgeNodeService;
    private final AsyncService asyncDocumentService;
    private final DocumentChunkMapper documentChunkMapper;

    /**
     * 上传并解析文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document uploadAndParse(MultipartFile file) {
        // 验证文件
        validateFile(file);

        // 保存文件
        String filePath = saveFile(file);

        // 获取文件信息
        String originalName = file.getOriginalFilename();
        String fileType = FileUtil.getSuffix(originalName);

        // 创建文档记录
        Document document = new Document();
        document.setName(FileUtil.mainName(originalName));
        document.setOriginalName(originalName);
        document.setFilePath(filePath);
        document.setFileType(fileType);
        document.setFileSize(file.getSize());
        document.setStatus(Document.STATUS_PENDING);

        // 保存到数据库
        this.save(document);
        // 事务之后执行
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                     @Override
                     public void afterCommit() {
                         // 事务提交后执行
                         // 异步解析文档（通过独立的AsyncDocumentService调用，避免同类调用@Async失效）
                         asyncDocumentService.asyncParseDocument(document.getId());
                     }
                }
            );
        }
        return document;
    }

    /**
     * 解析指定文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document parseDocument(Long documentId) {
        Document document = this.getById(documentId);
        if (document == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 更新状态为处理中
        document.setStatus(Document.STATUS_PROCESSING);
        this.updateById(document);

        try {
            // 解析文档内容（带页码）
            DocumentParser.ParseResult parseResult = documentParser.parseWithPages(
                    document.getFilePath(), document.getFileType());

            document.setContent(parseResult.getFullContent());

            // 删除旧的分块
            documentChunkMapper.deleteByDocumentId(documentId);

            // 创建分块并生成向量
            List<DocumentParser.PageContent> pages = parseResult.getPages();
            for (int i = 0; i < pages.size(); i++) {
                DocumentParser.PageContent page = pages.get(i);
                String content = page.getContent();

                if (StrUtil.isBlank(content)) {
                    continue;
                }

                // 创建分块记录
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setPageNum(page.getPageNum());
                chunk.setChunkIndex(i);
                chunk.setContent(content);

                // 生成向量
                float[] vector = ollamaService.generateEmbedding(content);
                documentChunkMapper.insert(chunk);

                // 存储向量（使用chunk类型，ID为chunk的ID）
                String vectorId = vectorService.insertVector(chunk.getId(), vector, "chunk");
                chunk.setVectorId(vectorId);
                documentChunkMapper.updateById(chunk);
            }

            // 更新状态为已完成
            document.setStatus(Document.STATUS_COMPLETED);
            document.setErrorMsg(null);

            log.info("文档解析成功, documentId={}, contentLength={}, chunks={}",
                    documentId, parseResult.getFullContent().length(), pages.size());

        } catch (Exception e) {
            log.error("文档解析失败, documentId={}", documentId, e);
            document.setStatus(Document.STATUS_FAILED);
            document.setErrorMsg(e.getMessage());
        }

        // 自动抽取知识并构建图谱
        try {
            KnowledgeExtractService.ExtractResult extractResult = knowledgeExtractService.extractFromDocument(document.getId(), document.getContent());
            log.info("OCR知识抽取完成, recordId={}, 节点数={}, 关系数={}",
                    document.getId(), extractResult.nodeCount(), extractResult.relationCount());
        } catch (Exception e) {
            log.warn("OCR知识抽取失败, recordId={}, error={}", document.getId(), e.getMessage());
            // 知识抽取失败不影响OCR识别的整体结果
        }

        this.updateById(document);
        return document;
    }

    /**
     * 分页查询文档
     */
    @Override
    public Page<DocumentVO> pageDocuments(Integer pageNum, Integer pageSize,
                                           String keyword, String fileType, Integer status) {
        // 构建查询条件
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), Document::getName, keyword)
                .or()
                .like(StrUtil.isNotBlank(keyword), Document::getOriginalName, keyword);
        wrapper.eq(StrUtil.isNotBlank(fileType), Document::getFileType, fileType);
        wrapper.eq(status != null, Document::getStatus, status);
        wrapper.orderByDesc(Document::getCreateTime);

        // 执行分页查询
        Page<Document> page = this.page(new Page<>(pageNum, pageSize), wrapper);

        // 转换为VO
        Page<DocumentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).toList());

        return voPage;
    }

    /**
     * 获取文档详情
     */
    @Override
    public DocumentVO getDocumentDetail(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return convertToVO(document);
    }

    /**
     * 删除文档
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 删除向量
        if (StrUtil.isNotBlank(document.getVectorId())) {
            vectorService.deleteVector(document.getVectorId());
        }

        // 删除关联的知识图谱节点（会同时删除Neo4j节点、向量和关系）
        List<KnowledgeNode> relatedNodes = knowledgeNodeMapper.selectBySourceDocId(id);
        for (KnowledgeNode node : relatedNodes) {
            try {
                knowledgeNodeService.deleteNode(node.getId());
                log.info("删除文档关联的知识节点, documentId={}, nodeId={}, nodeName={}",
                        id, node.getId(), node.getName());
            } catch (Exception e) {
                log.warn("删除知识节点失败, nodeId={}, error={}", node.getId(), e.getMessage());
            }
        }
        log.info("删除文档关联的知识图谱完成, documentId={}, 删除节点数={}", id, relatedNodes.size());

        // 删除文档分块及其向量
        List<DocumentChunk> chunks = documentChunkMapper.selectByDocumentId(id);
        for (DocumentChunk chunk : chunks) {
            if (StrUtil.isNotBlank(chunk.getVectorId())) {
                vectorService.deleteVector(chunk.getVectorId());
            }
        }
        documentChunkMapper.deleteByDocumentId(id);
        log.info("删除文档分块完成, documentId={}, 分块数={}", id, chunks.size());

        // 删除文件
        FileUtil.del(document.getFilePath());

        // 逻辑删除记录
        return this.removeById(id);
    }

    /**
     * 重新解析文档
     */
    @Override
    public Document reparseDocument(Long id) {
        return parseDocument(id);
    }

    /**
     * 生成文档摘要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateSummary(Long id) {
        Document document = this.getById(id);
        if (document == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (StrUtil.isBlank(document.getContent())) {
            throw new BusinessException("文档内容为空，无法生成摘要");
        }

        // 调用Ollama生成摘要
        String summary = ollamaService.generateSummary(document.getContent());
        document.setSummary(summary);
        this.updateById(document);

        return summary;
    }

    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        String fileType = FileUtil.getSuffix(originalName);

        if (!fileConfig.isAllowedType(fileType)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT,
                    "不支持的文件类型: " + fileType);
        }
    }

    /**
     * 保存文件到本地
     */
    private String saveFile(MultipartFile file) {
        try {
            // 生成存储路径：uploads/2024/01/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));

            // 使用绝对路径
            Path basePath = Paths.get(fileConfig.getUploadPath()).toAbsolutePath().normalize();
            Path uploadDir = basePath.resolve(datePath);
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
            log.error("文件保存失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 实体转VO
     */
    private DocumentVO convertToVO(Document document) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);

        // 文件大小可读格式
        vo.setFileSizeReadable(FileUtil.readableFileSize(document.getFileSize()));

        // 内容预览（截取前500字符）
        if (StrUtil.isNotBlank(document.getContent())) {
            vo.setContentPreview(StrUtil.sub(document.getContent(), 0, 500));
        }

        return vo;
    }
}
