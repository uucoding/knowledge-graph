package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 聊天发送响应对象
 * <p>
 * 包含AI回复和RAG检索结果
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "发送消息响应")
public class ChatSendResponse {

    /**
     * 用户消息
     */
    @Schema(description = "用户消息")
    private ChatMessageVO userMessage;

    /**
     * AI回复消息
     */
    @Schema(description = "AI回复消息")
    private ChatMessageVO assistantMessage;

    /**
     * 关联文档列表
     */
    @Schema(description = "关联文档列表")
    private List<RagDocument> ragDocuments;

    /**
     * 关联图谱节点列表
     */
    @Schema(description = "关联图谱节点列表")
    private List<RagNode> ragNodes;
}
