package com.uka.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识节点创建/更新DTO
 * <p>
 * 用于接收前端传入的节点数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "知识节点DTO")
public class KnowledgeNodeDTO {

    /**
     * 节点ID（更新时使用）
     */
    @Schema(description = "节点ID（更新时必填）")
    private Long id;

    /**
     * 节点名称
     */
    @NotBlank(message = "节点名称不能为空")
    @Size(max = 200, message = "节点名称长度不能超过200个字符")
    @Schema(description = "节点名称", required = true, example = "人工智能")
    private String name;

    /**
     * 节点类型
     */
    @NotBlank(message = "节点类型不能为空")
    @Size(max = 50, message = "节点类型长度不能超过50个字符")
    @Schema(description = "节点类型", required = true, example = "Concept")
    private String nodeType;

    /**
     * 节点描述
     */
    @Size(max = 2000, message = "节点描述长度不能超过2000个字符")
    @Schema(description = "节点描述", example = "人工智能是计算机科学的一个分支...")
    private String description;

    /**
     * 节点属性（JSON格式字符串）
     */
    @Schema(description = "节点属性（JSON格式）", example = "{\"category\":\"技术\",\"level\":1}")
    private String properties;

    /**
     * 来源文档ID
     */
    @Schema(description = "来源文档ID")
    private Long sourceDocId;

    /**
     * 来源类型
     */
    @Schema(description = "来源类型", example = "manual")
    private String sourceType;
}
