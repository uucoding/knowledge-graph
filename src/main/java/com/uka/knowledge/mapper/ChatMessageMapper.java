package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息Mapper接口
 * <p>
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 并扩展自定义查询方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 根据会话ID获取消息列表
     *
     * @param sessionId 会话ID
     * @return 消息列表（按时间正序）
     */
    @Select("SELECT * FROM kg_chat_message WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY create_time ASC")
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 获取会话最近N条消息（用于上下文）
     *
     * @param sessionId 会话ID
     * @param limit 数量限制
     * @return 消息列表
     */
    @Select("SELECT * FROM kg_chat_message WHERE session_id = #{sessionId} AND deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatMessage> selectRecentMessages(@Param("sessionId") Long sessionId, @Param("limit") int limit);

    /**
     * 统计会话消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    @Select("SELECT COUNT(*) FROM kg_chat_message WHERE session_id = #{sessionId} AND deleted = 0")
    int countBySessionId(@Param("sessionId") Long sessionId);
}
