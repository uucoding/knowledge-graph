<template>
  <div class="chat-view">
    <aside class="chat-sidebar" aria-label="会话列表">
      <div class="sidebar-header">
        <el-button
          type="primary"
          :icon="Plus"
          :loading="creatingSession"
          :disabled="!service.online"
          @click="createSession"
        >
          新对话
        </el-button>
      </div>

      <AsyncState
        v-if="showSidebarState"
        class="sidebar-state"
        compact
        :loading="sessionsLoading"
        :offline="!service.online"
        :error="sessionsError"
        :empty="!sessionsLoading && !sessionsError && !sessions.length"
        empty-title="还没有对话"
        empty-description="新建对话后，问答会按会话保存在本地。"
        error-title="会话列表没有加载成功"
        @retry="retrySessions"
      />

      <el-scrollbar v-else class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
        >
          <button type="button" class="session-select" @click="selectSession(session)">
            <span class="session-title">{{ session.title || '未命名会话' }}</span>
            <span class="session-meta">{{ formatTime(session.lastMessageTime || session.createTime) }}</span>
          </button>
          <el-dropdown trigger="click" @command="handleSessionCommand($event, session)" @click.stop>
            <button type="button" class="session-more" aria-label="会话操作">
              <el-icon><MoreFilled /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename" :disabled="!service.online">重命名</el-dropdown-item>
                <el-dropdown-item command="delete" divided :disabled="!service.online">删除</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-scrollbar>
    </aside>

    <section class="chat-main" aria-label="对话内容">
      <div v-if="!currentSessionId" class="chat-empty">
        <el-empty
          :description="service.online ? '选择或创建一个对话开始' : '离线时没有可查看的会话'"
          :image-size="72"
        />
        <p class="empty-note">
          {{ service.online
            ? '对话只保存在本地。开启「关联知识库」后，回答会引用文档、页码与图谱节点。'
            : '恢复本地数据服务后，可新建对话并继续提问。' }}
        </p>
      </div>

      <template v-else>
        <div class="chat-message-head">
          <div>
            <h2>{{ currentSessionTitle }}</h2>
            <p>消息保存在本地会话中</p>
          </div>
        </div>

        <AsyncState
          v-if="messagesLoading || messagesError"
          class="chat-state"
          compact
          :loading="messagesLoading"
          :offline="!service.online"
          :error="messagesError"
          error-title="消息没有加载成功"
          @retry="retryMessages"
        />

        <el-scrollbar v-else ref="messageScrollRef" class="message-list" aria-live="polite">
          <div
            v-for="message in messages"
            :key="message.id"
            :class="['message-item', message.role]"
          >
            <div class="message-avatar">
              <el-avatar :size="32" :icon="message.role === 'user' ? User : ChatDotRound" />
            </div>
            <div class="message-content">
              <el-collapse v-if="message.thinkingContent" class="thinking-block">
                <el-collapse-item title="思考过程">
                  <div class="thinking-content">{{ message.thinkingContent }}</div>
                </el-collapse-item>
              </el-collapse>

              <div class="message-text" v-html="renderMarkdown(message.content)"></div>
              <span v-if="message.isStreaming" class="streaming-cursor" aria-hidden="true"></span>
              <p v-if="message.failed" class="message-failed">回答中断。检查本地服务后可重新发送。</p>

              <div
                v-if="message.role === 'assistant' && message.ragDocuments?.length"
                class="inline-related-docs"
              >
                <div class="related-docs-header">
                  <el-icon><Folder /></el-icon>
                  <span>关联文档</span>
                </div>
                <div class="related-docs-list">
                  <button
                    v-for="doc in message.ragDocuments"
                    :key="doc.chunkId || doc.id"
                    type="button"
                    class="related-doc-item"
                    @click="goToDocument(doc)"
                  >
                    <span class="doc-line">
                      <el-icon><Document /></el-icon>
                      <span class="doc-name">{{ doc.name }}</span>
                      <span class="doc-score">{{ formatScore(doc.score) }}</span>
                    </span>
                    <span class="doc-meta">
                      <span v-if="doc.pageNum && doc.pageNum > 0" class="doc-chip is-page">
                        第 {{ doc.pageNum }} 页
                      </span>
                      <span class="doc-chip">{{ (doc.fileType || '').toUpperCase() }}</span>
                      <el-icon class="link-arrow"><Right /></el-icon>
                    </span>
                  </button>
                </div>
              </div>

              <div v-if="message.attachments?.length" class="message-attachments">
                <el-tag v-for="att in message.attachments" :key="att.id" size="small" type="info">
                  <el-icon><Document /></el-icon>
                  {{ att.fileName }}
                </el-tag>
              </div>

              <div class="message-footer">
                <span class="message-time">{{ formatTime(message.createTime) }}</span>
                <button
                  v-if="message.role === 'assistant' && !message.isStreaming && message.content"
                  type="button"
                  class="message-action"
                  @click="copyMessage(message)"
                >
                  复制回答
                </button>
              </div>
            </div>
          </div>

          <div v-if="sending && !streamingMessageId" class="message-item assistant">
            <div class="message-avatar">
              <el-avatar :size="32" :icon="ChatDotRound" />
            </div>
            <div class="message-content">
              <div class="message-text typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </el-scrollbar>

        <div class="chat-input">
          <p v-if="!service.online" class="input-offline">
            本地数据服务离线，输入已暂停。恢复连接后可继续对话。
          </p>

          <div v-if="attachments.length" class="attachment-preview">
            <el-tag
              v-for="att in attachments"
              :key="att.id"
              closable
              type="info"
              @close="removeAttachment(att)"
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
              :disabled="!service.online"
              accept=".pdf,.doc,.docx,.txt,.md"
              tabindex="-1"
            >
              <el-button
                :icon="Paperclip"
                circle
                :disabled="!service.online || sending"
                aria-label="添加附件"
              />
            </el-upload>

            <el-input
              v-model="inputMessage"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 4 }"
              :placeholder="service.online ? '输入消息，按 Enter 发送' : '本地服务离线，恢复后可继续对话'"
              :disabled="!service.online || sending"
              @keydown.enter.exact.prevent="sendMessage"
            />

            <el-button
              v-if="!sending"
              type="primary"
              :icon="Promotion"
              :disabled="!service.online || (!inputMessage.trim() && !attachments.length)"
              @click="sendMessage"
            >
              发送
            </el-button>
            <el-button v-else type="danger" @click="stopGeneration">
              <el-icon><VideoPause /></el-icon>
              停止
            </el-button>
          </div>

          <div class="input-options">
            <el-checkbox v-model="enableRag" :disabled="!service.online">关联知识库</el-checkbox>
            <span class="input-hint">回答会标注文档、页码与相似度。</span>
          </div>
        </div>
      </template>
    </section>

    <aside
      v-if="currentSessionId && ragNodes.length"
      class="related-panel"
      aria-label="关联图谱"
    >
      <div class="panel-header">
        <el-icon><Connection /></el-icon>
        <span>关联图谱</span>
        <span class="panel-count">{{ ragNodes.length }}</span>
      </div>
      <el-scrollbar class="related-scroll">
        <div ref="graphChartRef" class="related-graph"></div>
        <button
          v-for="node in ragNodes"
          :key="node.id"
          type="button"
          class="related-item"
          @click="goToNode(node)"
        >
          <div class="related-title">
            <el-icon><Connection /></el-icon>
            {{ node.name }}
            <el-icon class="link-icon"><Right /></el-icon>
          </div>
          <div class="related-meta">
            <span class="node-type-chip">{{ node.nodeType }}</span>
            <span>相似度 {{ formatScore(node.score) }}</span>
          </div>
          <div class="related-content">{{ node.description }}</div>
          <div v-if="node.relations?.length" class="node-relations">
            <span v-for="(rel, idx) in node.relations" :key="idx" class="relation-tag">
              {{ rel.name }} → {{ rel.targetNodeName }}
            </span>
          </div>
        </button>
      </el-scrollbar>
    </aside>

    <el-dialog v-model="renameDialogVisible" title="重命名会话" width="400px">
      <el-input v-model="newSessionTitle" placeholder="输入新标题" :disabled="!service.online" />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!service.online" @click="confirmRename">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { chatApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ChatDotRound,
  Connection,
  Document,
  Folder,
  MoreFilled,
  Paperclip,
  Plus,
  Promotion,
  Right,
  User,
  VideoPause
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import * as echarts from 'echarts'
import AsyncState from '@/components/AsyncState.vue'
import { GRAPH_COLORS, formatScore } from '@/constants/design'
import { useServiceStore } from '@/stores/service'

