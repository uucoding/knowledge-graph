package com.uka.knowledge.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.entity.OcrRecord;
import com.uka.knowledge.model.vo.OcrRecordVO;
import com.uka.knowledge.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR识别控制器
 * <p>
 * 提供图片OCR识别相关接口
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "OCR识别", description = "图片OCR识别相关接口")
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final OcrService ocrService;

    /**
     * 上传图片并进行OCR识别
     *
     * @param file 图片文件
     * @return OCR记录
     */
    @Operation(summary = "上传图片OCR", description = "上传图片并进行OCR文字识别")
    @PostMapping("/upload")
    public Result<OcrRecord> upload(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        OcrRecord record = ocrService.uploadAndRecognize(file);
        return Result.success(record, "图片上传成功，正在后台识别");
    }

    /**
     * 分页查询OCR记录
     *
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param keyword   搜索关键词
     * @param imageType 图片类型
     * @param status    处理状态
     * @return 分页结果
     */
    @Operation(summary = "OCR记录列表", description = "分页查询OCR识别记录")
    @GetMapping("/list")
    public Result<Page<OcrRecordVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "图片类型") @RequestParam(required = false) String imageType,
            @Parameter(description = "处理状态") @RequestParam(required = false) Integer status) {
        Page<OcrRecordVO> page = ocrService.pageRecords(pageNum, pageSize, keyword, imageType, status);
        return Result.success(page);
    }

    /**
     * 获取OCR记录详情
     *
     * @param id 记录ID
     * @return 记录详情
     */
    @Operation(summary = "OCR记录详情", description = "获取OCR识别记录详情")
    @GetMapping("/{id}")
    public Result<OcrRecordVO> detail(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id) {
        OcrRecordVO record = ocrService.getRecordDetail(id);
        return Result.success(record);
    }

    /**
     * 删除OCR记录
     *
     * @param id 记录ID
     * @return 操作结果
     */
    @Operation(summary = "删除记录", description = "删除OCR识别记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id) {
        ocrService.deleteRecord(id);
        return Result.success();
    }

    /**
     * 重新识别图片
     *
     * @param id 记录ID
     * @return OCR记录
     */
    @Operation(summary = "重新识别", description = "重新对图片进行OCR识别")
    @PostMapping("/{id}/rerecognize")
    public Result<OcrRecord> reRecognize(
            @Parameter(description = "记录ID", required = true) @PathVariable Long id) {
        OcrRecord record = ocrService.reRecognize(id);
        return Result.success(record, "正在重新识别");
    }
}
