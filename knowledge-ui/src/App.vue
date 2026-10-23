<template>
  <el-config-provider :locale="zhCn">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <div class="app-frame">
      <aside class="app-rail">
        <div class="rail-brand">
          <img src="/atlas-mark.svg" width="28" height="28" alt="" />
          <div>
            <strong>知识图谱</strong>
            <span>本地优先 · 可溯源</span>
          </div>
        </div>

        <nav class="rail-nav" aria-label="主导航">
          <div v-for="group in navGroups" :key="group.label" class="rail-group">
            <div class="rail-group-label">{{ group.label }}</div>
            <router-link
              v-for="item in group.items"
              :key="item.path"
              :to="item.path"
              class="rail-item"
              :class="{ 'is-active': isActive(item.path) }"
            >
              <el-icon :size="16"><component :is="item.icon" /></el-icon>
              <span class="rail-label">{{ item.label }}</span>
            </router-link>
          </div>
        </nav>

        <div class="rail-foot">
          <span class="rail-foot-dot" aria-hidden="true"></span>
          数据留在本机
        </div>
      </aside>

      <div class="app-body">
        <header class="app-header">
          <div class="page-heading">
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageSubtitle }}</p>
          </div>
          <ServiceStatus />
        </header>

        <div
          v-if="service.lastChecked && !service.online"
          class="offline-strip"
          role="status"
        >
          <span class="offline-dot" aria-hidden="true"></span>
          <strong>本地数据服务离线</strong>
          <span class="offline-text">只读地图模式：可浏览界面，写入与 AI 动作已停用。</span>
          <button type="button" class="offline-retry" @click="retryService">重试连接</button>
        </div>

        <main id="main-content" class="app-main">
          <router-view />
        </main>
      </div>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import {
  ChatDotRound,
  Coin,
  DataAnalysis,
  Document,
  MagicStick,
  Picture,
  Share
} from '@element-plus/icons-vue'
import ServiceStatus from '@/components/ServiceStatus.vue'
import { useServiceStore } from '@/stores/service'

const route = useRoute()
const service = useServiceStore()

const navGroups = [
  {
    label: '图谱',
    items: [
      { path: '/', label: '图谱可视化', icon: DataAnalysis },
      { path: '/nodes', label: '知识节点', icon: Coin },
      { path: '/relations', label: '知识关系', icon: Share }
    ]
  },
  {
    label: '资料',
    items: [
      { path: '/documents', label: '文档管理', icon: Document },
      { path: '/ocr', label: 'OCR 识别', icon: Picture }
    ]
  },
  {
    label: '问答',
    items: [{ path: '/chat', label: 'AI 对话', icon: ChatDotRound }]
  },
  {
    label: '工具',
    items: [{ path: '/ai', label: 'AI 服务', icon: MagicStick }]
  }
]

const pageMeta = {
  '/': {
    title: '图谱可视化',
    subtitle: '把文档、实体与关系放在同一张图上'
  },
  '/chat': {
    title: 'AI 对话',
    subtitle: '基于本地知识库提问，答案可回到来源'
  },
  '/nodes': {
    title: '知识节点',
    subtitle: '查看与维护图谱中的实体'
  },
  '/relations': {
    title: '知识关系',
    subtitle: '查看与维护实体之间的连接'
  },
  '/documents': {
    title: '文档管理',
    subtitle: '导入资料，进入解析与抽取队列'
  },
  '/ocr': {
    title: 'OCR 识别',
    subtitle: '从图片中提取文字并进入知识抽取'
  },
  '/ai': {
    title: 'AI 服务',
    subtitle: '调试抽取、检索与摘要能力'
  }
}

const pageTitle = computed(() => route.meta.title || pageMeta[route.path]?.title || '知识图谱')
const pageSubtitle = computed(() => pageMeta[route.path]?.subtitle || '')

const isActive = (path) => route.path === path

const retryService = async () => {
  await service.refresh()
  if (service.online) {
    window.dispatchEvent(new Event('atlas:service-online'))
  }
}

let healthTimer = null

