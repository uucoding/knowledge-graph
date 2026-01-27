# 部署文档

本文档详细介绍知识图谱系统的部署流程，包括开发环境和生产环境的部署方式。

## 目录

- [环境要求](#环境要求)
- [依赖服务安装](#依赖服务安装)
  - [MySQL](#1-mysql-80)
  - [Neo4j](#2-neo4j-5x)
  - [Milvus](#3-milvus-23)
  - [Ollama](#4-ollama)
  - [Tesseract OCR](#5-tesseract-ocr)
- [项目部署](#项目部署)
  - [后端部署](#后端部署)
  - [前端部署](#前端部署)
- [Docker 部署](#docker-部署)
- [生产环境配置](#生产环境配置)
- [常见问题](#常见问题)

---

## 环境要求

| 组件 | 最低版本 | 推荐版本 | 说明 |
|------|---------|---------|------|
| JDK | 17 | 17 | 后端运行环境 |
| Node.js | 18 | 20 LTS | 前端构建环境 |
| MySQL | 8.0 | 8.0+ | 关系型数据库 |
| Neo4j | 5.0 | 5.18+ | 图数据库 |
| Milvus | 2.3 | 2.3.4 | 向量数据库 |
| Ollama | - | 最新版 | 大模型推理 |
| Tesseract | 5.0 | 5.3+ | OCR引擎（可选） |

---

## 依赖服务安装

### 1. MySQL 8.0+

#### Docker 安装（推荐）

```bash
# 创建数据目录
mkdir -p ~/docker-data/mysql/{data,conf,logs}

# 启动 MySQL
docker run -d \
  --name mysql \
  --restart always \
  -p 3306:3306 \
  -v ~/docker-data/mysql/data:/var/lib/mysql \
  -v ~/docker-data/mysql/conf:/etc/mysql/conf.d \
  -v ~/docker-data/mysql/logs:/var/log/mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e TZ=Asia/Shanghai \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

# 验证
docker exec -it mysql mysql -uroot -p123456 -e "SELECT VERSION();"
```

#### 原生安装

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install mysql-server

# CentOS/RHEL
sudo yum install mysql-server
sudo systemctl start mysqld

# macOS
brew install mysql
brew services start mysql
```

#### 初始化数据库

```bash
# 连接 MySQL
mysql -u root -p

# 执行初始化脚本
source /path/to/knowledge/sql/init.sql

# 或者命令行执行
mysql -u root -p < sql/init.sql
```

---

### 2. Neo4j 5.x

#### Docker 安装（推荐）

```bash
# 创建数据目录
mkdir -p ~/docker-data/neo4j/{data,logs,import,plugins}

# 启动 Neo4j
docker run -d \
  --name neo4j \
  --restart always \
  -p 7474:7474 \
  -p 7687:7687 \
  -v ~/docker-data/neo4j/data:/data \
  -v ~/docker-data/neo4j/logs:/logs \
  -v ~/docker-data/neo4j/import:/var/lib/neo4j/import \
  -v ~/docker-data/neo4j/plugins:/plugins \
  -e NEO4J_AUTH=neo4j/neo4j123 \
  -e NEO4J_PLUGINS='["apoc"]' \
  -e NEO4J_dbms_security_procedures_unrestricted=apoc.* \
  neo4j:5.18.0

# 验证
# 浏览器访问 http://localhost:7474
# 使用 neo4j/neo4j123 登录
```

**端口说明：**
| 端口 | 说明 |
|------|------|
| 7474 | Web 浏览器界面 |
| 7687 | Bolt 协议端口（应用连接） |

#### 原生安装

```bash
# macOS
brew install neo4j
neo4j start

# 首次启动后访问 http://localhost:7474 修改密码
```

---

### 3. Milvus 2.3+

#### Docker Compose 安装（推荐）

```bash
# 创建目录
mkdir -p ~/docker-data/milvus
cd ~/docker-data/milvus

# 下载 docker-compose 配置
wget https://github.com/milvus-io/milvus/releases/download/v2.3.4/milvus-standalone-docker-compose.yml -O docker-compose.yml

# 启动
docker-compose up -d

# 验证
docker-compose ps

# 查看日志
docker-compose logs -f standalone
```

#### 端口说明
| 端口 | 说明 |
|------|------|
| 19530 | gRPC 端口（应用连接） |
| 9091 | 健康检查端口 |

#### 可视化管理工具（可选）

```bash
# 安装 Attu（Milvus 管理界面）
docker run -d \
  --name attu \
  -p 8000:3000 \
  -e MILVUS_URL=host.docker.internal:19530 \
  zilliz/attu:latest

# 访问 http://localhost:8000
```

---

### 4. Ollama

#### 安装

```bash
# Linux/macOS
curl -fsSL https://ollama.com/install.sh | sh

# macOS (Homebrew)
brew install ollama

# Windows
# 下载安装包：https://ollama.com/download/windows
```

#### 启动服务

```bash
# 启动 Ollama 服务
ollama serve

# 或后台运行
nohup ollama serve > /dev/null 2>&1 &
```

#### 下载模型

```bash
# 下载对话模型（推荐）
ollama pull deepseek-r1:1.5b    # 轻量级，支持思考链
ollama pull qwen2.5:7b          # 中文效果好
ollama pull llama3.1:8b         # 英文效果好

# 下载向量模型（必须）
ollama pull nomic-embed-text    # 768维向量

# 查看已下载模型
ollama list

# 测试模型
ollama run deepseek-r1:1.5b "你好"
```

#### 配置说明

| 模型 | 用途 | 内存需求 | 说明 |
|------|------|---------|------|
| deepseek-r1:1.5b | 对话 | 4GB+ | 支持思考链展示 |
| qwen2.5:7b | 对话 | 8GB+ | 中文效果优秀 |
| nomic-embed-text | 向量 | 2GB+ | 768维向量输出 |

---

### 5. Tesseract OCR

OCR 功能为可选，如不需要图片文字识别可跳过。

#### macOS 安装

```bash
# 使用 Homebrew
brew install tesseract tesseract-lang

# 验证安装
tesseract --version
tesseract --list-langs

# 语言数据目录
# Intel Mac: /usr/local/share/tessdata
# Apple Silicon: /opt/homebrew/share/tessdata
```

#### Ubuntu/Debian 安装

```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim tesseract-ocr-chi-tra

# 验证
tesseract --version
tesseract --list-langs

# 语言数据目录: /usr/share/tesseract-ocr/4.00/tessdata
```

#### CentOS/RHEL 安装

```bash
sudo yum install epel-release
sudo yum install tesseract tesseract-langpack-chi_sim

# 语言数据目录: /usr/share/tesseract/tessdata
```

#### Windows 安装

1. 下载安装包：https://github.com/UB-Mannheim/tesseract/wiki
2. 安装时勾选中文语言包（Chinese Simplified, Chinese Traditional）
3. 配置环境变量：
   - 添加 `C:\Program Files\Tesseract-OCR` 到 PATH
   - 新建 `TESSDATA_PREFIX` 变量，值为 `C:\Program Files\Tesseract-OCR\tessdata`

#### 手动下载语言包

如果安装时未包含中文语言包：

```bash
# 下载语言包
wget https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
wget https://github.com/tesseract-ocr/tessdata/raw/main/chi_tra.traineddata
wget https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata

# 移动到 tessdata 目录
sudo mv *.traineddata /opt/homebrew/share/tessdata/  # macOS Apple Silicon
```

---

## 项目部署

### 后端部署

#### 1. 获取代码

```bash
git clone <repository-url>
cd knowledge
```

#### 2. 修改配置

编辑 `src/main/resources/application.yml`：

```yaml
server:
  port: 8080

spring:
  # MySQL 配置
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_graph?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 123456  # 修改为实际密码

  # Neo4j 配置
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: neo4j123  # 修改为实际密码

  # Ollama 配置
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: deepseek-r1:1.5b  # 或其他已下载的模型
      embedding:
        model: nomic-embed-text

# Milvus 配置
milvus:
  host: localhost
  port: 19530
  collection-name: knowledge_vectors
  dimension: 768  # 与 nomic-embed-text 输出维度一致

# OCR 配置（根据系统调整路径）
ocr:
  data-path: /opt/homebrew/share/tessdata  # macOS Apple Silicon
  # data-path: /usr/local/share/tessdata   # macOS Intel
  # data-path: /usr/share/tesseract-ocr/4.00/tessdata  # Ubuntu
  # data-path: C:/Program Files/Tesseract-OCR/tessdata  # Windows
  language: chi_sim+eng

# 文件存储配置
file:
  upload-path: ./uploads
  allowed-types: pdf,doc,docx,txt,md,png,jpg,jpeg,gif,bmp
```

#### 3. 编译打包

```bash
# 编译（跳过测试）
mvn clean package -DskipTests

# 生成的 jar 文件位于 target/ 目录
ls target/knowledge-1.0-SNAPSHOT.jar
```

#### 4. 运行

```bash
# 开发环境
mvn spring-boot:run

# 生产环境
java -jar target/knowledge-1.0-SNAPSHOT.jar

# 指定配置文件
java -jar target/knowledge-1.0-SNAPSHOT.jar --spring.profiles.active=prod

# 指定内存（处理大文档时推荐）
java -Xmx4g -jar target/knowledge-1.0-SNAPSHOT.jar

# 后台运行
nohup java -Xmx4g -jar target/knowledge-1.0-SNAPSHOT.jar > app.log 2>&1 &
```

#### 5. 验证

- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/actuator/health

---

### 前端部署

#### 1. 安装依赖

```bash
cd knowledge-ui

# 使用 pnpm（推荐）
pnpm install

# 或使用 npm
npm install

# 或使用 yarn
yarn install
```

#### 2. 开发环境运行

```bash
pnpm dev
# 访问 http://localhost:5173
```

#### 3. 生产环境构建

```bash
# 构建
pnpm build

# 生成的文件位于 dist/ 目录
ls dist/
```

#### 4. 部署到 Nginx

```bash
# 复制构建产物到 Nginx 目录
cp -r dist/* /var/www/html/knowledge/

# Nginx 配置示例
```

**Nginx 配置 (`/etc/nginx/conf.d/knowledge.conf`)：**

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /var/www/html/knowledge;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # SSE 支持
        proxy_http_version 1.1;
        proxy_set_header Connection '';
        proxy_buffering off;
        proxy_cache off;
        chunked_transfer_encoding off;
        proxy_read_timeout 86400s;
    }

    # API 文档代理
    location /doc.html {
        proxy_pass http://localhost:8080;
    }

    location /swagger-ui {
        proxy_pass http://localhost:8080;
    }

    location /v3/api-docs {
        proxy_pass http://localhost:8080;
    }
}
```

```bash
# 测试配置
nginx -t

# 重载配置
nginx -s reload
```

---

## Docker 部署

### 一键启动所有服务

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  # MySQL
  mysql:
    image: mysql:8.0
    container_name: kg-mysql
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: knowledge_graph
      TZ: Asia/Shanghai
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  # Neo4j
  neo4j:
    image: neo4j:5.18.0
    container_name: kg-neo4j
    restart: always
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      NEO4J_AUTH: neo4j/neo4j123
      NEO4J_PLUGINS: '["apoc"]'
    volumes:
      - neo4j_data:/data
      - neo4j_logs:/logs

  # Milvus
  etcd:
    image: quay.io/coreos/etcd:v3.5.5
    container_name: kg-etcd
    environment:
      ETCD_AUTO_COMPACTION_MODE: revision
      ETCD_AUTO_COMPACTION_RETENTION: "1000"
      ETCD_QUOTA_BACKEND_BYTES: "4294967296"
    volumes:
      - etcd_data:/etcd
    command: etcd -advertise-client-urls=http://127.0.0.1:2379 -listen-client-urls http://0.0.0.0:2379 --data-dir /etcd

  minio:
    image: minio/minio:RELEASE.2023-03-20T20-16-18Z
    container_name: kg-minio
    environment:
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
    volumes:
      - minio_data:/minio_data
    command: minio server /minio_data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

  milvus:
    image: milvusdb/milvus:v2.3.4
    container_name: kg-milvus
    restart: always
    ports:
      - "19530:19530"
      - "9091:9091"
    depends_on:
      - etcd
      - minio
    environment:
      ETCD_ENDPOINTS: etcd:2379
      MINIO_ADDRESS: minio:9000
    volumes:
      - milvus_data:/var/lib/milvus

volumes:
  mysql_data:
  neo4j_data:
  neo4j_logs:
  etcd_data:
  minio_data:
  milvus_data:
```

### 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down

# 停止并删除数据
docker-compose down -v
```

---

## 生产环境配置

### 1. 创建生产环境配置文件

创建 `src/main/resources/application-prod.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/knowledge_graph?useUnicode=true&characterEncoding=utf-8&useSSL=true&serverTimezone=Asia/Shanghai
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:123456}
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50

  neo4j:
    uri: bolt://${NEO4J_HOST:localhost}:7687
    authentication:
      username: ${NEO4J_USER:neo4j}
      password: ${NEO4J_PASSWORD:neo4j123}

  ai:
    ollama:
      base-url: http://${OLLAMA_HOST:localhost}:11434

milvus:
  host: ${MILVUS_HOST:localhost}
  port: ${MILVUS_PORT:19530}

logging:
  level:
    root: INFO
    com.uka.knowledge: INFO
  file:
    name: logs/knowledge.log
    max-size: 100MB
    max-history: 30
```

### 2. 使用环境变量启动

```bash
export DB_HOST=your-mysql-host
export DB_PASSWORD=your-password
export NEO4J_HOST=your-neo4j-host
export NEO4J_PASSWORD=your-neo4j-password
export MILVUS_HOST=your-milvus-host
export OLLAMA_HOST=your-ollama-host

java -Xmx4g -jar knowledge-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 3. Systemd 服务配置

创建 `/etc/systemd/system/knowledge.service`：

```ini
[Unit]
Description=Knowledge Graph System
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/knowledge
ExecStart=/usr/bin/java -Xmx4g -jar knowledge-1.0-SNAPSHOT.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

Environment=DB_HOST=localhost
Environment=DB_PASSWORD=your-password
Environment=NEO4J_HOST=localhost
Environment=NEO4J_PASSWORD=your-password

[Install]
WantedBy=multi-user.target
```

```bash
# 重载配置
sudo systemctl daemon-reload

# 启动服务
sudo systemctl start knowledge

# 开机自启
sudo systemctl enable knowledge

# 查看状态
sudo systemctl status knowledge

# 查看日志
journalctl -u knowledge -f
```

---

## 常见问题

### Q1: Milvus 连接失败

**现象：** 启动时报错 `Failed to connect to Milvus`

**解决：**
1. 检查 Milvus 服务是否启动：`docker ps | grep milvus`
2. 检查端口是否可访问：`telnet localhost 19530`
3. 首次启动会自动创建集合，Milvus 未启动时会输出警告但不影响其他功能

### Q2: Neo4j 认证失败

**现象：** 报错 `The client is unauthorized due to authentication failure`

**解决：**
1. Neo4j 首次启动需要修改默认密码
2. 访问 http://localhost:7474 使用 neo4j/neo4j 登录后修改密码
3. 确保 application.yml 中密码配置正确

### Q3: Ollama 调用超时

**现象：** 调用大模型超时

**解决：**
1. 首次调用需要加载模型，可能较慢
2. 确保已下载所需模型：`ollama list`
3. 增加超时配置：`spring.ai.ollama.init.timeout: 600s`

### Q4: OCR 识别结果为空

**现象：** 上传图片后识别文本为空

**解决：**
1. 检查 Tesseract 是否安装：`tesseract --version`
2. 检查语言包是否存在：`tesseract --list-langs`
3. 确保配置路径正确，可执行：`tesseract test.png output -l chi_sim+eng`

### Q5: 文档解析失败

**现象：** 上传文档后状态显示"处理失败"

**解决：**
1. 检查文件格式是否支持（pdf,doc,docx,txt,md）
2. 检查文件是否损坏
3. 大文件增加 JVM 内存：`-Xmx4g`
4. 查看后端日志获取详细错误信息

### Q6: 前端无法访问后端 API

**现象：** 前端报 CORS 错误或 Network Error

**解决：**
1. 开发环境：检查 vite.config.js 中的代理配置
2. 生产环境：检查 Nginx 代理配置
3. 确保后端 WebConfig 中已配置 CORS

### Q7: SSE 流式响应不工作

**现象：** AI 回复不是逐字显示

**解决：**
1. 检查 Nginx 配置中是否关闭了缓冲：`proxy_buffering off`
2. 确保设置了正确的 HTTP 版本：`proxy_http_version 1.1`
3. 检查是否有其他代理层（如 CDN）缓存了响应

---

## 技术支持

如有问题，请提交 Issue 或联系维护人员。
