package com.uka.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uka.knowledge.model.entity.OcrRecord;
import com.uka.knowledge.model.vo.OcrRecordVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR服务接口
 * <p>
 * 定义图片OCR识别相关的业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface OcrService extends IService<OcrRecord> {

    /**
     * 上传图片并进行OCR识别
     *
     * @param file 上传的图片文件
     * @return OCR记录实体
     */
    OcrRecord uploadAndRecognize(MultipartFile file);

    /**
     * 对指定图片进行OCR识别
     *
     * @param recordId OCR记录ID
     * @return OCR记录实体
     */
    OcrRecord recognizeImage(Long recordId);

    /**
     * 分页查询OCR记录
     *
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param keyword   搜索关键词（在OCR文本中搜索）
     * @param imageType 图片类型
     * @param status    处理状态
     * @return 分页结果
     */
    Page<OcrRecordVO> pageRecords(Integer pageNum, Integer pageSize,
                                   String keyword, String imageType, Integer status);

    /**
     * 获取OCR记录详情
     *
     * @param id 记录ID
     * @return OCR记录VO
     */
    OcrRecordVO getRecordDetail(Long id);

    /**
     * 删除OCR记录
     *
     * @param id 记录ID
     * @return 是否成功
     */
    boolean deleteRecord(Long id);

    /**
     * 重新识别图片
     *
     * @param id 记录ID
     * @return OCR记录
     */
    OcrRecord reRecognize(Long id);
}
