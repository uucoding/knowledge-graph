<template>
  <div class="ai-view page-stack">
    <div class="ai-grid">
      <el-card class="extract-card">
        <template #header>
          <div class="card-head">
            <h2>知识抽取</h2>
            <p>粘贴文本，调用本地模型抽取实体与关系。</p>
          </div>
        </template>

        <el-form label-position="top">
          <el-form-item label="输入文本">
            <el-input
              v-model="extractText"
              type="textarea"
              :rows="8"
              placeholder="输入要抽取知识的文本..."
              :disabled="!service.online"
            />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="extractLoading" :disabled="!service.online" @click="extractKnowledge">
              抽取知识
            </el-button>
            <el-button text :disabled="extractLoading" @click="clearExtract">清空</el-button>
          </div>
        </el-form>

        <p v-if="extractError" class="inline-error">{{ extractError }}</p>

        <div v-if="extractResult" class="extract-result">
          <el-divider content-position="left">抽取结果</el-divider>
          <el-tabs v-model="activeTab">
            <el-tab-pane label="实体" name="entities">
              <el-table :data="extractResult.entities || []" max-height="250" size="small">
                <el-table-column prop="name" label="名称" />
                <el-table-column prop="nodeType" label="类型" width="100" />
                <el-table-column prop="description" label="描述" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="关系" name="relations">
              <el-table :data="extractResult.relations || []" max-height="250" size="small">
                <el-table-column prop="name" label="关系" />
                <el-table-column prop="relationType" label="类型" width="120" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
        <p v-else class="result-empty">抽取结果会显示在这里：先看到实体，再检查关系。</p>
      </el-card>

      <el-card class="search-card">
        <template #header>
          <div class="card-head">
            <h2>语义搜索</h2>
            <p>在本地向量库中按语义而不是关键词检索。</p>
          </div>
        </template>

        <el-form label-position="top">
          <el-form-item label="搜索内容">
            <el-input
              v-model="searchQuery"
              placeholder="输入搜索内容..."
              clearable
              :disabled="!service.online"
              @keyup.enter="semanticSearch"
            />
          </el-form-item>
          <div class="search-row">
            <el-select
              v-model="searchType"
              placeholder="范围"
              clearable
              :disabled="!service.online"
              aria-label="语义搜索范围"
            >
              <el-option label="全部" value="" />
              <el-option label="文档" value="document" />
              <el-option label="OCR" value="ocr" />
              <el-option label="节点" value="node" />
            </el-select>
            <el-button type="primary" :loading="searchLoading" :disabled="!service.online" @click="semanticSearch">
              搜索
            </el-button>
          </div>
        </el-form>

        <p v-if="searchError" class="inline-error">{{ searchError }}</p>

        <el-table v-if="searchResults.length" :data="searchResults" max-height="320" size="small">
          <el-table-column prop="id" label="ID" width="110" />
          <el-table-column prop="type" label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="score" label="相似度" width="90" align="right">
            <template #default="{ row }">
              {{ formatScore(row.score) }}
            </template>
          </el-table-column>
          <el-table-column label="内容" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.content || row.title || row.name || '—' }}
            </template>
          </el-table-column>
        </el-table>
        <p v-else-if="searchPerformed" class="result-empty">没有匹配结果。换一个关键词，或确认知识库已经导入文档。</p>
        <p v-else class="result-empty">语义搜索结果会显示在这里。</p>
      </el-card>

      <el-card class="summary-card">
        <template #header>
          <div class="card-head">
            <h2>文本摘要</h2>
            <p>生成本地摘要，用于快速浏览长文档或抽取前的预读。</p>
          </div>
        </template>

        <div class="summary-grid">
          <el-form label-position="top">
            <el-form-item label="输入文本">
              <el-input
                v-model="summaryText"
                type="textarea"
                :rows="8"
                placeholder="输入要生成摘要的文本..."
                :disabled="!service.online"
              />
            </el-form-item>
            <div class="form-actions">
              <el-button type="primary" :loading="summaryLoading" :disabled="!service.online" @click="generateSummary">
                生成摘要
              </el-button>
              <el-button text :disabled="summaryLoading" @click="clearSummary">清空</el-button>
            </div>
          </el-form>

          <el-form label-position="top">
            <el-form-item label="摘要结果">
              <el-input
                v-model="summaryResult"
                type="textarea"
                :rows="8"
                readonly
                placeholder="摘要结果将在这里显示..."
              />
            </el-form-item>
          </el-form>
        </div>

        <p v-if="summaryError" class="inline-error">{{ summaryError }}</p>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { aiApi } from '@/api'
