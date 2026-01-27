# Knowledge Graph System (知识图谱系统)

基于 Spring Boot 3 + Vue 3 的智能知识图谱管理系统，集成 RAG 检索增强、AI 对话、文档解析、OCR 识别等功能。

## 功能特性

### 核心功能

- **知识图谱可视化** - 基于 ECharts 的交互式图谱展示，支持节点拖拽、缩放、关系探索
- **AI 智能对话** - 基于 RAG 的智能问答，支持 SSE 流式响应、思考链展示、Markdown 渲染
- **文档管理** - 支持 PDF、Word、TXT、Markdown 等多格式文档上传与解析
- **OCR 识别** - 基于 Tesseract 的图片文字识别，支持中英文混合
- **知识抽取** - 利用大模型自动从文档中抽取实体和关系，构建知识图谱

### 技术亮点

- **RAG 检索增强** - 基于 Milvus 向量数据库实现语义检索，支持文档分块和页码定位
- **多模型支持** - 通过 Ollama 集成 DeepSeek、Llama、Qwen 等开源大模型
- **图数据库** - 使用 Neo4j 存储和查询复杂的知识关系网络
- **实时流式响应** - SSE 推送 AI 回复，支持思考链（Chain of Thought）展示

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.5 | 基础框架 |
| Spring AI | 1.1.2 | AI 模型集成 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Neo4j | 5.x | 图数据库 |
| Milvus | 2.3.x | 向量数据库 |
| MySQL | 8.x | 关系型数据库 |
| Tesseract | 5.x | OCR 引擎 |
| Apache PDFBox | 3.0.1 | PDF 解析 |
| Apache POI | 5.2.5 | Office 文档解析 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| Vue Router | 4.3.x | 路由管理 |
| Pinia | 2.1.x | 状态管理 |
| Element Plus | 2.6.x | UI 组件库 |
| ECharts | 5.5.x | 图表可视化 |
| markdown-it | 14.x | Markdown 渲染 |
| highlight.js | 11.x | 代码高亮 |

## 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                         │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐      │
│  │  图谱    │  AI对话  │  文档    │  OCR     │  节点    │      │
│  │  可视化  │  (RAG)   │  管理    │  识别    │  关系    │      │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Backend (Spring Boot 3)                      │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                     REST API Layer                        │  │
│  │  /api/chat  /api/document  /api/graph  /api/ocr  /api/ai │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Service Layer                          │  │
│  │  ChatService  RagService  DocumentService  OcrService    │  │
│  │  KnowledgeExtractService  VectorService  GraphService    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│    MySQL      │   │    Neo4j      │   │    Milvus     │
│  (元数据)     │   │  (图数据)     │   │  (向量)       │
└───────────────┘   └───────────────┘   └───────────────┘
                              │
                              ▼
                    ┌───────────────┐
                    │    Ollama     │
                    │  (LLM推理)    │
                    └───────────────┘
```

## 功能模块

### 1. AI 对话 (`/chat`)
- 多会话管理，支持创建、删除、历史记录
- RAG 检索增强，自动关联相关文档和知识节点
- SSE 流式响应，实时显示 AI 回复
- 思考链折叠展示（支持 DeepSeek-R1 等模型）
- Markdown 渲染 + 代码高亮
- 文件上传，支持文档/图片作为对话上下文
- 关联文档内联显示，包含页码定位

### 2. 图谱可视化 (`/`)
- 力导向图布局，交互式节点拖拽
- 节点类型着色区分
- 关系线条动态展示
- 支持缩放、平移、聚焦

### 3. 文档管理 (`/documents`)
- 多格式文档上传（PDF、Word、TXT、Markdown）
- 异步文档解析，状态实时更新
- 文档分块存储，支持页级别 RAG 检索
- 自动知识抽取，构建关联图谱
- 文档摘要生成

### 4. OCR 识别 (`/ocr`)
- 图片上传识别
- 支持中英文混合识别
- 识别结果自动进行知识抽取

### 5. 知识管理 (`/nodes`, `/relations`)
- 知识节点 CRUD
- 知识关系管理
- 支持多种节点类型：Person、Organization、Location、Concept、Technology、Product、Event 等
- 支持多种关系类型：BELONGS_TO、PART_OF、LOCATED_IN、WORKS_FOR、CREATED_BY 等

### 6. AI 服务 (`/ai`)
- 文本知识抽取
- 向量生成测试
- 模型配置管理

## 快速开始

详细部署步骤请参考 [部署文档](DEPLOY.md)

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Neo4j 5.x
- Milvus 2.3+
- Ollama
- Tesseract 5.x (OCR 功能需要)

### 快速启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd knowledge

# 2. 初始化数据库
mysql -u root -p < sql/init.sql

# 3. 启动后端
mvn spring-boot:run

# 4. 启动前端
cd knowledge-ui
pnpm install
pnpm dev
```

访问地址：
- 前端：http://localhost:5173
- 后端 API：http://localhost:8080
- API 文档：http://localhost:8080/doc.html

## 项目结构

