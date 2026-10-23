<template>
  <div class="ocr-view page-stack">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-upload
          ref="uploadRef"
          :show-file-list="false"
          :before-upload="handleUpload"
          :disabled="!service.online"
          accept="image/*"
          tabindex="-1"
        >
          <el-button type="primary" :disabled="!service.online">
            <el-icon><Upload /></el-icon>
            上传图片
          </el-button>
        </el-upload>
        <el-input
          v-model="searchForm.keyword"
          class="search-input"
          placeholder="搜索 OCR 文本..."
          clearable
          aria-label="搜索 OCR 文本"
        />
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
        <span class="toolbar-note">支持 PNG / JPG / BMP，中英文混合识别</span>
      </div>
    </el-card>

    <el-card class="list-card">
      <AsyncState
        v-if="showState"
        :loading="loading"
        :offline="!service.online"
        :error="loadError"
        :empty="!loading && !loadError && !ocrList.length"
        empty-title="还没有识别记录"
        empty-description="上传图片后，文字识别结果会出现在这里，并可进入知识抽取。"
        error-title="OCR 记录没有加载成功"
        @retry="retryLoad"
      >
        <template #action>
          <el-button type="primary" :disabled="!service.online" @click="triggerUpload">上传图片</el-button>
        </template>
      </AsyncState>

      <template v-else>
        <el-table :data="ocrList">
          <el-table-column prop="id" label="ID" width="100" show-overflow-tooltip />
          <el-table-column prop="imageName" label="图片名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="imageType" label="类型" width="84">
            <template #default="{ row }">
              <el-tag size="small">{{ (row.imageType || '').toUpperCase() }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="imageSizeReadable" label="大小" width="76" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <StatusTag :tone="getStatusMeta(row.status).tone" :label="row.statusDesc || getStatusMeta(row.status).label" />
            </template>
          </el-table-column>
          <el-table-column prop="ocrText" label="识别文本" min-width="140" show-overflow-tooltip />
          <el-table-column prop="createTime" label="创建时间" width="140" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
              <el-button type="info" link :disabled="!service.online || row.status === 1" @click="handleReRecognize(row)">重新识别</el-button>
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
            @size-change="loadOcrList"
            @current-change="loadOcrList"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="OCR 详情" width="760px">
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
            <StatusTag :tone="getStatusMeta(currentRecord.status).tone" :label="currentRecord.statusDesc || getStatusMeta(currentRecord.status).label" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ocrApi } from '@/api'
import AsyncState from '@/components/AsyncState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { statusMeta } from '@/constants/design'
import { useServiceStore } from '@/stores/service'

const service = useServiceStore()

const loading = ref(true)
const loadError = ref('')
const ocrList = ref([])
const detailDialogVisible = ref(false)
const currentRecord = ref(null)
const uploadRef = ref(null)

const searchForm = reactive({ keyword: '', status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const showState = computed(() => loading.value || Boolean(loadError.value) || !ocrList.value.length)

const getStatusMeta = (status) => statusMeta(status)

const loadOcrList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await ocrApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    ocrList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
    if (ocrList.value.length === 0 && pagination.total > 0 && pagination.pageNum > 1) {
      pagination.pageNum = 1
      await loadOcrList()
    }
  } catch (error) {
    ocrList.value = []
    pagination.total = 0
    loadError.value = error.friendlyMessage || 'OCR 记录没有加载成功'
  } finally {
    loading.value = false
  }
}

const retryLoad = async () => {
  await service.refresh()
  if (service.online) await loadOcrList()
}

const triggerUpload = () => {
  if (!service.online) return
  uploadRef.value?.$el?.querySelector('input')?.click()
}

const handleUpload = async (file) => {
  if (!service.online) return false
  try {
    await ocrApi.upload(file)
    ElMessage.success('图片已上传，正在后台识别')
    loadOcrList()
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '图片上传失败')
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
    ElMessage.error(error.friendlyMessage || 'OCR 详情加载失败')
  }
}

const handleReRecognize = async (row) => {
  if (!service.online) return
  try {
    await ocrApi.reRecognize(row.id)
    ElMessage.success('已开始重新识别')
    loadOcrList()
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '重新识别失败')
  }
}

const handleDelete = async (row) => {
  if (!service.online) return
  try {
    await ElMessageBox.confirm(
      `删除识别记录「${row.imageName}」？识别文本会一并移除。`,
      '删除记录',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await ocrApi.delete(row.id)
    ElMessage.success('记录已删除')
    loadOcrList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.friendlyMessage || '记录删除失败')
    }
  }
}

onMounted(() => {
  loadOcrList()
})
</script>

<style scoped lang="scss">
.search-input {
  width: 220px;
}

.filter-select {
  width: 130px;
}

.image-preview {
  text-align: center;
}

.ocr-text-box {
  margin-top: 16px;
}

.ocr-text-box h4 {
  margin-bottom: 8px;
  color: var(--atlas-text);
  font-size: 13px;
  font-weight: 500;
}

.ocr-text-box pre {
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
</style>
