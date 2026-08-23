import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(fileURLToPath(new URL('./AdminStaff.vue', import.meta.url)), 'utf8')

describe('AdminStaff mobile employee list', () => {
  it('provides labels for every employee field when rows become cards', () => {
    for (const label of ['Username', 'Họ tên', 'Email', 'Ca làm', 'Khu vực', 'Vị trí', 'Hành động']) {
      expect(source).toContain(`data-label="${label}"`)
    }
  })

  it('uses a card layout instead of clipping the employee table on mobile', () => {
    expect(source).toContain('@media (max-width: 600px)')
    expect(source).toMatch(/\.staff-table thead\s*{\s*display:\s*none;/)
    expect(source).toMatch(/\.staff-table td\s*{[^}]*grid-template-columns:/s)
    expect(source).toMatch(/\.staff-table \.staff-actions\s*{[^}]*grid-template-columns:/s)
  })
})
