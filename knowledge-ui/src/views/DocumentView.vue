<template>
  <div class="document-view page-stack">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-upload
          ref="uploadRef"
          :show-file-list="false"
          :before-upload="handleUpload"
          :disabled="!service.online"
          accept=".pdf,.doc,.docx,.txt,.md"
          tabindex="-1"
        >
          <el-button type="primary" :disabled="!service.online">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
        </el-upload>
        <el-input
          v-model="searchForm.keyword"
          class="search-input"
          placeholder="搜索文档..."
          clearable
          aria-label="搜索文档"
        />
        <el-select
          v-model="searchForm.fileType"
          class="filter-select"
          placeholder="文件类型"
          clearable
          aria-label="按文件类型筛选"
        >
          <el-option label="PDF" value="pdf" />
          <el-option label="Word" value="docx" />
          <el-option label="TXT" value="txt" />
          <el-option label="Markdown" value="md" />
        </el-select>
        <el-select
          v-model="searchForm.status"
          class="filter-select"
          placeholder="状态"
          clearable
          aria-label="按处理状态筛选"
        >
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="处理失败" :value="3" />
        </el-select>
        <el-button @click="handleSearch">搜索</el-button>
        <el-button text @click="resetSearch">重置</el-button>
        <span class="toolbar-note">支持 PDF / Word / TXT / Markdown</span>
      </div>
    </el-card>

    <el-card class="list-card">
      <AsyncState
        v-if="showState"
        :loading="loading"
        :offline="!service.online"
        :error="loadError"
        :empty="!loading && !loadError && !documentList.length"
        empty-title="还没有文档"
        empty-description="上传第一份资料后，系统会解析、分块并抽取知识。"
        error-title="文档列表没有加载成功"
        @retry="retryLoad"
      >
        <template #action>
          <el-button type="primary" :disabled="!service.online" @click="triggerUpload">上传文档</el-button>
        </template>
      </AsyncState>

      <template v-else>
        <el-table :data="documentList">
          <el-table-column prop="id" label="ID" width="100" show-overflow-tooltip />
          <el-table-column prop="name" label="文档名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="fileType" label="类型" width="84">
            <template #default="{ row }">
              <el-tag size="small">{{ (row.fileType || '').toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fileSizeReadable" label="大小" width="84" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <StatusTag :tone="getStatusMeta(row.status).tone" :label="row.statusDesc || getStatusMeta(row.status).label" />
            </template>
          </el-table-column>
          <el-table-column prop="summary" label="摘要" min-width="140" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="140" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
              <el-dropdown trigger="click" @command="handleDocumentCommand($event, row)">
                <el-button type="info" link :disabled="!service.online">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="reparse" :disabled="!service.online || row.status === 1">重新解析</el-dropdown-item>
                    <el-dropdown-item command="summary" :disabled="!service.online || row.status !== 2">生成摘要</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button type="danger" link :disabled="!service.online" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="pagination.total > 0" class="pager">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadDocumentList"
            @current-change="loadDocumentList"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="文档详情" width="720px">
      <el-descriptions v-if="currentDocument" :column="2" border>
        <el-descriptions-item label="文档名称" :span="2">{{ currentDocument.name }}</el-descriptions-item>
        <el-descriptions-item label="原始文件名">{{ currentDocument.originalName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ currentDocument.fileType }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ currentDocument.fileSizeReadable }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag :tone="getStatusMeta(currentDocument.status).tone" :label="currentDocument.statusDesc || getStatusMeta(currentDocument.status).label" />
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentDocument.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentDocument.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          <el-scrollbar height="120px">
            <div class="markdown-body" v-html="renderMarkdown(currentDocument.summary || '暂无')"></div>
          </el-scrollbar>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentDocument.errorMsg">
          <span class="error-text">{{ currentDocument.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="content-preview" v-if="currentDocument?.contentPreview">
        <h4>内容预览</h4>
        <el-scrollbar height="200px">
          <pre>{{ currentDocument.contentPreview }}</pre>
        </el-scrollbar>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import { documentApi } from '@/api'
import AsyncState from '@/components/AsyncState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { statusMeta } from '@/constants/design'
import { useServiceStore } from '@/stores/service'

const route = useRoute()
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

const loading = ref(true)
const loadError = ref('')
const documentList = ref([])
const detailDialogVisible = ref(false)
const currentDocument = ref(null)
const uploadRef = ref(null)

const searchForm = reactive({ keyword: '', fileType: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const showState = computed(() => loading.value || Boolean(loadError.value) || !documentList.value.length)

const getStatusMeta = (status) => statusMeta(status)

const renderMarkdown = (content) => (content ? md.render(content) : '')

const loadDocumentList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await documentApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    documentList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
    if (documentList.value.length === 0 && pagination.total > 0 && pagination.pageNum > 1) {
      pagination.pageNum = 1
      await loadDocumentList()
    }
  } catch (error) {
    documentList.value = []
    pagination.total = 0
    loadError.value = error.friendlyMessage || '文档列表没有加载成功'
  } finally {
    loading.value = false
  }
}

const retryLoad = async () => {
  await service.refresh()
  if (service.online) await loadDocumentList()
}

const triggerUpload = () => {
  if (!service.online) return
  uploadRef.value?.$el?.querySelector('input')?.click()
}

const handleUpload = async (file) => {
  if (!service.online) return false
  try {
    await documentApi.upload(file)
    ElMessage.success('文档已上传，正在后台解析')
    loadDocumentList()
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '文档上传失败')
  }
  return false
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadDocumentList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.fileType = ''
  searchForm.status = null
  handleSearch()
}

const viewDetail = async (row) => {
  try {
    const res = await documentApi.detail(row.id)
    currentDocument.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '文档详情加载失败')
  }
}

const viewDetailById = async (id) => {
  try {
    const res = await documentApi.detail(id)
    currentDocument.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '文档详情加载失败')
  }
}

const handleReparse = async (row) => {
  if (!service.online) return
  try {
    await documentApi.reparse(row.id)
    ElMessage.success('已开始重新解析')
    loadDocumentList()
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '重新解析失败')
  }
}

