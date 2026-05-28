/**
 * ============================================================
 * TOAST COMPOSABLE - Dùng toast ở bất kỳ đâu
 * ============================================================
 * Sử dụng: 
 *   import { useToast } from '@/composables/useToast'
 *   const toast = useToast()
 *   toast.success('Thành công!')
 *   toast.error('Có lỗi xảy ra')
 */
import { ref } from 'vue'

// Shared state (singleton pattern)
const toasts = ref([])
let toastId = 0

export function useToast() {
  function addToast({ type = 'info', title = '', message = '', duration = 4000 }) {
    const id = ++toastId
    toasts.value.push({ id, type, title, message, duration })
    if (duration > 0) {
      setTimeout(() => removeToast(id), duration)
    }
    return id
  }

  function removeToast(id) {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  function success(message, title = '') {
    return addToast({ type: 'success', title, message })
  }

  function error(message, title = '') {
    return addToast({ type: 'error', title, message })
  }

  function warning(message, title = '') {
    return addToast({ type: 'warning', title, message })
  }

  function info(message, title = '') {
    return addToast({ type: 'info', title, message })
  }

  return { toasts, addToast, removeToast, success, error, warning, info }
}
