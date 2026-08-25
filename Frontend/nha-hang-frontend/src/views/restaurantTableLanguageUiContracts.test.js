import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = relativePath => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8')

describe('restaurant table and customer language UI contracts', () => {
  it('keeps the customer language across refresh and route navigation', () => {
    const i18n = source('../i18n.js')
    expect(i18n).toContain("localStorage.getItem('lang')")
    expect(i18n).toContain("localStorage.setItem('lang', value)")
    expect(i18n).toContain('document.documentElement.lang = value')
  })

  it('uses the same business table identifier and hierarchy in staff views', () => {
    for (const view of ['Waiter.vue', 'CashierView.vue', 'AdminTable.vue']) {
      const content = source(`./${view}`)
      expect(content).toContain('groupTablesByFloorAndArea')
    }
    for (const view of ['Waiter.vue', 'CashierView.vue']) {
      const content = source(`./${view}`)
      expect(content).toContain('tableIdentifier(table)')
      expect(content).toMatch(/table\.capacity[^}]*}} người/)
    }
  })

  it('keeps active map controls readable without inline color overrides', () => {
    const admin = source('./AdminTable.vue')
    expect(admin).toContain('color: var(--color-on-primary)')
    expect(admin).toContain(':deep(svg)')
    const realisticButton = admin.split('\n').find(line => line.includes("'active': isRealisticView"))
    const layoutButton = admin.split('\n').find(line => line.includes("'active': layoutEditMode"))
    expect(realisticButton).not.toContain('style=')
    expect(layoutButton).not.toContain('style=')
  })

  it('shows capacity units in administration and booking views', () => {
    expect(source('./AdminTable.vue')).toContain('Sức chứa tối đa (người)')
    expect(source('./AdminTableArea.vue')).toContain('Sức chứa tối đa (người)')
    expect(source('./Reservation.vue')).toContain('text.people')
  })

  it('does not hardcode the primary customer home, menu, or account headings', () => {
    const home = source('./Home.vue')
    expect(home).toContain("$t('home.restaurantName')")
    expect(home).toContain("$t('home.openingHours')")
    expect(source('./ProductMenu.vue')).toContain("t('menu.remaining'")
    for (const view of ['Login.vue', 'Register.vue', 'CustomerProfile.vue', 'OrderHistory.vue']) {
      expect(source(`./${view}`)).toContain('useI18n')
    }
  })
})
