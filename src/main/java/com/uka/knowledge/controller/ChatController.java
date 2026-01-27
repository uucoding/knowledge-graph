package com.uka.knowledge.controller;

import com.uka.knowledge.common.Result;
import com.uka.knowledge.model.dto.ChatSendRequest;
import com.uka.knowledge.model.vo.ChatAttachmentVO;
import com.uka.knowledge.model.vo.ChatMessageVO;
import com.uka.knowledge.model.vo.ChatSendResponse;
import com.uka.knowledge.model.vo.ChatSessionVO;
import com.uka.knowledge.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天控制器
 * <p>
 * 提供AI对话相关的接口
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Tag(name = "AI对话", description = "AI对话相关接口，支持RAG增强")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 创建会话
     *
     * @return 新创建的会话
     */
    @Operation(summary = "创建会话", description = "创建新的对话会话")
    @PostMapping("/session")
    public Result<ChatSessionVO> createSession() {
        ChatSessionVO session = chatService.createSession();
        return Result.success(session, "会话创建成功");
    }

    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    @Operation(summary = "会话列表", description = "获取所有对话会话")
    @GetMapping("/session/list")
    public Result<List<ChatSessionVO>> listSessions() {
        List<ChatSessionVO> sessions = chatService.listSessions();
        return Result.success(sessions);
    }

    /**
     * 删除会话
     *
     * @param id 会话ID
     * @return 操作结果
     */
    @Operation(summary = "删除会话", description = "删除指定的对话会话")
    @DeleteMapping("/session/{id}")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID", required = true) @PathVariable Long id) {
        chatService.deleteSession(id);
        return Result.success(null, "会话删除成功");
    }

    /**
     * 更新会话标题
     *
     * @param id 会话ID
     * @param title 新标题
     * @return 更新后的会话
     */
    @Operation(summary = "更新会话标题", description = "更新对话会话的标题")
    @PutMapping("/session/{id}/title")
    public Result<ChatSessionVO> updateSessionTitle(
            @Parameter(description = "会话ID", required = true) @PathVariable Long id,
            @Parameter(description = "新标题", required = true) @RequestParam String title) {
        ChatSessionVO session = chatService.updateSessionTitle(id, title);
        return Result.success(session, "标题更新成功");
    }

    /**
     * 发送消息
     *
     * @param sessionId 会话ID
     * @param request 发送消息请求
     * @return 发送响应（包含AI回复和RAG结果）
     */
    @Operation(summary = "发送消息", description = "发送消息并获取AI回复，支持RAG增强")
    @PostMapping("/{sessionId}/send")
    public Result<ChatSendResponse> sendMessage(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId,
            @Valid @RequestBody ChatSendRequest request) {
        ChatSendResponse response = chatService.sendMessage(sessionId, request);
        return Result.success(response);
    }

    /**
     * 流式发送消息（SSE）
     *
     * @param sessionId 会话ID
     * @param request 发送消息请求
     * @return SSE流
     */
    @Operation(summary = "流式发送消息", description = "流式发送消息并获取AI回复，支持RAG增强，使用SSE协议")
    @PostMapping(value = "/{sessionId}/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId,
            @Valid @RequestBody ChatSendRequest request) {
        return chatService.sendMessageStream(sessionId, request);
    }

    /**
     * 获取消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    @Operation(summary = "消息历史", description = "获取会话的消息历史")
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageVO>> getMessages(
            @Parameter(description = "会话ID", required = true) @PathVariable Long sessionId) {
        List<ChatMessageVO> messages = chatService.getMessages(sessionId);
        return Result.success(messages);
    }

    /**
     * 上传附件
     *
     * @param file 文件
     * @return 附件信息
     */
    @Operation(summary = "上传附件", description = "上传聊天附件，支持PDF、Word、TXT等格式")
    @PostMapping("/upload")
    public Result<ChatAttachmentVO> uploadAttachment(
            @Parameter(description = "文件", required = true) @RequestParam("file") MultipartFile file) {
        ChatAttachmentVO attachment = chatService.uploadAttachment(file);
        return Result.success(attachment, "附件上传成功");
    }
}
