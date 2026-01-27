# 知识图谱系统

基于Spring Boot + Vue3的知识图谱管理系统，集成文档解析、OCR识别、知识抽取、图谱可视化等功能。

## 技术栈

### 后端
- **Spring Boot 3.2.5** - 核心框架
- **Spring AI** - Ollama大模型集成
- **MyBatis-Plus** - ORM框架
- **MySQL** - 关系型数据库
- **Neo4j** - 图数据库
- **Milvus** - 向量数据库
- **Tesseract OCR** - 图片文字识别
- **Apache Tika/POI/PDFBox** - 文档解析

### 前端
- **Vue 3** - 前端框架
- **Element Plus** - UI组件库
- **ECharts** - 图表可视化
- **Vue Router** - 路由管理
- **Pinia** - 状态管理
- **Axios** - HTTP客户端

## 项目结构

```
knowledge/
├── src/main/java/com/uka/knowledge/
│   ├── KnowledgeApplication.java     # 启动类
│   ├── config/                       # 配置类
│   ├── controller/                   # 控制器层
│   ├── service/                      # 服务层
│   │   └── impl/                     # 服务实现
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
├── sql/
│   └── init.sql                      # 数据库初始化脚本
├── knowledge-ui/                     # 前端项目
│   ├── src/
│   │   ├── api/                      # API接口
│   │   ├── router/                   # 路由配置
│   │   └── views/                    # 页面组件
│   ├── package.json
│   └── vite.config.js
└── pom.xml
```

## 环境准备

### 1. 安装依赖服务

#### MySQL 8.0+
```bash
# Docker方式
docker run -d --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123456 \
  mysql:8.0
```

#### Neo4j 5.x
```bash
# 1. 创建数据目录
mkdir -p ~/docker-data/neo4j/{data,logs,import,plugins}

# 2. Docker方式启动
docker run -d \
  --name neo4j \
  -p 7474:7474 \
  -p 7687:7687 \
  -v ~/docker-data/neo4j/data:/data \
  -v ~/docker-data/neo4j/logs:/logs \
  -v ~/docker-data/neo4j/import:/var/lib/neo4j/import \
  -v ~/docker-data/neo4j/plugins:/plugins \
  -e NEO4J_AUTH=neo4j/your_password \
  -e NEO4J_PLUGINS='["apoc"]' \
  neo4j:5.26.0

# 3. 常用命令
docker start neo4j   # 启动
docker stop neo4j    # 停止
docker logs neo4j    # 查看日志
```

