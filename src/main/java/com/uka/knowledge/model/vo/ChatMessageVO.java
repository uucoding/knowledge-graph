package com.uka.knowledge.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息视图对象
 * <p>
 * 用于返回给前端的消息数据
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "聊天消息VO")
public class ChatMessageVO {

    /**
     * 消息ID
     */
    @Schema(description = "消息ID")
    private Long id;

    /**
     * 会话ID
     */
    @Schema(description = "会话ID")
    private Long sessionId;

    /**
     * 角色（user/assistant/system）
     */
    @Schema(description = "角色")
    private String role;

    /**
     * 消息内容
     */
    @Schema(description = "消息内容")
    private String content;

    /**
     * 思考链内容
     */
    @Schema(description = "思考链内容")
    private String thinkingContent;

    /**
     * 附件列表
     */
    @Schema(description = "附件列表")
    private List<ChatAttachmentVO> attachments;

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

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
