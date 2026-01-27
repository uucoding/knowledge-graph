package com.uka.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import com.uka.knowledge.mapper.KnowledgeNodeMapper;
import com.uka.knowledge.mapper.KnowledgeRelationMapper;
import com.uka.knowledge.model.dto.KnowledgeRelationDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.model.vo.KnowledgeRelationVO;
import com.uka.knowledge.repository.Neo4jNodeRepository;
import com.uka.knowledge.service.KnowledgeRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 知识关系服务实现类
 * <p>
 * 实现知识关系的CRUD及相关业务逻辑
 * 同时维护MySQL和Neo4j中的数据一致性
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRelationServiceImpl extends ServiceImpl<KnowledgeRelationMapper, KnowledgeRelation>
        implements KnowledgeRelationService {

    private final KnowledgeRelationMapper relationMapper;
    private final KnowledgeNodeMapper nodeMapper;
    private final Neo4jNodeRepository neo4jNodeRepository;

    /**
     * 创建知识关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeRelation createRelation(KnowledgeRelationDTO dto) {
        // 验证节点是否存在
        KnowledgeNode sourceNode = nodeMapper.selectById(dto.getSourceNodeId());
        if (sourceNode == null) {
            throw new BusinessException(ResultCode.NODE_NOT_FOUND, "起始节点不存在");
        }

        KnowledgeNode targetNode = nodeMapper.selectById(dto.getTargetNodeId());
        if (targetNode == null) {
            throw new BusinessException(ResultCode.NODE_NOT_FOUND, "目标节点不存在");
        }

        // 检查是否已存在相同关系
        List<KnowledgeRelation> existingRelations = relationMapper.selectBySourceAndTarget(
                dto.getSourceNodeId(), dto.getTargetNodeId());
        for (KnowledgeRelation existing : existingRelations) {
            if (existing.getRelationType().equals(dto.getRelationType())) {
                throw new BusinessException(ResultCode.RELATION_ALREADY_EXISTS);
            }
        }

        // 创建MySQL实体
        KnowledgeRelation relation = new KnowledgeRelation();
        BeanUtils.copyProperties(dto, relation);

        // 设置默认权重
        if (relation.getWeight() == null) {
            relation.setWeight(1.0);
        }

        // 保存到MySQL
        this.save(relation);

        // 在Neo4j中创建关系
        neo4jNodeRepository.createRelation(
                dto.getSourceNodeId(),
                dto.getTargetNodeId(),
                dto.getRelationType(),
                dto.getName(),
                relation.getWeight()
        );

        log.info("创建知识关系成功, relationId={}, {} -[{}]-> {}",
                relation.getId(), sourceNode.getName(), dto.getName(), targetNode.getName());
        return relation;
    }

    /**
     * 更新知识关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeRelation updateRelation(KnowledgeRelationDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "关系ID不能为空");
        }

        KnowledgeRelation relation = this.getById(dto.getId());
        if (relation == null) {
            throw new BusinessException(ResultCode.RELATION_NOT_FOUND);
        }

        // 记录原始节点ID（用于更新Neo4j）
        Long oldSourceId = relation.getSourceNodeId();
        Long oldTargetId = relation.getTargetNodeId();

        // 更新MySQL实体
        if (StrUtil.isNotBlank(dto.getName())) {
            relation.setName(dto.getName());
        }
        if (StrUtil.isNotBlank(dto.getRelationType())) {
            relation.setRelationType(dto.getRelationType());
        }
        if (dto.getWeight() != null) {
            relation.setWeight(dto.getWeight());
        }
        if (dto.getProperties() != null) {
            relation.setProperties(dto.getProperties());
        }
        if (dto.getSourceNodeId() != null) {
            relation.setSourceNodeId(dto.getSourceNodeId());
        }
        if (dto.getTargetNodeId() != null) {
            relation.setTargetNodeId(dto.getTargetNodeId());
        }

        this.updateById(relation);

        // 更新Neo4j关系（先删除再创建）
        neo4jNodeRepository.deleteRelation(oldSourceId, oldTargetId);
        neo4jNodeRepository.createRelation(
                relation.getSourceNodeId(),
                relation.getTargetNodeId(),
                relation.getRelationType(),
                relation.getName(),
                relation.getWeight()
        );

        log.info("更新知识关系成功, relationId={}", relation.getId());
        return relation;
    }

    /**
     * 删除知识关系
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRelation(Long id) {
        KnowledgeRelation relation = this.getById(id);
        if (relation == null) {
            throw new BusinessException(ResultCode.RELATION_NOT_FOUND);
        }

        // 删除Neo4j中的关系
        neo4jNodeRepository.deleteRelation(relation.getSourceNodeId(), relation.getTargetNodeId());

        // 逻辑删除MySQL记录
        boolean result = this.removeById(id);

        log.info("删除知识关系成功, relationId={}", id);
        return result;
    }

    /**
     * 获取关系详情
     */
    @Override
    public KnowledgeRelationVO getRelationDetail(Long id) {
        KnowledgeRelation relation = this.getById(id);
        if (relation == null) {
            throw new BusinessException(ResultCode.RELATION_NOT_FOUND);
        }
        return convertToVO(relation);
    }

    /**
     * 分页查询关系
     */
    @Override
    public Page<KnowledgeRelationVO> pageRelations(Integer pageNum, Integer pageSize,
                                                    String relationType, String keyword) {
        LambdaQueryWrapper<KnowledgeRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), KnowledgeRelation::getName, keyword);
        wrapper.eq(StrUtil.isNotBlank(relationType), KnowledgeRelation::getRelationType, relationType);
        wrapper.orderByDesc(KnowledgeRelation::getCreateTime);

        Page<KnowledgeRelation> page = this.page(new Page<>(pageNum, pageSize), wrapper);

        Page<KnowledgeRelationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).toList());

        return voPage;
    }

    /**
     * 查询指定节点的所有关系
     */
    @Override
    public List<KnowledgeRelationVO> getRelationsByNodeId(Long nodeId) {
        return relationMapper.selectByNodeId(nodeId);
    }

    /**
     * 获取所有关系类型
     */
    @Override
    public List<String> getAllRelationTypes() {
        return relationMapper.selectAllRelationTypes();
    }

    /**
     * 检查两个节点之间是否存在指定类型的关系
     */
    @Override
    public boolean existsRelation(Long sourceNodeId, Long targetNodeId, String relationType) {
        List<KnowledgeRelation> relations = relationMapper.selectBySourceAndTarget(sourceNodeId, targetNodeId);

        if (StrUtil.isBlank(relationType)) {
            return !relations.isEmpty();
        }

        return relations.stream().anyMatch(r -> relationType.equals(r.getRelationType()));
    }

    /**
     * 实体转VO
     */
    private KnowledgeRelationVO convertToVO(KnowledgeRelation relation) {
        KnowledgeRelationVO vo = new KnowledgeRelationVO();
        BeanUtils.copyProperties(relation, vo);

        // 获取节点名称
        KnowledgeNode sourceNode = nodeMapper.selectById(relation.getSourceNodeId());
        KnowledgeNode targetNode = nodeMapper.selectById(relation.getTargetNodeId());

        if (sourceNode != null) {
            vo.setSourceNodeName(sourceNode.getName());
        }
        if (targetNode != null) {
            vo.setTargetNodeName(targetNode.getName());
        }

        // 解析属性JSON
        if (StrUtil.isNotBlank(relation.getProperties())) {
            vo.setProperties(JSON.parseObject(relation.getProperties(), Map.class));
        }

        return vo;
    }
}
