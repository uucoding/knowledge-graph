package com.uka.knowledge.service;

import com.uka.knowledge.model.dto.KnowledgeExtractDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Ollama大模型服务接口
 * <p>
 * 定义与Ollama大模型交互的业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface OllamaService {

    /**
     * 文本对话
     *
     * @param prompt 提示词
     * @return 模型回复
     */
    String chat(String prompt);

    /**
     * 流式对话
     *
     * @param prompt 提示词
     * @return 流式响应
     */
    Flux<String> chatStream(String prompt);

    /**
     * 从文本中抽取知识实体
     *
     * @param text 待抽取的文本
     * @return 抽取的实体列表
     */
    List<KnowledgeNode> extractEntities(String text);

    /**
     * 从文本中抽取知识关系
     *
     * @param text   待抽取的文本
     * @param entities 已识别的实体列表
     * @return 抽取的关系列表
     */
    List<KnowledgeRelation> extractRelations(String text, List<KnowledgeNode> entities);

    /**
     * 知识抽取（同时抽取实体和关系）
     *
     * @param dto 抽取请求DTO
     * @return 抽取结果（包含实体和关系）
     */
    KnowledgeExtractResult extractKnowledge(KnowledgeExtractDTO dto);

    /**
     * 生成文本摘要
     *
     * @param text 原文本
     * @return 摘要
     */
    String generateSummary(String text);

    /**
     * 生成文本的向量表示
     *
     * @param text 文本内容
     * @return 向量数组
     */
    float[] generateEmbedding(String text);

    /**
     * 知识抽取结果封装类
     */
    record KnowledgeExtractResult(
            List<KnowledgeNode> entities,
            List<KnowledgeRelation> relations
    ) {}
}
