package com.uka.knowledge.controller;

import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.dto.KnowledgeExtractDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.service.OllamaService;
import com.uka.knowledge.service.VectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI服务控制器
 * <p>
 * 提供AI相关功能接口，包括知识抽取、语义搜索等
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "AI服务", description = "AI相关功能接口")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final OllamaService ollamaService;
    private final VectorService vectorService;

    /**
     * AI对话
     *
     * @param prompt 提示词
     * @return AI回复
     */
    @Operation(summary = "AI对话", description = "与AI进行对话")
    @PostMapping("/chat")
    public Result<String> chat(
            @Parameter(description = "提示词", required = true)
            @RequestBody String prompt) {
        String response = ollamaService.chat(prompt);
        return Result.success(response);
    }

    /**
     * 知识抽取
     *
     * @param dto 抽取请求参数
     * @return 抽取结果
     */
    @Operation(summary = "知识抽取", description = "从文本中抽取知识实体和关系")
    @PostMapping("/extract")
    public Result<Map<String, Object>> extract(@RequestBody KnowledgeExtractDTO dto) {
        OllamaService.KnowledgeExtractResult result = ollamaService.extractKnowledge(dto);

        Map<String, Object> data = new HashMap<>();
        data.put("entities", result.entities());
        data.put("relations", result.relations());

        return Result.success(data);
    }

    /**
     * 实体抽取
     *
     * @param text 待抽取的文本
     * @return 实体列表
     */
    @Operation(summary = "实体抽取", description = "从文本中抽取知识实体")
    @PostMapping("/extract/entities")
    public Result<List<KnowledgeNode>> extractEntities(
            @Parameter(description = "待抽取的文本", required = true)
            @RequestBody String text) {
        List<KnowledgeNode> entities = ollamaService.extractEntities(text);
        return Result.success(entities);
    }

    /**
     * 生成摘要
     *
     * @param text 原文本
     * @return 摘要
     */
    @Operation(summary = "生成摘要", description = "使用AI生成文本摘要")
    @PostMapping("/summary")
    public Result<String> generateSummary(
            @Parameter(description = "原文本", required = true)
            @RequestBody String text) {
        String summary = ollamaService.generateSummary(text);
        return Result.success(summary);
    }

    /**
     * 语义搜索
     *
     * @param query 查询文本
     * @param topK  返回数量
     * @param type  数据类型过滤
     * @return 搜索结果
     */
    @Operation(summary = "语义搜索", description = "基于向量相似度的语义搜索")
    @GetMapping("/search")
    public Result<List<VectorService.VectorSearchResult>> semanticSearch(
            @Parameter(description = "查询文本", required = true) @RequestParam String query,
            @Parameter(description = "返回数量") @RequestParam(defaultValue = "10") Integer topK,
            @Parameter(description = "数据类型") @RequestParam(required = false) String type) {

        // 生成查询向量
        float[] queryVector = ollamaService.generateEmbedding(query);

        // 执行向量搜索
        List<VectorService.VectorSearchResult> results = vectorService.search(queryVector, topK, type);

        return Result.success(results);
    }
}
