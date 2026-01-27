<template>
  <div class="node-view">
    <!-- 搜索和操作栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索节点名称..."
          prefix-icon="Search"
          style="width: 250px"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-select v-model="searchForm.nodeType" placeholder="节点类型" clearable style="width: 150px">
          <el-option v-for="type in nodeTypes" :key="type.value" :label="type.label" :value="type.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>新建节点
        </el-button>
      </div>
    </el-card>

    <!-- 节点列表 -->
    <el-card class="list-card">
      <el-table :data="nodeList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="name" label="节点名称" min-width="150" />
        <el-table-column prop="nodeType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ getNodeTypeLabel(row.nodeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="relationCount" label="关联数" width="100" />
        <el-table-column prop="sourceType" label="来源" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sourceType === 'manual' ? 'info' : 'success'" size="small">
              {{ row.sourceType === 'manual' ? '手动' : '抽取' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="info" link @click="viewInGraph(row)">查看图谱</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
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
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑节点' : '新建节点'"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="nodeForm" :rules="formRules" label-width="100px">
        <el-form-item label="节点名称" prop="name">
          <el-input v-model="nodeForm.name" placeholder="如：阿里巴巴、张三、人工智能、北京" />
        </el-form-item>
        <el-form-item label="节点类型" prop="nodeType">
          <el-select v-model="nodeForm.nodeType" placeholder="选择或输入类型" filterable allow-create>
            <el-option v-for="type in nodeTypes" :key="type.value" :label="type.label" :value="type.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="节点描述" prop="description">
          <el-input
            v-model="nodeForm.description"
            type="textarea"
            :rows="4"
            placeholder="如：阿里巴巴集团是一家全球领先的互联网科技公司，于1999年创立"
          />
        </el-form-item>
        <el-form-item label="扩展属性" prop="properties">
          <el-input
            v-model="nodeForm.properties"
            type="textarea"
            :rows="3"
            placeholder='如：{"founded": "1999", "headquarters": "杭州", "industry": "互联网"}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { nodeApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// 数据
const loading = ref(false)
const submitting = ref(false)
const nodeList = ref([])
const nodeTypes = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

// 搜索表单
const searchForm = reactive({
  keyword: '',
  nodeType: ''
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 表单
const formRef = ref(null)
const nodeForm = reactive({
  id: null,
  name: '',
  nodeType: '',
  description: '',
  properties: ''
})

// 表单校验规则
const formRules = {
  name: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }]
}

// 加载节点列表
const loadNodeList = async () => {
  loading.value = true
  try {
    const res = await nodeApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      nodeType: searchForm.nodeType
    })
    nodeList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载节点列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 默认节点类型（中英文映射）
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

// 加载节点类型
const loadNodeTypes = async () => {
  try {
    const res = await nodeApi.getTypes()
    const existingTypes = res.data || []
    // 合并默认类型和已有类型
    const typeSet = new Set(defaultNodeTypes.map(t => t.value))
    const mergedTypes = [...defaultNodeTypes]
    existingTypes.forEach(type => {
      if (!typeSet.has(type)) {
        // 未知类型直接显示英文
        mergedTypes.push({ label: type, value: type })
      }
    })
    nodeTypes.value = mergedTypes
  } catch (error) {
    console.error('加载节点类型失败:', error)
    nodeTypes.value = defaultNodeTypes
  }
}

// 搜索
const handleSearch = () => {
  pagination.pageNum = 1
  loadNodeList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.nodeType = ''
  handleSearch()
}

// 打开新建弹窗
const openCreateDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 打开编辑弹窗
const openEditDialog = (row) => {
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

// 重置表单
const resetForm = () => {
  nodeForm.id = null
  nodeForm.name = ''
  nodeForm.nodeType = ''
  nodeForm.description = ''
  nodeForm.properties = ''
  formRef.value?.resetFields()
}

// 提交表单
const submitForm = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await nodeApi.update(nodeForm.id, nodeForm)
      ElMessage.success('更新成功')
    } else {
      await nodeApi.create(nodeForm)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadNodeList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

// 删除节点
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除节点"${row.name}"吗？相关的关系也会被删除。`, '提示', {
      type: 'warning'
    })
    await nodeApi.delete(row.id)
    ElMessage.success('删除成功')
    loadNodeList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 在图谱中查看
const viewInGraph = (row) => {
  router.push({ path: '/', query: { keyword: row.name } })
}

// 获取节点类型中文标签
const getNodeTypeLabel = (typeValue) => {
  const found = defaultNodeTypes.find(t => t.value === typeValue)
  return found ? found.label : typeValue
}

// 初始化
onMounted(() => {
  loadNodeList()
  loadNodeTypes()
})
</script>

<style lang="scss" scoped>
.node-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card {
  .toolbar {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

.list-card {
  .pagination-wrapper {
    margin-top: 16px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
