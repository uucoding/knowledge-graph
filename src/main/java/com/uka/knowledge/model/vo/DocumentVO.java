package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档视图对象
 * <p>
 * 用于返回给前端的文档数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "文档VO")
public class DocumentVO {

    /**
     * 文档ID
     */
    @Schema(description = "文档ID")
    private Long id;

    /**
     * 文档名称
     */
    @Schema(description = "文档名称")
    private String name;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 文件类型
     */
    @Schema(description = "文件类型")
    private String fileType;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    /**
     * 文件大小（可读格式）
     */
    @Schema(description = "文件大小（可读格式）")
    private String fileSizeReadable;

    /**
     * 文档内容预览
     */
    @Schema(description = "文档内容预览")
    private String contentPreview;

    /**
     * 内容摘要
     */
    @Schema(description = "内容摘要")
    private String summary;

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
     * 抽取的节点数量
     */
    @Schema(description = "抽取的节点数量")
    private Integer nodeCount;

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
