<template>
  <Teleport to="body">
    <Transition name="dialog-fade">
      <div v-if="dialogState.open" class="dialog-backdrop" @click.self="cancelDialog">
        <section class="app-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId" @keydown.esc="cancelDialog">
          <h2 :id="titleId">{{ dialogState.title }}</h2>
          <p>{{ dialogState.message }}</p>
          <label v-if="dialogState.mode === 'prompt'" class="dialog-field">
            <span>{{ dialogState.inputLabel }}</span>
            <textarea
              ref="input"
              v-model="dialogState.inputValue"
              :placeholder="dialogState.inputPlaceholder"
              rows="3"
              @keydown.ctrl.enter="submit"
            />
          </label>
          <div class="dialog-actions">
            <button type="button" class="dialog-cancel" @click="cancelDialog">{{ dialogState.cancelLabel }}</button>
            <button type="button" :class="['dialog-confirm', { danger: dialogState.danger }]" :disabled="invalid" @click="submit">
              {{ dialogState.confirmLabel }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, acceptDialog, cancelDialog } = useDialog()
const input = ref(null)
const titleId = 'global-dialog-title'
const invalid = computed(() => dialogState.mode === 'prompt'
  && dialogState.required
  && !dialogState.inputValue.trim())

const submit = () => {
  if (invalid.value) return
  acceptDialog(dialogState.mode === 'prompt' ? dialogState.inputValue.trim() : true)
}

watch(() => dialogState.open, async open => {
  if (open && dialogState.mode === 'prompt') {
    await nextTick()
    input.value?.focus()
  }
})
</script>

<style scoped>
.dialog-backdrop { position: fixed; inset: 0; z-index: 100000; display: grid; place-items: center; padding: 20px; background: rgba(15, 23, 42, .58); backdrop-filter: blur(3px); }
.app-dialog { width: min(480px, 100%); padding: 24px; border-radius: 16px; background: var(--color-surface, #fff); color: var(--color-on-surface, #172033); box-shadow: 0 24px 70px rgba(0, 0, 0, .3); }
.app-dialog h2 { margin: 0 0 10px; font-size: 1.25rem; }
.app-dialog p { margin: 0 0 18px; line-height: 1.55; white-space: pre-line; }
.dialog-field { display: grid; gap: 8px; margin-bottom: 18px; font-weight: 700; }
.dialog-field textarea { width: 100%; box-sizing: border-box; resize: vertical; padding: 12px; border: 1px solid color-mix(in srgb, var(--secondary) 28%, transparent); border-radius: 10px; font: inherit; }
.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; }
.dialog-actions button { min-height: 42px; padding: 0 18px; border: 0; border-radius: 9px; font-weight: 800; cursor: pointer; }
.dialog-cancel { background: color-mix(in srgb, var(--secondary) 10%, transparent); color: var(--color-on-surface, #172033); }
.dialog-confirm { background: var(--secondary); color: #fff; }
.dialog-confirm.danger { background: var(--primary); }
.dialog-confirm:disabled { cursor: not-allowed; opacity: .5; }
.dialog-fade-enter-active, .dialog-fade-leave-active { transition: opacity .18s ease; }
.dialog-fade-enter-from, .dialog-fade-leave-to { opacity: 0; }
</style>
