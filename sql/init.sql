-- ============================================
-- 知识图谱系统 - MySQL数据库初始化脚本
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS knowledge_graph DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowledge_graph;

-- ============================================
-- 文档表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_document (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(255) NOT NULL COMMENT '文档名称',
    original_name VARCHAR(255) COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    content LONGTEXT COMMENT '文档内容',
    summary TEXT COMMENT '内容摘要',
    status TINYINT DEFAULT 0 COMMENT '状态（0-待处理, 1-处理中, 2-已完成, 3-处理失败）',
    error_msg VARCHAR(500) COMMENT '错误信息',
    vector_id VARCHAR(100) COMMENT '向量ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除, 1-已删除）',
    INDEX idx_name (name),
    INDEX idx_file_type (file_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ============================================
-- OCR记录表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_ocr_record (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    image_name VARCHAR(255) NOT NULL COMMENT '图片名称',
    original_name VARCHAR(255) COMMENT '原始文件名',
    image_path VARCHAR(500) NOT NULL COMMENT '图片路径',
    image_type VARCHAR(20) NOT NULL COMMENT '图片类型',
    image_size BIGINT DEFAULT 0 COMMENT '图片大小（字节）',
    ocr_text LONGTEXT COMMENT 'OCR识别文本',
    confidence DOUBLE COMMENT '识别置信度',
    language VARCHAR(50) COMMENT '识别语言',
    status TINYINT DEFAULT 0 COMMENT '状态（0-待处理, 1-处理中, 2-已完成, 3-处理失败）',
    error_msg VARCHAR(500) COMMENT '错误信息',
    vector_id VARCHAR(100) COMMENT '向量ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_image_name (image_name),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OCR记录表';

-- ============================================
-- 知识节点表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_knowledge_node (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '节点名称',
    node_type VARCHAR(50) NOT NULL COMMENT '节点类型',
    description TEXT COMMENT '节点描述',
    properties JSON COMMENT '节点属性（JSON）',
    neo4j_id VARCHAR(100) COMMENT 'Neo4j节点ID',
    vector_id VARCHAR(100) COMMENT '向量ID',
    source_doc_id BIGINT COMMENT '来源文档ID',
    source_type VARCHAR(20) DEFAULT 'manual' COMMENT '来源类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_name (name),
    INDEX idx_node_type (node_type),
    INDEX idx_source_doc_id (source_doc_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识节点表';

-- ============================================
-- 知识关系表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_knowledge_relation (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '关系名称',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型',
    source_node_id BIGINT NOT NULL COMMENT '起始节点ID',
    target_node_id BIGINT NOT NULL COMMENT '目标节点ID',
    weight DOUBLE DEFAULT 1.0 COMMENT '关系权重',
    properties JSON COMMENT '关系属性（JSON）',
    neo4j_rel_id VARCHAR(100) COMMENT 'Neo4j关系ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_source_node (source_node_id),
    INDEX idx_target_node (target_node_id),
    INDEX idx_relation_type (relation_type),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (source_node_id) REFERENCES kg_knowledge_node(id) ON DELETE CASCADE,
    FOREIGN KEY (target_node_id) REFERENCES kg_knowledge_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识关系表';

-- ============================================
-- 聊天会话表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_chat_session (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) DEFAULT '新对话' COMMENT '会话标题',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    last_message_time DATETIME COMMENT '最后消息时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除（0-未删除, 1-已删除）',
    INDEX idx_create_time (create_time),
    INDEX idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- ============================================
-- 聊天消息表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_chat_message (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色（user/assistant/system）',
    content TEXT NOT NULL COMMENT '消息内容',
    thinking_content TEXT COMMENT '思考链内容',
    attachments JSON COMMENT '附件信息',
    rag_context JSON COMMENT 'RAG上下文',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time),
    FOREIGN KEY (session_id) REFERENCES kg_chat_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- ============================================
-- 聊天附件表
-- ============================================
CREATE TABLE IF NOT EXISTS kg_chat_attachment (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    message_id BIGINT COMMENT '消息ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_type VARCHAR(50) COMMENT '文件类型',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    parsed_content TEXT COMMENT '解析后的内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_message_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天附件表';

-- ============================================
-- 文档分块表（支持页级别RAG检索）
-- ============================================
CREATE TABLE IF NOT EXISTS kg_document_chunk (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    page_num INT DEFAULT 0 COMMENT '页码（从1开始，0表示无页码概念）',
    chunk_index INT DEFAULT 0 COMMENT '分块序号',
    content TEXT NOT NULL COMMENT '分块内容',
    vector_id VARCHAR(100) COMMENT '向量ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_document_id (document_id),
    INDEX idx_page_num (page_num),
    FOREIGN KEY (document_id) REFERENCES kg_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档分块表';

-- ============================================
-- 初始化测试数据（可选）
-- ============================================

-- 插入示例节点
INSERT INTO kg_knowledge_node (id, name, node_type, description, source_type) VALUES
(1, '人工智能', 'Concept', '人工智能（Artificial Intelligence，简称AI）是研究、开发用于模拟、延伸和扩展人的智能的理论、方法、技术及应用系统的一门新的技术科学。', 'manual'),
(2, '机器学习', 'Concept', '机器学习是人工智能的一个分支，是一门多领域交叉学科，涉及概率论、统计学、逼近论、凸分析、计算复杂性理论等多门学科。', 'manual'),
(3, '深度学习', 'Concept', '深度学习是机器学习的分支，是一种以人工神经网络为架构，对数据进行表征学习的算法。', 'manual'),
(4, '自然语言处理', 'Technology', '自然语言处理是人工智能和语言学领域的分支学科，研究能实现人与计算机之间用自然语言进行有效通信的各种理论和方法。', 'manual'),
(5, '知识图谱', 'Technology', '知识图谱是一种语义网络，其结点代表实体或者概念，边代表实体/概念之间的各种语义关系。', 'manual');

-- 插入示例关系
INSERT INTO kg_knowledge_relation (id, name, relation_type, source_node_id, target_node_id, weight) VALUES
(1, '包含', 'PART_OF', 1, 2, 1.0),
(2, '包含', 'PART_OF', 2, 3, 1.0),
(3, '包含', 'PART_OF', 1, 4, 1.0),
(4, '应用于', 'RELATED_TO', 4, 5, 0.8),
(5, '基于', 'BELONGS_TO', 5, 1, 0.9);
