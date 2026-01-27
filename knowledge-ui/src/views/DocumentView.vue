<template>
  <div class="document-view">
    <!-- 操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-upload
          :show-file-list="false"
          :before-upload="handleUpload"
          accept=".pdf,.doc,.docx,.txt,.md"
        >
          <el-button type="success">
            <el-icon><Upload /></el-icon>上传文档
          </el-button>
        </el-upload>
        <el-input v-model="searchForm.keyword" placeholder="搜索文档..." style="width: 250px" clearable />
        <el-select v-model="searchForm.fileType" placeholder="文件类型" clearable style="width: 120px">
          <el-option label="PDF" value="pdf" />
          <el-option label="Word" value="docx" />
          <el-option label="TXT" value="txt" />
          <el-option label="Markdown" value="md" />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待处理" :value="0" />
          <el-option label="处理中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="处理失败" :value="3" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
    </el-card>

    <!-- 文档列表 -->
    <el-card class="list-card">
      <el-table :data="documentList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="name" label="文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSizeReadable" label="大小" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
            <el-button type="info" link @click="handleReparse(row)" :disabled="row.status === 1">重新解析</el-button>
            <el-button type="success" link @click="handleSummary(row)" :disabled="row.status !== 2">生成摘要</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
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
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="文档详情" width="700px">
      <el-descriptions :column="2" border v-if="currentDocument">
        <el-descriptions-item label="文档名称" :span="2">{{ currentDocument.name }}</el-descriptions-item>
        <el-descriptions-item label="原始文件名">{{ currentDocument.originalName }}</el-descriptions-item>
        <el-descriptions-item label="文件类型">{{ currentDocument.fileType }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ currentDocument.fileSizeReadable }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentDocument.status)">{{ currentDocument.statusDesc }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentDocument.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentDocument.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">{{ currentDocument.summary || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentDocument.errorMsg">
          <el-text type="danger">{{ currentDocument.errorMsg }}</el-text>
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
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { documentApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const documentList = ref([])
const detailDialogVisible = ref(false)
const currentDocument = ref(null)

const searchForm = reactive({ keyword: '', fileType: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const loadDocumentList = async () => {
  loading.value = true
  try {
    const res = await documentApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    documentList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const handleUpload = async (file) => {
  try {
    await documentApi.upload(file)
    ElMessage.success('上传成功，正在后台解析')
    loadDocumentList()
  } catch (error) {
    console.error('上传失败:', error)
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
    console.error('获取详情失败:', error)
  }
}

// 根据ID查看详情
const viewDetailById = async (id) => {
  try {
    const res = await documentApi.detail(id)
    currentDocument.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

const handleReparse = async (row) => {
  try {
    await documentApi.reparse(row.id)
    ElMessage.success('正在重新解析')
    loadDocumentList()
  } catch (error) {
    console.error('重新解析失败:', error)
  }
}

const handleSummary = async (row) => {
  try {
    const res = await documentApi.generateSummary(row.id)
    ElMessage.success('摘要生成成功')
    row.summary = res.data
  } catch (error) {
    console.error('生成摘要失败:', error)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除文档"${row.name}"吗？`, '提示', { type: 'warning' })
    await documentApi.delete(row.id)
    ElMessage.success('删除成功')
    loadDocumentList()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

// 监听路由参数变化，自动打开详情
watch(() => route.query.id, (newId) => {
  if (newId) {
    viewDetailById(newId)
  }
}, { immediate: true })

onMounted(() => {
  loadDocumentList()
})
</script>

<style lang="scss" scoped>
.document-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card .toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.list-card .pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.content-preview {
  margin-top: 20px;

  h4 {
    margin-bottom: 10px;
    color: #606266;
  }

  pre {
    white-space: pre-wrap;
    word-wrap: break-word;
    font-size: 13px;
    color: #606266;
    background: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
  }
}
</style>
