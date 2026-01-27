package com.uka.knowledge.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 聊天发送消息请求DTO
 * <p>
 * 用于接收前端发送的聊天消息
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "发送消息请求")
public class ChatSendRequest {

    /**
     * 用户消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息内容", required = true, example = "什么是知识图谱？")
    private String message;

    /**
     * 是否启用RAG
     */
    @Schema(description = "是否启用RAG检索增强", example = "true")
    private Boolean enableRag = true;

    /**
     * 附件ID列表
     */
    @Schema(description = "附件ID列表")
    private List<Long> attachmentIds;
}
