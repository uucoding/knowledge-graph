<template>
  <el-tooltip placement="bottom-end" effect="light" :show-after="200">
    <template #content>
      <div class="service-pop">
        <div class="service-row">
          <span class="service-dot" :class="service.online ? 'is-up' : 'is-down'"></span>
          <span>后端服务</span>
          <strong>{{ service.online ? '在线' : '离线' }}</strong>
        </div>
        <div class="service-row">
          <span class="service-dot" :class="service.databaseOnline ? 'is-up' : 'is-down'"></span>
          <span>PostgreSQL</span>
          <strong>{{ service.databaseOnline ? '已连接' : '未连接' }}</strong>
        </div>
        <div class="service-row">
          <span class="service-dot" :class="service.redisOnline ? 'is-up' : 'is-down'"></span>
          <span>Redis</span>
          <strong>{{ service.redisOnline ? '已连接' : '未连接' }}</strong>
        </div>
        <p class="service-note">
          {{ service.lastError || '本地数据服务不可用时会自动进入只读地图模式。' }}
        </p>
        <p class="service-note">AI 模型（Ollama）状态未在此处检测。</p>
        <p class="service-note">上次检查：{{ checkedAt }}</p>
      </div>
    </template>

    <button
      type="button"
      class="service-chip"
      :class="{ 'is-offline': !service.online, 'is-checking': service.checking }"
      :aria-label="`${service.statusLabel}，点击重新检查`"
      @click="service.refresh()"
    >
      <span class="service-dot" :class="service.online ? 'is-up' : 'is-down'"></span>
      <span>{{ service.statusLabel }}</span>
      <el-icon v-if="service.checking" class="service-spin"><Loading /></el-icon>
    </button>
  </el-tooltip>
</template>

<script setup>
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { useServiceStore } from '@/stores/service'

const service = useServiceStore()

const checkedAt = computed(() => {
  if (!service.lastChecked) return '—'
  return service.lastChecked.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
})
</script>

<style scoped lang="scss">
.service-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid var(--atlas-line);
  border-radius: 4px;
  background: var(--atlas-paper);
  color: var(--atlas-text);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: border-color 200ms ease, color 200ms ease;
}

.service-chip:hover {
  border-color: var(--atlas-blue);
  color: var(--atlas-ink);
}

.service-chip.is-offline {
  border-color: #ffd8cc;
  color: var(--atlas-red-text);
}

.service-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--atlas-gray);
  flex: none;
}

.service-dot.is-up {
  background: var(--atlas-green);
}

.service-dot.is-down {
  background: var(--atlas-red);
}

.service-spin {
  animation: service-spin 900ms linear infinite;
}

@keyframes service-spin {
  to {
    transform: rotate(360deg);
  }
}

.service-pop {
  min-width: 216px;
  color: var(--atlas-text);
  font-size: 12px;
}

.service-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.service-row strong {
  margin-left: auto;
  color: var(--atlas-ink);
  font-weight: 500;
}

.service-note {
  margin-top: 8px;
  color: var(--atlas-text-muted);
  font-size: 12px;
  line-height: 1.5;
}
</style>
