<template>
  <div class="chat-view">
    <!-- 会话侧边栏 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" @click="createSession" :icon="Plus">
          新对话
        </el-button>
      </div>
      <el-scrollbar class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          :class="['session-item', { active: currentSessionId === session.id }]"
          @click="selectSession(session)"
        >
          <div class="session-info">
            <div class="session-title">{{ session.title }}</div>
            <div class="session-meta">{{ formatTime(session.lastMessageTime || session.createTime) }}</div>
          </div>
          <el-dropdown trigger="click" @command="handleSessionCommand($event, session)" @click.stop>
            <el-icon class="session-more"><MoreFilled /></el-icon>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-empty v-if="sessions.length === 0" description="暂无对话" :image-size="80" />
      </el-scrollbar>
    </div>

    <!-- 对话主区域 -->
    <div class="chat-main">
      <div v-if="!currentSessionId" class="chat-empty">
        <el-empty description="选择或创建一个对话开始">
          <el-button type="primary" @click="createSession">开始新对话</el-button>
        </el-empty>
      </div>
      <template v-else>
        <!-- 消息列表 -->
        <el-scrollbar ref="messageScrollRef" class="message-list">
          <div v-for="message in messages" :key="message.id" :class="['message-item', message.role]">
            <div class="message-avatar">
              <el-avatar :size="36" :icon="message.role === 'user' ? User : ChatDotRound" />
            </div>
            <div class="message-content">
              <!-- 思考链折叠 -->
              <el-collapse v-if="message.thinkingContent" class="thinking-block">
                <el-collapse-item>
                  <template #title>
                    <el-icon><Loading /></el-icon>
                    <span style="margin-left: 8px;">思考过程</span>
                  </template>
                  <div class="thinking-content">{{ message.thinkingContent }}</div>
                </el-collapse-item>
              </el-collapse>
              <!-- 消息正文(Markdown渲染) -->
              <div class="message-text" v-html="renderMarkdown(message.content)"></div>
              <!-- 流式输出光标 -->
              <span v-if="message.isStreaming" class="streaming-cursor"></span>
              <!-- 关联文档（内联显示） -->
              <div v-if="message.role === 'assistant' && message.ragDocuments?.length" class="inline-related-docs">
                <div class="related-docs-header">
                  <el-icon><Folder /></el-icon>
                  <span>关联文档</span>
                </div>
                <div class="related-docs-list">
                  <div
                    v-for="doc in message.ragDocuments"
                    :key="doc.chunkId || doc.id"
                    class="related-doc-item"
                    @click="goToDocument(doc)"
                  >
                    <el-icon><Document /></el-icon>
                    <span class="doc-name">{{ doc.name }}</span>
                    <el-tag v-if="doc.pageNum && doc.pageNum > 0" size="small" type="warning">
                      第{{ doc.pageNum }}页
                    </el-tag>
                    <el-tag size="small">{{ doc.fileType?.toUpperCase() }}</el-tag>
                    <span class="doc-score">{{ (doc.score * 100).toFixed(0) }}%</span>
                    <el-icon class="link-arrow"><Right /></el-icon>
                  </div>
                </div>
              </div>
              <!-- 附件展示 -->
              <div v-if="message.attachments?.length" class="message-attachments">
                <el-tag v-for="att in message.attachments" :key="att.id" size="small" type="info">
                  <el-icon><Document /></el-icon>
                  {{ att.fileName }}
                </el-tag>
              </div>
              <div class="message-time">{{ formatTime(message.createTime) }}</div>
            </div>
          </div>
          <div v-if="sending && !streamingMessageId" class="message-item assistant">
            <div class="message-avatar">
              <el-avatar :size="36" :icon="ChatDotRound" />
            </div>
            <div class="message-content">
              <div class="message-text typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </el-scrollbar>

        <!-- 输入区域 -->
        <div class="chat-input">
          <!-- 附件预览 -->
          <div v-if="attachments.length" class="attachment-preview">
            <el-tag
              v-for="att in attachments"
              :key="att.id"
              closable
              @close="removeAttachment(att)"
              type="info"
            >
              <el-icon><Document /></el-icon>
              {{ att.fileName }}
            </el-tag>
          </div>
          <div class="input-row">
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileChange"
              accept=".pdf,.doc,.docx,.txt,.md"
            >
              <el-button :icon="Paperclip" circle :disabled="sending" />
            </el-upload>
            <el-input
              v-model="inputMessage"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 4 }"
              placeholder="输入消息，按Enter发送..."
              @keydown.enter.exact.prevent="sendMessage"
              :disabled="sending"
            />
            <el-button
              v-if="!sending"
              type="primary"
              :icon="Promotion"
              @click="sendMessage"
              :disabled="!inputMessage.trim() && !attachments.length"
            >
              发送
            </el-button>
            <el-button
              v-else
              type="danger"
              @click="stopGeneration"
            >
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
          </div>
          <div class="input-options">
            <el-checkbox v-model="enableRag">关联知识库</el-checkbox>
          </div>
        </div>
      </template>
    </div>

    <!-- 关联图谱面板（仅显示图谱） -->
    <div class="related-panel" v-if="currentSessionId && ragNodes.length">
      <div class="panel-header">
        <el-icon><Connection /></el-icon>
        <span>关联图谱</span>
      </div>
      <el-scrollbar height="calc(100vh - 200px)">
        <!-- 图谱可视化 -->
        <div ref="graphChartRef" class="related-graph" v-show="ragNodes.length > 0"></div>
        <!-- 节点列表 -->
        <div
          v-for="node in ragNodes"
          :key="node.id"
          class="related-item clickable"
          @click="goToNode(node)"
        >
          <div class="related-title">
            <el-icon><Connection /></el-icon>
            {{ node.name }}
            <el-icon class="link-icon"><Right /></el-icon>
          </div>
          <div class="related-meta">
            <el-tag size="small" type="success">{{ node.nodeType }}</el-tag>
            <span>相似度: {{ (node.score * 100).toFixed(1) }}%</span>
          </div>
          <div class="related-content">{{ node.description }}</div>
          <div v-if="node.relations?.length" class="node-relations">
            <span v-for="(rel, idx) in node.relations" :key="idx" class="relation-tag">
              {{ rel.name }} → {{ rel.targetNodeName }}
            </span>
          </div>
        </div>
        <el-empty v-if="!ragNodes.length" description="暂无关联节点" :image-size="60" />
      </el-scrollbar>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog v-model="renameDialogVisible" title="重命名会话" width="400px">
      <el-input v-model="newSessionTitle" placeholder="输入新标题" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, MoreFilled, User, ChatDotRound, Document, Loading, Paperclip, Promotion, Connection, Right, VideoPause, Folder } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import * as echarts from 'echarts'

