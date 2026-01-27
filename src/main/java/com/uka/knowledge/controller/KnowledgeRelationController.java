package com.uka.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.dto.KnowledgeRelationDTO;
import com.uka.knowledge.model.entity.KnowledgeRelation;
import com.uka.knowledge.model.vo.KnowledgeRelationVO;
import com.uka.knowledge.service.KnowledgeRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识关系控制器
 * <p>
 * 提供知识关系的增删改查接口
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "知识关系", description = "知识关系的增删改查接口")
@RestController
@RequestMapping("/api/relation")
@RequiredArgsConstructor
public class KnowledgeRelationController {

    private final KnowledgeRelationService relationService;

    /**
     * 创建知识关系
     *
     * @param dto 关系信息
     * @return 创建的关系
     */
    @Operation(summary = "创建关系", description = "创建两个节点之间的关系")
    @PostMapping
    public Result<KnowledgeRelation> create(@Valid @RequestBody KnowledgeRelationDTO dto) {
        KnowledgeRelation relation = relationService.createRelation(dto);
        return Result.success(relation, "关系创建成功");
    }

    /**
     * 更新知识关系
     *
     * @param id  关系ID
     * @param dto 关系信息
     * @return 更新后的关系
     */
    @Operation(summary = "更新关系", description = "更新已有的知识关系")
    @PutMapping("/{id}")
    public Result<KnowledgeRelation> update(
            @Parameter(description = "关系ID", required = true) @PathVariable Long id,
            @Valid @RequestBody KnowledgeRelationDTO dto) {
        dto.setId(id);
        KnowledgeRelation relation = relationService.updateRelation(dto);
        return Result.success(relation, "关系更新成功");
    }

    /**
     * 删除知识关系
     *
     * @param id 关系ID
     * @return 操作结果
     */
    @Operation(summary = "删除关系", description = "删除知识关系")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "关系ID", required = true) @PathVariable Long id) {
        relationService.deleteRelation(id);
        return Result.success(null, "关系删除成功");
    }

    /**
     * 获取关系详情
     *
     * @param id 关系ID
     * @return 关系详情
     */
    @Operation(summary = "关系详情", description = "获取知识关系详细信息")
    @GetMapping("/{id}")
    public Result<KnowledgeRelationVO> detail(
            @Parameter(description = "关系ID", required = true) @PathVariable Long id) {
        KnowledgeRelationVO relation = relationService.getRelationDetail(id);
        return Result.success(relation);
    }

    /**
     * 分页查询关系列表
     *
     * @param pageNum      页码
     * @param pageSize     每页数量
     * @param relationType 关系类型
     * @param keyword      关系名称关键词
     * @return 分页结果
     */
    @Operation(summary = "关系列表", description = "分页查询知识关系列表")
    @GetMapping("/list")
    public Result<Page<KnowledgeRelationVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "关系类型") @RequestParam(required = false) String relationType,
            @Parameter(description = "关系名称关键词") @RequestParam(required = false) String keyword) {
        Page<KnowledgeRelationVO> page = relationService.pageRelations(pageNum, pageSize, relationType, keyword);
        return Result.success(page);
    }

    /**
     * 查询指定节点的所有关系
     *
     * @param nodeId 节点ID
     * @return 关系列表
     */
    @Operation(summary = "节点关系", description = "查询指定节点的所有关系")
    @GetMapping("/node/{nodeId}")
    public Result<List<KnowledgeRelationVO>> getByNodeId(
            @Parameter(description = "节点ID", required = true) @PathVariable Long nodeId) {
        List<KnowledgeRelationVO> relations = relationService.getRelationsByNodeId(nodeId);
        return Result.success(relations);
    }

    /**
     * 获取所有关系类型
     *
     * @return 关系类型列表
     */
    @Operation(summary = "关系类型列表", description = "获取所有关系类型")
    @GetMapping("/types")
    public Result<List<String>> getTypes() {
        List<String> types = relationService.getAllRelationTypes();
        return Result.success(types);
    }

    /**
     * 检查关系是否存在
     *
     * @param sourceNodeId 起始节点ID
     * @param targetNodeId 目标节点ID
     * @param relationType 关系类型（可选）
     * @return 是否存在
     */
    @Operation(summary = "检查关系", description = "检查两个节点之间是否存在关系")
    @GetMapping("/exists")
    public Result<Boolean> exists(
            @Parameter(description = "起始节点ID", required = true) @RequestParam Long sourceNodeId,
            @Parameter(description = "目标节点ID", required = true) @RequestParam Long targetNodeId,
            @Parameter(description = "关系类型") @RequestParam(required = false) String relationType) {
        boolean exists = relationService.existsRelation(sourceNodeId, targetNodeId, relationType);
        return Result.success(exists);
    }
}