import { formatScore } from '@/constants/design'
import { useServiceStore } from '@/stores/service'

const service = useServiceStore()

const extractText = ref('')
const extractLoading = ref(false)
const extractResult = ref(null)
const extractError = ref('')
const activeTab = ref('entities')

const searchQuery = ref('')
const searchType = ref('')
const searchLoading = ref(false)
const searchResults = ref([])
const searchError = ref('')
const searchPerformed = ref(false)

const summaryText = ref('')
const summaryLoading = ref(false)
const summaryResult = ref('')
const summaryError = ref('')

const clearExtract = () => {
  extractText.value = ''
  extractResult.value = null
  extractError.value = ''
}

const extractKnowledge = async () => {
  if (!service.online) return
  if (!extractText.value.trim()) {
    ElMessage.warning('请输入文本')
    return
  }

  extractLoading.value = true
  extractError.value = ''
  try {
    const res = await aiApi.extract({ text: extractText.value })
    extractResult.value = res.data
    ElMessage.success(`抽取完成：${res.data?.entities?.length || 0} 个实体，${res.data?.relations?.length || 0} 个关系`)
  } catch (error) {
    extractError.value = error.friendlyMessage || '抽取没有完成'
  } finally {
    extractLoading.value = false
  }
}

const semanticSearch = async () => {
  if (!service.online) return
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }

  searchLoading.value = true
  searchError.value = ''
  searchPerformed.value = true
  try {
    const res = await aiApi.semanticSearch(searchQuery.value.trim(), 10, searchType.value)
    searchResults.value = res.data || []
  } catch (error) {
    searchResults.value = []
    searchError.value = error.friendlyMessage || '搜索没有完成'
  } finally {
    searchLoading.value = false
  }
}

const clearSummary = () => {
  summaryText.value = ''
  summaryResult.value = ''
  summaryError.value = ''
}

const generateSummary = async () => {
  if (!service.online) return
  if (!summaryText.value.trim()) {
    ElMessage.warning('请输入文本')
    return
  }

  summaryLoading.value = true
  summaryError.value = ''
  try {
    const res = await aiApi.generateSummary(summaryText.value)
    summaryResult.value = res.data
    ElMessage.success('摘要已生成')
  } catch (error) {
    summaryError.value = error.friendlyMessage || '摘要生成失败'
  } finally {
    summaryLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.ai-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 0.85fr);
  gap: 16px;
  align-items: start;
}

.summary-card {
  grid-column: 1 / -1;
}

.card-head h2 {
  font-size: 15px;
}

.card-head p {
  margin-top: 2px;
  color: var(--atlas-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.card-head {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-row {
  display: flex;
  gap: 10px;
}

.search-row .el-select {
  width: 140px;
}

.search-row .el-button {
  flex: 1;
}

.extract-result {
  margin-top: 4px;
}

.result-empty {
  margin-top: 8px;
  padding: 18px 12px;
  border: 1px dashed var(--atlas-line-strong);
  border-radius: 4px;
  color: var(--atlas-text-muted);
  font-size: 13px;
  line-height: 1.6;
  text-align: center;
}

.inline-error {
  margin-top: 8px;
  color: var(--atlas-red-text);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

@media (max-width: 1180px) {
  .ai-grid,
  .summary-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .summary-card {
    grid-column: auto;
  }
}
</style>
