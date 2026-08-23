import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(fileURLToPath(new URL('./CashierView.vue', import.meta.url)), 'utf8')

describe('cashier responsive layout', () => {
  it('stacks the table map and invoice before mobile widths', () => {
    expect(source).toContain('@media (max-width: 900px)')
    expect(source).toMatch(/\.cashier-content\s*\{[\s\S]*?grid-template-columns:\s*minmax\(0, 1fr\)/)
  })

  it('keeps mobile table cards and payment actions usable', () => {
    expect(source).toContain('@media (max-width: 600px)')
    expect(source).toMatch(/\.table-grid\s*\{[\s\S]*?grid-template-columns:\s*minmax\(0, 1fr\)/)
    expect(source).toMatch(/\.action-buttons\s*\{[\s\S]*?flex-direction:\s*column/)
  })
})
