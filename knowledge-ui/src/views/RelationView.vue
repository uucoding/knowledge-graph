<template>
  <div class="relation-view">
    <!-- 搜索栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-input v-model="searchForm.keyword" placeholder="搜索关系名称..." style="width: 250px" clearable />
        <el-select v-model="searchForm.relationType" placeholder="关系类型" clearable style="width: 150px">
          <el-option v-for="type in relationTypes" :key="type" :label="type" :value="type" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>新建关系
        </el-button>
      </div>
    </el-card>

    <!-- 关系列表 -->
    <el-card class="list-card">
      <el-table :data="relationList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="180" />
        <el-table-column prop="name" label="关系名称" width="120" />
        <el-table-column prop="relationType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.relationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="起始节点" min-width="150">
          <template #default="{ row }">
            {{ row.sourceNodeName || row.sourceNodeId }}
          </template>
        </el-table-column>
        <el-table-column label="目标节点" min-width="150">
          <template #default="{ row }">
            {{ row.targetNodeName || row.targetNodeId }}
          </template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
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
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑关系' : '新建关系'" width="600px" @close="resetForm">
      <el-form ref="formRef" :model="relationForm" :rules="formRules" label-width="100px">
        <el-form-item label="关系名称" prop="name">
          <el-input v-model="relationForm.name" placeholder="如：属于、创建、包含" />
        </el-form-item>
        <el-form-item label="关系类型" prop="relationType">
          <el-select v-model="relationForm.relationType" placeholder="选择或输入类型" filterable allow-create>
            <el-option v-for="type in relationTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-form-item>
        <el-form-item label="起始节点" prop="sourceNodeId">
          <el-select
            v-model="relationForm.sourceNodeId"
            placeholder="搜索并选择节点"
            filterable
            remote
            :remote-method="searchNodes"
            :loading="nodeSearching"
          >
            <el-option
              v-for="node in nodeOptions"
              :key="node.id"
              :label="`${node.name} (${node.nodeType})`"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标节点" prop="targetNodeId">
          <el-select
            v-model="relationForm.targetNodeId"
            placeholder="搜索并选择节点"
            filterable
            remote
            :remote-method="searchNodes"
            :loading="nodeSearching"
          >
            <el-option
              v-for="node in nodeOptions"
              :key="node.id"
              :label="`${node.name} (${node.nodeType})`"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="权重" prop="weight">
          <el-slider v-model="relationForm.weight" :min="0" :max="1" :step="0.1" show-input />
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
import { relationApi, nodeApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

// 数据
const loading = ref(false)
const submitting = ref(false)
const nodeSearching = ref(false)
const relationList = ref([])
const relationTypes = ref(['BELONGS_TO', 'PART_OF', 'LOCATED_IN', 'WORKS_FOR', 'CREATED_BY', 'RELATED_TO'])
const nodeOptions = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)

// 搜索表单
const searchForm = reactive({ keyword: '', relationType: '' })

// 分页
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

// 表单
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
  targetNodeId: [{ required: true, message: '请选择目标节点', trigger: 'change' }]
}

// 加载关系列表
const loadRelationList = async () => {
  loading.value = true
  try {
    const res = await relationApi.list({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword,
      relationType: searchForm.relationType
    })
    relationList.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载关系类型
const loadRelationTypes = async () => {
  try {
    const res = await relationApi.getTypes()
    if (res.data && res.data.length > 0) {
      relationTypes.value = [...new Set([...relationTypes.value, ...res.data])]
    }
  } catch (error) {
    console.error('加载关系类型失败:', error)
  }
}

// 搜索节点
const searchNodes = async (query) => {
  if (!query) return
  nodeSearching.value = true
  try {
    const res = await nodeApi.search(query)
    nodeOptions.value = res.data || []
  } catch (error) {
    console.error('搜索节点失败:', error)
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
  isEdit.value = false
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  Object.assign(relationForm, {
    id: row.id,
    name: row.name,
    relationType: row.relationType,
    sourceNodeId: row.sourceNodeId,
    targetNodeId: row.targetNodeId,
    weight: row.weight || 1.0
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
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await relationApi.update(relationForm.id, relationForm)
      ElMessage.success('更新成功')
    } else {
      await relationApi.create(relationForm)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadRelationList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
    }
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除关系"${row.name}"吗？`, '提示', { type: 'warning' })
    await relationApi.delete(row.id)
    ElMessage.success('删除成功')
    loadRelationList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

onMounted(() => {
  loadRelationList()
  loadRelationTypes()
})
</script>

<style lang="scss" scoped>
.relation-view {
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
</style>