const router = useRouter()

// Markdown渲染器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

// 状态
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const sending = ref(false)
const enableRag = ref(true)
const attachments = ref([])
const uploadRef = ref(null)
const messageScrollRef = ref(null)
const graphChartRef = ref(null)
let graphChart = null

// 流式响应状态
const streamingMessageId = ref(null)
const streamingContent = ref('')
let cancelStream = null

// RAG结果
const ragDocuments = ref([])
const ragNodes = ref([])

// 重命名对话框
const renameDialogVisible = ref(false)
const renameSessionId = ref(null)
const newSessionTitle = ref('')

// 初始化
onMounted(async () => {
  await loadSessions()
})

onUnmounted(() => {
  if (graphChart) {
    graphChart.dispose()
  }
  // 取消任何进行中的流式请求
  if (cancelStream) {
    cancelStream()
  }
})

// 加载会话列表
const loadSessions = async () => {
  try {
    const res = await chatApi.listSessions()
    sessions.value = res.data || []
    // 如果有会话，默认选中第一个
    if (sessions.value.length > 0 && !currentSessionId.value) {
      selectSession(sessions.value[0])
    }
  } catch (error) {
    console.error('加载会话失败:', error)
  }
}

// 创建新会话
const createSession = async () => {
  try {
    const res = await chatApi.createSession()
    sessions.value.unshift(res.data)
    selectSession(res.data)
    ElMessage.success('创建成功')
  } catch (error) {
    console.error('创建会话失败:', error)
  }
}

