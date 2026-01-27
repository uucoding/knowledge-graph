package com.uka.knowledge.controller;

import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.vo.GraphVO;
import com.uka.knowledge.service.GraphService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 图谱可视化控制器
 * <p>
 * 提供图谱可视化数据查询接口
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "图谱可视化", description = "图谱可视化数据查询接口")
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class GraphController {

    private final GraphService graphService;

    /**
     * 获取完整图谱数据
     *
     * @param limit 返回节点数量限制
     * @return 图谱数据
     */
    @Operation(summary = "获取图谱数据", description = "获取完整图谱的可视化数据")
    @GetMapping
    public Result<GraphVO> getGraph(
            @Parameter(description = "节点数量限制")
            @RequestParam(defaultValue = "100") Integer limit) {
        GraphVO graph = graphService.getGraphData(limit);
        return Result.success(graph);
    }

    /**
     * 获取指定节点的子图
     *
     * @param nodeId 中心节点ID
     * @param depth  扩展深度
     * @return 子图数据
     */
    @Operation(summary = "获取子图", description = "获取以指定节点为中心的子图")
    @GetMapping("/subgraph/{nodeId}")
    public Result<GraphVO> getSubGraph(
            @Parameter(description = "中心节点ID", required = true) @PathVariable Long nodeId,
            @Parameter(description = "扩展深度") @RequestParam(defaultValue = "2") Integer depth) {
        GraphVO graph = graphService.getSubGraph(nodeId, depth);
        return Result.success(graph);
    }

    /**
     * 搜索图谱数据
     *
     * @param keyword 搜索关键词
     * @param limit   返回数量限制
     * @return 匹配的图谱数据
     */
    @Operation(summary = "搜索图谱", description = "根据关键词搜索图谱数据")
    @GetMapping("/search")
    public Result<GraphVO> searchGraph(
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "50") Integer limit) {
        GraphVO graph = graphService.searchGraph(keyword, limit);
        return Result.success(graph);
    }

    /**
     * 获取两个节点之间的路径图
     *
     * @param startNodeId 起始节点ID
     * @param endNodeId   目标节点ID
     * @return 路径图数据
     */
    @Operation(summary = "获取路径图", description = "获取两个节点之间最短路径的图谱数据")
    @GetMapping("/path")
    public Result<GraphVO> getPathGraph(
            @Parameter(description = "起始节点ID", required = true) @RequestParam Long startNodeId,
            @Parameter(description = "目标节点ID", required = true) @RequestParam Long endNodeId) {
        GraphVO graph = graphService.getPathGraph(startNodeId, endNodeId);
        return Result.success(graph);
    }
}