const router = useRouter()
const service = useServiceStore()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  typographer: true,
  breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer')
  return self.renderToken(tokens, idx, options)
}

const sessions = ref([])
const sessionsLoading = ref(true)
const sessionsError = ref('')
const creatingSession = ref(false)
const currentSessionId = ref(null)
const messages = ref([])
const messagesLoading = ref(false)
const messagesError = ref('')
const inputMessage = ref('')
const sending = ref(false)
const enableRag = ref(true)
const attachments = ref([])
const uploadRef = ref(null)
const messageScrollRef = ref(null)
const graphChartRef = ref(null)
let graphChart = null
let cancelStream = null
const streamingMessageId = ref(null)
const streamingContent = ref('')
const ragDocuments = ref([])
const ragNodes = ref([])

const renameDialogVisible = ref(false)
const renameSessionId = ref(null)
const newSessionTitle = ref('')

const currentSessionTitle = computed(() => {
  const session = sessions.value.find(item => item.id === currentSessionId.value)
  return session?.title || '未命名会话'
})

const showSidebarState = computed(() => (
  sessionsLoading.value || Boolean(sessionsError.value) || !sessions.value.length
))

onMounted(async () => {
  await loadSessions()
})

onUnmounted(() => {
  if (graphChart) {
    graphChart.dispose()
  }
  if (cancelStream) {
    cancelStream()
  }
})

