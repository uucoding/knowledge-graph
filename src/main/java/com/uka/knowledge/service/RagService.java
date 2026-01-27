package com.uka.knowledge.service;

import com.uka.knowledge.model.vo.RagDocument;
import com.uka.knowledge.model.vo.RagNode;

import java.util.List;

/**
 * RAG检索增强服务接口
 * <p>
 * 定义RAG相关的业务方法，用于从向量数据库检索相关内容
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface RagService {

    /**
     * RAG检索结果封装类
     */
    record RagResult(
            List<RagDocument> documents,
            List<RagNode> nodes,
            String contextPrompt
    ) {}

    /**
     * 执行RAG检索
     * 根据用户输入检索相关文档和知识图谱节点
     *
     * @param query 用户查询
     * @param topK 返回的最大数量
     * @return RAG检索结果
     */
    RagResult search(String query, int topK);

    /**
     * 检索相关文档
     *
     * @param query 用户查询
     * @param topK 返回数量
     * @return 相关文档列表
     */
    List<RagDocument> searchDocuments(String query, int topK);

    /**
     * 检索相关知识节点
     *
     * @param query 用户查询
     * @param topK 返回数量
     * @return 相关节点列表
     */
    List<RagNode> searchNodes(String query, int topK);

    /**
     * 构建RAG上下文提示词
     *
     * @param documents 相关文档
     * @param userQuery 用户问题
     * @return 完整的提示词
     */
    String buildContextPrompt(List<RagDocument> documents, String userQuery);
}
