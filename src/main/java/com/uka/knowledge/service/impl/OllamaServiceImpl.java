package com.uka.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import com.uka.knowledge.model.dto.KnowledgeExtractDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Ollama大模型服务实现类
 * <p>
 * 实现与Ollama大模型的交互，包括：
 * - 文本对话
 * - 知识实体抽取
 * - 知识关系抽取
 * - 文本摘要生成
 * - 文本向量生成
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaServiceImpl implements OllamaService {

    private final OllamaChatModel chatModel;
    private final OllamaEmbeddingModel embeddingModel;

    /**
     * 文本对话
     */
    @Override
    public String chat(String prompt) {
        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            String response = chatClient.prompt()

                    .user(prompt)
                    .call()
                    .content();
            return response;
        } catch (Exception e) {
            log.error("Ollama对话失败", e);
            throw new BusinessException(ResultCode.OLLAMA_ERROR, "对话失败: " + e.getMessage());
        }
    }

    /**
     * 流式对话
     */
    @Override
    public Flux<String> chatStream(String prompt) {
        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            return chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("Ollama流式对话失败", e);
            return Flux.error(new BusinessException(ResultCode.OLLAMA_ERROR, "对话失败: " + e.getMessage()));
        }
    }

    /**
     * 从文本中抽取知识实体
     */
    @Override
    public List<KnowledgeNode> extractEntities(String text) {
        if (StrUtil.isBlank(text)) {
            return new ArrayList<>();
        }

        // 构建实体抽取提示词
        String prompt = buildEntityExtractionPrompt(text);

        try {
            String response = chat(prompt);
            return parseEntitiesFromResponse(response);
        } catch (Exception e) {
            log.error("实体抽取失败", e);
            throw new BusinessException(ResultCode.EXTRACT_FAILED, "实体抽取失败: " + e.getMessage());
        }
    }

    /**
     * 从文本中抽取知识关系
     */
    @Override
    public List<KnowledgeRelation> extractRelations(String text, List<KnowledgeNode> entities) {
        if (StrUtil.isBlank(text) || entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建关系抽取提示词
        String prompt = buildRelationExtractionPrompt(text, entities);

        try {
            String response = chat(prompt);
            return parseRelationsFromResponse(response, entities);
        } catch (Exception e) {
            log.error("关系抽取失败", e);
            throw new BusinessException(ResultCode.EXTRACT_FAILED, "关系抽取失败: " + e.getMessage());
        }
    }

    /**
     * 知识抽取（同时抽取实体和关系）
     */
    @Override
    public KnowledgeExtractResult extractKnowledge(KnowledgeExtractDTO dto) {
        String text = dto.getText();

        // 如果没有直接提供文本，则需要从文档或OCR记录获取
        // 这里简化处理，实际应该注入相关服务获取
        if (StrUtil.isBlank(text)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "待抽取文本不能为空");
        }

        // 抽取实体
        List<KnowledgeNode> entities = extractEntities(text);

        // 抽取关系
        List<KnowledgeRelation> relations = extractRelations(text, entities);

        return new KnowledgeExtractResult(entities, relations);
    }

    /**
     * 生成文本摘要
     */
    @Override
    public String generateSummary(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }

        // 如果文本太长，截取前面部分
        if (text.length() > 10000) {
            text = text.substring(0, 10000);
        }

        String prompt = """
                请对以下文本生成一个简洁的摘要，要求：
                1. 摘要长度在100-300字之间
                2. 保留文本的核心要点
                3. 语言简洁明了
                4. 直接输出摘要内容，不要添加额外说明

                文本内容：
                %s
                """.formatted(text);

        try {
            return chat(prompt);
        } catch (Exception e) {
            log.error("摘要生成失败", e);
            throw new BusinessException(ResultCode.OLLAMA_ERROR, "摘要生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成文本的向量表示
     */
    @Override
    public float[] generateEmbedding(String text) {
        if (StrUtil.isBlank(text)) {
            // 返回零向量
            return new float[1024];
        }

        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            log.error("向量生成失败", e);
            // 返回零向量，避免程序中断
            return new float[1024];
        }
    }

    /**
     * 构建实体抽取提示词
     */
    private String buildEntityExtractionPrompt(String text) {
        return """
                请从以下文本中抽取知识实体，并以JSON数组格式输出。

                要求：
                1. 识别文本中的人物、组织、地点、概念、事件等实体
                2. 每个实体包含：name(名称), nodeType(类型), description(描述)
                3. 实体类型包括：Person(人物), Organization(组织), Location(地点), Concept(概念), Event(事件), Technology(技术), Product(产品)
                4. 只输出JSON数组，不要其他内容

                输出格式示例：
                [
                  {"name": "张三", "nodeType": "Person", "description": "某公司CEO"},
                  {"name": "人工智能", "nodeType": "Concept", "description": "计算机科学分支"}
                ]

                文本内容：
                %s

                请抽取实体（JSON格式）：
                """.formatted(text);
    }

    /**
     * 构建关系抽取提示词
     */
    private String buildRelationExtractionPrompt(String text, List<KnowledgeNode> entities) {
        // 构建实体列表字符串
        StringBuilder entityList = new StringBuilder();
        for (int i = 0; i < entities.size(); i++) {
            entityList.append(i).append(". ").append(entities.get(i).getName())
                    .append("(").append(entities.get(i).getNodeType()).append(")\n");
        }

        return """
                根据以下文本和已识别的实体，抽取实体之间的关系。

                已识别的实体列表（序号. 名称(类型)）：
                %s

                要求：
                1. 识别实体之间的关系
                2. 每个关系包含：sourceIndex(起始实体序号), targetIndex(目标实体序号), name(关系名称), relationType(关系类型)
                3. 关系类型包括：BELONGS_TO(属于), PART_OF(组成部分), LOCATED_IN(位于), WORKS_FOR(工作于), CREATED_BY(创建), RELATED_TO(相关)
                4. 只输出JSON数组，不要其他内容

                输出格式示例：
                [
                  {"sourceIndex": 0, "targetIndex": 1, "name": "创立", "relationType": "CREATED_BY"},
                  {"sourceIndex": 2, "targetIndex": 3, "name": "属于", "relationType": "BELONGS_TO"}
                ]

                文本内容：
                %s

                请抽取关系（JSON格式）：
                """.formatted(entityList.toString(), text);
    }

    /**
     * 解析实体抽取响应
     */
    private List<KnowledgeNode> parseEntitiesFromResponse(String response) {
        List<KnowledgeNode> entities = new ArrayList<>();

        try {
            // 尝试提取JSON数组部分
            String jsonStr = extractJsonArray(response);
            if (StrUtil.isBlank(jsonStr)) {
                return entities;
            }

            JSONArray jsonArray = JSON.parseArray(jsonStr);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                KnowledgeNode node = new KnowledgeNode();
                node.setName(obj.getString("name"));
                node.setNodeType(obj.getString("nodeType"));
                node.setDescription(obj.getString("description"));
                node.setSourceType("extract");

                entities.add(node);
            }
        } catch (Exception e) {
            log.warn("解析实体响应失败: {}", e.getMessage());
        }

        return entities;
    }

    /**
     * 解析关系抽取响应
     */
    private List<KnowledgeRelation> parseRelationsFromResponse(String response, List<KnowledgeNode> entities) {
        List<KnowledgeRelation> relations = new ArrayList<>();

        try {
            // 尝试提取JSON数组部分
            String jsonStr = extractJsonArray(response);
            if (StrUtil.isBlank(jsonStr)) {
                return relations;
            }

            JSONArray jsonArray = JSON.parseArray(jsonStr);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                int sourceIndex = obj.getIntValue("sourceIndex");
                int targetIndex = obj.getIntValue("targetIndex");

                // 验证索引有效性
                if (sourceIndex < 0 || sourceIndex >= entities.size()
                        || targetIndex < 0 || targetIndex >= entities.size()) {
                    continue;
                }

                KnowledgeRelation relation = new KnowledgeRelation();
                relation.setName(obj.getString("name"));
                relation.setRelationType(obj.getString("relationType"));
                relation.setWeight(1.0);

                // 临时存储索引信息，后续需要转换为实际的节点ID
                // 这里使用负数标记为索引
                relation.setSourceNodeId((long) -sourceIndex - 1);
                relation.setTargetNodeId((long) -targetIndex - 1);

                relations.add(relation);
            }
        } catch (Exception e) {
            log.warn("解析关系响应失败: {}", e.getMessage());
        }

        return relations;
    }

    /**
     * 从响应文本中提取JSON数组
     */
    private String extractJsonArray(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }

        // 查找第一个 [ 和最后一个 ]
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return null;
    }
}
