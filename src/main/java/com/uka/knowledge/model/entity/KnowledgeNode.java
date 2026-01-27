package com.uka.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识节点实体类（MySQL存储）
 * <p>
 * 存储知识图谱节点的基本信息
 * 对应数据库表：kg_knowledge_node
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@TableName("kg_knowledge_node")
public class KnowledgeNode implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型（如：Person, Concept, Event, Location等）
     */
    private String nodeType;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点属性（JSON格式存储扩展属性）
     */
    private String properties;

    /**
     * Neo4j中的节点ID
     */
    private String neo4jId;

    /**
     * 向量ID（Milvus中的向量标识）
     */
    private String vectorId;

    /**
     * 来源文档ID
     */
    private Long sourceDocId;

    /**
     * 来源类型（document/ocr/manual）
     */
    private String sourceType;

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
