package com.uka.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uka.knowledge.model.dto.KnowledgeNodeDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.vo.KnowledgeNodeVO;

import java.util.List;
import java.util.Map;

/**
 * 知识节点服务接口
 * <p>
 * 定义知识节点的CRUD及相关业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface KnowledgeNodeService extends IService<KnowledgeNode> {

    /**
     * 创建知识节点
     * 同时在MySQL和Neo4j中创建
     *
     * @param dto 节点DTO
     * @return 创建的节点
     */
    KnowledgeNode createNode(KnowledgeNodeDTO dto);

    /**
     * 更新知识节点
     * 同时更新MySQL和Neo4j中的数据
     *
     * @param dto 节点DTO
     * @return 更新后的节点
     */
    KnowledgeNode updateNode(KnowledgeNodeDTO dto);

    /**
     * 删除知识节点
     * 同时删除MySQL和Neo4j中的数据及相关关系
     *
     * @param id 节点ID
     * @return 是否成功
     */
    boolean deleteNode(Long id);

    /**
     * 获取节点详情
     *
     * @param id 节点ID
     * @return 节点VO
     */
    KnowledgeNodeVO getNodeDetail(Long id);

    /**
     * 分页查询节点
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词
     * @param nodeType 节点类型
     * @return 分页结果
     */
    Page<KnowledgeNodeVO> pageNodes(Integer pageNum, Integer pageSize,
                                     String keyword, String nodeType);

    /**
     * 根据名称搜索节点
     *
     * @param name 节点名称关键词
     * @return 节点列表
     */
    List<KnowledgeNodeVO> searchByName(String name);

    /**
     * 获取所有节点类型
     *
     * @return 节点类型列表
     */
    List<String> getAllNodeTypes();

    /**
     * 统计各类型节点数量
     *
     * @return 统计结果 Map<类型, 数量>
     */
    List<Map<String, Object>> countByNodeType();

    /**
     * 获取节点的相邻节点
     *
     * @param nodeId 节点ID
     * @return 相邻节点列表
     */
    List<KnowledgeNodeVO> getNeighborNodes(Long nodeId);

    /**
     * 查询两个节点之间的最短路径
     *
     * @param startNodeId 起始节点ID
     * @param endNodeId   目标节点ID
     * @return 路径上的节点列表
     */
    List<KnowledgeNodeVO> findShortestPath(Long startNodeId, Long endNodeId);
}
