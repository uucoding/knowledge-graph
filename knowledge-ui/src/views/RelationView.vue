<template>
  <div class="relation-view page-stack">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="searchForm.keyword"
          class="search-input"
          placeholder="搜索关系名称..."
          clearable
          aria-label="搜索关系名称"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.relationType"
          class="type-select"
          placeholder="关系类型"
          clearable
          aria-label="按关系类型筛选"
        >
          <el-option v-for="type in relationTypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
        <el-button @click="handleSearch">搜索</el-button>
        <el-button text @click="resetSearch">重置</el-button>
        <span class="toolbar-spacer"></span>
        <el-button type="primary" :disabled="!service.online" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建关系
        </el-button>
      </div>
    </el-card>

    <el-card class="list-card">
      <AsyncState
        v-if="showState"
        :loading="loading"
        :offline="!service.online"
        :error="loadError"
        :empty="!loading && !loadError && !relationList.length"
        empty-title="还没有关系"
        empty-description="新建关系，或从文档抽取后让实体自动连接。"
        error-title="关系列表没有加载成功"
        @retry="retryLoad"
      >
        <template #action>
          <el-button type="primary" :disabled="!service.online" @click="openCreateDialog">新建关系</el-button>
        </template>
      </AsyncState>

      <template v-else>
        <el-table :data="relationList">
          <el-table-column prop="id" label="ID" width="140" show-overflow-tooltip />
          <el-table-column prop="name" label="关系名称" width="100" />
          <el-table-column prop="relationType" label="类型" width="96">
            <template #default="{ row }">
              <el-tag>{{ getRelationTypeLabel(row.relationType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="起始节点" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.sourceNodeName || row.sourceNodeId }}
            </template>
          </el-table-column>
          <el-table-column label="目标节点" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.targetNodeName || row.targetNodeId }}
            </template>
          </el-table-column>
          <el-table-column prop="weight" label="权重" width="64" align="right">
            <template #default="{ row }">
              {{ Number(row.weight ?? 1).toFixed(1) }}
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="140" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="primary" link :disabled="!service.online" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="danger" link :disabled="!service.online" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="pagination.total > 0" class="pager">
          <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadRelationList"
            @current-change="loadRelationList"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑关系' : '新建关系'" width="600px" @close="resetForm">
      <p class="form-hint dialog-hint">关系方向：起始节点 → 目标节点；权重用于图谱中的边宽。</p>
      <el-form ref="formRef" :model="relationForm" :rules="formRules" label-width="100px">
        <el-form-item label="关系名称" prop="name">
          <el-input
            v-model="relationForm.name"
            placeholder="如：创立、任职于、位于、发明"
            :disabled="!service.online"
          />
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-select
            v-model="relationForm.relationType"
            placeholder="选择或输入类型"
            filterable
            allow-create
            :disabled="!service.online"
            aria-label="关系类型"
          >
            <el-option v-for="type in relationTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="起始节点" prop="sourceNodeId">
          <el-select
            v-model="relationForm.sourceNodeId"
            placeholder="输入关键词搜索节点，如：法外狂徒"
            filterable
            remote
            :remote-method="searchNodes"
            :loading="nodeSearching"
            :disabled="!service.online"
            aria-label="起始节点"
          >
            <el-option
              v-for="node in nodeOptions"
              :key="node.id"
              :label="formatNodeOption(node)"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标节点" prop="targetNodeId">
          <el-select
            v-model="relationForm.targetNodeId"
            placeholder="输入关键词搜索节点，如：张三"
            filterable
            remote
            :remote-method="searchNodes"
            :loading="nodeSearching"
            :disabled="!service.online"
            aria-label="目标节点"
          >
            <el-option
              v-for="node in nodeOptions"
              :key="node.id"
              :label="formatNodeOption(node)"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-slider v-model="relationForm.weight" :min="0" :max="1" :step="0.1" show-input />
          <p class="form-hint">0–1，数值越大，图谱中的连接越粗。</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!service.online" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { nodeApi, relationApi } from '@/api'
import AsyncState from '@/components/AsyncState.vue'
import { useServiceStore } from '@/stores/service'

const service = useServiceStore()

const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const nodeSearching = ref(false)
const relationList = ref([])
const nodeOptions = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const defaultRelationTypes = [
  { label: '属于', value: 'BELONGS_TO' },
  { label: '包含/组成', value: 'PART_OF' },
  { label: '位于', value: 'LOCATED_IN' },
  { label: '任职于', value: 'WORKS_FOR' },
  { label: '创建/发明', value: 'CREATED_BY' },
  { label: '相关', value: 'RELATED_TO' },
  { label: '发生于', value: 'HAPPENED_AT' },
  { label: '使用', value: 'USED_BY' },
  { label: '影响', value: 'INFLUENCED_BY' }
]
const relationTypes = ref([...defaultRelationTypes])

