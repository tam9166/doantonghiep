<template>
  <Teleport to="body">
    <TransitionGroup name="toast" tag="div" class="toast-container">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast-item"
        :class="[`toast-${toast.type}`]"
        @click="removeToast(toast.id)"
      >
        <div class="toast-icon">
          <span v-if="toast.type === 'success'">✅</span>
          <span v-else-if="toast.type === 'error'">❌</span>
          <span v-else-if="toast.type === 'warning'">⚠️</span>
          <span v-else>ℹ️</span>
        </div>
        <div class="toast-body">
          <p class="toast-title" v-if="toast.title">{{ toast.title }}</p>
          <p class="toast-message">{{ toast.message }}</p>
        </div>
        <button class="toast-close" aria-label="Đóng thông báo" @click.stop="removeToast(toast.id)">✕</button>
        <div class="toast-progress" :style="{ animationDuration: toast.duration + 'ms' }"></div>
      </div>
    </TransitionGroup>
  </Teleport>
</template>

<script setup>
import { useToast } from '@/composables/useToast'

const { toasts, removeToast } = useToast()
</script>

<style scoped>
.toast-container {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 99999;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 420px;
  width: 100%;
  pointer-events: none;
}

.toast-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 20px;
  border-radius: 16px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 0 0 1px rgba(255, 255, 255, 0.05) inset;
  cursor: pointer;
  pointer-events: all;
  position: relative;
  overflow: hidden;
}

.toast-success { background: color-mix(in srgb, var(--secondary) 12%, transparent); border-color: color-mix(in srgb, var(--secondary) 25%, transparent); }
.toast-error { background: color-mix(in srgb, var(--primary) 12%, transparent); border-color: color-mix(in srgb, var(--primary) 25%, transparent); }
.toast-warning { background: color-mix(in srgb, var(--color-tertiary) 12%, transparent); border-color: color-mix(in srgb, var(--color-tertiary) 25%, transparent); }
.toast-info { background: color-mix(in srgb, var(--secondary) 12%, transparent); border-color: color-mix(in srgb, var(--secondary) 25%, transparent); }

.toast-icon { font-size: 1.3rem; flex-shrink: 0; margin-top: 2px; }
.toast-body { flex: 1; min-width: 0; }
.toast-title { margin: 0 0 4px 0; font-weight: 800; font-size: 0.92rem; color: #FFFFFF; }
.toast-message { margin: 0; font-size: 0.88rem; color: rgba(255, 255, 255, 0.75); line-height: 1.5; }

.toast-close {
  background: none; border: none; color: rgba(255, 255, 255, 0.4);
  font-size: 0.9rem; cursor: pointer; padding: 4px; flex-shrink: 0;
  transition: color 0.2s;
}
.toast-close:hover { color: #FFFFFF; }

.toast-progress {
  position: absolute; bottom: 0; left: 0; height: 3px;
  border-radius: 0 0 16px 16px;
  animation: toastProgress linear forwards;
}
.toast-success .toast-progress { background: color-mix(in srgb, var(--secondary) 60%, transparent); }
.toast-error .toast-progress { background: color-mix(in srgb, var(--primary) 60%, transparent); }
.toast-warning .toast-progress { background: color-mix(in srgb, var(--color-tertiary) 60%, transparent); }
.toast-info .toast-progress { background: color-mix(in srgb, var(--secondary) 60%, transparent); }

@keyframes toastProgress { from { width: 100%; } to { width: 0%; } }

.toast-enter-active { animation: toastSlideIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275); }
.toast-leave-active { transition: all 0.3s cubic-bezier(0.6, -0.28, 0.735, 0.045); }
.toast-leave-to { transform: translateX(100%) scale(0.8); opacity: 0; }

@keyframes toastSlideIn { from { transform: translateX(100%) scale(0.8); opacity: 0; } to { transform: translateX(0) scale(1); opacity: 1; } }

@media (max-width: 640px) {
  .toast-container {
    top: 12px;
    right: 12px;
    left: 12px;
    width: auto;
    max-width: calc(100vw - 24px);
    box-sizing: border-box;
  }

  .toast-close {
    min-width: 44px;
    min-height: 44px;
    margin: -10px -12px -10px 0;
  }
}
</style>
