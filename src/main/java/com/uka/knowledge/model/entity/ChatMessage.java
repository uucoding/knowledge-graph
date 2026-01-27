package com.uka.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 * <p>
 * 存储AI对话消息记录
 * 对应数据库表：kg_chat_message
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@TableName("kg_chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 角色（user/assistant/system）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 思考链内容（用于展示AI的思考过程）
     */
    private String thinkingContent;

    /**
     * 附件信息（JSON格式）
     */
    private String attachments;

    /**
     * RAG上下文（JSON格式，包含关联的文档和节点）
     */
    private String ragContext;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标识（0-未删除, 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 角色常量
     */
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";
    public static final String ROLE_SYSTEM = "system";
}
