package com.uka.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uka.knowledge.model.dto.ChatSendRequest;
import com.uka.knowledge.model.entity.ChatAttachment;
import com.uka.knowledge.model.entity.ChatSession;
import com.uka.knowledge.model.vo.ChatAttachmentVO;
import com.uka.knowledge.model.vo.ChatMessageVO;
import com.uka.knowledge.model.vo.ChatSendResponse;
import com.uka.knowledge.model.vo.ChatSessionVO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 聊天服务接口
 * <p>
 * 定义AI对话相关的业务方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public interface ChatService extends IService<ChatSession> {

    /**
     * 创建新会话
     *
     * @return 会话VO
     */
    ChatSessionVO createSession();

    /**
     * 获取会话列表
     *
     * @return 会话列表
     */
    List<ChatSessionVO> listSessions();

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @return 是否成功
     */
    boolean deleteSession(Long sessionId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title 新标题
     * @return 会话VO
     */
    ChatSessionVO updateSessionTitle(Long sessionId, String title);

    /**
     * 发送消息（带RAG增强）
     *
     * @param sessionId 会话ID
     * @param request 发送请求
     * @return 发送响应（包含AI回复和RAG结果）
     */
    ChatSendResponse sendMessage(Long sessionId, ChatSendRequest request);

    /**
     * 流式发送消息（带RAG增强）
     *
     * @param sessionId 会话ID
     * @param request 发送请求
     * @return SSE发射器
     */
    SseEmitter sendMessageStream(Long sessionId, ChatSendRequest request);

    /**
     * 获取会话消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessageVO> getMessages(Long sessionId);

    /**
     * 上传聊天附件
     *
     * @param file 文件
     * @return 附件VO
     */
    ChatAttachmentVO uploadAttachment(MultipartFile file);

    /**
     * 获取附件详情
     *
     * @param attachmentId 附件ID
     * @return 附件实体
     */
    ChatAttachment getAttachment(Long attachmentId);
}
