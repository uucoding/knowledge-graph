package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * RAG检索图谱节点对象
 * <p>
 * 表示从向量数据库检索到的相关知识图谱节点
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "RAG关联图谱节点")
public class RagNode {

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
     * 相似度分数
     */
    @Schema(description = "相似度分数")
    private Double score;

    /**
     * 关联的关系（用于图谱展示）
     */
    @Schema(description = "关联的关系")
    private List<RagRelation> relations;

    /**
     * RAG关联关系
     */
    @Data
    @Schema(description = "RAG关联关系")
    public static class RagRelation {
        @Schema(description = "关系名称")
        private String name;

        @Schema(description = "关系类型")
        private String relationType;

        @Schema(description = "目标节点ID")
        private Long targetNodeId;

        @Schema(description = "目标节点名称")
        private String targetNodeName;
    }
}
