package com.uka.knowledge.repository;

import com.uka.knowledge.model.neo4j.Neo4jKnowledgeNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Neo4j知识节点Repository接口
 * <p>
 * 提供Neo4j图数据库的节点操作方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Repository
public interface Neo4jNodeRepository extends Neo4jRepository<Neo4jKnowledgeNode, Long> {

    /**
     * 根据MySQL ID查找节点
     *
     * @param mysqlId MySQL中的节点ID
     * @return 节点（可选）
     */
    Optional<Neo4jKnowledgeNode> findByMysqlId(Long mysqlId);

    /**
     * 根据名称查找节点
     *
     * @param name 节点名称
     * @return 节点列表
     */
    List<Neo4jKnowledgeNode> findByName(String name);

    /**
     * 根据节点类型查找节点
     *
     * @param nodeType 节点类型
     * @return 节点列表
     */
    List<Neo4jKnowledgeNode> findByNodeType(String nodeType);

    /**
     * 模糊查询节点名称
     *
     * @param keyword 关键词
     * @return 节点列表
     */
    @Query("MATCH (n:KnowledgeNode) WHERE n.name CONTAINS $keyword RETURN n")
    List<Neo4jKnowledgeNode> findByNameContaining(@Param("keyword") String keyword);

    /**
     * 创建节点间的关系
     *
     * @param sourceId     起始节点MySQL ID
     * @param targetId     目标节点MySQL ID
     * @param relationType 关系类型
     * @param relName      关系名称
     * @param weight       关系权重
     */
    @Query("""
            MATCH (source:KnowledgeNode {mysqlId: $sourceId})
            MATCH (target:KnowledgeNode {mysqlId: $targetId})
            MERGE (source)-[r:RELATES_TO {type: $relationType}]->(target)
            SET r.name = $relName, r.weight = $weight, r.createTime = datetime()
            """)
    void createRelation(@Param("sourceId") Long sourceId,
                        @Param("targetId") Long targetId,
                        @Param("relationType") String relationType,
                        @Param("relName") String relName,
                        @Param("weight") Double weight);

    /**
     * 删除节点间的关系
     *
     * @param sourceId 起始节点MySQL ID
     * @param targetId 目标节点MySQL ID
     */
    @Query("""
            MATCH (source:KnowledgeNode {mysqlId: $sourceId})-[r]->(target:KnowledgeNode {mysqlId: $targetId})
            DELETE r
            """)
    void deleteRelation(@Param("sourceId") Long sourceId, @Param("targetId") Long targetId);

    /**
     * 查询节点的所有相邻节点（一度关系）
     *
     * @param mysqlId 节点MySQL ID
     * @return 相邻节点列表
     */
    @Query("""
            MATCH (n:KnowledgeNode {mysqlId: $mysqlId})-[r]-(neighbor)
            RETURN neighbor
            """)
    List<Neo4jKnowledgeNode> findNeighbors(@Param("mysqlId") Long mysqlId);

    /**
     * 查询两个节点之间的最短路径
     *
     * @param startId 起始节点MySQL ID
     * @param endId   目标节点MySQL ID
     * @return 路径上的节点列表
     */
    @Query("""
            MATCH path = shortestPath((start:KnowledgeNode {mysqlId: $startId})-[*]-(end:KnowledgeNode {mysqlId: $endId}))
            RETURN nodes(path)
            """)
    List<Neo4jKnowledgeNode> findShortestPath(@Param("startId") Long startId, @Param("endId") Long endId);

    /**
     * 获取图谱可视化数据（限制数量）
     *
     * @param limit 返回节点数量限制
     * @return 节点和关系数据
     */
    @Query("""
            MATCH (n:KnowledgeNode)
            OPTIONAL MATCH (n)-[r]-(m)
            RETURN n, r, m
            LIMIT $limit
            """)
    List<Map<String, Object>> getGraphData(@Param("limit") Integer limit);

    /**
     * 根据MySQL ID删除节点
     *
     * @param mysqlId MySQL节点ID
     */
    @Query("MATCH (n:KnowledgeNode {mysqlId: $mysqlId}) DETACH DELETE n")
    void deleteByMysqlId(@Param("mysqlId") Long mysqlId);
}