```
knowledge/
├── src/main/java/com/uka/knowledge/
│   ├── KnowledgeApplication.java     # 启动类
│   ├── config/                       # 配置类
│   │   ├── MilvusConfig.java         # Milvus配置
│   │   ├── OllamaConfig.java         # Ollama配置
│   │   ├── OcrConfig.java            # OCR配置
│   │   └── ...
│   ├── controller/                   # 控制器层
│   │   ├── ChatController.java       # 对话接口
│   │   ├── DocumentController.java   # 文档接口
│   │   ├── GraphController.java      # 图谱接口
│   │   └── ...
│   ├── service/                      # 服务层
│   │   └── impl/
│   │       ├── ChatServiceImpl.java  # 对话服务
│   │       ├── RagServiceImpl.java   # RAG服务
│   │       ├── VectorServiceImpl.java# 向量服务
│   │       └── ...
│   ├── mapper/                       # MyBatis Mapper
│   ├── repository/                   # Neo4j Repository
│   ├── model/                        # 数据模型
│   │   ├── entity/                   # MySQL实体
│   │   ├── neo4j/                    # Neo4j实体
│   │   ├── dto/                      # 数据传输对象
│   │   └── vo/                       # 视图对象
│   ├── common/                       # 通用类
│   ├── exception/                    # 异常处理
│   └── util/                         # 工具类
├── src/main/resources/
│   └── application.yml               # 配置文件
├── knowledge-ui/                     # 前端项目
│   ├── src/
│   │   ├── api/                      # API接口
│   │   ├── views/                    # 页面组件
│   │   │   ├── ChatView.vue          # AI对话页面
│   │   │   ├── GraphView.vue         # 图谱可视化
│   │   │   ├── DocumentView.vue      # 文档管理
│   │   │   └── ...
│   │   ├── router/                   # 路由配置
│   │   ├── App.vue                   # 根组件
│   │   └── main.js                   # 入口文件
│   ├── package.json
│   └── vite.config.js
├── sql/
│   └── init.sql                      # 数据库初始化脚本
├── uploads/                          # 文件上传目录
├── pom.xml                           # Maven配置
├── README.md                         # 项目说明
└── DEPLOY.md                         # 部署文档
```

## API 接口

### 对话接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/chat/session` | 创建会话 |
| GET | `/api/chat/session/list` | 会话列表 |
| DELETE | `/api/chat/session/{id}` | 删除会话 |
| POST | `/api/chat/{sessionId}/send` | 发送消息（SSE流式） |
| GET | `/api/chat/{sessionId}/messages` | 获取消息历史 |
| POST | `/api/chat/upload` | 上传对话附件 |

### 文档接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/document/upload` | 上传文档 |
| GET | `/api/document/list` | 文档列表 |
| GET | `/api/document/{id}` | 文档详情 |
| DELETE | `/api/document/{id}` | 删除文档 |
| POST | `/api/document/{id}/reparse` | 重新解析 |
| POST | `/api/document/{id}/summary` | 生成摘要 |

### 知识图谱接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/graph` | 获取图谱数据 |
| POST | `/api/node` | 创建节点 |
| GET | `/api/node/list` | 节点列表 |
| PUT | `/api/node/{id}` | 更新节点 |
| DELETE | `/api/node/{id}` | 删除节点 |
| POST | `/api/relation` | 创建关系 |
| GET | `/api/relation/list` | 关系列表 |
| DELETE | `/api/relation/{id}` | 删除关系 |

### OCR接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ocr/upload` | 上传图片识别 |
| GET | `/api/ocr/list` | 识别记录列表 |
| DELETE | `/api/ocr/{id}` | 删除记录 |

### AI服务接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | AI对话 |
| POST | `/api/ai/extract` | 知识抽取 |
| GET | `/api/ai/search` | 语义搜索 |

完整 API 文档请访问：http://localhost:8080/doc.html

## 配置说明

主要配置项 (`application.yml`)：

```yaml
server:
  port: 8080

spring:
  # MySQL 配置
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_graph
    username: root
    password: 123456

  # Neo4j 配置
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j123

  # Ollama 配置
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: deepseek-r1:1.5b      # 对话模型
      embedding:
        model: nomic-embed-text       # 向量模型

# Milvus 向量数据库配置
milvus:
  host: localhost
  port: 19530
  collection-name: knowledge_vectors
  dimension: 768

# OCR 配置
ocr:
  data-path: /opt/homebrew/share/tessdata  # tessdata路径
  language: chi_sim+eng                     # 识别语言

# 文件存储配置
file:
  upload-path: ./uploads
  allowed-types: pdf,doc,docx,txt,md,png,jpg,jpeg,gif,bmp
```

## 数据库表结构

| 表名 | 说明 |
|------|------|
| kg_document | 文档表 |
| kg_document_chunk | 文档分块表（支持页级RAG） |
| kg_ocr_record | OCR识别记录表 |
| kg_knowledge_node | 知识节点表 |
| kg_knowledge_relation | 知识关系表 |
| kg_chat_session | 聊天会话表 |
| kg_chat_message | 聊天消息表 |
| kg_chat_attachment | 聊天附件表 |

## 常见问题

### Q: Milvus 连接失败
A: 检查 Milvus 服务是否启动，端口 19530 是否可访问。首次启动会自动创建向量集合。

### Q: OCR 识别结果为空
A: 检查 Tesseract 是否安装，语言包是否存在，配置路径是否正确。

### Q: Neo4j 认证失败
A: 检查用户名密码是否正确，Neo4j 首次启动需要修改默认密码。

### Q: Ollama 调用超时
A: 首次调用需要加载模型可能较慢，可增加超时时间配置。确保已下载所需模型。

### Q: 文档解析失败
A: 检查文件格式是否支持，文件是否损坏。大文件建议增加 JVM 内存：`-Xmx4g`

### Q: RAG 检索不到内容
A: 确保文档已完成解析（状态为"已完成"），且 Milvus 服务正常运行。
## 联系沟通
<img src="imgs/img.png" style="zoom:20%;" />

## License

MIT License
