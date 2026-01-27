package com.uka.knowledge.common;

import lombok.Getter;

/**
 * 响应码枚举
 * <p>
 * 定义系统中所有的响应码和对应的消息
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Getter
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 服务器内部错误
     */
    ERROR(500, "服务器内部错误"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源未找到
     */
    NOT_FOUND(404, "资源未找到"),

    /**
     * 文件类型不支持
     */
    FILE_TYPE_NOT_SUPPORT(1001, "文件类型不支持"),

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(1002, "文件上传失败"),

    /**
     * 文件解析失败
     */
    FILE_PARSE_FAILED(1003, "文件解析失败"),

    /**
     * OCR识别失败
     */
    OCR_FAILED(1004, "OCR识别失败"),

    /**
     * 节点不存在
     */
    NODE_NOT_FOUND(2001, "知识节点不存在"),

    /**
     * 节点已存在
     */
    NODE_ALREADY_EXISTS(2002, "知识节点已存在"),

    /**
     * 关系不存在
     */
    RELATION_NOT_FOUND(2003, "知识关系不存在"),

    /**
     * 关系已存在
     */
    RELATION_ALREADY_EXISTS(2004, "知识关系已存在"),

    /**
     * Neo4j操作失败
     */
    NEO4J_ERROR(3001, "图数据库操作失败"),

    /**
     * Milvus操作失败
     */
    MILVUS_ERROR(3002, "向量数据库操作失败"),

    /**
     * Ollama调用失败
     */
    OLLAMA_ERROR(3003, "大模型调用失败"),

    /**
     * 知识抽取失败
     */
    EXTRACT_FAILED(4001, "知识抽取失败");

    /**
     * 响应码
     */
    private final Integer code;

    /**
     * 响应消息
     */
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
