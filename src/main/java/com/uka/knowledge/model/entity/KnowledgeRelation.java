package com.uka.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识关系实体类（MySQL存储）
 * <p>
 * 存储知识图谱中节点间关系的基本信息
 * 对应数据库表：kg_knowledge_relation
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@TableName("kg_knowledge_relation")
public class KnowledgeRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关系名称（如：属于、包含、创建等）
     */
    private String name;

    /**
     * 关系类型
     */
    private String relationType;

    /**
     * 起始节点ID
     */
    private Long sourceNodeId;

    /**
     * 目标节点ID
     */
    private Long targetNodeId;

    /**
     * 关系权重（0-1之间的值，表示关系强度）
     */
    private Double weight;

    /**
     * 关系属性（JSON格式）
     */
    private String properties;

    /**
     * Neo4j中的关系ID
     */
    private String neo4jRelId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识
     */
    @TableLogic
    private Integer deleted;
}
