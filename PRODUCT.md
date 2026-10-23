# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

当前处于产品化预研阶段，主要用户画像与使用场景尚未锁定，也没有可验证的用户反馈。已确认的技术现状（无登录鉴权、依赖本机服务、中文界面）表明现阶段按单人/小团队自托管使用推进；多用户与租户模型尚未决定。

## Product Purpose

本地大模型驱动的知识图谱管理系统：把文档与图片中的知识自动抽取为实体和关系，形成可视化知识网络，并基于 RAG 提供可追溯的智能问答。当前目标是验证该产品在 PostgreSQL/pgvector 技术栈上重构后的可行性与使用价值，为后续产品化决策提供依据。

## Positioning

在一个自托管应用内打通「文档/OCR → 大模型知识抽取 → 图谱 → RAG 问答」的完整链路，数据与推理默认留在本机（Ollama），不以云端服务为前提。差异点在端到端整合与答案的来源可追溯，而非单点的向量检索或图谱展示。

## Operating Context

- 自托管/本地运行：PostgreSQL（含 pgvector）、Redis、Ollama 需在本机或内网可用；Ollama 不在线时后端仍可启动。
- 中文优先的界面与文档；文档解析、OCR、节点与关系类型均面向中英文混合内容。
- 主要工作流：上传文档 → 异步解析与分块 → 大模型抽取实体/关系 → 图谱浏览与人工维护 → 基于检索的对话问答。
- 当前无认证/授权机制，默认单租户使用。

## Capabilities and Constraints

已确认的功能范围（保持原有 7 项能力，迁移到 pgvector 技术栈）：

1. 文档管理：PDF/Word/TXT/Markdown 上传、异步解析、分块与状态跟踪、摘要生成。
2. OCR 识别：图片文字识别（中英文混合），识别结果可进入知识抽取。
3. 知识抽取：调用本地大模型从文本中抽取实体与关系。
4. 图谱可视化：交互式力导向图，节点类型着色、拖拽、缩放、关系探索。
5. 知识节点与关系管理：CRUD，支持多种节点与关系类型。
6. AI 对话（RAG）：多会话管理、SSE 流式响应、思考链展示、Markdown 渲染、关联文档及页码定位。
7. AI 服务：知识抽取调试、向量生成测试、模型配置。

技术约束（当前分支）：

- 后端 Spring Boot 4.1.1、Spring AI 2.0.1、JDK 21；PostgreSQL + pgvector 替代原 Milvus/Neo4j/MySQL；Redis 缓存；MyBatis-Plus；经 Ollama 调用对话模型 deepseek-r1:1.5b 与向量模型 bge-m3（1024 维）。
- 后端业务代码已清空，仅保留 `/api/health` 探活接口；旧接口文档与 README 不代表当前实现。
- `knowledge-ui` 为旧版前端，仅作为能力与交互参考；重构完成后将重新设计并替换，不在旧视觉体系上叠加演进。

待决事实：

- 主要用户画像与使用场景。
- 租户、账号与权限模型（当前无鉴权）。
- OCR 引擎在新架构中的选型与接入方式（旧版依赖 Tesseract，新骨架未包含）。
- 前端重做的时间点与技术选型。

## Brand Commitments

产品名称：知识图谱系统（Knowledge Graph System）。中文优先。仓库采用 MIT License。除此之外无已确认的品牌语气、视觉资产或宣传承诺。

## Evidence on Hand

- `README.md`：旧版功能与 API 说明；技术栈部分已过时（Milvus/Neo4j/MySQL/Spring Boot 3）。
- `knowledge-ui/src/views/*.vue`：7 个视图的现有实现，可作为功能与信息结构参考。
- `docs/sql/init.sql`：旧版数据库结构（MySQL）。
- `src/main/java/com/uka/knowledge/controller/HealthController.java`：当前唯一后端接口，用于验证 PostgreSQL/Redis 连通性。
- `docs/DEPLOY.md`、`docs/README_BACKUP.md`：历史文档，需按新架构重新核实。
- 无用户调研、案例、测评数据或对外宣传素材；后续工作不得虚构这些证据。

## Product Principles

1. 数据不出本机：文档、向量与推理默认全部本地化，云端仅作为可选扩展而非前提。
2. 一条链路贯通：入库、抽取、图谱与问答共享同一套数据与来源引用，避免成为互不相通的工具拼盘。
3. 答案可溯源：RAG 输出应指向具体文档、分块与页码，知识可被人工核对与修正。
4. 预研先于包装：当前阶段以验证能力与产品可行性为先，不为未验证的用户群过度设计。
5. 中文知识工作优先：术语、解析、OCR 与交互默认服务中英文混合内容。
