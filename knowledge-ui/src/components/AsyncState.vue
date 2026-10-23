<template>
  <div
    v-if="loading"
    class="async-state is-loading"
    :class="{ 'is-compact': compact }"
    role="status"
    aria-live="polite"
  >
    <el-icon class="state-icon spin"><Loading /></el-icon>
    <h3>正在载入</h3>
    <p>正在读取本地知识库，请稍候。</p>
  </div>

  <div
    v-else-if="offline"
    class="async-state is-offline"
    :class="{ 'is-compact': compact }"
    role="status"
  >
    <el-icon class="state-icon"><Connection /></el-icon>
    <h3>本地数据服务离线</h3>
    <p>当前为只读地图模式。请启动 PostgreSQL、Redis 与后端服务后重试；Ollama 状态需单独确认。</p>
    <div class="state-actions">
      <el-button type="primary" @click="$emit('retry')">重试连接</el-button>
    </div>
  </div>

  <div
    v-else-if="error"
    class="async-state is-error"
    :class="{ 'is-compact': compact }"
    role="alert"
  >
    <el-icon class="state-icon"><WarningFilled /></el-icon>
    <h3>{{ errorTitle }}</h3>
    <p>{{ errorText }}</p>
    <div class="state-actions">
      <el-button type="primary" @click="$emit('retry')">{{ retryText }}</el-button>
      <slot name="error-action"></slot>
    </div>
  </div>

  <div
    v-else-if="empty"
    class="async-state is-empty"
    :class="{ 'is-compact': compact }"
  >
    <el-icon class="state-icon"><Files /></el-icon>
    <h3>{{ emptyTitle }}</h3>
    <p>{{ emptyDescription }}</p>
    <div v-if="$slots.action" class="state-actions">
      <slot name="action"></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Connection, Files, Loading, WarningFilled } from '@element-plus/icons-vue'

const props = defineProps({
  loading: { type: Boolean, default: false },
  offline: { type: Boolean, default: false },
  error: { type: [String, Boolean, Object], default: false },
  empty: { type: Boolean, default: false },
  compact: { type: Boolean, default: false },
  errorTitle: { type: String, default: '没有加载成功' },
  emptyTitle: { type: String, default: '这里还没有内容' },
  emptyDescription: { type: String, default: '接入资料后，内容会显示在这里。' },
  retryText: { type: String, default: '重试' }
})

defineEmits(['retry'])

const errorText = computed(() => {
  if (typeof props.error === 'string' && props.error.trim()) return props.error
  if (props.error && typeof props.error === 'object' && props.error.friendlyMessage) {
    return props.error.friendlyMessage
  }
  return '本地服务暂时不可用。请检查服务状态后重试。'
})
</script>

<style scoped lang="scss">
.async-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 280px;
  padding: 40px 24px;
  text-align: center;
}

.async-state.is-compact {
  min-height: 160px;
  padding: 24px 16px;
}

.state-icon {
  width: 26px;
  height: 26px;
  margin-bottom: 12px;
  color: var(--atlas-text-muted);
}

.is-error .state-icon {
  color: var(--atlas-red-text);
}

.is-offline .state-icon {
  color: var(--atlas-orange-text);
}

.spin {
  animation: state-spin 900ms linear infinite;
}

@keyframes state-spin {
  to {
    transform: rotate(360deg);
  }
}

h3 {
  color: var(--atlas-ink);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
}

p {
  max-width: 46ch;
  margin-top: 6px;
  color: var(--atlas-text-muted);
  font-size: 13px;
  line-height: 1.6;
}

.state-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 14px;
}
</style>
