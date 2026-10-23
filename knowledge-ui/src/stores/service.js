import { defineStore } from 'pinia'

const HEALTH_TIMEOUT = 4000

export const useServiceStore = defineStore('service', {
  state: () => ({
    online: false,
    checking: false,
    database: 'unknown',
    redis: 'unknown',
    lastChecked: null,
    lastError: ''
  }),

  getters: {
    statusLabel(state) {
      if (state.checking && !state.lastChecked) return '检查本地数据服务'
      return state.online ? '本地数据服务在线' : '本地数据服务离线'
    },
    databaseOnline: (state) => state.database === 'up',
    redisOnline: (state) => state.redis === 'up'
  },

  actions: {
    async refresh() {
      this.checking = true
      const controller = new AbortController()
      const timer = setTimeout(() => controller.abort(), HEALTH_TIMEOUT)

      try {
        const response = await fetch('/api/health', {
          signal: controller.signal,
          cache: 'no-store',
          headers: { Accept: 'application/json' }
        })
        if (!response.ok) throw new Error(`HTTP ${response.status}`)

        const data = await response.json()
        this.online = true
        this.database = String(data.database || '').startsWith('UP') ? 'up' : 'down'
        this.redis = String(data.redis || '').startsWith('UP') ? 'up' : 'down'
        this.lastError = ''
      } catch (error) {
        this.online = false
        this.database = 'down'
        this.redis = 'down'
        this.lastError = error.name === 'AbortError' ? '健康检查超时' : '无法连接本地服务'
      } finally {
        clearTimeout(timer)
        this.checking = false
        this.lastChecked = new Date()
      }
    }
  }
})
