package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天附件视图对象
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "聊天附件VO")
public class ChatAttachmentVO {

    /**
     * 附件ID
     */
    @Schema(description = "附件ID")
    private Long id;

    /**
     * 文件名
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 文件路径
     */
    @Schema(description = "文件路径")
    private String filePath;

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
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
