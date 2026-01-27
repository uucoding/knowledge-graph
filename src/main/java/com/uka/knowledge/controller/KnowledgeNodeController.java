package com.uka.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.dto.KnowledgeNodeDTO;
import com.uka.knowledge.model.entity.KnowledgeNode;
import com.uka.knowledge.model.vo.KnowledgeNodeVO;
import com.uka.knowledge.service.KnowledgeNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识节点控制器
 * <p>
 * 提供知识节点的增删改查接口
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "知识节点", description = "知识节点的增删改查接口")
@RestController
@RequestMapping("/api/node")
@RequiredArgsConstructor
public class KnowledgeNodeController {

    private final KnowledgeNodeService nodeService;

    /**
     * 创建知识节点
     *
     * @param dto 节点信息
     * @return 创建的节点
     */
    @Operation(summary = "创建节点", description = "创建新的知识节点")
    @PostMapping
    public Result<KnowledgeNode> create(@Valid @RequestBody KnowledgeNodeDTO dto) {
        KnowledgeNode node = nodeService.createNode(dto);
        return Result.success(node, "节点创建成功");
    }

    /**
     * 更新知识节点
     *
     * @param id  节点ID
     * @param dto 节点信息
     * @return 更新后的节点
     */
    @Operation(summary = "更新节点", description = "更新已有的知识节点")
    @PutMapping("/{id}")
    public Result<KnowledgeNode> update(
            @Parameter(description = "节点ID", required = true) @PathVariable Long id,
            @Valid @RequestBody KnowledgeNodeDTO dto) {
        dto.setId(id);
        KnowledgeNode node = nodeService.updateNode(dto);
        return Result.success(node, "节点更新成功");
    }

    /**
     * 删除知识节点
     *
     * @param id 节点ID
     * @return 操作结果
     */
    @Operation(summary = "删除节点", description = "删除知识节点及其关联关系")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "节点ID", required = true) @PathVariable Long id) {
        nodeService.deleteNode(id);
        return Result.success(null, "节点删除成功");
    }

    /**
     * 获取节点详情
     *
     * @param id 节点ID
     * @return 节点详情
     */
    @Operation(summary = "节点详情", description = "获取知识节点详细信息")
    @GetMapping("/{id}")
    public Result<KnowledgeNodeVO> detail(
            @Parameter(description = "节点ID", required = true) @PathVariable Long id) {
        KnowledgeNodeVO node = nodeService.getNodeDetail(id);
        return Result.success(node);
    }

    /**
     * 分页查询节点列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词
     * @param nodeType 节点类型
     * @return 分页结果
     */
    @Operation(summary = "节点列表", description = "分页查询知识节点列表")
    @GetMapping("/list")
    public Result<Page<KnowledgeNodeVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "节点类型") @RequestParam(required = false) String nodeType) {
        Page<KnowledgeNodeVO> page = nodeService.pageNodes(pageNum, pageSize, keyword, nodeType);
        return Result.success(page);
    }

    /**
     * 搜索节点
     *
     * @param name 节点名称关键词
     * @return 匹配的节点列表
     */
    @Operation(summary = "搜索节点", description = "根据名称搜索知识节点")
    @GetMapping("/search")
    public Result<List<KnowledgeNodeVO>> search(
            @Parameter(description = "节点名称关键词", required = true)
            @RequestParam String name) {
        List<KnowledgeNodeVO> nodes = nodeService.searchByName(name);
        return Result.success(nodes);
    }

    /**
     * 获取所有节点类型
     *
     * @return 节点类型列表
     */
    @Operation(summary = "节点类型列表", description = "获取所有节点类型")
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        List<String> types = nodeService.getAllNodeTypes();
        return Result.success(types);
    }

    /**
     * 统计各类型节点数量
     *
     * @return 统计结果
     */
    @Operation(summary = "节点统计", description = "统计各类型节点数量")
    @GetMapping("/statistics")
    public Result<List<Map<String, Object>>> statistics() {
        List<Map<String, Object>> stats = nodeService.countByNodeType();
        return Result.success(stats);
    }

    /**
     * 获取节点的相邻节点
     *
     * @param id 节点ID
     * @return 相邻节点列表
     */
    @Operation(summary = "相邻节点", description = "获取指定节点的相邻节点")
    @GetMapping("/{id}/neighbors")
    public Result<List<KnowledgeNodeVO>> getNeighbors(
            @Parameter(description = "节点ID", required = true) @PathVariable Long id) {
        List<KnowledgeNodeVO> neighbors = nodeService.getNeighborNodes(id);
        return Result.success(neighbors);
    }

    /**
     * 查询两个节点之间的最短路径
     *
     * @param startId 起始节点ID
     * @param endId   目标节点ID
     * @return 路径上的节点列表
     */
    @Operation(summary = "最短路径", description = "查询两个节点之间的最短路径")
    @GetMapping("/path")
    public Result<List<KnowledgeNodeVO>> findPath(
            @Parameter(description = "起始节点ID", required = true) @RequestParam Long startId,
            @Parameter(description = "目标节点ID", required = true) @RequestParam Long endId) {
        List<KnowledgeNodeVO> path = nodeService.findShortestPath(startId, endId);
        return Result.success(path);
    }
}