// 选择会话
const selectSession = async (session) => {
  currentSessionId.value = session.id
  ragDocuments.value = []
  ragNodes.value = []
  await loadMessages()
}

// 加载消息历史
const loadMessages = async () => {
  if (!currentSessionId.value) return
  try {
    const res = await chatApi.getMessages(currentSessionId.value)
    messages.value = res.data || []
    // 找到最后一条assistant消息的RAG数据
    for (let i = messages.value.length - 1; i >= 0; i--) {
      const msg = messages.value[i]
      if (msg.role === 'assistant') {
        if (msg.ragDocuments?.length) ragDocuments.value = msg.ragDocuments
        if (msg.ragNodes?.length) ragNodes.value = msg.ragNodes
        break
      }
    }
    nextTick(() => {
      scrollToBottom()
      renderGraph()
    })
  } catch (error) {
    console.error('加载消息失败:', error)
  }
}

// 发送消息（流式）
const sendMessage = async () => {
  const content = inputMessage.value.trim()
  if (!content && !attachments.value.length) return

  sending.value = true
  streamingContent.value = ''
  streamingMessageId.value = null

  // 先添加用户消息到列表（立即显示）
  const tempUserMsg = {
    id: 'temp-user-' + Date.now(),
    role: 'user',
    content: content,
    attachments: attachments.value.map(a => ({ id: a.id, fileName: a.fileName })),
    createTime: new Date().toISOString()
  }
  messages.value.push(tempUserMsg)

  // 清空输入
  const currentAttachmentIds = attachments.value.map(a => a.id)
  inputMessage.value = ''
  attachments.value = []

  nextTick(() => scrollToBottom())

  try {
    // 使用流式API
    cancelStream = chatApi.sendMessageStream(
      currentSessionId.value,
      {
        message: content,
        enableRag: enableRag.value,
        attachmentIds: currentAttachmentIds
      },
      // onMessage - 处理每个SSE消息
      (data) => {
        if (data.type === 'init') {
          // 初始化：更新用户消息ID，创建AI消息占位
          const userMsgIndex = messages.value.findIndex(m => m.id === tempUserMsg.id)
          if (userMsgIndex !== -1) {
            messages.value[userMsgIndex].id = data.userMessageId
          }

          // 创建AI消息占位（包含RAG文档以便内联显示）
          streamingMessageId.value = data.assistantMessageId
          const assistantMsg = {
            id: data.assistantMessageId,
            role: 'assistant',
            content: '',
            thinkingContent: null,
            ragDocuments: data.ragDocuments || [],
            ragNodes: data.ragNodes || [],
            createTime: new Date().toISOString(),
            isStreaming: true
          }
          messages.value.push(assistantMsg)

          // 更新全局RAG结果（用于右侧面板）
          if (data.ragDocuments?.length) ragDocuments.value = data.ragDocuments
          if (data.ragNodes?.length) ragNodes.value = data.ragNodes

          nextTick(() => {
            scrollToBottom()
            renderGraph()
          })
        } else if (data.type === 'chunk') {
          // 接收内容片段
          streamingContent.value += data.content

          // 更新消息内容
          const msgIndex = messages.value.findIndex(m => m.id === streamingMessageId.value)
          if (msgIndex !== -1) {
            messages.value[msgIndex].content = streamingContent.value
          }

          nextTick(() => scrollToBottom())
        } else if (data.type === 'done') {
          // 流式响应完成
          const msgIndex = messages.value.findIndex(m => m.id === streamingMessageId.value)
          if (msgIndex !== -1) {
            messages.value[msgIndex].content = data.content
            messages.value[msgIndex].thinkingContent = data.thinkingContent
            messages.value[msgIndex].isStreaming = false
          }
        } else if (data.type === 'error') {
          ElMessage.error(data.message || '发送失败')
        }
      },
      // onError - 处理错误
      (error) => {
        console.error('流式响应错误:', error)
        ElMessage.error('发送失败，请重试')
        sending.value = false
        streamingMessageId.value = null
      },
      // onComplete - 流式响应完成
      () => {
        sending.value = false
        streamingMessageId.value = null
        cancelStream = null
        // 更新会话列表
        loadSessions()
      }
    )
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送失败，请重试')
    sending.value = false
  }
}

