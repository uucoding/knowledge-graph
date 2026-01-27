package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 聊天会话Mapper接口
 * <p>
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 并扩展自定义查询方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    /**
     * 获取会话列表，按最后消息时间倒序
     *
     * @return 会话列表
     */
    @Select("SELECT * FROM kg_chat_session WHERE deleted = 0 ORDER BY COALESCE(last_message_time, create_time) DESC")
    List<ChatSession> selectAllOrderByLastMessageTime();

    /**
     * 更新会话消息统计
     *
     * @param sessionId 会话ID
     */
    @Update("UPDATE kg_chat_session SET message_count = message_count + 1, last_message_time = NOW(), update_time = NOW() WHERE id = #{sessionId}")
    void incrementMessageCount(@Param("sessionId") Long sessionId);
}
