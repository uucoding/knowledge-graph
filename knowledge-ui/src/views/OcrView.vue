<template>
  <div class="ocr-view">
    <!-- 操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-upload :show-file-list="false" :before-upload="handleUpload" accept="image/*">
          <el-button type="success">
            <el-icon><Upload /></el-icon>上传图片
          </el-button>
        </el-upload>
        <el-input v-model="searchForm.keyword" placeholder="搜索OCR文本..." style="width: 250px" clearable />
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

    <!-- OCR记录列表 -->
    <el-card class="list-card">
      <el-table :data="ocrList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="imageName" label="图片名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="imageType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.imageType?.toUpperCase() }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="imageSizeReadable" label="大小" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ocrText" label="识别文本" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
            <el-button type="info" link @click="handleReRecognize(row)" :disabled="row.status === 1">重新识别</el-button>
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
          @size-change="loadOcrList"
          @current-change="loadOcrList"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="OCR详情" width="800px">
      <div class="ocr-detail" v-if="currentRecord">
        <div class="image-preview">
          <el-image :src="currentRecord.imageUrl" fit="contain" style="max-height: 300px" />
        </div>
        <el-divider />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="图片名称" :span="2">{{ currentRecord.imageName }}</el-descriptions-item>
          <el-descriptions-item label="文件类型">{{ currentRecord.imageType }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ currentRecord.imageSizeReadable }}</el-descriptions-item>
          <el-descriptions-item label="识别语言">{{ currentRecord.language }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentRecord.status)">{{ currentRecord.statusDesc }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="ocr-text-box" v-if="currentRecord.ocrText">
          <h4>识别结果</h4>
          <el-scrollbar height="200px">
            <pre>{{ currentRecord.ocrText }}</pre>
          </el-scrollbar>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ocrApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const ocrList = ref([])
const detailDialogVisible = ref(false)
const currentRecord = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const loadOcrList = async () => {
  loading.value = true
  try {
    const res = await ocrApi.list({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, ...searchForm })
    ocrList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

const handleUpload = async (file) => {
  try {
    await ocrApi.upload(file)
    ElMessage.success('上传成功，正在后台识别')
    loadOcrList()
  } catch (error) {
    console.error('上传失败:', error)
  }
  return false
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadOcrList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = null
  handleSearch()
}

const viewDetail = async (row) => {
  try {
    const res = await ocrApi.detail(row.id)
    currentRecord.value = res.data
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取详情失败:', error)
  }
}

const handleReRecognize = async (row) => {
  try {
    await ocrApi.reRecognize(row.id)
    ElMessage.success('正在重新识别')
    loadOcrList()
  } catch (error) {
    console.error('重新识别失败:', error)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.imageName}"吗？`, '提示', { type: 'warning' })
    await ocrApi.delete(row.id)
    ElMessage.success('删除成功')
    loadOcrList()
  } catch (error) {
    if (error !== 'cancel') console.error('删除失败:', error)
  }
}

onMounted(() => {
  loadOcrList()
})
</script>

<style lang="scss" scoped>
.ocr-view {
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

.ocr-detail {
  .image-preview {
    text-align: center;
  }

  .ocr-text-box {
    margin-top: 16px;

    h4 {
      margin-bottom: 10px;
      color: #606266;
    }

    pre {
      white-space: pre-wrap;
      word-wrap: break-word;
      font-size: 13px;
      background: #f5f7fa;
      padding: 12px;
      border-radius: 4px;
    }
  }
}
</style>
