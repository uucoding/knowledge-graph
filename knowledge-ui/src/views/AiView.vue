<template>
  <div class="ai-view">
    <el-row :gutter="16">
      <!-- 知识抽取 -->
      <el-col :span="12">
        <el-card class="extract-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><MagicStick /></el-icon> 知识抽取</span>
            </div>
          </template>
          <el-form label-position="top">
            <el-form-item label="输入文本">
              <el-input
                v-model="extractText"
                type="textarea"
                :rows="8"
                placeholder="输入要抽取知识的文本..."
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="extractKnowledge" :loading="extractLoading">
                抽取知识
              </el-button>
              <el-button @click="extractText = ''">清空</el-button>
            </el-form-item>
          </el-form>

          <div class="extract-result" v-if="extractResult">
            <el-divider content-position="left">抽取结果</el-divider>
            <el-tabs v-model="activeTab">
              <el-tab-pane label="实体" name="entities">
                <el-table :data="extractResult.entities" max-height="250" size="small">
                  <el-table-column prop="name" label="名称" />
                  <el-table-column prop="nodeType" label="类型" width="100" />
                  <el-table-column prop="description" label="描述" show-overflow-tooltip />
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="关系" name="relations">
                <el-table :data="extractResult.relations" max-height="250" size="small">
                  <el-table-column prop="name" label="关系" />
                  <el-table-column prop="relationType" label="类型" width="120" />
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-col>

      <!-- 语义搜索 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><Search /></el-icon> 语义搜索</span>
            </div>
          </template>
          <el-form :inline="true">
            <el-form-item>
              <el-input v-model="searchQuery" placeholder="输入搜索内容..." style="width: 300px" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchType" placeholder="类型" clearable style="width: 120px">
                <el-option label="全部" value="" />
                <el-option label="文档" value="document" />
                <el-option label="OCR" value="ocr" />
                <el-option label="节点" value="node" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="semanticSearch" :loading="searchLoading">搜索</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="searchResults" max-height="300" size="small" v-loading="searchLoading">
            <el-table-column prop="id" label="ID" width="180" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="score" label="相似度" width="100">
              <template #default="{ row }">
                {{ (row.score * 100).toFixed(1) }}%
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 文本摘要 -->
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span><el-icon><Document /></el-icon> 文本摘要</span>
            </div>
          </template>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form label-position="top">
                <el-form-item label="输入文本">
                  <el-input v-model="summaryText" type="textarea" :rows="8" placeholder="输入要生成摘要的文本..." />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="generateSummary" :loading="summaryLoading">生成摘要</el-button>
                  <el-button @click="summaryText = ''; summaryResult = ''">清空</el-button>
                </el-form-item>
              </el-form>
            </el-col>
            <el-col :span="12">
              <el-form label-position="top">
                <el-form-item label="摘要结果">
                  <el-input v-model="summaryResult" type="textarea" :rows="8" readonly placeholder="摘要结果将在这里显示..." />
                </el-form-item>
              </el-form>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <!-- 提示信息 -->
    <el-alert
      style="margin-top: 16px"
      title="AI对话功能已移至独立页面"
      type="info"
      show-icon
    >
      <template #default>
        如需进行AI对话，请访问左侧菜单的「AI对话」功能，支持RAG增强、文件上传、思考链展示等功能。
      </template>
    </el-alert>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { aiApi } from '@/api'
import { ElMessage } from 'element-plus'

// 知识抽取
const extractText = ref('')
const extractLoading = ref(false)
const extractResult = ref(null)
const activeTab = ref('entities')

const extractKnowledge = async () => {
  if (!extractText.value.trim()) {
    ElMessage.warning('请输入文本')
    return
  }

  extractLoading.value = true
  try {
    const res = await aiApi.extract({ text: extractText.value })
    extractResult.value = res.data
    ElMessage.success(`抽取完成：${res.data.entities?.length || 0}个实体，${res.data.relations?.length || 0}个关系`)
  } catch (error) {
    console.error('抽取失败:', error)
  } finally {
    extractLoading.value = false
  }
}

// 语义搜索
const searchQuery = ref('')
const searchType = ref('')
const searchLoading = ref(false)
const searchResults = ref([])

const semanticSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入搜索内容')
    return
  }

  searchLoading.value = true
  try {
    const res = await aiApi.semanticSearch(searchQuery.value, 10, searchType.value)
    searchResults.value = res.data
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    searchLoading.value = false
  }
}

// 文本摘要
const summaryText = ref('')
const summaryLoading = ref(false)
const summaryResult = ref('')

const generateSummary = async () => {
  if (!summaryText.value.trim()) {
    ElMessage.warning('请输入文本')
    return
  }

  summaryLoading.value = true
  try {
    const res = await aiApi.generateSummary(summaryText.value)
    summaryResult.value = res.data
    ElMessage.success('摘要生成成功')
  } catch (error) {
    console.error('生成失败:', error)
  } finally {
    summaryLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.ai-view {
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .extract-result {
    margin-top: 10px;
  }
}
</style>
