package com.uka.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识抽取请求DTO
 * <p>
 * 用于请求从文本中抽取知识实体和关系
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "知识抽取请求DTO")
public class KnowledgeExtractDTO {

    /**
     * 文档ID
     */
    @Schema(description = "文档ID（从文档抽取时使用）")
    private Long documentId;

    /**
     * OCR记录ID
     */
    @Schema(description = "OCR记录ID（从OCR结果抽取时使用）")
    private Long ocrRecordId;

    /**
     * 待抽取的文本内容
     */
    @Schema(description = "待抽取的文本内容（直接传入文本时使用）")
    private String text;

    /**
     * 抽取模式（auto/entity/relation/all）
     */
    @Schema(description = "抽取模式", example = "all", allowableValues = {"auto", "entity", "relation", "all"})
    private String mode = "all";

    /**
     * 指定抽取的实体类型（可选，多个类型用逗号分隔）
     */
    @Schema(description = "指定抽取的实体类型", example = "Person,Organization,Location")
    private String entityTypes;
}