// 停止生成
const stopGeneration = () => {
  if (cancelStream) {
    cancelStream()
    cancelStream = null
    sending.value = false
    streamingMessageId.value = null
    ElMessage.info('已停止生成')
  }
}

// 处理文件选择
const handleFileChange = async (uploadFile) => {
  try {
    const res = await chatApi.uploadAttachment(uploadFile.raw)
    attachments.value.push(res.data)
    ElMessage.success('附件上传成功')
  } catch (error) {
    console.error('上传附件失败:', error)
    ElMessage.error('附件上传失败')
  }
}

// 移除附件
const removeAttachment = (att) => {
  attachments.value = attachments.value.filter(a => a.id !== att.id)
}

// 会话操作菜单
const handleSessionCommand = async (command, session) => {
  if (command === 'rename') {
    renameSessionId.value = session.id
    newSessionTitle.value = session.title
    renameDialogVisible.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm('确定删除该对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await chatApi.deleteSession(session.id)
      if (currentSessionId.value === session.id) {
        currentSessionId.value = null
        messages.value = []
      }
      await loadSessions()
      ElMessage.success('删除成功')
    }).catch(() => {})
  }
}

// 确认重命名
const confirmRename = async () => {
  if (!newSessionTitle.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  try {
    await chatApi.updateSessionTitle(renameSessionId.value, newSessionTitle.value)
    await loadSessions()
    renameDialogVisible.value = false
    ElMessage.success('重命名成功')
  } catch (error) {
    console.error('重命名失败:', error)
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageScrollRef.value) {
    messageScrollRef.value.setScrollTop(99999)
  }
}

// 渲染Markdown
const renderMarkdown = (content) => {
  if (!content) return ''
  return md.render(content)
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

// 渲染关联图谱
const renderGraph = () => {
  if (!graphChartRef.value || !ragNodes.value.length) return

  // 检查容器是否有有效尺寸
  const container = graphChartRef.value
  if (container.offsetWidth === 0 || container.offsetHeight === 0) {
    return
  }

  // 如果已有实例，先销毁再重新创建（解决Tab切换后尺寸问题）
  if (graphChart) {
    graphChart.dispose()
    graphChart = null
  }

  graphChart = echarts.init(container)

  const nodes = []
  const links = []
  const nodeMap = new Map()

  // 添加主节点
  ragNodes.value.forEach((node, index) => {
    if (!nodeMap.has(node.id)) {
      nodeMap.set(node.id, true)
      nodes.push({
        id: String(node.id),
        name: node.name,
        category: 0,
        symbolSize: 40,
        label: { show: true }
      })
    }

    // 添加关联节点和关系
    if (node.relations) {
      node.relations.forEach(rel => {
        if (!nodeMap.has(rel.targetNodeId)) {
          nodeMap.set(rel.targetNodeId, true)
          nodes.push({
            id: String(rel.targetNodeId),
            name: rel.targetNodeName,
            category: 1,
            symbolSize: 30,
            label: { show: true }
          })
        }
        links.push({
          source: String(node.id),
          target: String(rel.targetNodeId),
          name: rel.name
        })
      })
    }
  })

  // 如果没有节点数据，不渲染
  if (nodes.length === 0) return

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return params.data.name
        } else if (params.dataType === 'edge') {
          return params.data.name || ''
        }
        return ''
      }
    },
    legend: {
      data: ['匹配节点', '关联节点'],
      bottom: 0
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links: links,
      categories: [
        { name: '匹配节点', itemStyle: { color: '#409eff' } },
        { name: '关联节点', itemStyle: { color: '#67c23a' } }
      ],
      roam: true,
      draggable: true,
      label: {
        show: true,
        position: 'right',
        formatter: '{b}',
        fontSize: 11
      },
      force: {
        repulsion: 150,
        edgeLength: [80, 120],
        gravity: 0.1
      },
      lineStyle: {
        color: '#aaa',
        curveness: 0.2,
        width: 1.5
      },
      edgeLabel: {
        show: true,
        formatter: '{c}',
        fontSize: 10,
        color: '#666'
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 3
        }
      }
    }]
  }

  graphChart.setOption(option)

  // 点击节点跳转
  graphChart.on('click', (params) => {
    if (params.dataType === 'node') {
      const nodeId = params.data.id
      router.push({ path: '/', query: { nodeId: nodeId, keyword: params.data.name } })
    }
  })
}

