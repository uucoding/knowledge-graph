package com.uka.knowledge.service.impl;

import com.uka.knowledge.config.MilvusConfig;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import com.uka.knowledge.service.VectorService;
import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.*;
import io.milvus.param.*;
import io.milvus.param.collection.*;
import io.milvus.param.dml.*;
import io.milvus.param.index.*;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
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

    private final MilvusServiceClient milvusClient;
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

        // 检查集合是否存在
        R<Boolean> hasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );

        if (hasCollection.getData()) {
            log.info("集合 {} 已存在", collectionName);
            // 加载集合到内存
            loadCollection();
            return;
        }

        // 创建集合字段
        List<FieldType> fieldTypes = new ArrayList<>();

        // 主键字段
        fieldTypes.add(FieldType.newBuilder()
                .withName(FIELD_ID)
                .withDataType(DataType.Int64)
                .withPrimaryKey(true)
                .withAutoID(true)
                .build());

        // 业务ID字段
        fieldTypes.add(FieldType.newBuilder()
                .withName(FIELD_BUSINESS_ID)
                .withDataType(DataType.Int64)
                .build());

        // 类型字段
        fieldTypes.add(FieldType.newBuilder()
                .withName(FIELD_TYPE)
                .withDataType(DataType.VarChar)
                .withMaxLength(50)
                .build());

        // 向量字段
        fieldTypes.add(FieldType.newBuilder()
                .withName(FIELD_VECTOR)
                .withDataType(DataType.FloatVector)
                .withDimension(milvusConfig.getDimension())
                .build());

        // 创建集合
        CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                .withCollectionName(collectionName)
                .withDescription("知识图谱向量集合")
                .withFieldTypes(fieldTypes)
                .build();

        R<RpcStatus> createResult = milvusClient.createCollection(createParam);
        if (createResult.getStatus() != R.Status.Success.getCode()) {
            throw new BusinessException(ResultCode.MILVUS_ERROR, "创建集合失败: " + createResult.getMessage());
        }

        // 创建索引
        createIndex();

        // 加载集合到内存
        loadCollection();

        log.info("集合 {} 创建成功", collectionName);
    }

    /**
     * 创建向量索引
     */
    private void createIndex() {
        CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFieldName(FIELD_VECTOR)
                .withIndexType(milvusConfig.getIndexTypeEnum())
                .withMetricType(milvusConfig.getMetricTypeEnum())
                .withExtraParam("{\"nlist\": 1024}")
                .build();

        R<RpcStatus> createIndexResult = milvusClient.createIndex(indexParam);
        if (createIndexResult.getStatus() != R.Status.Success.getCode()) {
            log.warn("创建索引失败: {}", createIndexResult.getMessage());
        }
    }

    /**
     * 加载集合到内存
     */
    private void loadCollection() {
        LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .build();

        R<RpcStatus> loadResult = milvusClient.loadCollection(loadParam);
        if (loadResult.getStatus() != R.Status.Success.getCode()) {
            log.warn("加载集合失败: {}", loadResult.getMessage());
        }
    }

    /**
     * 插入向量数据
     */
    @Override
    public String insertVector(Long id, float[] vector, String type) {
        List<Long> ids = Collections.singletonList(id);
        List<float[]> vectors = Collections.singletonList(vector);
        List<String> types = Collections.singletonList(type);

        List<String> vectorIds = insertVectorsBatch(ids, vectors, types);
        return vectorIds.isEmpty() ? null : vectorIds.get(0);
    }

    /**
     * 批量插入向量数据
     */
    @Override
    public List<String> insertVectors(List<Long> ids, List<float[]> vectors, String type) {
        List<String> types = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            types.add(type);
        }
        return insertVectorsBatch(ids, vectors, types);
    }

    /**
     * 批量插入向量数据
     */
    private List<String> insertVectorsBatch(List<Long> businessIds, List<float[]> vectors, List<String> types) {
        // 准备插入数据
        List<InsertParam.Field> fields = new ArrayList<>();

        // 业务ID
        fields.add(new InsertParam.Field(FIELD_BUSINESS_ID, businessIds));

        // 类型
        fields.add(new InsertParam.Field(FIELD_TYPE, types));

        // 向量
        List<List<Float>> vectorList = new ArrayList<>();
        for (float[] vector : vectors) {
            List<Float> v = new ArrayList<>();
            for (float f : vector) {
                v.add(f);
            }
            vectorList.add(v);
        }
        fields.add(new InsertParam.Field(FIELD_VECTOR, vectorList));

        // 执行插入
        InsertParam insertParam = InsertParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withFields(fields)
                .build();

        R<MutationResult> insertResult = milvusClient.insert(insertParam);
        if (insertResult.getStatus() != R.Status.Success.getCode()) {
            log.error("插入向量失败: {}", insertResult.getMessage());
            throw new BusinessException(ResultCode.MILVUS_ERROR, "插入向量失败");
        }

        // 返回生成的ID
        List<String> vectorIds = new ArrayList<>();
        MutationResult data = insertResult.getData();
        for (Long id : data.getIDs().getIntId().getDataList()) {
            vectorIds.add(String.valueOf(id));
        }

        return vectorIds;
    }

    /**
     * 删除向量
     */
    @Override
    public boolean deleteVector(String vectorId) {
        try {
            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(milvusConfig.getCollectionName())
                    .withExpr(FIELD_ID + " == " + vectorId)
                    .build();

            R<MutationResult> deleteResult = milvusClient.delete(deleteParam);
            return deleteResult.getStatus() == R.Status.Success.getCode();
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
        List<VectorSearchResult> results = new ArrayList<>();

        // 准备查询向量
        List<List<Float>> searchVectors = new ArrayList<>();
        List<Float> v = new ArrayList<>();
        for (float f : queryVector) {
            v.add(f);
        }
        searchVectors.add(v);

        // 构建搜索参数
        SearchParam.Builder searchBuilder = SearchParam.newBuilder()
                .withCollectionName(milvusConfig.getCollectionName())
                .withMetricType(milvusConfig.getMetricTypeEnum())
                .withOutFields(Arrays.asList(FIELD_BUSINESS_ID, FIELD_TYPE))
                .withTopK(topK)
                .withVectors(searchVectors)
                .withVectorFieldName(FIELD_VECTOR)
                .withParams("{\"nprobe\": 10}");

        // 类型过滤
        if (type != null && !type.isEmpty()) {
            searchBuilder.withExpr(FIELD_TYPE + " == \"" + type + "\"");
        }

        // 执行搜索
        R<SearchResults> searchResult = milvusClient.search(searchBuilder.build());
        if (searchResult.getStatus() != R.Status.Success.getCode()) {
            log.error("向量搜索失败: {}", searchResult.getMessage());
            return results;
        }

        // 解析搜索结果
        SearchResultsWrapper wrapper = new SearchResultsWrapper(searchResult.getData().getResults());
        List<SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);

        for (SearchResultsWrapper.IDScore idScore : idScores) {
            // 获取字段值
            Object businessIdObj = wrapper.getFieldData(FIELD_BUSINESS_ID, 0);
            Object typeObj = wrapper.getFieldData(FIELD_TYPE, 0);

            Long businessId = null;
            String resultType = null;

            if (businessIdObj instanceof List) {
                List<?> list = (List<?>) businessIdObj;
                int idx = idScores.indexOf(idScore);
                if (idx < list.size()) {
                    businessId = ((Number) list.get(idx)).longValue();
                }
            }

            if (typeObj instanceof List) {
                List<?> list = (List<?>) typeObj;
                int idx = idScores.indexOf(idScore);
                if (idx < list.size()) {
                    resultType = String.valueOf(list.get(idx));
                }
            }

            results.add(new VectorSearchResult(businessId, resultType, idScore.getScore()));
        }

        return results;
    }
}
