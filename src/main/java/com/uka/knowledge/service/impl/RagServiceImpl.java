package com.uka.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.uka.knowledge.mapper.DocumentChunkMapper;
import com.uka.knowledge.mapper.KnowledgeRelationMapper;
import com.uka.knowledge.model.entity.Document;
import com.uka.knowledge.model.entity.DocumentChunk;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.model.vo.RagDocument;
import com.uka.knowledge.model.vo.RagNode;
import com.uka.knowledge.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG检索增强服务实现类
 * <p>
 * 实现RAG检索功能，从向量数据库检索相关文档和知识节点
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final OllamaService ollamaService;
    private final VectorService vectorService;
    private final DocumentService documentService;
    private final KnowledgeNodeService nodeService;
    private final KnowledgeRelationMapper relationMapper;
    private final DocumentChunkMapper documentChunkMapper;

    private static final String PROMPT_TEMPLATE = """
        你是一个精准的文档问答助手。你的任务是根据提供的参考内容回答用户问题。

        【重要规则】
        1. 只能使用下方"参考内容"中的信息来回答问题
        2. 如果参考内容中没有相关信息，必须明确回答："根据已上传的文档，未找到与此问题相关的信息。"
        3. 回答时在末尾标注信息来源，格式：[来源：文档名, 第X页]
        4. 禁止编造、推测或使用参考内容之外的知识
        5. 保持回答简洁准确，直接回答问题

        【参考内容】
        %s

        请根据以上参考内容回答用户的问题。""";

    /**
     * 执行RAG检索
     */
    @Override
    public RagResult search(String query, int topK) {
        // 检索相关文档
        List<RagDocument> documents = searchDocuments(query, topK);

        // 检索相关节点
        List<RagNode> nodes = searchNodes(query, topK);

        // 构建上下文提示词
        String contextPrompt = buildContextPrompt(documents, query);
        return new RagResult(documents, nodes, contextPrompt);
    }

    /**
     * 检索相关文档（基于分块检索，返回页码信息）
     */
    @Override
    public List<RagDocument> searchDocuments(String query, int topK) {
        List<RagDocument> results = new ArrayList<>();

        try {
            // 生成查询向量
            float[] queryVector = ollamaService.generateEmbedding(query);

            // 向量搜索 - 搜索chunk类型
            List<VectorService.VectorSearchResult> searchResults =
                    vectorService.search(queryVector, topK, "chunk");

            // 获取分块和文档详情
            for (VectorService.VectorSearchResult result : searchResults) {
                if (result.id() == null) continue;

                try {
                    // 查询分块信息
                    DocumentChunk chunk = documentChunkMapper.selectByChunkId(result.id());
                    if (chunk == null) continue;

                    // 查询所属文档
                    Document document = documentService.getById(chunk.getDocumentId());
                    if (document == null) continue;

                    RagDocument ragDoc = new RagDocument();
                    ragDoc.setId(document.getId());
                    ragDoc.setChunkId(chunk.getId());
                    ragDoc.setName(document.getName());
                    ragDoc.setFileType(document.getFileType());
                    ragDoc.setPageNum(chunk.getPageNum());
                    ragDoc.setSummary(document.getSummary());
                    ragDoc.setScore((double) result.score());

                    // 使用分块内容作为匹配片段
                    ragDoc.setMatchedContent(chunk.getContent());

                    results.add(ragDoc);
                } catch (Exception e) {
                    log.warn("获取分块详情失败, chunkId={}", result.id(), e);
                }
            }
        } catch (Exception e) {
            log.error("文档检索失败", e);
        }

        return results;
    }

    /**
     * 检索相关知识节点
     */
    @Override
    public List<RagNode> searchNodes(String query, int topK) {
        List<RagNode> results = new ArrayList<>();

        try {
            // 生成查询向量
            float[] queryVector = ollamaService.generateEmbedding(query);

            // 向量搜索 - 搜索节点类型
            List<VectorService.VectorSearchResult> searchResults =
                    vectorService.search(queryVector, topK, "node");

            // 获取节点详情及其关系
            for (VectorService.VectorSearchResult result : searchResults) {
                if (result.id() == null) continue;

                try {
                    KnowledgeNode node = nodeService.getById(result.id());
                    if (node != null) {
                        RagNode ragNode = new RagNode();
                        ragNode.setId(node.getId());
                        ragNode.setName(node.getName());
                        ragNode.setNodeType(node.getNodeType());
                        ragNode.setDescription(node.getDescription());
                        ragNode.setScore((double) result.score());

                        // 解析属性
                        if (StrUtil.isNotBlank(node.getProperties())) {
                            ragNode.setProperties(JSON.parseObject(node.getProperties(), Map.class));
                        }

                        // 获取关联关系
                        List<RagNode.RagRelation> relations = getNodeRelations(node.getId());
                        ragNode.setRelations(relations);

                        results.add(ragNode);
                    }
                } catch (Exception e) {
                    log.warn("获取节点详情失败, id={}", result.id(), e);
                }
            }
        } catch (Exception e) {
            log.error("节点检索失败", e);
        }

        return results;
    }

    /**
     * 获取节点的关联关系
     */
    private List<RagNode.RagRelation> getNodeRelations(Long nodeId) {
        List<RagNode.RagRelation> relations = new ArrayList<>();

        try {
            // 获取以该节点为起点的关系
            List<KnowledgeRelation> outRelations = relationMapper.selectBySourceNodeId(nodeId);
            for (KnowledgeRelation rel : outRelations) {
                KnowledgeNode targetNode = nodeService.getById(rel.getTargetNodeId());
                if (targetNode != null) {
                    RagNode.RagRelation ragRel = new RagNode.RagRelation();
                    ragRel.setName(rel.getName());
                    ragRel.setRelationType(rel.getRelationType());
                    ragRel.setTargetNodeId(targetNode.getId());
                    ragRel.setTargetNodeName(targetNode.getName());
                    relations.add(ragRel);
                }
            }
        } catch (Exception e) {
            log.warn("获取节点关系失败, nodeId={}", nodeId, e);
        }

        return relations;
    }

    /**
     * 构建RAG上下文提示词
     */
    @Override
    public String buildContextPrompt(List<RagDocument> documents, String userQuery) {
        StringBuilder prompt = new StringBuilder();

        // 添加相关文档
        if (documents != null && !documents.isEmpty()) {
            prompt.append("【相关文档】\n");
            for (int i = 0; i < documents.size(); i++) {
                RagDocument doc = documents.get(i);
                prompt.append(String.format("- 文档%d「%s」", i + 1, doc.getName()));

                // 添加页码信息
                if (doc.getPageNum() != null && doc.getPageNum() > 0) {
                    prompt.append(String.format("（第%d页）", doc.getPageNum()));
                }
                prompt.append(": ");

                if (StrUtil.isNotBlank(doc.getMatchedContent())) {
                    prompt.append(doc.getMatchedContent());
                } else if (StrUtil.isNotBlank(doc.getSummary())) {
                    prompt.append(doc.getSummary());
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        return PROMPT_TEMPLATE.formatted(prompt.toString()) + "\n\n【用户问题】\n" + userQuery;
    }
}
