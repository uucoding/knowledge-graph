package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 图谱可视化数据VO
 * <p>
 * 用于前端图谱可视化展示的数据格式
 * 兼容ECharts关系图和其他可视化库
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "图谱可视化数据VO")
public class GraphVO {

    /**
     * 节点列表
     */
    @Schema(description = "节点列表")
    private List<GraphNode> nodes;

    /**
     * 边（关系）列表
     */
    @Schema(description = "边列表")
    private List<GraphLink> links;

    /**
     * 节点类型列表（用于图例）
     */
    @Schema(description = "节点类型列表")
    private List<String> categories;

    /**
     * 图谱节点
     */
    @Data
    @Schema(description = "图谱节点")
    public static class GraphNode {

        /**
         * 节点ID
         */
        @Schema(description = "节点ID")
        private String id;

        /**
         * 节点名称
         */
        @Schema(description = "节点名称")
        private String name;

        /**
         * 节点类型（对应category索引）
         */
        @Schema(description = "节点类型索引")
        private Integer category;

        /**
         * 节点类型名称
         */
        @Schema(description = "节点类型名称")
        private String nodeType;

        /**
         * 节点大小（根据关联数量计算）
         */
        @Schema(description = "节点大小")
        private Integer symbolSize;

        /**
         * 节点描述
         */
        @Schema(description = "节点描述")
        private String description;

        /**
         * 节点值（可用于表示重要度）
         */
        @Schema(description = "节点值")
        private Double value;
    }

    /**
     * 图谱边（关系）
     */
    @Data
    @Schema(description = "图谱边")
    public static class GraphLink {

        /**
         * 关系ID
         */
        @Schema(description = "关系ID")
        private String id;

        /**
         * 起始节点ID
         */
        @Schema(description = "起始节点ID")
        private String source;

        /**
         * 目标节点ID
         */
        @Schema(description = "目标节点ID")
        private String target;

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
         * 关系权重（影响边的粗细）
         */
        @Schema(description = "关系权重")
        private Double weight;
    }
}
