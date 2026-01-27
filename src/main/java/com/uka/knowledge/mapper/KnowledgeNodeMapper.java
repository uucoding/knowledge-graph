package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.KnowledgeNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识节点Mapper接口
 * <p>
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 并扩展自定义查询方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface KnowledgeNodeMapper extends BaseMapper<KnowledgeNode> {

    /**
     * 根据名称模糊查询节点
     *
     * @param name 节点名称关键词
     * @return 节点列表
     */
    @Select("SELECT * FROM kg_knowledge_node WHERE name LIKE CONCAT('%', #{name}, '%') AND deleted = 0")
    List<KnowledgeNode> selectByNameLike(@Param("name") String name);

    /**
     * 根据节点类型查询节点
     *
     * @param nodeType 节点类型
     * @return 节点列表
     */
    @Select("SELECT * FROM kg_knowledge_node WHERE node_type = #{nodeType} AND deleted = 0")
    List<KnowledgeNode> selectByNodeType(@Param("nodeType") String nodeType);

    /**
     * 获取所有节点类型
     *
     * @return 节点类型列表
     */
    @Select("SELECT DISTINCT node_type FROM kg_knowledge_node WHERE deleted = 0")
    List<String> selectAllNodeTypes();

    /**
     * 统计各类型节点数量
     *
     * @return 统计结果
     */
    @Select("SELECT node_type, COUNT(*) as count FROM kg_knowledge_node WHERE deleted = 0 GROUP BY node_type")
    List<java.util.Map<String, Object>> countByNodeType();

    /**
     * 根据来源文档ID查询节点
     *
     * @param sourceDocId 来源文档ID
     * @return 节点列表
     */
    @Select("SELECT * FROM kg_knowledge_node WHERE source_doc_id = #{sourceDocId} AND deleted = 0")
    List<KnowledgeNode> selectBySourceDocId(@Param("sourceDocId") Long sourceDocId);
}
