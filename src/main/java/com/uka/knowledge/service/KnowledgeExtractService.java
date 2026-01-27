package com.uka.knowledge.service;

import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;

import java.util.List;

/**
 * 知识抽取服务接口
 * <p>
 * 负责从文本中自动抽取知识实体和关系，并构建知识图谱
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface KnowledgeExtractService {

    /**
     * 从文档中抽取知识并构建图谱
     *
     * @param documentId 文档ID
     * @param content    文档内容
     * @return 抽取结果
     */
    ExtractResult extractFromDocument(Long documentId, String content);

    /**
     * 从OCR结果中抽取知识并构建图谱
     *
     * @param ocrRecordId OCR记录ID
     * @param ocrText     OCR识别文本
     * @return 抽取结果
     */
    ExtractResult extractFromOcr(Long ocrRecordId, String ocrText);

    /**
     * 从文本中抽取知识并构建图谱
     *
     * @param text       文本内容
     * @param sourceId   来源ID
     * @param sourceType 来源类型（document/ocr/manual）
     * @return 抽取结果
     */
    ExtractResult extractAndBuildGraph(String text, Long sourceId, String sourceType);

    /**
     * 抽取结果封装类
     */
    record ExtractResult(
            List<KnowledgeNode> nodes,
            List<KnowledgeRelation> relations,
            int nodeCount,
            int relationCount,
            String message
    ) {
        public static ExtractResult empty(String message) {
            return new ExtractResult(List.of(), List.of(), 0, 0, message);
        }

        public static ExtractResult of(List<KnowledgeNode> nodes, List<KnowledgeRelation> relations) {
            return new ExtractResult(
                    nodes, relations,
                    nodes != null ? nodes.size() : 0,
                    relations != null ? relations.size() : 0,
                    "抽取成功"
            );
        }
    }
}
