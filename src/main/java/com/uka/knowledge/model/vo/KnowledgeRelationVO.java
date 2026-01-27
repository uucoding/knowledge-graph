package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识关系视图对象
 * <p>
 * 用于返回给前端的关系数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "知识关系VO")
public class KnowledgeRelationVO {

    /**
     * 关系ID
     */
    @Schema(description = "关系ID")
    private Long id;

    /**
     * 关系名称
     */
    @Schema(description = "关系名称")
    private String name;

    /**
     * 关系类型
     */
    @Schema(description = "关系类型")
    private String relationType;

    /**
     * 起始节点ID
     */
    @Schema(description = "起始节点ID")
    private Long sourceNodeId;

    /**
     * 起始节点名称
     */
    @Schema(description = "起始节点名称")
    private String sourceNodeName;

    /**
     * 目标节点ID
     */
    @Schema(description = "目标节点ID")
    private Long targetNodeId;

    /**
     * 目标节点名称
     */
    @Schema(description = "目标节点名称")
    private String targetNodeName;

    /**
     * 关系权重
     */
    @Schema(description = "关系权重")
    private Double weight;

    /**
     * 关系属性
     */
    @Schema(description = "关系属性")
    private Map<String, Object> properties;

    /**
     * Neo4j关系ID
     */
    @Schema(description = "Neo4j关系ID")
    private String neo4jRelId;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
