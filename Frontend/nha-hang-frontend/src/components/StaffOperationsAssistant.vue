<template>
  <section class="staff-assistant" :class="{ open: isOpen }" aria-live="polite">
    <button class="assistant-toggle" type="button" @click="isOpen = !isOpen">
      {{ isOpen ? 'Đóng trợ lý' : 'Tra cứu nhanh' }}
    </button>
    <form v-if="isOpen" class="assistant-panel" @submit.prevent="ask">
      <p class="assistant-title">Trợ lý vận hành</p>
      <p class="assistant-hint">Tồn kho, lô sắp hết hạn và món khả dụng.</p>
      <label class="sr-only" for="staff-assistant-question">Câu hỏi vận hành</label>
      <input id="staff-assistant-question" v-model.trim="question" maxlength="300"
        placeholder="Ví dụ: món nào đang khả dụng?" :disabled="loading" />
      <p v-if="reply" class="assistant-reply">{{ reply }}</p>
      <dl v-if="metadata" class="assistant-data">
        <template v-if="metadata.invoiceCount !== undefined">
          <dt>Hóa đơn chưa thanh toán</dt><dd>{{ metadata.invoiceCount }}</dd>
        </template>
        <template v-if="metadata.remainingAmount !== undefined">
          <dt>Còn phải thu</dt><dd>{{ formatCurrency(metadata.remainingAmount) }}</dd>
        </template>
        <template v-if="metadata.paidInvoiceCount !== undefined">
          <dt>Hóa đơn đã thanh toán hôm nay</dt><dd>{{ metadata.paidInvoiceCount }}</dd>
        </template>
        <template v-if="metadata.collectedAmount !== undefined">
          <dt>Đã thu hôm nay</dt><dd>{{ formatCurrency(metadata.collectedAmount) }}</dd>
        </template>
      </dl>
      <p v-if="error" class="assistant-error">{{ error }}</p>
      <button class="assistant-submit" type="submit" :disabled="loading || !question">
        {{ loading ? 'Đang tra cứu...' : 'Gửi câu hỏi' }}
      </button>
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/services/api'

const isOpen = ref(false)
const question = ref('')
const reply = ref('')
const error = ref('')
const loading = ref(false)
const metadata = ref(null)

function formatCurrency(value) {
  return new Intl.NumberFormat('vi-VN').format(Number(value || 0)) + ' đ'
}

async function ask() {
  if (!question.value || loading.value) return
  loading.value = true
  error.value = ''
  metadata.value = null
  try {
    const response = await api.post('/api/staff/assistant/query', { message: question.value, locale: 'vi' })
    reply.value = response.data.reply
    metadata.value = Object.keys(response.data.data || {}).length ? response.data.data : null
  } catch (requestError) {
    error.value = requestError.response?.data?.message || 'Không thể tra cứu dữ liệu lúc này.'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.staff-assistant { position: fixed; right: 20px; bottom: 20px; z-index: 80; width: min(360px, calc(100vw - 32px)); }
.assistant-toggle, .assistant-submit { min-height: 44px; border: 0; border-radius: 6px; font-weight: 700; cursor: pointer; }
.assistant-toggle { width: 100%; background: var(--primary); color: #fff; box-shadow: 0 8px 22px rgba(30, 45, 25, .25); }
.assistant-panel { margin-top: 8px; padding: 16px; border: 1px solid var(--color-outline-variant); border-radius: 8px; background: var(--bg-card); box-shadow: 0 14px 34px rgba(0, 0, 0, .18); }
.assistant-title { margin: 0; color: var(--text-primary); font-weight: 800; }
.assistant-hint { margin: 5px 0 12px; color: var(--text-muted); font-size: .85rem; }
.assistant-panel input { width: 100%; min-height: 42px; padding: 9px 10px; border: 1px solid var(--color-outline-variant); border-radius: 6px; box-sizing: border-box; color: var(--text-primary); background: var(--bg-root); }
.assistant-reply, .assistant-error { margin: 10px 0; padding: 10px; border-radius: 6px; line-height: 1.45; white-space: pre-line; }
.assistant-reply { color: var(--text-primary); background: var(--color-surface-container); }
.assistant-error { color: #9f2c20; background: #fff0ed; }
.assistant-data { display: grid; grid-template-columns: 1fr auto; gap: 6px 12px; margin: 10px 0; padding: 10px; background: var(--bg-root); border: 1px solid var(--color-outline-variant); border-radius: 6px; }
.assistant-data dt, .assistant-data dd { margin: 0; color: var(--text-primary); font-size: .85rem; }
.assistant-data dd { font-weight: 800; text-align: right; }
.assistant-submit { width: 100%; margin-top: 10px; background: var(--secondary); color: #201d14; }
.assistant-submit:disabled { cursor: not-allowed; opacity: .65; }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }
@media (max-width: 640px) { .staff-assistant { right: 12px; bottom: 12px; } }
</style>
