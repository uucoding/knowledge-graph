package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识节点视图对象
 * <p>
 * 用于返回给前端的节点数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "知识节点VO")
public class KnowledgeNodeVO {

    /**
     * 节点ID
     */
    @Schema(description = "节点ID")
    private Long id;

    /**
     * 节点名称
     */
    @Schema(description = "节点名称")
    private String name;

    /**
     * 节点类型
     */
    @Schema(description = "节点类型")
    private String nodeType;

    /**
     * 节点描述
     */
    @Schema(description = "节点描述")
    private String description;

    /**
     * 节点属性
     */
    @Schema(description = "节点属性")
    private Map<String, Object> properties;

    /**
     * Neo4j节点ID
     */
    @Schema(description = "Neo4j节点ID")
    private String neo4jId;

    /**
     * 来源文档ID
     */
    @Schema(description = "来源文档ID")
    private Long sourceDocId;

    /**
     * 来源类型
     */
    @Schema(description = "来源类型")
    private String sourceType;

    /**
     * 关联关系数量
     */
    @Schema(description = "关联关系数量")
    private Integer relationCount;

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
