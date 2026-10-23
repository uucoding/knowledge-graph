<template>
  <span class="status-tag" :class="`is-${tone}`">
    <i class="status-dot" aria-hidden="true"></i>
    <span><slot>{{ label }}</slot></span>
  </span>
</template>

<script setup>
defineProps({
  tone: {
    type: String,
    default: 'info',
    validator: (value) => ['success', 'warning', 'danger', 'info'].includes(value)
  },
  label: { type: String, default: '' }
})
</script>

<style scoped lang="scss">
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 8px;
  border-radius: 4px;
  background: var(--atlas-sunken);
  color: var(--atlas-text);
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--atlas-gray);
  flex: none;
}

.is-success .status-dot {
  background: var(--atlas-green);
}

.is-warning .status-dot {
  background: var(--atlas-orange);
}

.is-danger .status-dot {
  background: var(--atlas-red);
}
</style>
