package com.uka.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.entity.Document;
import com.uka.knowledge.model.vo.DocumentVO;
import com.uka.knowledge.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理控制器
 * <p>
 * 提供文档上传、解析、查询等接口
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "文档管理", description = "文档上传、解析、查询等接口")
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 上传并解析文档
     *
     * @param file 上传的文件
     * @return 文档信息
     */
    @Operation(summary = "上传文档", description = "上传文档并自动解析，支持PDF、Word、TXT、Markdown格式")
    @PostMapping("/upload")
    public Result<Document> upload(
            @Parameter(description = "文档文件", required = true)
            @RequestParam("file") MultipartFile file) {
        Document document = documentService.uploadAndParse(file);
        return Result.success(document, "文档上传成功，正在后台解析");
    }

    /**
     * 分页查询文档列表
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param keyword  搜索关键词
     * @param fileType 文件类型
     * @param status   处理状态
     * @return 分页结果
     */
    @Operation(summary = "文档列表", description = "分页查询文档列表")
    @GetMapping("/list")
    public Result<Page<DocumentVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "文件类型") @RequestParam(required = false) String fileType,
            @Parameter(description = "处理状态") @RequestParam(required = false) Integer status) {
        Page<DocumentVO> page = documentService.pageDocuments(pageNum, pageSize, keyword, fileType, status);
        return Result.success(page);
    }

    /**
     * 获取文档详情
     *
     * @param id 文档ID
     * @return 文档详情
     */
    @Operation(summary = "文档详情", description = "获取文档详细信息")
    @GetMapping("/{id}")
    public Result<DocumentVO> detail(
            @Parameter(description = "文档ID", required = true) @PathVariable Long id) {
        DocumentVO document = documentService.getDocumentDetail(id);
        return Result.success(document);
    }

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 操作结果
     */
    @Operation(summary = "删除文档", description = "删除指定文档及相关数据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "文档ID", required = true) @PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 重新解析文档
     *
     * @param id 文档ID
     * @return 文档信息
     */
    @Operation(summary = "重新解析", description = "重新解析指定文档")
    @PostMapping("/{id}/reparse")
    public Result<Document> reparse(
            @Parameter(description = "文档ID", required = true) @PathVariable Long id) {
        Document document = documentService.reparseDocument(id);
        return Result.success(document, "正在重新解析");
    }

    /**
     * 生成文档摘要
     *
     * @param id 文档ID
     * @return 摘要内容
     */
    @Operation(summary = "生成摘要", description = "使用AI生成文档摘要")
    @PostMapping("/{id}/summary")
    public Result<String> generateSummary(
            @Parameter(description = "文档ID", required = true) @PathVariable Long id) {
        String summary = documentService.generateSummary(id);
        return Result.success(summary);
    }
}