**端口说明：**
| 端口 | 说明 |
|------|------|
| 7474 | Web 浏览器界面 (http://localhost:7474) |
| 7687 | Bolt 协议端口 (应用程序连接) |

**目录说明：**
| 目录 | 说明 |
|------|------|
| /data | 数据库文件 |
| /logs | 日志文件 |
| /import | CSV 导入目录 |
| /plugins | APOC 等插件目录 |

> **注意**：请将 `your_password` 替换为实际密码，并确保与 `application.yml` 中配置一致。

#### Milvus 2.3+
```bash
# Docker Compose方式（推荐）
# 参考官方文档：https://milvus.io/docs/install_standalone-docker.md
wget https://github.com/milvus-io/milvus/releases/download/v2.3.4/milvus-standalone-docker-compose.yml -O docker-compose.yml
docker-compose up -d
```

#### Ollama
```bash
# 安装Ollama
curl -fsSL https://ollama.com/install.sh | sh

# 下载模型
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
```

#### Tesseract OCR（用于图片文字识别）

##### Windows 安装（推荐）

1. **下载安装包**
   - 访问 [UB Mannheim Tesseract](https://github.com/UB-Mannheim/tesseract/wiki) 下载页面
   - 下载最新版本的 Windows 安装包（如 `tesseract-ocr-w64-setup-5.3.3.20231005.exe`）
   - 或直接访问：https://digi.bib.uni-mannheim.de/tesseract/

2. **安装步骤**
   ```
   1. 运行安装程序
   2. 选择安装路径（建议：C:\Program Files\Tesseract-OCR）
   3. 在"Choose Components"步骤中，展开"Additional language data"
   4. 勾选"Chinese (Simplified)"和"Chinese (Traditional)"（中文语言包）
   5. 完成安装
   ```

3. **配置环境变量**
   ```
   1. 右键"此电脑" -> 属性 -> 高级系统设置 -> 环境变量
   2. 在"系统变量"中找到"Path"，点击编辑
   3. 添加 Tesseract 安装路径：C:\Program Files\Tesseract-OCR
   4. 新建系统变量 TESSDATA_PREFIX，值为：C:\Program Files\Tesseract-OCR\tessdata
   ```

4. **验证安装**
   ```cmd
   tesseract --version
   tesseract --list-langs
   ```

5. **修改项目配置**（application.yml）
   ```yaml
   ocr:
     # Windows路径配置
     data-path: C:/Program Files/Tesseract-OCR/tessdata
     language: chi_sim+eng
   ```

> **注意**：Windows路径中使用正斜杠 `/` 或双反斜杠 `\\`

##### macOS 安装
```bash
brew install tesseract tesseract-lang

# 语言数据目录：/usr/local/share/tessdata
# Apple Silicon(M1/M2)：/opt/homebrew/share/tessdata
```

##### Ubuntu/Debian 安装
```bash
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim tesseract-ocr-chi-tra

# 语言数据目录：/usr/share/tesseract-ocr/4.00/tessdata
```

##### CentOS/RHEL 安装
```bash
sudo yum install tesseract tesseract-langpack-chi_sim

# 语言数据目录：/usr/share/tesseract/tessdata
```

##### 手动下载语言包

如果安装时未包含中文语言包，可手动下载：

1. 访问 https://github.com/tesseract-ocr/tessdata
2. 下载 `chi_sim.traineddata`（简体中文）和 `chi_tra.traineddata`（繁体中文）
3. 将文件放入对应的 tessdata 目录

##### 各系统 tessdata 目录汇总

| 操作系统 | tessdata 目录 |
|---------|--------------|
| Windows | `C:\Program Files\Tesseract-OCR\tessdata` |
| macOS (Intel) | `/usr/local/share/tessdata` |
| macOS (Apple Silicon) | `/opt/homebrew/share/tessdata` |
| Ubuntu/Debian | `/usr/share/tesseract-ocr/4.00/tessdata` |
| CentOS/RHEL | `/usr/share/tesseract/tessdata` |

### 2. 初始化数据库

```bash
# 连接MySQL执行初始化脚本
mysql -u root -p < sql/init.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，根据实际环境修改：

```yaml
# MySQL配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_graph
    username: root
    password: root123456

  # Neo4j配置
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j123456

  # Ollama配置
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b

# Milvus配置
milvus:
  host: localhost
  port: 19530

# OCR配置
ocr:
  data-path: /usr/local/share/tessdata
  language: chi_sim+eng
```

## 启动项目

### 后端启动

```bash
# 进入项目目录
cd knowledge

# 编译项目
mvn clean package -DskipTests

# 运行项目
mvn spring-boot:run
# 或
java -jar target/knowledge-1.0-SNAPSHOT.jar
```

后端启动后访问：
- API文档：http://localhost:8080/doc.html
- Swagger UI：http://localhost:8080/swagger-ui.html

### 前端启动

```bash
# 进入前端目录
cd knowledge-ui

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端访问地址：http://localhost:3000

## 核心功能

### 1. 文档解析
- 支持 PDF、Word(doc/docx)、TXT、Markdown 格式
- 自动提取文档文本内容
- 支持生成AI摘要
- 文档内容向量化存储

### 2. OCR识别
- 支持 PNG、JPG、JPEG、BMP、GIF 等图片格式
- 基于Tesseract实现中英文识别
- 识别结果向量化存储

### 3. 知识图谱
- 知识节点CRUD（同时维护MySQL和Neo4j）
- 知识关系管理
- 图谱可视化（基于ECharts）
- 子图查询
- 最短路径查询

### 4. AI功能
- AI对话（基于Ollama）
- 知识实体抽取
- 知识关系抽取
- 语义相似度搜索（基于Milvus）
- 文本摘要生成

## API接口示例

### 上传文档
```bash
curl -X POST "http://localhost:8080/api/document/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test.pdf"
```

### 上传图片OCR
```bash
curl -X POST "http://localhost:8080/api/ocr/upload" \
  -H "Content-Type: multipart/form-data" \
  -F "file=@test.png"
```

### 创建知识节点
```bash
curl -X POST "http://localhost:8080/api/node" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "人工智能",
    "nodeType": "Concept",
    "description": "人工智能是计算机科学的一个分支"
  }'
```

### 创建知识关系
```bash
curl -X POST "http://localhost:8080/api/relation" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "包含",
    "relationType": "PART_OF",
    "sourceNodeId": 1,
    "targetNodeId": 2,
    "weight": 1.0
  }'
```

### 获取图谱数据
```bash
curl "http://localhost:8080/api/graph?limit=100"
```

### AI对话
```bash
curl -X POST "http://localhost:8080/api/ai/chat" \
  -H "Content-Type: text/plain" \
  -d "什么是知识图谱？"
```

### 知识抽取
```bash
curl -X POST "http://localhost:8080/api/ai/extract" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "阿里巴巴集团由马云于1999年在杭州创立，是一家全球领先的互联网公司。"
  }'
```

### 语义搜索
```bash
curl "http://localhost:8080/api/ai/search?query=人工智能技术&topK=10"
```

## 注意事项

1. **Milvus初始化**：首次启动时会自动创建向量集合，如果Milvus服务未启动会输出警告但不影响其他功能。

2. **OCR语言包**：需要下载Tesseract的中文语言包(chi_sim.traineddata)，放到配置的tessdata目录。

3. **Ollama模型**：需要预先下载所需的模型，推荐使用qwen2.5:7b作为对话模型，nomic-embed-text作为向量模型。

4. **内存配置**：如果处理大文档，建议增加JVM内存：`-Xmx4g`

5. **Neo4j版本**：使用Neo4j 5.x版本，与Spring Data Neo4j 7.x兼容。

## 常见问题

### Q: Milvus连接失败
A: 检查Milvus服务是否启动，端口19530是否可访问。

### Q: OCR识别结果为空
A: 检查Tesseract是否安装，语言包是否存在，配置路径是否正确。

### Q: Neo4j认证失败
A: 检查用户名密码是否正确，Neo4j首次启动需要修改默认密码。

### Q: Ollama调用超时
A: 首次调用需要加载模型，可能较慢，可增加超时时间配置。

## License

Apache 2.0
