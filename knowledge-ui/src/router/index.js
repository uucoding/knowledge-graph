import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
  {
    path: '/',
    name: 'Graph',
    component: () => import('@/views/GraphView.vue'),
    meta: { title: '图谱可视化' }
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { title: 'AI对话' }
  },
  {
    path: '/nodes',
    name: 'Nodes',
    component: () => import('@/views/NodeView.vue'),
    meta: { title: '知识节点' }
  },
  {
    path: '/relations',
    name: 'Relations',
    component: () => import('@/views/RelationView.vue'),
    meta: { title: '知识关系' }
  },
  {
    path: '/documents',
    name: 'Documents',
    component: () => import('@/views/DocumentView.vue'),
    meta: { title: '文档管理' }
  },
  {
    path: '/ocr',
    name: 'Ocr',
    component: () => import('@/views/OcrView.vue'),
    meta: { title: 'OCR识别' }
  },
  {
    path: '/ai',
    name: 'AI',
    component: () => import('@/views/AiView.vue'),
    meta: { title: 'AI服务' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 知识图谱系统` : '知识图谱系统'
  next()
})

export default router