const loadSessions = async () => {
  sessionsLoading.value = true
  sessionsError.value = ''
  try {
    const res = await chatApi.listSessions()
    sessions.value = res.data || []
    if (sessions.value.length > 0 && !currentSessionId.value) {
      selectSession(sessions.value[0])
    }
  } catch (error) {
    sessions.value = []
    sessionsError.value = error.friendlyMessage || '会话列表没有加载成功'
  } finally {
    sessionsLoading.value = false
  }
}

const retrySessions = async () => {
  await service.refresh()
  if (service.online) await loadSessions()
}

const createSession = async () => {
  if (!service.online) return
  creatingSession.value = true
  try {
    const res = await chatApi.createSession()
    sessions.value.unshift(res.data)
    selectSession(res.data)
    ElMessage.success('已创建新对话')
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '创建会话失败')
  } finally {
    creatingSession.value = false
  }
}

const selectSession = async (session) => {
  currentSessionId.value = session.id
  ragDocuments.value = []
  ragNodes.value = []
  await loadMessages()
}

const loadMessages = async () => {
  if (!currentSessionId.value) return
  messagesLoading.value = true
  messagesError.value = ''
  try {
    const res = await chatApi.getMessages(currentSessionId.value)
    messages.value = res.data || []
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
    messages.value = []
    messagesError.value = error.friendlyMessage || '消息没有加载成功'
  } finally {
    messagesLoading.value = false
  }
}

const retryMessages = async () => {
  await service.refresh()
  if (service.online) await loadMessages()
}

const markStreamFailed = (message) => {
  const index = messages.value.findIndex(item => item.id === streamingMessageId.value)
  if (index !== -1) {
    messages.value[index].isStreaming = false
    messages.value[index].failed = true
    if (!messages.value[index].content) messages.value[index].content = '回答没有完成。'
  } else {
    messages.value.push({
      id: `failed-${Date.now()}`,
      role: 'assistant',
      content: '回答没有完成。',
      failed: true,
      createTime: new Date().toISOString()
    })
  }
  ElMessage.error(message)
  sending.value = false
  streamingMessageId.value = null
}

