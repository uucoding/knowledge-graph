package com.uka.knowledge.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.uka.knowledge.model.dto.KnowledgeRelationDTO;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.model.vo.KnowledgeRelationVO;

import java.util.List;

/**
 * 知识关系服务接口
 * <p>
 * 定义知识关系的CRUD及相关业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface KnowledgeRelationService extends IService<KnowledgeRelation> {

    /**
     * 创建知识关系
     * 同时在MySQL和Neo4j中创建
     *
     * @param dto 关系DTO
     * @return 创建的关系
     */
    KnowledgeRelation createRelation(KnowledgeRelationDTO dto);

    /**
     * 更新知识关系
     * 同时更新MySQL和Neo4j中的数据
     *
     * @param dto 关系DTO
     * @return 更新后的关系
     */
    KnowledgeRelation updateRelation(KnowledgeRelationDTO dto);

    /**
     * 删除知识关系
     * 同时删除MySQL和Neo4j中的关系
     *
     * @param id 关系ID
     * @return 是否成功
     */
    boolean deleteRelation(Long id);

    /**
     * 获取关系详情
     *
     * @param id 关系ID
     * @return 关系VO
     */
    KnowledgeRelationVO getRelationDetail(Long id);

    /**
     * 分页查询关系
     *
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @param relationType 关系类型
     * @param keyword      关系名称关键词
     * @return 分页结果
     */
    Page<KnowledgeRelationVO> pageRelations(Integer pageNum, Integer pageSize,
                                             String relationType, String keyword);

    /**
     * 查询指定节点的所有关系
     *
     * @param nodeId 节点ID
     * @return 关系列表
     */
    List<KnowledgeRelationVO> getRelationsByNodeId(Long nodeId);

    /**
     * 获取所有关系类型
     *
     * @return 关系类型列表
     */
    List<String> getAllRelationTypes();

    /**
     * 检查两个节点之间是否存在指定类型的关系
     *
     * @param sourceNodeId 起始节点ID
     * @param targetNodeId 目标节点ID
     * @param relationType 关系类型（可选）
     * @return 是否存在
     */
    boolean existsRelation(Long sourceNodeId, Long targetNodeId, String relationType);
}