const refreshOnFocus = () => {
  service.refresh()
}

onMounted(() => {
  service.refresh()
  healthTimer = window.setInterval(() => service.refresh(), 30000)
  window.addEventListener('focus', refreshOnFocus)
})

onUnmounted(() => {
  if (healthTimer) window.clearInterval(healthTimer)
  window.removeEventListener('focus', refreshOnFocus)
})
</script>

<style scoped lang="scss">
.app-frame {
  display: flex;
  height: 100vh;
  min-height: 0;
  background: var(--atlas-desk);
  color: var(--atlas-text);
}

.app-rail {
  width: var(--atlas-rail);
  flex: none;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--atlas-paper);
  border-right: 1px solid var(--atlas-line);
}

.rail-brand {
  height: var(--atlas-header);
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid var(--atlas-line);
}

.rail-brand img {
  border-radius: 7px;
  flex: none;
}

.rail-brand strong {
  display: block;
  color: var(--atlas-ink);
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
}

.rail-brand span {
  display: block;
  color: var(--atlas-text-muted);
  font-size: 11px;
  line-height: 1.4;
}

.rail-nav {
  flex: 1;
  overflow-y: auto;
  padding: 12px 10px 20px;
}

.rail-group + .rail-group {
  margin-top: 18px;
}

.rail-group-label {
  padding: 0 10px 6px;
  color: var(--atlas-text-muted);
  font-size: 11px;
  font-weight: 500;
}

.rail-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  height: 36px;
  padding: 0 10px;
  border-radius: 4px;
  color: var(--atlas-text);
  font-size: 14px;
  text-decoration: none;
  transition: background 200ms ease, color 200ms ease;
}

.rail-item:hover {
  background: var(--atlas-sunken);
  text-decoration: none;
}

.rail-item.is-active {
  background: var(--atlas-blue-soft);
  color: var(--atlas-blue);
  font-weight: 500;
}

.rail-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 2px;
  height: 16px;
  border-radius: 1px;
  background: var(--atlas-blue);
}

.rail-item .el-icon {
  flex: none;
}

.rail-foot {
  flex: none;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 14px 20px;
  border-top: 1px solid var(--atlas-line);
  color: var(--atlas-text-muted);
  font-size: 11px;
}

.rail-foot-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--atlas-green);
}

.app-body {
  flex: 1;
  min-width: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  height: var(--atlas-header);
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 24px;
  background: var(--atlas-desk);
  border-bottom: 1px solid var(--atlas-line);
}

.page-heading {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-heading h1 {
  white-space: nowrap;
}

.page-heading p {
  margin: 0;
  color: var(--atlas-text-muted-strong);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.offline-strip {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  padding: 7px 24px;
  background: #fff7e8;
  border-bottom: 1px solid #ffe4ba;
  color: var(--atlas-orange-text);
  font-size: 12px;
}

.offline-strip strong {
  font-weight: 600;
}

.offline-text {
  color: var(--atlas-text);
}

.offline-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--atlas-orange);
  flex: none;
}

.offline-retry {
  margin-left: auto;
  padding: 3px 8px;
  border: 1px solid #ffcf8b;
  border-radius: 4px;
  background: transparent;
  color: var(--atlas-orange-text);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
}

.offline-retry:hover {
  border-color: var(--atlas-orange-text);
}

.app-main {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  padding: 20px 24px 28px;
}

@media (max-width: 1180px) {
  .page-heading p {
    display: none;
  }
}

@media (max-width: 1024px) {
  .app-rail {
    width: 72px;
  }

  .rail-brand {
    justify-content: center;
    padding: 0;
  }

  .rail-brand div,
  .rail-group-label,
  .rail-label {
    display: none;
  }

  .rail-item {
    justify-content: center;
    padding: 0;
  }

  .rail-foot {
    justify-content: center;
    padding: 14px 0;
    font-size: 0;
  }

  .app-header {
    padding: 0 16px;
  }

  .app-main {
    padding: 16px;
  }

  .offline-strip {
    padding: 7px 16px;
  }

  .offline-text {
    display: none;
  }
}
</style>
