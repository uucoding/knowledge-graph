package com.uka.knowledge.service;

import java.util.List;

/**
 * 向量服务接口
 * <p>
 * 定义与Milvus向量数据库交互的业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface VectorService {

    /**
     * 初始化向量集合
     * 如果集合不存在则创建
     */
    void initCollection();

    /**
     * 插入向量数据
     *
     * @param id     业务ID
     * @param vector 向量数据
     * @param type   数据类型（document/ocr/node）
     * @return 向量ID
     */
    String insertVector(Long id, float[] vector, String type);

    /**
     * 批量插入向量数据
     *
     * @param ids     业务ID列表
     * @param vectors 向量数据列表
     * @param type    数据类型
     * @return 向量ID列表
     */
    List<String> insertVectors(List<Long> ids, List<float[]> vectors, String type);

    /**
     * 删除向量
     *
     * @param vectorId 向量ID
     * @return 是否成功
     */
    boolean deleteVector(String vectorId);

    /**
     * 向量相似性搜索
     *
     * @param queryVector 查询向量
     * @param topK        返回数量
     * @param type        数据类型过滤（可选）
     * @return 相似结果列表（包含ID和相似度分数）
     */
    List<VectorSearchResult> search(float[] queryVector, int topK, String type);

    /**
     * 向量搜索结果封装类
     */
    record VectorSearchResult(
            Long id,
            String type,
            float score
    ) {}
}
