---
version: 1
slug: "knowledge-ui-src-app-vue"
primary_target: "knowledge-ui/src/App.vue"
related_targets: ["knowledge-ui/src/views/GraphView.vue","knowledge-ui/src/views/ChatView.vue","knowledge-ui/src/views/NodeView.vue","knowledge-ui/src/views/RelationView.vue","knowledge-ui/src/views/DocumentView.vue","knowledge-ui/src/views/OcrView.vue","knowledge-ui/src/views/AiView.vue"]
---

# Surface brief: knowledge-ui 统一改版

- Scope: 整个 knowledge-ui 旧前端（App shell + 7 条路由），前端范围内全面统一；后端不新增端点。
- Visitor mode: Operate.
- Audience & job: 单人/小团队本地知识工作者；导入资料、检查图谱、核对来源、向知识库提问。
- Constraints: 保留 7 个现有路由与 API 契约；后端只有 /api/health 时，其余接口缺失显示离线/未接入；中文优先；继续使用 Vue 3 + Element Plus。
- Chosen direction: DESIGN.md 已确立的「知识舆图 The Atlas」；Arco 10 级色板、浅色索引栏、扁平描边、仪器级密度。
- Memorable moment: 图谱页的图幅索引图例 + 离线只读地图；聊天中的证据行。
- Unresolved: 后端恢复后的真实数据密度；移动端布局（本轮不做）。

## Direction contract

THESIS: 把通用后台壳换成一张可操作的知识地图；统一外壳、七个视图与所有状态，而不是给旧模板换色。
OWN-WORLD: 舆图蓝 rgb(22,93,255) 单一强调；白图纸、桌面色、1px 细界线；232px 浅色索引栏；4/6/8px 圆角；等宽数位。
STORY: 用户看见资料入库 → 处理 → 图谱检查 → 问答的链路；离线时进入只读地图；引用最终能回到来源。
FIRST VIEWPORT: 浅色索引栏按「图谱 / 资料 / 问答 / 工具」分组；56px 内容头部含页面标题、说明与本地服务状态；图谱页为工具条 + 满幅画布 + 右上角图幅索引图例。
FORM: 已确立世界的扩展，code-led，无概念 seed；不重开身份工作。
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.
