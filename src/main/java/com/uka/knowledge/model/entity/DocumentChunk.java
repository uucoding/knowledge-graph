package com.uka.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分块实体
 * <p>
 * 用于存储文档按页/段落拆分后的内容，支持页级别的RAG检索
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@TableName("kg_document_chunk")
public class DocumentChunk {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联的文档ID
     */
    private Long documentId;

    /**
     * 页码（从1开始，0表示无页码概念）
     */
    private Integer pageNum;

    /**
     * 分块序号（同一页内的顺序）
     */
    private Integer chunkIndex;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 向量ID（Milvus中的ID）
     */
    private String vectorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    private Integer deleted;
}