const sendMessage = async () => {
  if (!service.online) return
  const content = inputMessage.value.trim()
  if (!content && !attachments.value.length) return

  sending.value = true
  streamingContent.value = ''
  streamingMessageId.value = null

  const tempUserMsg = {
    id: `temp-user-${Date.now()}`,
    role: 'user',
    content,
    attachments: attachments.value.map(item => ({ id: item.id, fileName: item.fileName })),
    createTime: new Date().toISOString()
  }
  messages.value.push(tempUserMsg)

  const currentAttachmentIds = attachments.value.map(item => item.id)
  inputMessage.value = ''
  attachments.value = []
  nextTick(() => scrollToBottom())

  try {
    cancelStream = chatApi.sendMessageStream(
      currentSessionId.value,
      {
        message: content,
        enableRag: enableRag.value,
        attachmentIds: currentAttachmentIds
      },
      (data) => {
        if (data.type === 'init') {
          const userMsgIndex = messages.value.findIndex(item => item.id === tempUserMsg.id)
          if (userMsgIndex !== -1) {
            messages.value[userMsgIndex].id = data.userMessageId
          }

          streamingMessageId.value = data.assistantMessageId
          messages.value.push({
            id: data.assistantMessageId,
            role: 'assistant',
            content: '',
            thinkingContent: null,
            ragDocuments: data.ragDocuments || [],
            ragNodes: data.ragNodes || [],
            createTime: new Date().toISOString(),
            isStreaming: true
          })

          if (data.ragDocuments?.length) ragDocuments.value = data.ragDocuments
          if (data.ragNodes?.length) ragNodes.value = data.ragNodes

          nextTick(() => {
            scrollToBottom()
            renderGraph()
          })
        } else if (data.type === 'chunk') {
          streamingContent.value += data.content
          const msgIndex = messages.value.findIndex(item => item.id === streamingMessageId.value)
          if (msgIndex !== -1) {
            messages.value[msgIndex].content = streamingContent.value
          }
          nextTick(() => scrollToBottom())
        } else if (data.type === 'done') {
          const msgIndex = messages.value.findIndex(item => item.id === streamingMessageId.value)
          if (msgIndex !== -1) {
            messages.value[msgIndex].content = data.content
            messages.value[msgIndex].thinkingContent = data.thinkingContent
            messages.value[msgIndex].isStreaming = false
          }
        } else if (data.type === 'error') {
          markStreamFailed(data.message || '回答中断，请检查本地服务')
        }
      },
      () => {
        markStreamFailed('回答中断，请检查本地服务')
      },
      () => {
        sending.value = false
        streamingMessageId.value = null
        cancelStream = null
        loadSessions()
      }
    )
  } catch (error) {
    markStreamFailed(error.friendlyMessage || '发送失败，请重试')
  }
}

const stopGeneration = () => {
  if (cancelStream) {
    cancelStream()
    cancelStream = null
    sending.value = false
    streamingMessageId.value = null
    ElMessage.info('已停止生成')
  }
}

const handleFileChange = async (uploadFile) => {
  if (!service.online) return
  try {
    const res = await chatApi.uploadAttachment(uploadFile.raw)
    attachments.value.push(res.data)
    ElMessage.success('附件已上传')
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '附件上传失败')
  }
}

const removeAttachment = (att) => {
  attachments.value = attachments.value.filter(item => item.id !== att.id)
}

const handleSessionCommand = async (command, session) => {
  if (!service.online) return
  if (command === 'rename') {
    renameSessionId.value = session.id
    newSessionTitle.value = session.title
    renameDialogVisible.value = true
  } else if (command === 'delete') {
    ElMessageBox.confirm('删除该对话？本地记录会一并移除。', '删除对话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      await chatApi.deleteSession(session.id)
      if (currentSessionId.value === session.id) {
        currentSessionId.value = null
        messages.value = []
      }
      await loadSessions()
      ElMessage.success('已删除对话')
    }).catch(() => {})
  }
}

