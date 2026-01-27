package com.uka.knowledge.service.impl;

import cn.hutool.core.util.StrUtil;
import com.uka.knowledge.mapper.KnowledgeNodeMapper;
import com.uka.knowledge.mapper.KnowledgeRelationMapper;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.model.vo.GraphVO;
import com.uka.knowledge.model.vo.KnowledgeNodeVO;
import com.uka.knowledge.model.vo.KnowledgeRelationVO;
import com.uka.knowledge.service.GraphService;
import com.uka.knowledge.service.KnowledgeNodeService;
import com.uka.knowledge.service.KnowledgeRelationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图谱服务实现类
 * <p>
 * 实现图谱可视化数据的查询和组装
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphServiceImpl implements GraphService {

    private final KnowledgeNodeMapper nodeMapper;
    private final KnowledgeRelationMapper relationMapper;
    private final KnowledgeNodeService nodeService;
    private final KnowledgeRelationService relationService;

    /**
     * 获取完整图谱数据
     */
    @Override
    public GraphVO getGraphData(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100;
        }

        // 查询节点
        List<KnowledgeNode> nodes = nodeMapper.selectList(null);
        if (nodes.size() > limit) {
            nodes = nodes.subList(0, limit);
        }

        // 获取节点ID集合
        Set<Long> nodeIds = nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toSet());

        // 查询关系
        List<KnowledgeRelation> relations = relationMapper.selectList(null);
        // 过滤只保留两端节点都在结果集中的关系
        relations = relations.stream()
                .filter(r -> nodeIds.contains(r.getSourceNodeId()) && nodeIds.contains(r.getTargetNodeId()))
                .collect(Collectors.toList());

        return buildGraphVO(nodes, relations);
    }

    /**
     * 获取指定节点的子图
     */
    @Override
    public GraphVO getSubGraph(Long nodeId, Integer depth) {
        if (depth == null || depth <= 0) {
            depth = 2;
        }

        // 使用BFS获取指定深度内的所有节点
        Set<Long> visitedNodeIds = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.offer(nodeId);
        visitedNodeIds.add(nodeId);

        int currentDepth = 0;
        while (!queue.isEmpty() && currentDepth < depth) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                Long currentNodeId = queue.poll();

                // 获取当前节点的所有相邻节点
                List<KnowledgeRelationVO> relations = relationMapper.selectByNodeId(currentNodeId);
                for (KnowledgeRelationVO relation : relations) {
                    Long neighborId = relation.getSourceNodeId().equals(currentNodeId)
                            ? relation.getTargetNodeId() : relation.getSourceNodeId();
                    if (!visitedNodeIds.contains(neighborId)) {
                        visitedNodeIds.add(neighborId);
                        queue.offer(neighborId);
                    }
                }
            }
            currentDepth++;
        }

        // 查询所有相关节点
        List<KnowledgeNode> nodes = new ArrayList<>();
        for (Long id : visitedNodeIds) {
            KnowledgeNode node = nodeMapper.selectById(id);
            if (node != null) {
                nodes.add(node);
            }
        }

        // 查询相关关系
        List<KnowledgeRelation> relations = relationMapper.selectList(null);
        relations = relations.stream()
                .filter(r -> visitedNodeIds.contains(r.getSourceNodeId())
                        && visitedNodeIds.contains(r.getTargetNodeId()))
                .collect(Collectors.toList());

        return buildGraphVO(nodes, relations);
    }

    /**
     * 根据关键词搜索图谱数据
     */
    @Override
    public GraphVO searchGraph(String keyword, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 50;
        }

        // 搜索匹配的节点
        List<KnowledgeNode> nodes = nodeMapper.selectByNameLike(keyword);
        if (nodes.size() > limit) {
            nodes = nodes.subList(0, limit);
        }

        // 获取节点ID集合
        Set<Long> nodeIds = nodes.stream().map(KnowledgeNode::getId).collect(Collectors.toSet());

        // 扩展：获取匹配节点的一度关联节点
        Set<Long> expandedNodeIds = new HashSet<>(nodeIds);
        for (Long nodeId : nodeIds) {
            List<KnowledgeNodeVO> neighbors = nodeService.getNeighborNodes(nodeId);
            for (KnowledgeNodeVO neighbor : neighbors) {
                expandedNodeIds.add(neighbor.getId());
            }
        }

        // 查询扩展后的所有节点
        List<KnowledgeNode> expandedNodes = new ArrayList<>();
        for (Long id : expandedNodeIds) {
            KnowledgeNode node = nodeMapper.selectById(id);
            if (node != null) {
                expandedNodes.add(node);
            }
        }

        // 查询关系
        List<KnowledgeRelation> relations = relationMapper.selectList(null);
        relations = relations.stream()
                .filter(r -> expandedNodeIds.contains(r.getSourceNodeId())
                        && expandedNodeIds.contains(r.getTargetNodeId()))
                .collect(Collectors.toList());

        return buildGraphVO(expandedNodes, relations);
    }

    /**
     * 获取两个节点之间的路径图
     */
    @Override
    public GraphVO getPathGraph(Long startNodeId, Long endNodeId) {
        // 获取最短路径上的节点
        List<KnowledgeNodeVO> pathNodes = nodeService.findShortestPath(startNodeId, endNodeId);

        if (pathNodes.isEmpty()) {
            return new GraphVO();
        }

        // 获取节点ID
        Set<Long> nodeIds = pathNodes.stream().map(KnowledgeNodeVO::getId).collect(Collectors.toSet());

        // 查询节点
        List<KnowledgeNode> nodes = new ArrayList<>();
        for (Long id : nodeIds) {
            KnowledgeNode node = nodeMapper.selectById(id);
            if (node != null) {
                nodes.add(node);
            }
        }

        // 查询路径上的关系
        List<KnowledgeRelation> relations = relationMapper.selectList(null);
        relations = relations.stream()
                .filter(r -> nodeIds.contains(r.getSourceNodeId()) && nodeIds.contains(r.getTargetNodeId()))
                .collect(Collectors.toList());

        return buildGraphVO(nodes, relations);
    }

    /**
     * 构建图谱可视化数据对象
     *
     * @param nodes     节点列表
     * @param relations 关系列表
     * @return GraphVO对象
     */
    private GraphVO buildGraphVO(List<KnowledgeNode> nodes, List<KnowledgeRelation> relations) {
        GraphVO graphVO = new GraphVO();

        // 收集所有节点类型
        List<String> categories = nodes.stream()
                .map(KnowledgeNode::getNodeType)
                .distinct()
                .toList();
        graphVO.setCategories(categories);

        // 创建类型到索引的映射
        Map<String, Integer> categoryIndexMap = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            categoryIndexMap.put(categories.get(i), i);
        }

        // 转换节点
        List<GraphVO.GraphNode> graphNodes = new ArrayList<>();
        Map<Long, Integer> nodeRelationCount = new HashMap<>();

        // 统计每个节点的关系数量（用于计算节点大小）
        for (KnowledgeRelation relation : relations) {
            nodeRelationCount.merge(relation.getSourceNodeId(), 1, Integer::sum);
            nodeRelationCount.merge(relation.getTargetNodeId(), 1, Integer::sum);
        }

        for (KnowledgeNode node : nodes) {
            GraphVO.GraphNode graphNode = new GraphVO.GraphNode();
            graphNode.setId(String.valueOf(node.getId()));
            graphNode.setName(node.getName());
            graphNode.setNodeType(node.getNodeType());
            graphNode.setCategory(categoryIndexMap.getOrDefault(node.getNodeType(), 0));
            graphNode.setDescription(node.getDescription());

            // 根据关系数量计算节点大小
            int relationCount = nodeRelationCount.getOrDefault(node.getId(), 0);
            graphNode.setSymbolSize(Math.max(20, Math.min(60, 20 + relationCount * 5)));
            graphNode.setValue((double) relationCount);

            graphNodes.add(graphNode);
        }
        graphVO.setNodes(graphNodes);

        // 转换关系
        List<GraphVO.GraphLink> graphLinks = new ArrayList<>();
        for (KnowledgeRelation relation : relations) {
            GraphVO.GraphLink link = new GraphVO.GraphLink();
            link.setId(String.valueOf(relation.getId()));
            link.setSource(String.valueOf(relation.getSourceNodeId()));
            link.setTarget(String.valueOf(relation.getTargetNodeId()));
            link.setName(relation.getName());
            link.setRelationType(relation.getRelationType());
            link.setWeight(relation.getWeight());

            graphLinks.add(link);
        }
        graphVO.setLinks(graphLinks);

        return graphVO;
    }
}