// 监听RAG节点变化，自动渲染图谱
watch(ragNodes, () => {
  if (ragNodes.value.length > 0) {
    nextTick(() => {
      setTimeout(() => {
        renderGraph()
      }, 100)
    })
  }
}, { deep: true })

// 跳转到文档详情页
const goToDocument = (doc) => {
  router.push({ path: '/documents', query: { id: doc.id } })
}

// 跳转到节点详情页（图谱页面并聚焦该节点）
const goToNode = (node) => {
  router.push({ path: '/', query: { nodeId: node.id, keyword: node.name } })
}
</script>

<style lang="scss" scoped>
.chat-view {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

// 会话侧边栏
.chat-sidebar {
  width: 260px;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;

  .sidebar-header {
    padding: 16px;
    border-bottom: 1px solid #e6e6e6;

    .el-button {
      width: 100%;
    }
  }

  .session-list {
    flex: 1;
    padding: 8px;
  }

  .session-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    margin-bottom: 4px;

    &:hover {
      background: #f5f7fa;
    }

    &.active {
      background: #ecf5ff;
    }

    .session-info {
      flex: 1;
      overflow: hidden;
    }

    .session-title {
      font-size: 14px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .session-meta {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }

    .session-more {
      opacity: 0;
      transition: opacity 0.2s;
    }

    &:hover .session-more {
      opacity: 1;
    }
  }
}

// 对话主区域
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;

  .chat-empty {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .message-list {
    flex: 1;
    padding: 20px;
  }

  .message-item {
    display: flex;
    margin-bottom: 24px;

    &.user {
      flex-direction: row-reverse;

      .message-content {
        align-items: flex-end;
      }

      .message-text {
        background: #409eff;
        color: #fff;
      }
    }

    &.assistant {
      .message-text {
        background: #f4f4f5;
      }
    }

    .message-avatar {
      margin: 0 12px;
    }

    .message-content {
      max-width: 70%;
      display: flex;
      flex-direction: column;
    }

    .thinking-block {
      margin-bottom: 8px;
      border: 1px solid #e6e6e6;
      border-radius: 8px;

      :deep(.el-collapse-item__header) {
        padding: 0 12px;
        font-size: 12px;
        color: #909399;
      }

      .thinking-content {
        padding: 12px;
        font-size: 13px;
        color: #666;
        background: #fafafa;
        white-space: pre-wrap;
      }
    }

    .message-text {
      padding: 12px 16px;
      border-radius: 8px;
      line-height: 1.6;

      :deep(pre) {
        margin: 8px 0;
        border-radius: 4px;
        overflow-x: auto;
      }

      :deep(code) {
        font-family: Consolas, Monaco, monospace;
      }

      :deep(p) {
        margin: 0 0 8px 0;

        &:last-child {
          margin-bottom: 0;
        }
      }

      :deep(ul), :deep(ol) {
        padding-left: 20px;
        margin: 8px 0;
      }

      &.typing {
        display: flex;
        gap: 4px;

        span {
          width: 8px;
          height: 8px;
          background: #999;
          border-radius: 50%;
          animation: typing 1.4s infinite ease-in-out both;

          &:nth-child(1) { animation-delay: -0.32s; }
          &:nth-child(2) { animation-delay: -0.16s; }
        }
      }
    }

    .message-attachments {
      margin-top: 8px;
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }

    // 内联关联文档样式
    .inline-related-docs {
      margin-top: 12px;
      padding: 12px;
      background: #f8fafc;
      border-radius: 8px;
      border: 1px solid #e6e6e6;

      .related-docs-header {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 13px;
        font-weight: 500;
        color: #606266;
        margin-bottom: 10px;
      }

      .related-docs-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .related-doc-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        background: #fff;
        border-radius: 6px;
        border: 1px solid #e6e6e6;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          border-color: #409eff;
          box-shadow: 0 2px 6px rgba(64, 158, 255, 0.15);

          .link-arrow {
            opacity: 1;
            transform: translateX(0);
          }
        }

        .doc-name {
          flex: 1;
          font-size: 13px;
          color: #303133;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .doc-score {
          font-size: 12px;
          color: #909399;
        }

        .link-arrow {
          color: #409eff;
          opacity: 0;
          transform: translateX(-4px);
          transition: all 0.2s;
        }
      }
    }

    .message-time {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }
  }
}

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

