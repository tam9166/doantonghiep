// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from 'vitest'
import { useDialog } from '@/composables/useDialog'
import { printElement } from '@/utils/printElement'

describe('shared destructive-action dialog', () => {
  it('resolves confirm and required prompt through one global state', async () => {
    const dialog = useDialog()
    const confirmation = dialog.confirmDialog({ title: 'Xóa bàn', danger: true })
    expect(dialog.dialogState.open).toBe(true)
    expect(dialog.dialogState.danger).toBe(true)
    dialog.acceptDialog(true)
    await expect(confirmation).resolves.toBe(true)

    const prompt = dialog.promptDialog({ title: 'Hủy món', required: true })
    dialog.acceptDialog('Hết nguyên liệu')
    await expect(prompt).resolves.toBe('Hết nguyên liệu')
  })
})

describe('isolated invoice printing', () => {
  beforeEach(() => {
    document.body.innerHTML = '<main id="invoice"><strong>HĐ-01</strong></main>'
  })

  it('prints a cloned element without replacing application body state', () => {
    const original = document.getElementById('invoice')
    const nativeCreate = document.createElement.bind(document)
    document.createElement = tag => {
      const node = nativeCreate(tag)
      if (tag === 'iframe') {
        Object.defineProperty(node, 'contentWindow', { value: { addEventListener() {}, focus() {}, print() {} } })
        Object.defineProperty(node, 'contentDocument', { value: document.implementation.createHTMLDocument('print') })
      }
      return node
    }

    try {
      printElement('invoice')
      expect(document.getElementById('invoice')).toBe(original)
      expect(document.body.textContent).toContain('HĐ-01')
    } finally {
      document.createElement = nativeCreate
    }
  })
})
