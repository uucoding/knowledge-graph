package com.uka.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uka.knowledge.model.entity.Document;
import com.uka.knowledge.model.vo.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档服务接口
 * <p>
 * 定义文档上传、解析、查询等业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface DocumentService extends IService<Document> {

    /**
     * 上传并解析文档
     *
     * @param file 上传的文件
     * @return 文档实体
     */
    Document uploadAndParse(MultipartFile file);

    /**
     * 解析指定文档
     *
     * @param documentId 文档ID
     * @return 解析后的文档实体
     */
    Document parseDocument(Long documentId);

    /**
     * 分页查询文档
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词
     * @param fileType 文件类型
     * @param status   处理状态
     * @return 分页结果
     */
    Page<DocumentVO> pageDocuments(Integer pageNum, Integer pageSize,
                                    String keyword, String fileType, Integer status);

    /**
     * 获取文档详情
     *
     * @param id 文档ID
     * @return 文档VO
     */
    DocumentVO getDocumentDetail(Long id);

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 是否成功
     */
    boolean deleteDocument(Long id);

    /**
     * 重新解析文档
     *
     * @param id 文档ID
     * @return 解析后的文档
     */
    Document reparseDocument(Long id);

    /**
     * 生成文档摘要
     *
     * @param id 文档ID
     * @return 摘要内容
     */
    String generateSummary(Long id);
}
