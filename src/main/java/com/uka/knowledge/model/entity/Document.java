package com.uka.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文档实体类
 * <p>
 * 存储上传文档的元数据信息
 * 对应数据库表：kg_document
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@TableName("kg_document")
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID（雪花算法生成）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 文档名称
     */
    private String name;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件类型（pdf/doc/docx/txt/md等）
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文档内容（解析后的文本）
     */
    private String content;

    /**
     * 内容摘要
     */
    private String summary;

    /**
     * 文档状态（0-待处理, 1-处理中, 2-已完成, 3-处理失败）
     */
    private Integer status;

    /**
     * 处理错误信息
     */
    private String errorMsg;

    /**
     * 向量ID（Milvus中的向量标识）
     */
    private String vectorId;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识（0-未删除, 1-已删除）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 文档状态常量
     */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_FAILED = 3;
}
