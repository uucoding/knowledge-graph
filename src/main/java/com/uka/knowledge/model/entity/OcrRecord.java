package com.uka.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OCR记录实体类
 * <p>
 * 存储图片OCR识别的元数据和结果
 * 对应数据库表：kg_ocr_record
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Data
@TableName("kg_ocr_record")
public class OcrRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片名称
     */
    private String imageName;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 图片路径
     */
    private String imagePath;

    /**
     * 图片类型（png/jpg/jpeg等）
     */
    private String imageType;

    /**
     * 图片大小（字节）
     */
    private Long imageSize;

    /**
     * OCR识别结果文本
     */
    private String ocrText;

    /**
     * 识别置信度
     */
    private Double confidence;

    /**
     * 识别语言
     */
    private String language;

    /**
     * 处理状态（0-待处理, 1-处理中, 2-已完成, 3-处理失败）
     */
    private Integer status;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 向量ID
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
     * 逻辑删除标识
     */
    @TableLogic
    private Integer deleted;

    /**
     * 状态常量
     */
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_PROCESSING = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_FAILED = 3;
}
