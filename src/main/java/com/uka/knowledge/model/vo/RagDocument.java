package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * RAG检索文档对象
 * <p>
 * 表示从向量数据库检索到的相关文档
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "RAG关联文档")
public class RagDocument {

    /**
     * 文档ID
     */
    @Schema(description = "文档ID")
    private Long id;

    /**
     * 分块ID（用于精确定位）
     */
    @Schema(description = "分块ID")
    private Long chunkId;

    /**
     * 文档名称
     */
    @Schema(description = "文档名称")
    private String name;

    /**
     * 文档类型
     */
    @Schema(description = "文档类型")
    private String fileType;

    /**
     * 页码（从1开始，0表示无页码概念）
     */
    @Schema(description = "页码")
    private Integer pageNum;

    /**
     * 内容摘要
     */
    @Schema(description = "内容摘要")
    private String summary;

    /**
     * 匹配的内容片段
     */
    @Schema(description = "匹配的内容片段")
    private String matchedContent;

    /**
     * 相似度分数
     */
    @Schema(description = "相似度分数")
    private Double score;
}
