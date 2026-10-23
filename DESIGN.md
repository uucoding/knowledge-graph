---
name: 知识图谱系统
description: 本地优先的知识图谱研究与问答系统，以地图式导航组织文档、实体与答案。
colors:
  primary: "rgb(22, 93, 255)"
  primary-hover: "rgb(64, 128, 255)"
  primary-active: "rgb(14, 66, 210)"
  primary-soft: "rgb(232, 243, 255)"
  on-primary: "rgb(255, 255, 255)"
  success: "rgb(0, 180, 42)"
  success-text: "rgb(0, 128, 38)"
  warning: "rgb(255, 125, 0)"
  warning-text: "rgb(166, 69, 0)"
  danger: "rgb(245, 63, 63)"
  danger-text: "rgb(203, 38, 52)"
  info: "rgb(134, 144, 156)"
  ink: "#1d2129"
  text-secondary: "#4e5969"
  text-tertiary: "#6b7785"
  text-muted-strong: "#5f6b7a"
  text-disabled: "#c9cdd4"
  border: "#e5e6eb"
  border-strong: "#c9cdd4"
  surface: "#ffffff"
  canvas: "#f7f8fa"
  surface-sunken: "#f2f3f5"
  category-1: "rgb(22, 93, 255)"
  category-2: "rgb(0, 180, 42)"
  category-3: "rgb(255, 125, 0)"
  category-4: "rgb(245, 63, 63)"
  category-5: "rgb(20, 201, 201)"
  category-6: "rgb(247, 186, 30)"
  category-7: "rgb(114, 46, 209)"
  category-8: "rgb(245, 49, 157)"
  category-9: "rgb(159, 219, 29)"
typography:
  title:
    fontFamily: "Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial, sans-serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: 1.4
  section:
    fontFamily: "Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial, sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1.5
  body:
    fontFamily: "Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial, sans-serif"
    fontSize: "13px"
    fontWeight: 500
    lineHeight: 1.5
  caption:
    fontFamily: "Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.5
  data:
    fontFamily: "Helvetica Neue, Helvetica, PingFang SC, Hiragino Sans GB, Microsoft YaHei, Arial, sans-serif"
    fontSize: "13px"
    fontWeight: 500
    lineHeight: 1.5
    fontFeature: "tnum"
  code:
    fontFamily: "Consolas, Monaco, monospace"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.6
rounded:
  sm: "4px"
  md: "6px"
  lg: "8px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  "2xl": "24px"
  "3xl": "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "0 14px"
    height: "32px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-primary-active:
    backgroundColor: "{colors.primary-active}"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-secondary}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "0 14px"
    height: "32px"
  input-default:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.sm}"
    padding: "0 12px"
    height: "32px"
  card-surface:
    backgroundColor: "{colors.surface}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: "16px"
  nav-item:
    backgroundColor: "transparent"
    textColor: "{colors.text-secondary}"
    typography: "{typography.body}"
    rounded: "{rounded.sm}"
    padding: "0 12px"
    height: "36px"
  nav-item-active:
    backgroundColor: "{colors.primary-soft}"
    textColor: "{colors.primary}"
  tag-neutral:
    backgroundColor: "{colors.surface-sunken}"
    textColor: "{colors.text-secondary}"
    typography: "{typography.caption}"
    rounded: "{rounded.sm}"
    padding: "0 8px"
    height: "22px"
  table-row:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-secondary}"
    typography: "{typography.body}"
    height: "40px"
  chat-bubble-user:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: "12px 16px"
  chat-bubble-assistant:
    backgroundColor: "{colors.surface-sunken}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.lg}"
    padding: "12px 16px"
---

# Design System: 知识图谱系统

## Overview

**Creative North Star: "知识舆图 The Atlas"**

知识图谱系统不是一块控制面板，而是一册可操作的地图集。文档是疆域，实体是地标，关系是道路，RAG 回答是钉回来源坐标的批注。每一屏都应当像一张地图图幅：安静的边框，完整的数据画布，清晰的图例；使用者不是在“操作后台”，而是在同一张图上定位、比对、行走。

密度是有意为之：32px 控件、40px 表格行、1px 界线、灰桌面上的白图纸。层次来自描边与底色差，而不是投影或渐变。颜色是一套地图墨水：仅一种信号蓝标记当前位置与主动作，语义色只表示状态，类别色只出现在数据标记与图例中。

