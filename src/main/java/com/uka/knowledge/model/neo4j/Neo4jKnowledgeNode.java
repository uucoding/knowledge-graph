package com.uka.knowledge.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Neo4j知识节点实体
 * <p>
 * 存储在Neo4j图数据库中的节点实体
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Node("KnowledgeNode")
public class Neo4jKnowledgeNode {

    /**
     * Neo4j内部ID
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * MySQL中的节点ID（用于关联）
     */
    @Property("mysqlId")
    private Long mysqlId;

    /**
     * 节点名称
     */
    @Property("name")
    private String name;

    /**
     * 节点类型
     */
    @Property("nodeType")
    private String nodeType;

    /**
     * 节点描述
     */
    @Property("description")
    private String description;

    /**
     * 创建时间
     */
    @Property("createTime")
    private LocalDateTime createTime;

    /**
     * 动态属性（存储扩展属性）
     */
    @CompositeProperty
    private Map<String, Object> properties;

    /**
     * 无参构造函数
     */
    public Neo4jKnowledgeNode() {
    }

    /**
     * 从MySQL实体构造Neo4j节点
     *
     * @param mysqlId     MySQL节点ID
     * @param name        节点名称
     * @param nodeType    节点类型
     * @param description 节点描述
     */
    public Neo4jKnowledgeNode(Long mysqlId, String name, String nodeType, String description) {
        this.mysqlId = mysqlId;
        this.name = name;
        this.nodeType = nodeType;
        this.description = description;
        this.createTime = LocalDateTime.now();
    }
}
