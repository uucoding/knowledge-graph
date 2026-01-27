package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天会话视图对象
 * <p>
 * 用于返回给前端的会话数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "聊天会话VO")
public class ChatSessionVO {

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private Long id;

    /**
     * 会话标题
     */
    @Schema(description = "会话标题")
    private String title;

    /**
     * 消息数量
     */
    @Schema(description = "消息数量")
    private Integer messageCount;

    /**
     * 最后消息时间
     */
    @Schema(description = "最后消息时间")
    private LocalDateTime lastMessageTime;

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
}
