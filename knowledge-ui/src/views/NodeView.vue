<template>
  <div class="node-view page-stack">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="searchForm.keyword"
          class="search-input"
          placeholder="搜索节点名称..."
          prefix-icon="Search"
          clearable
          aria-label="搜索节点名称"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.nodeType"
          class="type-select"
          placeholder="节点类型"
          clearable
          aria-label="按节点类型筛选"
        >
          <el-option v-for="type in nodeTypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
        <el-button @click="handleSearch">搜索</el-button>
        <el-button text @click="resetSearch">重置</el-button>
        <span class="toolbar-spacer"></span>
        <el-button type="primary" :disabled="!service.online" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新建节点
        </el-button>
      </div>
    </el-card>

    <el-card class="list-card">
      <AsyncState
        v-if="showState"
        :loading="loading"
        :offline="!service.online"
        :error="loadError"
        :empty="!loading && !loadError && !nodeList.length"
        empty-title="还没有节点"
        empty-description="新建节点，或先从文档中抽取知识。"
        error-title="节点列表没有加载成功"
        @retry="retryLoad"
      >
        <template #action>
          <el-button type="primary" :disabled="!service.online" @click="openCreateDialog">新建节点</el-button>
        </template>
      </AsyncState>

      <template v-else>
        <el-table :data="nodeList">
          <el-table-column prop="id" label="ID" width="140" show-overflow-tooltip />
          <el-table-column prop="name" label="节点名称" min-width="130" />
          <el-table-column prop="nodeType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag>{{ getNodeTypeLabel(row.nodeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
          <el-table-column prop="relationCount" label="关联数" width="76" />
          <el-table-column prop="sourceType" label="来源" width="76">
            <template #default="{ row }">
              <StatusTag
                :tone="row.sourceType === 'manual' ? 'info' : 'success'"
                :label="row.sourceType === 'manual' ? '手动' : '抽取'"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="140" />
          <el-table-column label="操作" width="156">
            <template #default="{ row }">
              <el-button type="primary" link :disabled="!service.online" @click="openEditDialog(row)">编辑</el-button>
              <el-button type="info" link aria-label="查看图谱" @click="viewInGraph(row)">图谱</el-button>
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
            @size-change="loadNodeList"
            @current-change="loadNodeList"
          />
        </div>
      </template>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑节点' : '新建节点'" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="nodeForm" :rules="formRules" label-width="100px">
        <el-form-item label="节点名称" prop="name">
          <el-input
            v-model="nodeForm.name"
            placeholder="如：阿里巴巴、张三、人工智能、北京"
            :disabled="!service.online"
          />
        </el-form-item>
        <el-form-item label="节点类型" prop="nodeType">
          <el-select
            v-model="nodeForm.nodeType"
            placeholder="选择或输入类型"
            filterable
            allow-create
            :disabled="!service.online"
            aria-label="节点类型"
          >
            <el-option v-for="type in nodeTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点描述" prop="description">
          <el-input
            v-model="nodeForm.description"
            type="textarea"
            :rows="4"
            placeholder="如：阿里巴巴集团是一家全球领先的互联网科技公司，于1999年创立"
            :disabled="!service.online"
          />
        </el-form-item>
        <el-form-item label="扩展属性" prop="properties">
          <el-input
            v-model="nodeForm.properties"
            type="textarea"
            :rows="3"
            placeholder='如：{"founded": "1999", "headquarters": "杭州"}'
            :disabled="!service.online"
          />
          <p class="form-hint">可选。使用 JSON 保存额外字段；留空表示没有扩展属性。</p>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { nodeApi } from '@/api'
import AsyncState from '@/components/AsyncState.vue'
import StatusTag from '@/components/StatusTag.vue'
import { useServiceStore } from '@/stores/service'

const router = useRouter()
const service = useServiceStore()

const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const nodeList = ref([])
const nodeTypes = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

const searchForm = reactive({ keyword: '', nodeType: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const formRef = ref(null)
const nodeForm = reactive({
  id: null,
  name: '',
  nodeType: '',
  description: '',
  properties: ''
})

const formRules = {
  name: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }]
}

const showState = computed(() => loading.value || Boolean(loadError.value) || !nodeList.value.length)

const loadNodeList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await nodeApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      nodeType: searchForm.nodeType
    })
    nodeList.value = res.data?.records || []
    pagination.total = res.data?.total || 0
    if (nodeList.value.length === 0 && pagination.total > 0 && pagination.pageNum > 1) {
      pagination.pageNum = 1
      await loadNodeList()
    }
  } catch (error) {
    nodeList.value = []
    pagination.total = 0
    loadError.value = error.friendlyMessage || '节点列表没有加载成功'
  } finally {
    loading.value = false
  }
}

const retryLoad = async () => {
  await service.refresh()
  if (service.online) await loadNodeList()
}

const defaultNodeTypes = [
  { label: '人物', value: 'Person' },
  { label: '组织/公司', value: 'Organization' },
  { label: '地点', value: 'Location' },
  { label: '概念', value: 'Concept' },
  { label: '事件', value: 'Event' },
  { label: '技术', value: 'Technology' },
  { label: '产品', value: 'Product' },
  { label: '时间', value: 'Time' },
  { label: '其他', value: 'Other' }
]

const loadNodeTypes = async () => {
  try {
    const res = await nodeApi.getTypes()
    const existingTypes = res.data || []
    const typeSet = new Set(defaultNodeTypes.map(item => item.value))
    const mergedTypes = [...defaultNodeTypes]
    existingTypes.forEach(type => {
      if (!typeSet.has(type)) {
        mergedTypes.push({ label: type, value: type })
      }
    })
    nodeTypes.value = mergedTypes
  } catch (error) {
    nodeTypes.value = defaultNodeTypes
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadNodeList()
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.nodeType = ''
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
  Object.assign(nodeForm, {
    id: row.id,
    name: row.name,
    nodeType: row.nodeType,
    description: row.description,
    properties: row.properties ? JSON.stringify(row.properties) : ''
  })
  dialogVisible.value = true
}

const resetForm = () => {
  nodeForm.id = null
  nodeForm.name = ''
  nodeForm.nodeType = ''
  nodeForm.description = ''
  nodeForm.properties = ''
  formRef.value?.resetFields()
}

const submitForm = async () => {
  if (!service.online) return
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await nodeApi.update(nodeForm.id, nodeForm)
      ElMessage.success('节点已更新')
    } else {
      await nodeApi.create(nodeForm)
      ElMessage.success('节点已创建')
    }

    dialogVisible.value = false
    loadNodeList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.friendlyMessage || '节点保存失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  if (!service.online) return
  try {
    await ElMessageBox.confirm(
      `删除节点「${row.name}」？与它相连的关系也会被删除。`,
      '删除节点',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await nodeApi.delete(row.id)
    ElMessage.success('节点已删除')
    loadNodeList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.friendlyMessage || '节点删除失败')
    }
  }
}

const viewInGraph = (row) => {
  router.push({ path: '/', query: { nodeId: row.id } })
}

const getNodeTypeLabel = (typeValue) => {
  const found = defaultNodeTypes.find(item => item.value === typeValue)
  return found ? found.label : typeValue
}

onMounted(() => {
  loadNodeList()
  loadNodeTypes()
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
</style>
