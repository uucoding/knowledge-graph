package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.model.vo.KnowledgeRelationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识关系Mapper接口
 * <p>
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 并扩展自定义查询方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface KnowledgeRelationMapper extends BaseMapper<KnowledgeRelation> {

    /**
     * 查询指定节点的所有关系（包含节点名称）
     *
     * @param nodeId 节点ID
     * @return 关系VO列表
     */
    @Select("""
            SELECT r.*,
                   sn.name as source_node_name,
                   tn.name as target_node_name
            FROM kg_knowledge_relation r
            LEFT JOIN kg_knowledge_node sn ON r.source_node_id = sn.id
            LEFT JOIN kg_knowledge_node tn ON r.target_node_id = tn.id
            WHERE (r.source_node_id = #{nodeId} OR r.target_node_id = #{nodeId})
              AND r.deleted = 0
            """)
    List<KnowledgeRelationVO> selectByNodeId(@Param("nodeId") Long nodeId);

    /**
     * 查询两个节点之间的关系
     *
     * @param sourceNodeId 起始节点ID
     * @param targetNodeId 目标节点ID
     * @return 关系列表
     */
    @Select("""
            SELECT * FROM kg_knowledge_relation
            WHERE source_node_id = #{sourceNodeId}
              AND target_node_id = #{targetNodeId}
              AND deleted = 0
            """)
    List<KnowledgeRelation> selectBySourceAndTarget(@Param("sourceNodeId") Long sourceNodeId,
                                                     @Param("targetNodeId") Long targetNodeId);

    /**
     * 获取所有关系类型
     *
     * @return 关系类型列表
     */
    @Select("SELECT DISTINCT relation_type FROM kg_knowledge_relation WHERE deleted = 0")
    List<String> selectAllRelationTypes();

    /**
     * 统计节点的关系数量
     *
     * @param nodeId 节点ID
     * @return 关系数量
     */
    @Select("""
            SELECT COUNT(*) FROM kg_knowledge_relation
            WHERE (source_node_id = #{nodeId} OR target_node_id = #{nodeId})
              AND deleted = 0
            """)
    Integer countByNodeId(@Param("nodeId") Long nodeId);

    /**
     * 根据起始节点ID查询关系
     *
     * @param sourceNodeId 起始节点ID
     * @return 关系列表
     */
    @Select("SELECT * FROM kg_knowledge_relation WHERE source_node_id = #{sourceNodeId} AND deleted = 0")
    List<KnowledgeRelation> selectBySourceNodeId(@Param("sourceNodeId") Long sourceNodeId);
}
