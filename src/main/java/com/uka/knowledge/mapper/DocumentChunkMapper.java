package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档分块Mapper接口
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    /**
     * 根据文档ID查询所有分块
     *
     * @param documentId 文档ID
     * @return 分块列表
     */
    @Select("SELECT * FROM kg_document_chunk WHERE document_id = #{documentId} AND deleted = 0 ORDER BY page_num, chunk_index")
    List<DocumentChunk> selectByDocumentId(@Param("documentId") Long documentId);

    /**
     * 根据分块ID查询
     *
     * @param id 分块ID
     * @return 分块信息
     */
    @Select("SELECT * FROM kg_document_chunk WHERE id = #{id} AND deleted = 0")
    DocumentChunk selectByChunkId(@Param("id") Long id);

    /**
     * 删除文档的所有分块
     *
     * @param documentId 文档ID
     * @return 影响行数
     */
    @org.apache.ibatis.annotations.Update("UPDATE kg_document_chunk SET deleted = 1 WHERE document_id = #{documentId}")
    int deleteByDocumentId(@Param("documentId") Long documentId);
}
