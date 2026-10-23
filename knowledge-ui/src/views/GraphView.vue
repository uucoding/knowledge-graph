<template>
  <div class="graph-view">
    <section class="graph-toolbar" aria-label="图谱工具条">
      <div class="toolbar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索节点..."
          clearable
          class="search-input"
          aria-label="搜索图谱节点"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :loading="loading" @click="handleSearch">搜索</el-button>
        <el-button :disabled="loading" @click="activeSubGraph ? backToFullGraph() : resetAndLoad()">
          {{ activeSubGraph ? '返回全图' : '全图' }}
        </el-button>
        <span class="toolbar-spacer"></span>
        <el-select
          v-model="nodeLimit"
          class="limit-select"
          aria-label="节点数量上限"
          @change="loadGraphData"
        >
          <el-option label="50 个节点" :value="50" />
          <el-option label="100 个节点" :value="100" />
          <el-option label="200 个节点" :value="200" />
          <el-option label="500 个节点" :value="500" />
        </el-select>
      </div>
    </section>

    <section class="graph-surface" aria-labelledby="graph-title">
      <div class="graph-topline">
        <div>
          <h2 id="graph-title">{{ activeSubGraph ? `子图 · ${activeSubGraph}` : '全图' }}</h2>
          <p>{{ graphSummary }}</p>
        </div>
        <span v-if="lastLoadedAt" class="graph-updated">更新于 {{ lastLoadedAt }}</span>
      </div>

      <div class="graph-stage">
        <AsyncState
          v-if="showState"
          class="graph-state"
          :loading="loading"
          :offline="!service.online"
          :error="loadError"
          :empty="!loading && !loadError && !nodes.length"
          empty-title="这是一张还没有绘图的地图"
          empty-description="导入文档并完成知识抽取后，实体与关系会出现在这里。"
          error-title="图谱没有加载成功"
          @retry="retryLoad"
        >
          <template #action>
            <el-button type="primary" @click="router.push('/documents')">上传文档</el-button>
            <el-button @click="loadGraphData">刷新</el-button>
          </template>
        </AsyncState>

        <div
          v-show="!showState"
          ref="chartRef"
          class="chart-container"
          role="img"
          :aria-label="graphAriaLabel"
        ></div>

        <ul class="sr-only" aria-label="图谱节点列表">
          <li v-for="node in nodes" :key="`a11y-node-${node.id}`">
            {{ node.name }}，类型 {{ node.nodeType || node.category || '未分类' }}{{ node.description ? `，${node.description}` : '' }}
          </li>
        </ul>
        <ul class="sr-only" aria-label="图谱关系列表">
          <li v-for="(link, index) in links" :key="`a11y-link-${index}`">
            {{ link.name || '关系' }}：{{ nodeLabel(link.source) }} 到 {{ nodeLabel(link.target) }}
          </li>
        </ul>

        <aside v-if="legend.length && !showState" class="graph-legend" aria-label="节点类型图例">
          <div class="legend-title">图层 · 节点类型</div>
          <ul>
            <li v-for="item in legend" :key="item.name">
              <i :style="{ background: item.color }" aria-hidden="true"></i>
              <span class="legend-name">{{ item.name }}</span>
              <span class="legend-count">{{ item.count }}</span>
            </li>
          </ul>
        </aside>
      </div>
    </section>

    <el-dialog v-model="detailDialogVisible" title="节点详情" width="500px">
      <el-descriptions v-if="selectedNode" :column="1" border>
        <el-descriptions-item label="名称">{{ selectedNode.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag>{{ selectedNode.nodeType || selectedNode.category || '未分类' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ selectedNode.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="关联数">{{ selectedNode.value || 0 }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!service.online" @click="viewSubGraph">查看子图</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import AsyncState from '@/components/AsyncState.vue'
import { graphApi } from '@/api'
import { GRAPH_COLORS } from '@/constants/design'
import { useServiceStore } from '@/stores/service'

const route = useRoute()
const router = useRouter()
const service = useServiceStore()

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const loading = ref(true)
const loadError = ref('')
const searchKeyword = ref('')
const nodeLimit = ref(100)
const graphData = ref({ nodes: [], links: [], categories: [] })
const detailDialogVisible = ref(false)
const selectedNode = ref(null)
const activeSubGraph = ref('')
const lastLoadedAt = ref('')

const nodes = computed(() => graphData.value.nodes || [])
const links = computed(() => graphData.value.links || [])
const showState = computed(() => loading.value || Boolean(loadError.value) || !nodes.value.length)

const graphSummary = computed(() => {
  if (loading.value) return '正在读取本地图谱'
  if (!nodes.value.length) return '暂无节点与关系'
  return `共 ${nodes.value.length} 个节点 · ${links.value.length} 条关系`
})

const graphAriaLabel = computed(() => {
  const summary = graphSummary.value
  const legendText = legend.value.map(item => `${item.name} ${item.count}`).join('，')
  return `知识图谱。${summary}${legendText ? `。图例：${legendText}` : ''}`
})

const colorFor = (index) => GRAPH_COLORS[index % GRAPH_COLORS.length]

const nodeLabel = (id) => {
  const node = nodes.value.find(item => String(item.id) === String(id))
  return node?.name || String(id)
}

const categoryNames = computed(() => {
  const categories = graphData.value.categories || []
  return categories.length
    ? categories.map(item => (typeof item === 'string' ? item : item.name))
    : [...new Set(nodes.value.map(node => node.nodeType || '其他'))]
})

const legend = computed(() => categoryNames.value
  .map((name, index) => ({
    name,
    color: colorFor(index),
    count: nodes.value.filter(node => (node.category ?? index) === index).length
  }))
  .filter(item => item.count > 0))

const escapeHtml = (value) => String(value ?? '')
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&#39;')

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (!nodes.value.length) return

  chartInstance = echarts.init(chartRef.value)
  const showLabels = nodes.value.length <= 120

  const option = {
    animationDuration: 400,
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#e5e6eb',
      textStyle: { color: '#1d2129', fontSize: 13 },
      formatter: (params) => {
        if (params.dataType === 'node') {
          const type = params.data.nodeType || params.data.category || '未分类'
          return `<b>${escapeHtml(params.data.name)}</b><br/>类型：${escapeHtml(type)}`
        }
        if (params.dataType === 'edge') {
          return escapeHtml(params.data.name || '关系')
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: nodes.value.map((node, index) => ({
        ...node,
        symbolSize: node.symbolSize || 30,
        category: node.category ?? index,
        itemStyle: { color: colorFor(node.category ?? index) }
      })),
      links: links.value.map(link => ({
        ...link,
        lineStyle: { width: (link.weight || 1) * 2 }
      })),
      categories: categoryNames.value.map((name, index) => ({
        name,
        itemStyle: { color: colorFor(index) }
      })),
      roam: true,
      draggable: true,
      force: {
        repulsion: 360,
        edgeLength: [120, 260],
        gravity: 0.08
      },
      label: {
        show: showLabels,
        position: 'right',
        formatter: '{b}',
        color: '#4e5969',
        fontSize: 12
      },
      labelLayout: {
        hideOverlap: true
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      },
      edgeLabel: {
        show: false
      },
      emphasis: {
        focus: 'adjacency',
        label: { show: true },
        lineStyle: { width: 4 }
      }
    }]
  }

  chartInstance.setOption(option)
  chartInstance.on('click', params => {
    if (params.dataType === 'node') {
      selectedNode.value = params.data
      detailDialogVisible.value = true
    }
  })
}

const applyGraph = (data, focusNodeId) => {
  graphData.value = {
    nodes: data?.nodes || [],
    links: data?.links || [],
    categories: data?.categories || []
  }
  lastLoadedAt.value = new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })

  nextTick(() => {
    initChart()
    if (focusNodeId) {
      const focusNode = nodes.value.find(node => String(node.id) === String(focusNodeId))
      if (focusNode) {
        selectedNode.value = focusNode
        detailDialogVisible.value = true
      }
    }
  })
}

const loadGraphData = async () => {
  loading.value = true
  loadError.value = ''
  activeSubGraph.value = ''

  try {
    const res = await graphApi.getGraph(nodeLimit.value)
    applyGraph(res.data, route.query.nodeId)
  } catch (error) {
    graphData.value = { nodes: [], links: [], categories: [] }
    loadError.value = error.friendlyMessage || '无法加载图谱数据'
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    loadGraphData()
    return
  }

  loading.value = true
  loadError.value = ''
  activeSubGraph.value = ''

  try {
    const res = await graphApi.searchGraph(searchKeyword.value.trim(), nodeLimit.value)
    applyGraph(res.data)
  } catch (error) {
    graphData.value = { nodes: [], links: [], categories: [] }
    loadError.value = error.friendlyMessage || '搜索没有完成'
  } finally {
    loading.value = false
  }
}

const resetAndLoad = () => {
  searchKeyword.value = ''
  loadGraphData()
}

const retryLoad = async () => {
  await service.refresh()
  if (service.online) loadGraphData()
}

const viewSubGraph = async () => {
  if (!selectedNode.value) return
  try {
    const res = await graphApi.getSubGraph(selectedNode.value.id, 2)
    activeSubGraph.value = selectedNode.value.name
    detailDialogVisible.value = false
    applyGraph(res.data)
  } catch (error) {
    ElMessage.error(error.friendlyMessage || '无法加载子图')
  }
}

const backToFullGraph = () => {
  searchKeyword.value = ''
  loadGraphData()
}

const handleResize = () => {
  chartInstance?.resize()
}

watch(nodeLimit, () => loadGraphData())

watch(() => route.query.keyword, (keyword) => {
  if (keyword) {
    searchKeyword.value = keyword
    handleSearch()
  }
}, { immediate: true })

watch(() => route.query.nodeId, (nodeId) => {
  if (!nodeId) return
  const node = nodes.value.find(item => String(item.id) === String(nodeId))
  if (node) {
    selectedNode.value = node
    detailDialogVisible.value = true
  }
})

onMounted(() => {
  if (!route.query.keyword) loadGraphData()
  window.addEventListener('resize', handleResize)
  if (typeof ResizeObserver !== 'undefined' && chartRef.value) {
    resizeObserver = new ResizeObserver(handleResize)
    resizeObserver.observe(chartRef.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  resizeObserver?.disconnect()
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style scoped lang="scss">
.graph-view {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.graph-toolbar {
  flex: none;
  padding: 12px 16px;
  border: 1px solid var(--atlas-line);
  border-radius: 8px;
  background: var(--atlas-paper);
}

.search-input {
  width: 260px;
}

.limit-select {
  width: 132px;
}

.graph-surface {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--atlas-line);
  border-radius: 8px;
  background: var(--atlas-paper);
  overflow: hidden;
}

.graph-topline {
  flex: none;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--atlas-line);
}

.graph-topline h2 {
  font-size: 15px;
}

.graph-topline p {
  margin-top: 2px;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.graph-updated {
  color: var(--atlas-text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.graph-stage {
  position: relative;
  flex: 1;
  min-height: 460px;
}

.graph-state {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.chart-container {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.graph-legend {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  width: 188px;
  padding: 12px;
  border: 1px solid var(--atlas-line);
  border-radius: 8px;
  background: var(--atlas-paper);
}

.legend-title {
  margin-bottom: 6px;
  color: var(--atlas-text-muted);
  font-size: 12px;
}

.graph-legend ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.graph-legend li {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 24px;
  color: var(--atlas-text);
  font-size: 12px;
}

.graph-legend i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: none;
}

.legend-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-count {
  margin-left: auto;
  color: var(--atlas-text-muted);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 1180px) {
  .search-input {
    width: 220px;
  }

  .graph-legend {
    width: 164px;
  }
}
</style>
