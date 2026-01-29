package com.uka.knowledge.service.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.uka.knowledge.config.MilvusConfig;
import com.uka.knowledge.service.VectorService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.DeleteResp;
import io.milvus.v2.service.vector.response.InsertResp;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 向量服务实现类
 * <p>
 * 实现与Milvus向量数据库的交互，包括：
 * - 集合初始化
 * - 向量插入
 * - 向量删除
 * - 相似性搜索
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private final MilvusClientV2 milvusClient;
    private final MilvusConfig milvusConfig;

    /**
     * 字段名称常量
     */
    private static final String FIELD_ID = "id";
    private static final String FIELD_BUSINESS_ID = "business_id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_VECTOR = "vector";

    /**
     * 服务启动时初始化集合
     */
    @PostConstruct
    public void init() {
        try {
            initCollection();
            log.info("Milvus集合初始化成功");
        } catch (Exception e) {
            log.warn("Milvus集合初始化失败，可能Milvus服务未启动: {}", e.getMessage());
        }
    }

    /**
     * 初始化向量集合
     */
    @Override
    public void initCollection() {
        String collectionName = milvusConfig.getCollectionName();

        // 1、检查集合是否存在
        Boolean hasCollection = milvusClient.hasCollection(
                HasCollectionReq.builder().collectionName(collectionName).build()
        );

        if (hasCollection) {
            log.info("集合 {} 已存在", collectionName);
            // 加载集合到内存
            loadCollection();
            return;
        }
        // 构建collection
        CreateCollectionReq collection = CreateCollectionReq.builder()
                .collectionName(collectionName)
                .description("知识图谱向量集合")
                .indexParams(createIndex())
                .collectionSchema(createSchema())
                .build();

        milvusClient.createCollection(collection);
        // 加载集合到内存
        loadCollection();

        log.info("集合 {} 创建成功", collectionName);
    }

    /**
     * 创建集合的schema
     * @return
     */
    private CreateCollectionReq.CollectionSchema createSchema() {
        // 2、构建schema
        CreateCollectionReq.CollectionSchema schema = milvusClient.createSchema();
        // 3 向schema添加字段
        // 主键字段
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_ID)
                .dataType(DataType.Int64)
                .isPrimaryKey(true)
                .autoID(true)
                .build());
        // 业务ID字段
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_BUSINESS_ID)
                .dataType(DataType.Int64)
                .build());

        // 类型字段
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_TYPE)
                .dataType(DataType.VarChar)
                .maxLength(50)
                .build());
        // 向量字段
        schema.addField(AddFieldReq.builder()
                .fieldName(FIELD_VECTOR)
                .dataType(DataType.FloatVector)
                .dimension(milvusConfig.getDimension())
                .build());
        return schema;
    }

    /**
     * 创建向量索引
     */
    private List<IndexParam> createIndex() {
        IndexParam indexParamForVectorField = IndexParam.builder()
                .fieldName(FIELD_VECTOR)
                .indexType(milvusConfig.getIndexTypeEnum())
                .metricType(IndexParam.MetricType.COSINE)
                .extraParams(Map.of("nlist", 1024))
                .build();
        return Arrays.asList(indexParamForVectorField);
    }

    /**
     * 加载集合到内存
     */
    private void loadCollection() {
        LoadCollectionReq loadCollectionReq = LoadCollectionReq.builder()
                .collectionName(milvusConfig.getCollectionName())
                .build();
        milvusClient.loadCollection(loadCollectionReq);
    }

    /**
     * 插入向量数据
     */
    @Override
    public String insertVector(VectorInsertData vectorInsertData) {
        List<String> vectorIds = insertVectors(Arrays.asList(vectorInsertData));
        return vectorIds.isEmpty() ? null : vectorIds.get(0);
    }

    /**
     * 批量插入向量数据
     */
    public List<String> insertVectors(List<VectorInsertData> vectorInsertDataList) {
        // 构建向量数据
        List<JsonObject> dataList = vectorInsertDataList.stream().map(vectorInsertData -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(FIELD_BUSINESS_ID, vectorInsertData.businessId());
            jsonObject.addProperty(FIELD_TYPE, vectorInsertData.type());
            // 向量转换为JSON数组
            JsonArray vectorArray = new JsonArray();
            for (float v : vectorInsertData.vector()) {
                vectorArray.add(v);
            }
            jsonObject.add(FIELD_VECTOR, vectorArray);
            return jsonObject;
        }).toList();
        // 执行插入
        InsertReq insertReq = InsertReq.builder()
                .collectionName(milvusConfig.getCollectionName())
                .data(dataList)
                .build();
        InsertResp insertResp = milvusClient.insert(insertReq);

        // 返回生成的ID
        return insertResp.getPrimaryKeys().stream().map(String::valueOf).toList();
    }

    /**
     * 删除向量
     */
    @Override
    public boolean deleteVector(String vectorId) {
        try {
            DeleteReq deleteReq = DeleteReq.builder()
                    .collectionName(milvusConfig.getCollectionName())
                    .ids(Collections.singletonList(vectorId))
                    .build();
            DeleteResp deleteResp = milvusClient.delete(deleteReq);
            return deleteResp.getDeleteCnt() > 0;
        } catch (Exception e) {
            log.error("删除向量失败", e);
            return false;
        }
    }

    /**
     * 向量相似性搜索
     */
    @Override
    public List<VectorSearchResult> search(float[] queryVector, int topK, String type) {
        // 构建搜索参数
        SearchReq searchReq = SearchReq.builder()
                .collectionName(milvusConfig.getCollectionName())
                .metricType(milvusConfig.getMetricTypeEnum())
                .outputFields(Arrays.asList(FIELD_BUSINESS_ID, FIELD_TYPE))
                .data(Arrays.asList(new FloatVec(queryVector))) // 搜索向量
                .annsField(FIELD_VECTOR)
                .searchParams(Map.of("nprobe", 10)) // 要搜索的群集数量
                .filter(FIELD_TYPE + " == \"" + type + "\"") // 类型过滤
                .limit(topK)
                .build();

        // 执行搜索
        SearchResp searchResp = milvusClient.search(searchReq);
        List<SearchResp.SearchResult> searchResults = searchResp.getSearchResults().stream().flatMap(List::stream).toList();

        // 解析搜索结果
        List<VectorSearchResult> results = new ArrayList<>();
        for (SearchResp.SearchResult searchResult : searchResults) {
            // 获取字段值
            Map<String, Object> entity = searchResult.getEntity();
            Object businessIdObj = entity.getOrDefault(FIELD_BUSINESS_ID, 0);
            Object typeObj =  entity.getOrDefault(FIELD_TYPE, 0);
            results.add(new VectorSearchResult(Long.parseLong(businessIdObj.toString()), typeObj.toString(), searchResult.getScore()));
        }

        return results;
    }
}
