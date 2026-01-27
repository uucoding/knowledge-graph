package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.ChatAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天附件Mapper接口
 * <p>
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * 并扩展自定义查询方法
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface ChatAttachmentMapper extends BaseMapper<ChatAttachment> {

    /**
     * 根据消息ID获取附件列表
     *
     * @param messageId 消息ID
     * @return 附件列表
     */
    @Select("SELECT * FROM kg_chat_attachment WHERE message_id = #{messageId}")
    List<ChatAttachment> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * 根据ID列表获取附件
     *
     * @param ids 附件ID列表
     * @return 附件列表
     */
    @Select("<script>SELECT * FROM kg_chat_attachment WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<ChatAttachment> selectByIds(@Param("ids") List<Long> ids);
}