// 流式输出光标
.streaming-cursor {
  display: inline-block;
  width: 8px;
  height: 18px;
  background-color: #409eff;
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

// 输入区域
.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #e6e6e6;

  .attachment-preview {
    margin-bottom: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .input-row {
    display: flex;
    gap: 12px;
    align-items: flex-end;

    .el-input {
      flex: 1;
    }
  }

  .input-options {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }
}

// 关联内容面板
.related-panel {
  width: 350px;
  border-left: 1px solid #e6e6e6;
  padding: 16px;

  .panel-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 15px;
    font-weight: 500;
    color: #303133;
    padding-bottom: 12px;
    margin-bottom: 12px;
    border-bottom: 1px solid #e6e6e6;
  }

  .related-item {
    padding: 12px;
    border: 1px solid #e6e6e6;
    border-radius: 8px;
    margin-bottom: 12px;
    transition: all 0.2s;

    &.clickable {
      cursor: pointer;

      &:hover {
        border-color: #409eff;
        box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);

        .link-icon {
          opacity: 1;
          transform: translateX(0);
        }
      }
    }

    .related-title {
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;

      .link-icon {
        margin-left: auto;
        color: #409eff;
        opacity: 0;
        transform: translateX(-4px);
        transition: all 0.2s;
      }
    }

    .related-meta {
      margin-top: 8px;
      font-size: 12px;
      color: #909399;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .related-content {
      margin-top: 8px;
      font-size: 13px;
      color: #666;
      display: -webkit-box;
      -webkit-line-clamp: 3;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .node-relations {
      margin-top: 8px;
      display: flex;
      flex-wrap: wrap;
      gap: 4px;

      .relation-tag {
        font-size: 12px;
        padding: 2px 6px;
        background: #f0f9eb;
        color: #67c23a;
        border-radius: 4px;
      }
    }
  }

  .related-graph {
    height: 200px;
    margin-bottom: 16px;
    border: 1px solid #e6e6e6;
    border-radius: 8px;
  }
}
</style>
