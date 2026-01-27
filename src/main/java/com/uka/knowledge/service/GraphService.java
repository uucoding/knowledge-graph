package com.uka.knowledge.service;

import com.uka.knowledge.model.vo.GraphVO;

/**
 * 图谱服务接口
 * <p>
 * 定义图谱可视化数据查询等业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface GraphService {

    /**
     * 获取完整图谱数据（限制数量）
     *
     * @param limit 返回节点数量限制
     * @return 图谱可视化数据
     */
    GraphVO getGraphData(Integer limit);

    /**
     * 获取指定节点的子图
     *
     * @param nodeId 中心节点ID
     * @param depth  扩展深度
     * @return 子图数据
     */
    GraphVO getSubGraph(Long nodeId, Integer depth);

    /**
     * 根据关键词搜索图谱数据
     *
     * @param keyword 搜索关键词
     * @param limit   返回数量限制
     * @return 匹配的图谱数据
     */
    GraphVO searchGraph(String keyword, Integer limit);

    /**
     * 获取两个节点之间的路径图
     *
     * @param startNodeId 起始节点ID
     * @param endNodeId   目标节点ID
     * @return 路径图数据
     */
    GraphVO getPathGraph(Long startNodeId, Long endNodeId);
}
