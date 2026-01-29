package com.uka.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import com.uka.knowledge.mapper.KnowledgeNodeMapper;
import com.uka.knowledge.mapper.KnowledgeRelationMapper;
import com.uka.knowledge.model.dto.KnowledgeNodeDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.neo4j.Neo4jKnowledgeNode;
import com.uka.knowledge.model.vo.KnowledgeNodeVO;
import com.uka.knowledge.repository.Neo4jNodeRepository;
import com.uka.knowledge.service.KnowledgeNodeService;
import com.uka.knowledge.service.OllamaService;
import com.uka.knowledge.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 知识节点服务实现类
 * <p>
 * 实现知识节点的CRUD及相关业务逻辑
 * 同时维护MySQL和Neo4j中的数据一致性
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeNodeServiceImpl extends ServiceImpl<KnowledgeNodeMapper, KnowledgeNode>
        implements KnowledgeNodeService {

    private final KnowledgeNodeMapper nodeMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final Neo4jNodeRepository neo4jNodeRepository;
    private final OllamaService ollamaService;
    private final VectorService vectorService;

    /**
     * 创建知识节点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeNode createNode(KnowledgeNodeDTO dto) {
        // 创建MySQL实体
        KnowledgeNode node = new KnowledgeNode();
        BeanUtils.copyProperties(dto, node);

        // 设置默认值
        if (StrUtil.isBlank(node.getSourceType())) {
            node.setSourceType("manual");
        }

        // 保存到MySQL
        this.save(node);

        // 创建Neo4j节点
        Neo4jKnowledgeNode neo4jNode = new Neo4jKnowledgeNode(
                node.getId(),
                node.getName(),
                node.getNodeType(),
                node.getDescription()
        );

        // 解析属性JSON
        if (StrUtil.isNotBlank(dto.getProperties())) {
            Map<String, Object> props = JSON.parseObject(dto.getProperties(), Map.class);
            neo4jNode.setProperties(props);
        }

        Neo4jKnowledgeNode savedNeo4jNode = neo4jNodeRepository.save(neo4jNode);
        node.setNeo4jId(String.valueOf(savedNeo4jNode.getId()));

        // 生成向量并存储
        String textForEmbedding = node.getName() + " " + node.getDescription();
        float[] vector = ollamaService.generateEmbedding(textForEmbedding);
        String vectorId = vectorService.insertVector(new VectorService.VectorInsertData(node.getId(), vector, "node"));
        node.setVectorId(vectorId);

        // 更新MySQL记录
        this.updateById(node);

        log.info("创建知识节点成功, nodeId={}, name={}", node.getId(), node.getName());
        return node;
    }

    /**
     * 更新知识节点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeNode updateNode(KnowledgeNodeDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "节点ID不能为空");
        }

        KnowledgeNode node = this.getById(dto.getId());
        if (node == null) {
            throw new BusinessException(ResultCode.NODE_NOT_FOUND);
        }

        // 更新MySQL实体
        if (StrUtil.isNotBlank(dto.getName())) {
            node.setName(dto.getName());
        }
        if (StrUtil.isNotBlank(dto.getNodeType())) {
            node.setNodeType(dto.getNodeType());
        }
        if (dto.getDescription() != null) {
            node.setDescription(dto.getDescription());
        }
        if (dto.getProperties() != null) {
            node.setProperties(dto.getProperties());
        }

        this.updateById(node);

        // 更新Neo4j节点
        Optional<Neo4jKnowledgeNode> neo4jNodeOpt = neo4jNodeRepository.findByMysqlId(node.getId());
        if (neo4jNodeOpt.isPresent()) {
            Neo4jKnowledgeNode neo4jNode = neo4jNodeOpt.get();
            neo4jNode.setName(node.getName());
            neo4jNode.setNodeType(node.getNodeType());
            neo4jNode.setDescription(node.getDescription());

            if (StrUtil.isNotBlank(node.getProperties())) {
                Map<String, Object> props = JSON.parseObject(node.getProperties(), Map.class);
                neo4jNode.setProperties(props);
            }

            neo4jNodeRepository.save(neo4jNode);
        }

        // 更新向量
        if (StrUtil.isNotBlank(node.getVectorId())) {
            vectorService.deleteVector(node.getVectorId());
        }
        String textForEmbedding = node.getName() + " " + node.getDescription();
        float[] vector = ollamaService.generateEmbedding(textForEmbedding);
        String vectorId = vectorService.insertVector(new VectorService.VectorInsertData(node.getId(), vector, "node"));
        node.setVectorId(vectorId);
        this.updateById(node);

        log.info("更新知识节点成功, nodeId={}, name={}", node.getId(), node.getName());
        return node;
    }

    /**
     * 删除知识节点
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNode(Long id) {
        KnowledgeNode node = this.getById(id);
        if (node == null) {
            throw new BusinessException(ResultCode.NODE_NOT_FOUND);
        }

        // 删除向量
        if (StrUtil.isNotBlank(node.getVectorId())) {
            vectorService.deleteVector(node.getVectorId());
        }

        // 删除Neo4j节点（会同时删除相关关系）
        neo4jNodeRepository.deleteByMysqlId(id);

        // 删除MySQL中的相关关系
        LambdaQueryWrapper<com.uka.knowledge.model.entity.KnowledgeRelation> relationWrapper =
                new LambdaQueryWrapper<>();
        relationWrapper.eq(com.uka.knowledge.model.entity.KnowledgeRelation::getSourceNodeId, id)
                .or()
                .eq(com.uka.knowledge.model.entity.KnowledgeRelation::getTargetNodeId, id);
        relationMapper.delete(relationWrapper);

        // 逻辑删除MySQL节点
        boolean result = this.removeById(id);

        log.info("删除知识节点成功, nodeId={}", id);
        return result;
    }

    /**
     * 获取节点详情
     */
    @Override
    public KnowledgeNodeVO getNodeDetail(Long id) {
        KnowledgeNode node = this.getById(id);
        if (node == null) {
            throw new BusinessException(ResultCode.NODE_NOT_FOUND);
        }
        return convertToVO(node);
    }

    /**
     * 分页查询节点
     */
    @Override
    public Page<KnowledgeNodeVO> pageNodes(Integer pageNum, Integer pageSize,
                                            String keyword, String nodeType) {
        LambdaQueryWrapper<KnowledgeNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(keyword), KnowledgeNode::getName, keyword);
        wrapper.eq(StrUtil.isNotBlank(nodeType), KnowledgeNode::getNodeType, nodeType);
        wrapper.orderByDesc(KnowledgeNode::getCreateTime);

        Page<KnowledgeNode> page = this.page(new Page<>(pageNum, pageSize), wrapper);

        Page<KnowledgeNodeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).toList());

        return voPage;
    }

    /**
     * 根据名称搜索节点
     */
    @Override
    public List<KnowledgeNodeVO> searchByName(String name) {
        List<KnowledgeNode> nodes = nodeMapper.selectByNameLike(name);
        return nodes.stream().map(this::convertToVO).toList();
    }

    /**
     * 获取所有节点类型
     */
    @Override
    public List<String> getAllNodeTypes() {
        return nodeMapper.selectAllNodeTypes();
    }

    /**
     * 统计各类型节点数量
     */
    @Override
    public List<Map<String, Object>> countByNodeType() {
        return nodeMapper.countByNodeType();
    }

    /**
     * 获取节点的相邻节点
     */
    @Override
    public List<KnowledgeNodeVO> getNeighborNodes(Long nodeId) {
        List<Neo4jKnowledgeNode> neighbors = neo4jNodeRepository.findNeighbors(nodeId);

        List<KnowledgeNodeVO> result = new ArrayList<>();
        for (Neo4jKnowledgeNode neo4jNode : neighbors) {
            KnowledgeNode node = this.getById(neo4jNode.getMysqlId());
            if (node != null) {
                result.add(convertToVO(node));
            }
        }

        return result;
    }

    /**
     * 查询两个节点之间的最短路径
     */
    @Override
    public List<KnowledgeNodeVO> findShortestPath(Long startNodeId, Long endNodeId) {
        List<Neo4jKnowledgeNode> pathNodes = neo4jNodeRepository.findShortestPath(startNodeId, endNodeId);

        List<KnowledgeNodeVO> result = new ArrayList<>();
        for (Neo4jKnowledgeNode neo4jNode : pathNodes) {
            KnowledgeNode node = this.getById(neo4jNode.getMysqlId());
            if (node != null) {
                result.add(convertToVO(node));
            }
        }

        return result;
    }

    /**
     * 实体转VO
     */
    private KnowledgeNodeVO convertToVO(KnowledgeNode node) {
        KnowledgeNodeVO vo = new KnowledgeNodeVO();
        BeanUtils.copyProperties(node, vo);

        // 解析属性JSON
        if (StrUtil.isNotBlank(node.getProperties())) {
            vo.setProperties(JSON.parseObject(node.getProperties(), Map.class));
        }

        // 获取关系数量
        Integer relationCount = relationMapper.countByNodeId(node.getId());
        vo.setRelationCount(relationCount);

        return vo;
    }
}
