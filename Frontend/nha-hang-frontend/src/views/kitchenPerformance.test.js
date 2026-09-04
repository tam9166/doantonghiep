import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(fileURLToPath(new URL('./Kitchen.vue', import.meta.url)), 'utf8')

describe('kitchen realtime fallback', () => {
  it('uses the bounded kitchen board and a 60-second polling fallback', () => {
    expect(source).toContain("/api/admin/orders/kitchen/board")
    expect(source).not.toContain("api.get('/api/admin/orders'")
    expect(source).toContain('}, 60000)')
    expect(source).not.toContain('}, 5000)')
  })

  it('pages kitchen menu after sorting available dishes before unavailable dishes', () => {
    expect(source).toContain('const MENU_PAGE_SIZE = 12')
    expect(source).toContain('sortedMenuProducts')
    expect(source).toContain('menuPagedProducts')
    expect(source).toContain("const aAvailable = a.available ? 0 : 1")
    expect(source).toContain('class="menu-pagination-wrap"')
  })
})
