<template>
  <div class="graph-view">
    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索节点..."
          prefix-icon="Search"
          style="width: 300px"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="loadGraphData">刷新</el-button>
        <el-select v-model="nodeLimit" placeholder="节点数量" style="width: 120px">
          <el-option label="50个" :value="50" />
          <el-option label="100个" :value="100" />
          <el-option label="200个" :value="200" />
          <el-option label="500个" :value="500" />
        </el-select>
      </div>
    </el-card>

    <!-- 图谱容器 -->
    <el-card class="graph-card">
      <div ref="chartRef" class="chart-container"></div>
    </el-card>

    <!-- 节点详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="节点详情" width="500px">
      <el-descriptions :column="1" border v-if="selectedNode">
        <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag>{{ selectedNode.nodeType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ selectedNode.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="关联数">{{ selectedNode.value || 0 }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="viewSubGraph">查看子图</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { graphApi } from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const chartRef = ref(null)
let chartInstance = null

// 数据
const searchKeyword = ref('')
const nodeLimit = ref(100)
const graphData = ref({ nodes: [], links: [], categories: [] })
const detailDialogVisible = ref(false)
const selectedNode = ref(null)

// 初始化图表
const initChart = () => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `<b>${params.data.name}</b><br/>类型: ${params.data.nodeType}`
        } else if (params.dataType === 'edge') {
          return `${params.data.name}`
        }
        return ''
      }
    },
    legend: {
      data: graphData.value.categories,
      orient: 'vertical',
      right: 10,
      top: 20
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: graphData.value.nodes.map((node, index) => ({
        ...node,
        symbolSize: node.symbolSize || 30,
        category: node.category || 0,
        itemStyle: {
          color: getColorByCategory(node.category)
        }
      })),
      links: graphData.value.links.map(link => ({
        ...link,
        lineStyle: {
          width: (link.weight || 1) * 2
        }
      })),
      categories: graphData.value.categories.map((name, index) => ({
        name,
        itemStyle: {
          color: getColorByCategory(index)
        }
      })),
      roam: true,
      draggable: true,
      force: {
        repulsion: 200,
        edgeLength: [100, 200],
        gravity: 0.1
      },
      label: {
        show: true,
        position: 'right',
        formatter: '{b}'
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      },
      edgeLabel: {
        show: true,
        formatter: '{c}',
        fontSize: 10
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 4
        }
      }
    }]
  }

  chartInstance.setOption(option)

  // 点击事件
  chartInstance.on('click', (params) => {
    if (params.dataType === 'node') {
      selectedNode.value = params.data
      detailDialogVisible.value = true
    }
  })
}

// 获取分类颜色
const getColorByCategory = (index) => {
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
  return colors[index % colors.length]
}

// 加载图谱数据
const loadGraphData = async () => {
  try {
    const res = await graphApi.getGraph(nodeLimit.value)
    graphData.value = res.data
    initChart()
  } catch (error) {
    console.error('加载图谱数据失败:', error)
  }
}

// 搜索
const handleSearch = async () => {
  if (!searchKeyword.value) {
    loadGraphData()
    return
  }

  try {
    const res = await graphApi.searchGraph(searchKeyword.value, nodeLimit.value)
    graphData.value = res.data
    initChart()
    ElMessage.success(`找到 ${res.data.nodes.length} 个相关节点`)
  } catch (error) {
    console.error('搜索失败:', error)
  }
}

// 查看子图
const viewSubGraph = async () => {
  if (!selectedNode.value) return

  try {
    const res = await graphApi.getSubGraph(selectedNode.value.id, 2)
    graphData.value = res.data
    initChart()
    detailDialogVisible.value = false
    ElMessage.success('已加载子图')
  } catch (error) {
    console.error('加载子图失败:', error)
  }
}

// 监听节点数量变化
watch(nodeLimit, () => {
  loadGraphData()
})

// 监听路由参数
watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    searchKeyword.value = newKeyword
    handleSearch()
  }
}, { immediate: true })

// 挂载时加载数据
onMounted(() => {
  if (!route.query.keyword) {
    loadGraphData()
  }
})
</script>

<style lang="scss" scoped>
.graph-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card {
  flex-shrink: 0;

  .toolbar {
    display: flex;
    gap: 12px;
    align-items: center;
  }
}

.graph-card {
  flex: 1;
  min-height: 0;

  :deep(.el-card__body) {
    height: 100%;
    padding: 0;
  }

  .chart-container {
    width: 100%;
    height: 100%;
    min-height: 500px;
  }
}
</style>