const confirmRename = async () => {
  if (!service.online) return
  if (!newSessionTitle.value.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  try {
    await chatApi.updateSessionTitle(renameSessionId.value, newSessionTitle.value.trim())
    await loadSessions()
    renameDialogVisible.value = false
    ElMessage.success('已重命名')
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '重命名失败')
  }
}

const scrollToBottom = () => {
  if (messageScrollRef.value) {
    messageScrollRef.value.setScrollTop(99999)
  }
}

const renderMarkdown = (content) => (content ? md.render(content) : '')

const copyMessage = async (message) => {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(message.content || '')
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = message.content || ''
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      textarea.remove()
    }
    ElMessage.success('已复制回答')
  } catch (error) {
    ElMessage.error('复制失败，请手动选择文本')
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

const renderGraph = () => {
  if (!graphChartRef.value || !ragNodes.value.length) return
  const container = graphChartRef.value
  if (container.offsetWidth === 0 || container.offsetHeight === 0) return

  if (graphChart) {
    graphChart.dispose()
    graphChart = null
  }

  graphChart = echarts.init(container)

  const nodes = []
  const links = []
  const nodeMap = new Map()

  ragNodes.value.forEach(node => {
    if (!nodeMap.has(node.id)) {
      nodeMap.set(node.id, true)
      nodes.push({
        id: String(node.id),
        name: node.name,
        category: 0,
        symbolSize: 34,
        label: { show: true }
      })
    }

    if (node.relations) {
      node.relations.forEach(rel => {
        if (!nodeMap.has(rel.targetNodeId)) {
          nodeMap.set(rel.targetNodeId, true)
          nodes.push({
            id: String(rel.targetNodeId),
            name: rel.targetNodeName,
            category: 1,
            symbolSize: 26,
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

  if (nodes.length === 0) return

  graphChart.setOption({
    animationDuration: 300,
    tooltip: {
      trigger: 'item',
      formatter: (params) => (params.dataType === 'node' ? params.data.name : params.data.name || '')
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes,
      links,
      categories: [
        { name: '匹配节点', itemStyle: { color: GRAPH_COLORS[0] } },
        { name: '关联节点', itemStyle: { color: GRAPH_COLORS[1] } }
      ],
      roam: true,
      draggable: true,
      label: {
        show: true,
        position: 'right',
        formatter: '{b}',
        color: '#4e5969',
        fontSize: 11
      },
      force: {
        repulsion: 150,
        edgeLength: [80, 120],
        gravity: 0.1
      },
      lineStyle: {
        color: '#c9cdd4',
        curveness: 0.2,
        width: 1.5
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3 }
      }
    }]
  })

  graphChart.on('click', params => {
    if (params.dataType === 'node') {
      router.push({ path: '/', query: { nodeId: params.data.id } })
    }
  })
}

watch(ragNodes, () => {
  if (ragNodes.value.length > 0) {
    nextTick(() => {
      setTimeout(renderGraph, 100)
    })
  } else if (graphChart) {
    graphChart.dispose()
    graphChart = null
  }
}, { deep: true })

const goToDocument = (doc) => {
  router.push({ path: '/documents', query: { id: doc.id } })
}

const goToNode = (node) => {
  router.push({ path: '/', query: { nodeId: node.id } })
}
</script>

<style scoped lang="scss">
.chat-view {
  flex: 1;
  min-height: 0;
  display: flex;
  border: 1px solid var(--atlas-line);
  border-radius: 8px;
  background: var(--atlas-paper);
  overflow: hidden;
}

.chat-sidebar {
  width: 260px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--atlas-line);
  background: var(--atlas-paper);
}

.sidebar-header {
  flex: none;
  padding: 12px;
  border-bottom: 1px solid var(--atlas-line);
}

.sidebar-header .el-button {
  width: 100%;
}

.sidebar-state {
  flex: 1;
}

.session-list {
  flex: 1;
  min-height: 0;
}

.session-list :deep(.el-scrollbar__view) {
  padding: 8px;
}

.session-item {
  display: flex;
  align-items: stretch;
  gap: 2px;
  margin-bottom: 2px;
  border-radius: 4px;
  transition: background 200ms ease;
}

.session-item:hover {
  background: var(--atlas-desk);
}

.session-item.active {
  background: var(--atlas-blue-soft);
}

.session-select {
  flex: 1;
  min-width: 0;
  padding: 9px 4px 9px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

.session-title {
  display: block;
  overflow: hidden;
  color: var(--atlas-ink);
  font-size: 14px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-title {
  color: var(--atlas-blue);
  font-weight: 500;
}

.session-meta {
  display: block;
  margin-top: 2px;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.session-more {
  width: 30px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--atlas-text-muted);
  cursor: pointer;
  opacity: 0.55;
  transition: opacity 160ms ease, color 160ms ease;
}

.session-item:hover .session-more,
.session-more:focus-visible {
  opacity: 1;
}

.session-more:hover {
  color: var(--atlas-blue);
}

.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--atlas-paper);
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
}

.empty-note {
  max-width: 44ch;
  color: var(--atlas-text-muted);
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.chat-message-head {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--atlas-line);
}

.chat-message-head h2 {
  font-size: 15px;
}

.chat-message-head p {
  margin-top: 1px;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.chat-state {
  flex: 1;
}

.message-list {
  flex: 1;
  min-height: 0;
}

.message-list :deep(.el-scrollbar__view) {
  padding: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex: none;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 72%;
  min-width: 0;
}

.message-item.user .message-content {
  align-items: flex-end;
}

.thinking-block {
  width: 100%;
  margin-bottom: 8px;
  border: 1px solid var(--atlas-line);
  border-radius: 6px;
  background: var(--atlas-paper);
  overflow: hidden;
}

.thinking-block :deep(.el-collapse-item__header) {
  height: 32px;
  padding: 0 12px;
  border: 0;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.thinking-block :deep(.el-collapse-item__wrap) {
  border: 0;
}

.thinking-content {
  padding: 12px;
  background: var(--atlas-desk);
  color: var(--atlas-text);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message-text {
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--atlas-sunken);
  color: var(--atlas-ink);
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}

.message-item.user .message-text {
  background: var(--atlas-blue);
  color: #ffffff;
}

.message-text :deep(p) {
  margin: 0 0 8px;
}

.message-text :deep(p:last-child) {
  margin-bottom: 0;
}

.message-text :deep(ul),
.message-text :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-text :deep(a) {
  color: var(--atlas-blue);
  text-decoration: underline;
  text-underline-offset: 3px;
}

.message-item.user .message-text :deep(a) {
  color: #ffffff;
}

.message-text :deep(pre) {
  margin: 8px 0;
  padding: 12px;
  border: 1px solid var(--atlas-line);
  border-radius: 4px;
  background: var(--atlas-desk);
  color: var(--atlas-ink);
  overflow-x: auto;
}

.message-text :deep(code) {
  font-family: var(--atlas-mono);
  font-size: 13px;
}

.message-text.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  min-height: 40px;
}

.message-text.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--atlas-text-muted);
  animation: typing 1.4s infinite ease-in-out both;
}

.message-text.typing span:nth-child(1) {
  animation-delay: -0.32s;
}

.message-text.typing span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%,
  80%,
  100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.streaming-cursor {
  display: inline-block;
  width: 6px;
  height: 16px;
  margin-top: 2px;
  background: var(--atlas-blue);
  vertical-align: text-bottom;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

.message-failed {
  margin-top: 6px;
  color: var(--atlas-red-text);
  font-size: 12px;
}

.inline-related-docs {
  width: 100%;
  margin-top: 10px;
  padding: 10px;
  border: 1px solid var(--atlas-line);
  border-radius: 6px;
  background: var(--atlas-desk);
}

.related-docs-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: var(--atlas-text);
  font-size: 12px;
  font-weight: 500;
}

.related-docs-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.related-doc-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--atlas-line);
  border-radius: 4px;
  background: var(--atlas-paper);
  color: var(--atlas-text);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 200ms ease, box-shadow 200ms ease;
}

.related-doc-item:hover {
  border-color: var(--atlas-blue);
  box-shadow: var(--atlas-lift);
}

.related-doc-item:hover .link-arrow {
  opacity: 1;
  transform: translateX(0);
}

.doc-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.doc-line :deep(.el-icon) {
  flex: none;
}

.doc-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: var(--atlas-ink);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-score {
  flex: none;
  color: var(--atlas-text-muted);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.doc-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.doc-chip,
.node-type-chip {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border-radius: 4px;
  background: var(--atlas-sunken);
  color: var(--atlas-text);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
}

.doc-chip.is-page {
  background: #fff7e8;
  color: var(--atlas-orange-text);
}

.doc-meta .link-arrow {
  margin-left: auto;
}

.link-arrow {
  flex: none;
  color: var(--atlas-blue);
  opacity: 0;
  transform: translateX(-3px);
  transition: opacity 160ms ease, transform 160ms ease;
}

.message-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 5px;
}

.message-time {
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.message-action {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--atlas-text-muted);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.message-action:hover {
  color: var(--atlas-blue);
}

.chat-input {
  flex: none;
  padding: 12px 20px 16px;
  border-top: 1px solid var(--atlas-line);
}

.input-offline {
  margin-bottom: 8px;
  padding: 7px 10px;
  border: 1px solid #ffe4ba;
  border-radius: 4px;
  background: #fff7e8;
  color: var(--atlas-orange-text);
  font-size: 12px;
}

.attachment-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-row :deep(.el-input) {
  flex: 1;
}

.input-row :deep(.el-textarea__inner) {
  min-height: 32px !important;
  padding: 6px 12px;
  line-height: 1.5;
}

.input-options {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.input-hint {
  color: var(--atlas-text-muted);
}

.related-panel {
  width: 340px;
  flex: none;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--atlas-line);
  background: var(--atlas-paper);
}

.panel-header {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--atlas-line);
  color: var(--atlas-ink);
  font-size: 15px;
  font-weight: 600;
}

.panel-count {
  margin-left: auto;
  color: var(--atlas-text-muted);
  font-size: 12px;
  font-weight: 400;
  font-variant-numeric: tabular-nums;
}

.related-scroll {
  flex: 1;
  min-height: 0;
}

.related-scroll :deep(.el-scrollbar__view) {
  padding: 12px 16px 20px;
}

.related-graph {
  height: 180px;
  margin-bottom: 12px;
  border: 1px solid var(--atlas-line);
  border-radius: 6px;
}

.related-item {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid var(--atlas-line);
  border-radius: 6px;
  background: var(--atlas-paper);
  color: var(--atlas-text);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 200ms ease, box-shadow 200ms ease;
}

.related-item:hover {
  border-color: var(--atlas-blue);
  box-shadow: var(--atlas-lift);
}

.related-item:hover .link-icon {
  opacity: 1;
  transform: translateX(0);
}

.related-title {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--atlas-ink);
  font-size: 14px;
  font-weight: 500;
}

.link-icon {
  margin-left: auto;
  color: var(--atlas-blue);
  opacity: 0;
  transform: translateX(-3px);
  transition: opacity 160ms ease, transform 160ms ease;
}

.related-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.related-content {
  display: -webkit-box;
  margin-top: 8px;
  overflow: hidden;
  color: var(--atlas-text);
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.node-relations {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.relation-tag {
  padding: 2px 6px;
  border-radius: 4px;
  background: var(--atlas-sunken);
  color: var(--atlas-text);
  font-size: 12px;
}

@media (max-width: 1320px) {
  .related-panel {
    width: 300px;
  }
}

@media (max-width: 1120px) {
  .chat-sidebar {
    width: 220px;
  }

  .related-panel {
    display: none;
  }
}
</style>