const nodeTypeLabels = {
  Person: '人物',
  Organization: '组织/公司',
  Location: '地点',
  Concept: '概念',
  Event: '事件',
  Technology: '技术',
  Product: '产品',
  Time: '时间',
  Other: '其他'
}

const searchForm = reactive({ keyword: '', relationType: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formRef = ref(null)
const relationForm = reactive({
  id: null,
  name: '',
  relationType: '',
  sourceNodeId: null,
  targetNodeId: null,
  weight: 1.0
})

const formRules = {
  name: [{ required: true, message: '请输入关系名称', trigger: 'blur' }],
  relationType: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
  sourceNodeId: [{ required: true, message: '请选择起始节点', trigger: 'change' }],
  targetNodeId: [
    { required: true, message: '请选择目标节点', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value && value === relationForm.sourceNodeId) {
          callback(new Error('起始节点与目标节点不能相同'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const showState = computed(() => loading.value || Boolean(loadError.value) || !relationList.value.length)

const loadRelationList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await relationApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      relationType: searchForm.relationType
    })
    relationList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
    if (relationList.value.length === 0 && pagination.total > 0 && pagination.pageNum > 1) {
      pagination.pageNum = 1
      await loadRelationList()
    }
  } catch (error) {
    relationList.value = []
    pagination.total = 0
    loadError.value = error.friendlyMessage || '关系列表没有加载成功'
  } finally {
    loading.value = false
  }
}

const retryLoad = async () => {
  await service.refresh()
  if (service.online) await loadRelationList()
}

const loadRelationTypes = async () => {
  try {
    const res = await relationApi.getTypes()
    const existingTypes = res.data || []
    const typeSet = new Set(defaultRelationTypes.map(item => item.value))
    const mergedTypes = [...defaultRelationTypes]
    existingTypes.forEach(type => {
      if (!typeSet.has(type)) {
        mergedTypes.push({ label: type, value: type })
      }
    })
    relationTypes.value = mergedTypes
  } catch (error) {
    relationTypes.value = [...defaultRelationTypes]
  }
}

const getRelationTypeLabel = (typeValue) => {
  const found = defaultRelationTypes.find(item => item.value === typeValue)
  return found ? found.label : typeValue
}

const formatNodeOption = (node) => `${node.name} (${nodeTypeLabels[node.nodeType] || node.nodeType || '未分类'})`

const searchNodes = async (query) => {
  if (!query) return
  nodeSearching.value = true
  try {
    const res = await nodeApi.search(query)
    nodeOptions.value = res.data || []
  } catch (error) {
    nodeOptions.value = []
    ElMessage.error(error.friendlyMessage || '节点搜索失败')
  } finally {
    nodeSearching.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadRelationList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.relationType = ''
  handleSearch()
}

const openCreateDialog = () => {
  if (!service.online) return
  isEdit.value = false
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  if (!service.online) return
  isEdit.value = true
  Object.assign(relationForm, {
    id: row.id,
    name: row.name,
    relationType: row.relationType,
    sourceNodeId: row.sourceNodeId,
    targetNodeId: row.targetNodeId,
    weight: row.weight ?? 1.0
  })
  dialogVisible.value = true
}

const resetForm = () => {
  relationForm.id = null
  relationForm.name = ''
  relationForm.relationType = ''
  relationForm.sourceNodeId = null
  relationForm.targetNodeId = null
  relationForm.weight = 1.0
  formRef.value?.resetFields()
}

const submitForm = async () => {
  if (!service.online) return
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await relationApi.update(relationForm.id, relationForm)
      ElMessage.success('关系已更新')
    } else {
      await relationApi.create(relationForm)
      ElMessage.success('关系已创建')
    }

    dialogVisible.value = false
    loadRelationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.friendlyMessage || '关系保存失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  if (!service.online) return
  try {
    await ElMessageBox.confirm(
      `删除关系「${row.name}」？这条连接会从图谱中移除。`,
      '删除关系',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await relationApi.delete(row.id)
    ElMessage.success('关系已删除')
    loadRelationList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.friendlyMessage || '关系删除失败')
    }
  }
}

onMounted(() => {
  loadRelationList()
  loadRelationTypes()
})
</script>

<style scoped lang="scss">
.search-input {
  width: 240px;
}

.type-select {
  width: 150px;
}

.form-hint {
  margin-top: 6px;
  color: var(--atlas-text-muted);
  font-size: 12px;
  line-height: 1.5;
}

.dialog-hint {
  margin: 0 0 14px;
  padding: 8px 10px;
  border: 1px solid var(--atlas-line);
  border-radius: 4px;
  background: var(--atlas-desk);
}
</style>