已确认的反向参考：通用后台模板（深灰蓝侧栏、面包屑、卡片堆表格）、霓虹科技感（发光、星空、蓝紫渐变）、信息过载、营销页式过度留白。拒绝这四者本身就是方向的一部分。

**Key Characteristics:**
- 图幅式构图：安静框架 + 全幅数据画布。
- 单一信号蓝；其余颜色只在语义或数据中出场。
- 扁平描边与纸张表面；投影仅用于浮层与交互。
- 仪器级紧凑密度，数字使用等宽数位。
- 中文优先，系统字体栈，无展示体、无装饰字。

## Colors

一套地图墨水色板：Arco 风格 10 级明暗色阶，浅色为规范；dark 色阶已随色板提供，但暗色界面的规则尚未定义。

### Primary
- **舆图蓝 Atlas Blue** (rgb(22, 93, 255))：当前位置与主动作。用于主按钮、激活导航、焦点环、链接与选中态；悬停与按下沿同一色阶变化。它是界面中唯一的强调色。

### Semantic
- **勘定绿 Survey Green** (rgb(0, 180, 42))：完成、解析成功、正向确认。
- **标尺橙 Bench Orange** (rgb(255, 125, 0))：处理中、警示、引用页码等需要注意但不危险的信息。
- **警界红 Boundary Red** (rgb(245, 63, 63))：删除、失败与越界操作。
- **注记灰 Annotation Gray** (#86909C)：中性提示、非文本标记与停用图标的底。
- **文字用色**：语义色出现在文字上时，使用同一色阶的深一级（success-text / warning-text / danger-text），确保 4.5:1；圆点与数据标记仍用原色。

### Categorical
- **图谱分类墨**：蓝、绿、橙、红、青、金、紫、品红、酸橙九色，只用于节点、边、图表与图例。它们没有语义，不得借用给按钮、边框或背景。

### Neutral
- **墨色 Ink** (#1D2129)：标题与正文主色。
- **次级墨** (#4E5969)：表格正文、次级标签、表单值。
- **三级墨** (#6B7785)：时间戳、说明、占位符与元数据；白底上的最小可读灰。
- **桌面次级墨** (#5F6B7A)：桌面底色上的说明文字，保证 4.5:1。
- **残墨** (#C9CDD4)：禁用文字与弱化描边。
- **细界线** (#E5E6EB)：面板边界、表格分隔、卡片内分割。
- **界线** (#C9CDD4)：输入框与次级按钮的可见描边。
- **图纸** (#FFFFFF)：面板、输入框、表格行、图例底。
- **桌面** (#F7F8FA)：应用画布与表格行悬停。
- **凹面** (#F2F3F5)：助手气泡、标签底、凹陷槽位。

### Named Rules
**The One Voice Rule.** 舆图蓝在任何一屏的覆盖不超过 10%。导航、一个主动作、焦点与实时数据状态之外，不出现蓝色。

**The Legend Rule.** 分类色是数据，不是装饰。图例里的颜色永远不出现在按钮、边框或面板背景上。

**The Paper Rule.** 静止表面是白图纸 + 1px 细界线；深度靠桌面/凹面的底色差，不靠色块或投影。

## Typography

**Display Font:** 系统无衬线（Helvetica Neue、Helvetica、PingFang SC、Hiragino Sans GB、Microsoft YaHei、Arial、sans-serif）
**Body Font:** 同上
**Label/Mono Font:** Consolas、Monaco、monospace（仅代码、抽取结果与 OCR 原文）

**Character:** 务实的中英混排工具字：拉丁与中文共享同一光学字号，没有展示体、没有装饰衬线；技术感来自对齐、字重与数字，不来自字体本身。

### Hierarchy
- **Title** (600, 18px, 1.4)：页面标题、弹窗标题；全系统最大字号。
- **Section** (600, 15px, 1.5)：面板标题、聊天头部、图例标题。
- **Body** (400, 14px, 1.6)：表格、表单、消息正文；长文控制在 70ch 内。
- **Label** (500, 13px, 1.5)：按钮、标签页、字段标签。
- **Caption** (400, 12px, 1.5)：时间戳、元数据、计数。
- **Data** (500, 13px, 1.5, tnum)：ID、计数、相似度、边权重、时间。
- **Code** (400, 13px, 1.6)：代码块、JSON 属性、OCR 原文。

### Named Rules
**The One Scale Rule.** 标题止步于 18px。层次只用字重、颜色与间距建立，不引入展示字体或超大字号。

**The Data Voice Rule.** 一切会被纵向比较的数字——ID、得分、数量、日期——使用等宽数位（tnum），绝不允许列间跳动。

## Layout

框架：232px 的浅色索引栏（白底、右侧 1px 界线），56px 内容头部，桌面色画布，20–24px 内容内边距。索引栏与内容同属一个纸张世界，不使用深色侧栏，也不在头部重复面包屑。

构图：每屏只有一个主工作表面（图谱画布、消息流或表格），工具条紧贴其上方并保持单行；信息按“主表面 → 证据/属性 → 次要操作”排列，不把全部元数据一次铺开。

密度：控件 32px，表格行 40px，工具条间距 12px，面板间距 16px，面板内边距 16px。聊天为 260px 会话索引 / 流式消息区 / 340px 证据面板三栏；图谱为工具条 + 满幅画布；列表为工具条 + 表格 + 分页。

桌面优先：1280px 以下收紧表格列宽，1024px 以下索引栏折叠为图标栏；完整移动端布局仍未定义。

间距节奏：4 / 8 / 12 / 16 / 20 / 24 / 32。

## Elevation & Depth

扁平优先。静止的面板、表格与卡片没有投影，只靠 1px 细界线与桌面/凹面的底色差分层。投影只属于两类时刻：交互反馈与浮层。

### Shadow Vocabulary
- **Focus ring** (`0 0 0 3px rgba(22, 93, 255, 0.16)`): 键盘焦点与输入聚焦。
- **Hover lift** (`0 2px 8px rgba(22, 93, 255, 0.12)`): 可点击面板、证据行、图例项的悬停；同时描边转为舆图蓝。
- **Overlay** (`0 12px 32px rgba(17, 24, 39, 0.16)`): 弹窗、下拉、浮层。

### Named Rules
**The Flat-By-Default Rule.** 表面静止时全平；投影是状态的回应或浮层的身份证，不是装饰。

## Shapes

紧凑的圆角矩形：控件与标签 4px，小容器 6px，面板与气泡 8px。所有边界默认 1px；不使用胶囊按钮、重描边或斜切角。

圆形只属于数据与身份：图谱节点、头像、状态点、图例墨点。图谱边保持 0.3 曲率，线宽 1–4px 随关系权重；节点默认 30px，邻接强调而非发光。

### Named Rules
**The Two-Geometry Rule.** 界面是圆角矩形，数据是圆。两种几何不互相借用。

## Components

### Buttons
- **Shape:** 32px 高，4px 圆角，13px/500 标签；水平内边距 14px。
- **Primary:** 舆图蓝底白字；hover 升至第 5 级，active 降至第 7 级；focus 用 3px 蓝色环。
- **Secondary:** 白底 + 1px 界线 + 次级墨文字；hover 时描边与文字转舆图蓝，底色转桌面。
- **Table actions:** 文字按钮，右对齐；一行最多一个主链接与一个危险链接，绝不并排三个实心按钮。

### Inputs / Fields
- **Style:** 32px 高，白底，1px 界线，4px 圆角，内边距 12px；占位符用三级墨。
- **Focus:** 描边转舆图蓝并叠加 3px 浅蓝环；不改变高度，不跳动。
- **Error:** 描边与提示文字使用警界红；禁用态保留 1px 残墨描边与三级墨文字。

### Tags / Status
- **Style:** 22px 高，4px 圆角，凹面底 + 次级墨文字，12px 字。
- **State:** 状态由 6px 墨点 + 文本表达（完成绿、处理橙、失败红、中性灰），不用整块高饱和底色。页码、文件类型等元数据标签使用中性底。

### Cards / Panels
- **Corner:** 8px。
- **Background:** 图纸白；桌面为画布；凹面用于内嵌信息。
- **Border:** 1px 细界线；可点击面板 hover 时描边转舆图蓝并出现 Hover lift。
- **Padding:** 16px；面板标题 15px/600，标题下可用 1px 细界线分隔。
- **Shadow:** 静止无投影。

### Tables
- 行高 40px，行间 1px 细界线，无外框；表头 12px 三级墨；正文 14px 次级墨；数字列使用 Data 角色与右对齐。
- 行悬停用桌面色；操作列固定在右侧，使用文字按钮；空态使用一句说明 + 一个主动作，不放大面积插画。

### Navigation
- 浅色索引栏，条目 36px 高、4px 圆角、16px 图标 + 14px 文字；hover 凹面，激活为浅蓝底 + 舆图蓝文字 + 左侧 2×16px 位置标记。
- 页面标题放在内容头部，与右侧主操作同一行；不重复面包屑，不把搜索框常驻在顶栏。

### Service Status & Offline
- 头部状态 chip：28px 高、白底 + 1px 界线 + 6px 墨点；在线绿、离线红。tooltip 列出后端、PostgreSQL、Redis，并注明 Ollama 未在此处检测。
- 离线条：36px 高、`#FFF7E8` 底、标尺橙文字、右侧一个重试按钮；整站进入只读地图模式，保留浏览、禁用写入与 AI 动作。
- 每个数据区域使用持久状态面板表达 loading / empty / error / offline；toast 只用于临时动作确认。

### Evidence Chips
- 聊天证据行分两行：第一行文档名 + 等宽相似度，第二行页码 chip + 文件类型 chip；chip 20px 高、4px 圆角、原生 span，不使用可被压缩的标签组件。

### Chat
- 会话项 8px 圆角，激活浅蓝底；消息气泡 8px 圆角，宽度不超过 70%。
- 用户气泡为舆图蓝底白字，助手气泡为凹面底墨色字；思考过程收进 1px 细界线容器，标题 12px 三级墨。
- 证据行：白底、1px 界线、6px 圆角；页码用标尺橙标签，相似度用等宽数位；hover 出现蓝色描边、轻抬升与右箭头。
- 流式光标为 8×18px 舆图蓝竖条，1s 闪烁；输入框随内容 1–4 行自适应。

### Graph Canvas (signature)
- 满幅白图纸置于 1px 界线面板内；分类色取自 Categorical 墨，不使用 ECharts 默认色板。
- 图例是右上角的“图幅索引”：白底面板，每行 8px 墨点 + 12px 名称 + 右对齐计数（tnum）；它不是一组彩色标签。
- 节点默认 30px 圆，边 0.3 曲率并按权重 1–4px；交互强调邻接关系，不加发光、阴影或星空背景。

## Do's and Don'ts

### Do:
- **Do** 每屏只给一个主动作使用舆图蓝；其余动作退为次级或文字按钮。
- **Do** 用图纸白 + 1px 细界线 + 桌面底色建立层次，静止面板不加投影。
- **Do** 保持 32px 控件与 40px 表格行的仪器级密度。
- **Do** 让 ID、计数、相似度、时间使用等宽数位并右对齐数字列。
- **Do** 让分类色只出现在节点、边、图表与图例中。
- **Do** 用墨点 + 文本表达状态，把饱和色留给数据与危险操作。
- **Do** 保持中文优先；英文仅作为缩写、类型名或代码出现。
- **Do** 把离线当作只读地图：保留浏览，禁用写入与 AI 动作，并给出重试入口。
- **Do** 用持久状态面板表达加载、空、错误与离线；toast 只做临时确认。

### Don't:
- **Don't** 使用深灰蓝侧栏、常驻面包屑与“卡片堆表格”的通用后台框架。
- **Don't** 使用渐变、发光、星空粒子或蓝紫霓虹。
- **Don't** 在同一屏堆满所有字段、面板与图例；按“主表面 → 证据 → 次要”分层。
- **Don't** 使用营销页式大间距或超过 18px 的页面标题。
- **Don't** 给静止卡片加投影，或用分类色装饰按钮、边框与背景。
- **Don't** 使用胶囊按钮、超过 8px 的圆角或重描边。
- **Don't** 用英文 axios 错误或 3 秒 toast 作为唯一错误反馈。
- **Don't** 在按钮内嵌套会收缩的标签组件；证据元数据使用固定宽度的原生 chip。
