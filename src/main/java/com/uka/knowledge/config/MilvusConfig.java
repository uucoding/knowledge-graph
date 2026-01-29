package com.uka.knowledge.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
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
    private String uri = "http://localhost:19530";

    /**
     * 集合名称
     */
    private String collectionName = "knowledge_vectors";

    /**
     * 向量维度
     */
    private Integer dimension = 1024;

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
     * @return MilvusClientV2实例
     */
    @Bean
    public MilvusClientV2 milvusClient() {
        // 1、创建连接参数
        ConnectConfig connectConfig = ConnectConfig
                .builder()
                .uri(uri)
                .build();
        // 2、创建客户端
        return new MilvusClientV2(connectConfig);
    }

    /**
     * 获取索引类型枚举
     *
     * @return IndexType枚举值
     */
    public IndexParam.IndexType getIndexTypeEnum() {
        return IndexParam.IndexType.valueOf(indexType);
    }

    /**
     * 获取度量类型枚举
     *
     * @return MetricType枚举值
     */
    public IndexParam.MetricType getMetricTypeEnum() {
        return IndexParam.MetricType.valueOf(metricType);
    }
}
