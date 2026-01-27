package com.uka.knowledge.model.neo4j;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

/**
 * Neo4j知识关系实体
 * <p>
 * 存储在Neo4j图数据库中的关系实体
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@RelationshipProperties
public class Neo4jKnowledgeRelation {

    /**
     * Neo4j关系内部ID
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * MySQL中的关系ID
     */
    @Property("mysqlId")
    private Long mysqlId;

    /**
     * 关系名称
     */
    @Property("name")
    private String name;

    /**
     * 关系类型
     */
    @Property("relationType")
    private String relationType;

    /**
     * 关系权重
     */
    @Property("weight")
    private Double weight;

    /**
     * 创建时间
     */
    @Property("createTime")
    private LocalDateTime createTime;

    /**
     * 目标节点
     */
    @TargetNode
    private Neo4jKnowledgeNode targetNode;

    /**
     * 无参构造函数
     */
    public Neo4jKnowledgeRelation() {
    }

    /**
     * 构造关系实体
     *
     * @param mysqlId      MySQL关系ID
     * @param name         关系名称
     * @param relationType 关系类型
     * @param weight       关系权重
     * @param targetNode   目标节点
     */
    public Neo4jKnowledgeRelation(Long mysqlId, String name, String relationType,
                                   Double weight, Neo4jKnowledgeNode targetNode) {
        this.mysqlId = mysqlId;
        this.name = name;
        this.relationType = relationType;
        this.weight = weight;
        this.targetNode = targetNode;
        this.createTime = LocalDateTime.now();
    }
}
