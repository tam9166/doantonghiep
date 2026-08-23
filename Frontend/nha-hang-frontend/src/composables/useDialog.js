import { reactive } from 'vue'

const state = reactive({
  open: false,
  mode: 'confirm',
  title: '',
  message: '',
  confirmLabel: 'Xác nhận',
  cancelLabel: 'Hủy',
  danger: false,
  inputLabel: '',
  inputValue: '',
  inputPlaceholder: '',
  required: false,
})

let settleCurrent = null

function openDialog(options) {
  if (settleCurrent) settleCurrent(null)
  Object.assign(state, {
    open: true,
    mode: options.mode || 'confirm',
    title: options.title || 'Xác nhận thao tác',
    message: options.message || '',
    confirmLabel: options.confirmLabel || 'Xác nhận',
    cancelLabel: options.cancelLabel || 'Hủy',
    danger: Boolean(options.danger),
    inputLabel: options.inputLabel || '',
    inputValue: options.defaultValue || '',
    inputPlaceholder: options.inputPlaceholder || '',
    required: Boolean(options.required),
  })
  return new Promise(resolve => { settleCurrent = resolve })
}

function settle(value) {
  const resolve = settleCurrent
  settleCurrent = null
  state.open = false
  if (resolve) resolve(value)
}

export function useDialog() {
  const confirmDialog = options => openDialog({ ...options, mode: 'confirm' })
  const promptDialog = options => openDialog({ ...options, mode: 'prompt' })
  return {
    dialogState: state,
    confirmDialog,
    promptDialog,
    acceptDialog: value => settle(value),
    cancelDialog: () => settle(null),
  }
}
