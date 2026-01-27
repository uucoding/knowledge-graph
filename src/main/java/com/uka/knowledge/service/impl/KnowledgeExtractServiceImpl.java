package com.uka.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.uka.knowledge.model.dto.KnowledgeNodeDTO;
import com.uka.knowledge.model.dto.KnowledgeRelationDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.service.KnowledgeExtractService;
import com.uka.knowledge.service.KnowledgeNodeService;
import com.uka.knowledge.service.KnowledgeRelationService;
import com.uka.knowledge.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识抽取服务实现类
 * <p>
 * 使用大模型从文本中自动抽取实体和关系，并保存到知识图谱
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeExtractServiceImpl implements KnowledgeExtractService {

    private final OllamaService ollamaService;
    private final KnowledgeNodeService nodeService;
    private final KnowledgeRelationService relationService;

    /**
     * 文本分段大小（避免超过模型上下文限制）
     */
    private static final int CHUNK_SIZE = 2000;

    /**
     * 从文档中抽取知识
     */
    @Override
    public ExtractResult extractFromDocument(Long documentId, String content) {
        return extractAndBuildGraph(content, documentId, "document");
    }

    /**
     * 从OCR结果中抽取知识
     */
    @Override
    public ExtractResult extractFromOcr(Long ocrRecordId, String ocrText) {
        return extractAndBuildGraph(ocrText, ocrRecordId, "ocr");
    }

    /**
     * 从文本中抽取知识并构建图谱
     */
    @Override
    public ExtractResult extractAndBuildGraph(String text, Long sourceId, String sourceType) {
        if (StrUtil.isBlank(text)) {
            return ExtractResult.empty("文本内容为空");
        }

        log.info("开始从文本抽取知识, sourceType={}, sourceId={}, textLength={}",
                sourceType, sourceId, text.length());

        try {
            // 1. 将长文本分段处理
            List<String> chunks = splitText(text, CHUNK_SIZE);
            log.info("文本分段完成, 共{}段", chunks.size());

            // 2. 存储所有抽取的实体和关系
            List<KnowledgeNode> allNodes = new ArrayList<>();
            List<KnowledgeRelation> allRelations = new ArrayList<>();

            // 3. 对每段文本进行抽取
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                log.info("处理第{}/{}段文本", i + 1, chunks.size());

                try {
                    // 调用大模型抽取实体和关系
                    String extractResult = callLlmForExtraction(chunk);

                    // 解析抽取结果
                    ParsedResult parsed = parseExtractionResult(extractResult);

                    if (parsed != null) {
                        allNodes.addAll(parsed.nodes);
                        allRelations.addAll(parsed.relations);
                    }
                } catch (Exception e) {
                    log.warn("第{}段文本抽取失败: {}", i + 1, e.getMessage());
                }
            }

            // 4. 去重合并实体
            Map<String, KnowledgeNode> uniqueNodes = mergeNodes(allNodes);
            log.info("实体去重完成, 原始{}个, 去重后{}个", allNodes.size(), uniqueNodes.size());

            // 5. 保存实体到数据库
            Map<String, Long> nodeNameToIdMap = new HashMap<>();
            List<KnowledgeNode> savedNodes = new ArrayList<>();

            for (KnowledgeNode node : uniqueNodes.values()) {
                try {
                    // 检查是否已存在同名节点
                    List<KnowledgeNode> existing = nodeService.list(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeNode>()
                                    .eq(KnowledgeNode::getName, node.getName())
                                    .eq(KnowledgeNode::getNodeType, node.getNodeType())
                    );

                    if (!existing.isEmpty()) {
                        // 使用已存在的节点
                        nodeNameToIdMap.put(node.getName(), existing.get(0).getId());
                        log.debug("节点已存在, 复用: {}", node.getName());
                    } else {
                        // 创建新节点
                        KnowledgeNodeDTO dto = new KnowledgeNodeDTO();
                        dto.setName(node.getName());
                        dto.setNodeType(node.getNodeType());
                        dto.setDescription(node.getDescription());
                        dto.setSourceDocId(sourceId);
                        dto.setSourceType(sourceType);

                        KnowledgeNode savedNode = nodeService.createNode(dto);
                        nodeNameToIdMap.put(node.getName(), savedNode.getId());
                        savedNodes.add(savedNode);
                        log.debug("创建新节点: {} ({})", node.getName(), node.getNodeType());
                    }
                } catch (Exception e) {
                    log.warn("保存节点失败: {}, error: {}", node.getName(), e.getMessage());
                }
            }

            // 6. 保存关系到数据库
            List<KnowledgeRelation> savedRelations = new ArrayList<>();

            for (KnowledgeRelation relation : allRelations) {
                try {
                    // 获取源节点和目标节点的ID
                    Long sourceNodeId = nodeNameToIdMap.get(relation.getName());
                    Long targetNodeId = null;

                    // 关系中临时存储的是节点名称，需要转换
                    String sourceName = (String) relation.getProperties();
                    String targetName = relation.getNeo4jRelId();

                    if (sourceName != null) {
                        sourceNodeId = nodeNameToIdMap.get(sourceName);
                    }
                    if (targetName != null) {
                        targetNodeId = nodeNameToIdMap.get(targetName);
                    }

                    if (sourceNodeId == null || targetNodeId == null) {
                        log.debug("关系节点不存在, 跳过: {} -> {}", sourceName, targetName);
                        continue;
                    }

                    // 检查关系是否已存在
                    if (relationService.existsRelation(sourceNodeId, targetNodeId, relation.getRelationType())) {
                        log.debug("关系已存在, 跳过: {} -[{}]-> {}", sourceName, relation.getName(), targetName);
                        continue;
                    }

                    // 创建关系
                    KnowledgeRelationDTO dto = new KnowledgeRelationDTO();
                    dto.setName(relation.getName());
                    dto.setRelationType(relation.getRelationType());
                    dto.setSourceNodeId(sourceNodeId);
                    dto.setTargetNodeId(targetNodeId);
                    dto.setWeight(1.0);

                    KnowledgeRelation savedRelation = relationService.createRelation(dto);
                    savedRelations.add(savedRelation);
                    log.debug("创建新关系: {} -[{}]-> {}", sourceName, relation.getName(), targetName);

                } catch (Exception e) {
                    log.warn("保存关系失败: {}, error: {}", relation.getName(), e.getMessage());
                }
            }

            log.info("知识抽取完成, 保存节点{}个, 关系{}个", savedNodes.size(), savedRelations.size());

            return ExtractResult.of(savedNodes, savedRelations);

        } catch (Exception e) {
            log.error("知识抽取失败", e);
            return ExtractResult.empty("抽取失败: " + e.getMessage());
        }
    }

    /**
     * 调用大模型进行知识抽取
     */
    private String callLlmForExtraction(String text) {
        String prompt = """
                你是一个知识图谱构建专家。请从以下文本中抽取知识实体和关系。

                要求：
                1. 识别文本中的重要实体（人物、组织、地点、概念、技术、产品、事件等）
                2. 识别实体之间的关系
                3. 严格按照JSON格式输出，不要输出其他内容

                输出格式：
                {
                  "entities": [
                    {"name": "实体名称", "type": "实体类型", "description": "简短描述"}
                  ],
                  "relations": [
                    {"source": "源实体名称", "target": "目标实体名称", "relation": "关系名称", "type": "关系类型"}
                  ]
                }

                实体类型包括：Person(人物), Organization(组织), Location(地点), Concept(概念), Technology(技术), Product(产品), Event(事件), Time(时间), Other(其他)

                关系类型包括：BELONGS_TO(属于), PART_OF(包含/组成), LOCATED_IN(位于), WORKS_FOR(工作于), CREATED_BY(创建/发明), RELATED_TO(相关), HAPPENED_AT(发生于), USED_BY(使用), INFLUENCED_BY(影响)

                文本内容：
                %s

                请输出JSON（只输出JSON，不要其他文字）：
                """.formatted(text);

        return ollamaService.chat(prompt);
    }

    /**
     * 解析大模型返回的抽取结果
     */
    private ParsedResult parseExtractionResult(String result) {
        if (StrUtil.isBlank(result)) {
            return null;
        }

        try {
            // 提取JSON部分
            String jsonStr = extractJson(result);
            if (jsonStr == null) {
                log.warn("未找到有效的JSON: {}", result.substring(0, Math.min(100, result.length())));
                return null;
            }

            JSONObject json = JSON.parseObject(jsonStr);

            List<KnowledgeNode> nodes = new ArrayList<>();
            List<KnowledgeRelation> relations = new ArrayList<>();

            // 解析实体
            JSONArray entities = json.getJSONArray("entities");
            if (entities != null) {
                for (int i = 0; i < entities.size(); i++) {
                    JSONObject entity = entities.getJSONObject(i);
                    KnowledgeNode node = new KnowledgeNode();
                    node.setName(entity.getString("name"));
                    node.setNodeType(mapEntityType(entity.getString("type")));
                    node.setDescription(entity.getString("description"));
                    nodes.add(node);
                }
            }

            // 解析关系
            JSONArray rels = json.getJSONArray("relations");
            if (rels != null) {
                for (int i = 0; i < rels.size(); i++) {
                    JSONObject rel = rels.getJSONObject(i);
                    KnowledgeRelation relation = new KnowledgeRelation();
                    relation.setName(rel.getString("relation"));
                    relation.setRelationType(mapRelationType(rel.getString("type")));
                    // 临时存储源和目标节点名称
                    relation.setProperties(rel.getString("source"));
                    relation.setNeo4jRelId(rel.getString("target"));
                    relations.add(relation);
                }
            }

            return new ParsedResult(nodes, relations);

        } catch (Exception e) {
            log.warn("解析抽取结果失败: {}, 原始内容: {}", e.getMessage(),
                    result.substring(0, Math.min(500, result.length())));
            return null;
        }
    }

    /**
     * 从文本中提取JSON
     */
    private String extractJson(String text) {
        // 尝试找到 { 和 } 的配对
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {
            String json = text.substring(start, end + 1);
            // 清理可能存在的格式问题
            return sanitizeJson(json);
        }
        return null;
    }

    /**
     * 清理JSON中的格式问题
     * 处理LLM返回的非法JSON格式，如JavaScript风格的字符串拼接
     */
    private String sanitizeJson(String json) {
        if (json == null) {
            return null;
        }

        // 处理 "xxx" + "yyy" 这种JavaScript风格的字符串拼接
        // 将其合并为 "xxxyyy"
        String sanitized = json;

        // 正则匹配: "字符串1" + "字符串2" 模式，支持多次连续拼接
        // 需要多次处理，直到没有匹配项
        String pattern = "\"\\s*\\+\\s*\"";
        while (sanitized.matches("(?s).*" + pattern + ".*")) {
            sanitized = sanitized.replaceAll(pattern, "");
        }

        // 处理可能的换行符在字符串值中（非法的JSON）
        // 将字符串值内的换行替换为空格
        sanitized = fixNewlinesInStrings(sanitized);

        return sanitized;
    }

    /**
     * 修复字符串值内的非法换行符
     */
    private String fixNewlinesInStrings(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                result.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }

            // 如果在字符串内遇到换行符，替换为空格
            if (inString && (c == '\n' || c == '\r')) {
                result.append(' ');
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * 映射实体类型
     */
    private String mapEntityType(String type) {
        if (StrUtil.isBlank(type)) {
            return "Concept";
        }

        return switch (type.toLowerCase()) {
            case "person", "人物" -> "Person";
            case "organization", "组织", "机构", "公司" -> "Organization";
            case "location", "地点", "地区", "城市" -> "Location";
            case "concept", "概念" -> "Concept";
            case "technology", "技术" -> "Technology";
            case "product", "产品" -> "Product";
            case "event", "事件" -> "Event";
            case "time", "时间" -> "Time";
            default -> "Concept";
        };
    }

    /**
     * 映射关系类型
     */
    private String mapRelationType(String type) {
        if (StrUtil.isBlank(type)) {
            return "RELATED_TO";
        }

        return switch (type.toUpperCase()) {
            case "BELONGS_TO", "属于" -> "BELONGS_TO";
            case "PART_OF", "包含", "组成" -> "PART_OF";
            case "LOCATED_IN", "位于" -> "LOCATED_IN";
            case "WORKS_FOR", "工作于", "任职" -> "WORKS_FOR";
            case "CREATED_BY", "创建", "发明", "创立" -> "CREATED_BY";
            case "HAPPENED_AT", "发生于" -> "HAPPENED_AT";
            case "USED_BY", "使用" -> "USED_BY";
            case "INFLUENCED_BY", "影响" -> "INFLUENCED_BY";
            default -> "RELATED_TO";
        };
    }

    /**
     * 合并去重实体
     */
    private Map<String, KnowledgeNode> mergeNodes(List<KnowledgeNode> nodes) {
        Map<String, KnowledgeNode> uniqueNodes = new HashMap<>();

        for (KnowledgeNode node : nodes) {
            String key = node.getName();
            if (!uniqueNodes.containsKey(key)) {
                uniqueNodes.put(key, node);
            } else {
                // 如果已存在，合并描述信息
                KnowledgeNode existing = uniqueNodes.get(key);
                if (StrUtil.isBlank(existing.getDescription()) && StrUtil.isNotBlank(node.getDescription())) {
                    existing.setDescription(node.getDescription());
                }
            }
        }

        return uniqueNodes;
    }

    /**
     * 将长文本分段
     */
    private List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= chunkSize) {
            chunks.add(text);
            return chunks;
        }

        // 按段落分割优先
        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }

                // 如果单个段落超过限制，强制切分
                if (paragraph.length() > chunkSize) {
                    for (int i = 0; i < paragraph.length(); i += chunkSize) {
                        chunks.add(paragraph.substring(i, Math.min(i + chunkSize, paragraph.length())));
                    }
                } else {
                    currentChunk.append(paragraph);
                }
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    /**
     * 解析结果内部类
     */
    private record ParsedResult(List<KnowledgeNode> nodes, List<KnowledgeRelation> relations) {
    }
}
