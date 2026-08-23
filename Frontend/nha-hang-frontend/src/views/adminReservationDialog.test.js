import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('./AdminReservation.vue', import.meta.url)),
  'utf8',
)

describe('admin reservation workflow dialogs', () => {
  it('uses the shared dialog and requires a real reservation code for waitlist conversion', () => {
    expect(source).toContain("import { useDialog } from '@/composables/useDialog'")
    expect(source).toContain('const { promptDialog } = useDialog()')
    expect(source).not.toMatch(/window\.(prompt|confirm)\s*\(/)
    expect(source).toContain("inputLabel: 'Mã đặt bàn'")
    expect(source).toContain('required: true')
    expect(source).toContain('if (linkedReservationCode === null) return')
  })
})
