package com.uka.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识关系创建/更新DTO
 * <p>
 * 用于接收前端传入的关系数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "知识关系DTO")
public class KnowledgeRelationDTO {

    /**
     * 关系ID（更新时使用）
     */
    @Schema(description = "关系ID（更新时必填）")
    private Long id;

    /**
     * 关系名称
     */
    @NotBlank(message = "关系名称不能为空")
    @Size(max = 100, message = "关系名称长度不能超过100个字符")
    @Schema(description = "关系名称", required = true, example = "属于")
    private String name;

    /**
     * 关系类型
     */
    @NotBlank(message = "关系类型不能为空")
    @Size(max = 50, message = "关系类型长度不能超过50个字符")
    @Schema(description = "关系类型", required = true, example = "BELONGS_TO")
    private String relationType;

    /**
     * 起始节点ID
     */
    @NotNull(message = "起始节点ID不能为空")
    @Schema(description = "起始节点ID", required = true)
    private Long sourceNodeId;

    /**
     * 目标节点ID
     */
    @NotNull(message = "目标节点ID不能为空")
    @Schema(description = "目标节点ID", required = true)
    private Long targetNodeId;

    /**
     * 关系权重（0-1之间）
     */
    @Schema(description = "关系权重", example = "0.8")
    private Double weight;

    /**
     * 关系属性（JSON格式）
     */
    @Schema(description = "关系属性（JSON格式）", example = "{\"confidence\":0.9}")
    private String properties;
}
