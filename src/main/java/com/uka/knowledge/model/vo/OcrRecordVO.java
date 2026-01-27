package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OCR记录视图对象
 * <p>
 * 用于返回给前端的OCR识别结果数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "OCR记录VO")
public class OcrRecordVO {

    /**
     * 记录ID
     */
    @Schema(description = "记录ID")
    private Long id;

    /**
     * 图片名称
     */
    @Schema(description = "图片名称")
    private String imageName;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 图片类型
     */
    @Schema(description = "图片类型")
    private String imageType;

    /**
     * 图片大小
     */
    @Schema(description = "图片大小（字节）")
    private Long imageSize;

    /**
     * 图片大小（可读格式）
     */
    @Schema(description = "图片大小（可读格式）")
    private String imageSizeReadable;

    /**
     * 图片访问URL
     */
    @Schema(description = "图片访问URL")
    private String imageUrl;

    /**
     * OCR识别文本
     */
    @Schema(description = "OCR识别文本")
    private String ocrText;

    /**
     * 识别置信度
     */
    @Schema(description = "识别置信度")
    private Double confidence;

    /**
     * 识别语言
     */
    @Schema(description = "识别语言")
    private String language;

    /**
     * 处理状态
     */
    @Schema(description = "处理状态（0-待处理, 1-处理中, 2-已完成, 3-处理失败）")
    private Integer status;

    /**
     * 状态描述
     */
    @Schema(description = "状态描述")
    private String statusDesc;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMsg;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 获取状态描述
     */
    public String getStatusDesc() {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "处理中";
            case 2 -> "已完成";
            case 3 -> "处理失败";
            default -> "未知";
        };
    }
}