const handleSummary = async (row) => {
  if (!service.online) return
  try {
    const res = await documentApi.generateSummary(row.id)
    ElMessage.success('摘要已生成')
    row.summary = res.data
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '摘要生成失败')
  }
}

const handleDocumentCommand = (command, row) => {
  if (command === 'reparse') handleReparse(row)
  if (command === 'summary') handleSummary(row)
}

const handleDelete = async (row) => {
  if (!service.online) return
  try {
    await ElMessageBox.confirm(
      `删除文档「${row.name}」？已生成的摘要与分块会一并移除，图谱中的抽取结果需要单独核对。`,
      '删除文档',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await documentApi.delete(row.id)
    ElMessage.success('文档已删除')
    loadDocumentList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.friendlyMessage || '文档删除失败')
    }
  }
}

watch(() => route.query.id, (newId) => {
  if (newId) viewDetailById(newId)
}, { immediate: true })

onMounted(() => {
  loadDocumentList()
})
</script>

<style scoped lang="scss">
.search-input {
  width: 220px;
}

.filter-select {
  width: 130px;
}

.content-preview {
  margin-top: 16px;
}

.content-preview h4 {
  margin-bottom: 8px;
  color: var(--atlas-text);
  font-size: 13px;
  font-weight: 500;
}

.content-preview pre {
  padding: 12px;
  border: 1px solid var(--atlas-line);
  border-radius: 4px;
  background: var(--atlas-desk);
  color: var(--atlas-text);
  font-family: var(--atlas-mono);
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.markdown-body {
  color: var(--atlas-text);
  font-size: 13px;
  line-height: 1.6;
}

.error-text {
  color: var(--atlas-red-text);
}
</style>
