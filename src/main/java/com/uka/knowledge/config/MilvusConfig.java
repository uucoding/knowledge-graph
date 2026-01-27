package com.uka.knowledge.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus向量数据库配置类
 * <p>
 * 配置Milvus客户端连接参数，用于存储和检索文档的向量表示
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
public class MilvusConfig {

    /**
     * Milvus服务器地址
     */
    private String host = "localhost";

    /**
     * Milvus服务器端口
     */
    private Integer port = 19530;

    /**
     * 集合名称
     */
    private String collectionName = "knowledge_vectors";

    /**
     * 向量维度
     */
    private Integer dimension = 768;

    /**
     * 索引类型
     */
    private String indexType = "IVF_FLAT";

    /**
     * 度量类型（相似度计算方式）
     */
    private String metricType = "COSINE";

    /**
     * 创建Milvus客户端Bean
     *
     * @return MilvusServiceClient实例
     */
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        // 构建连接参数
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build();

        // 创建并返回Milvus客户端
        return new MilvusServiceClient(connectParam);
    }

    /**
     * 获取索引类型枚举
     *
     * @return IndexType枚举值
     */
    public IndexType getIndexTypeEnum() {
        return IndexType.valueOf(indexType);
    }

    /**
     * 获取度量类型枚举
     *
     * @return MetricType枚举值
     */
    public MetricType getMetricTypeEnum() {
        return MetricType.valueOf(metricType);
    }
}
