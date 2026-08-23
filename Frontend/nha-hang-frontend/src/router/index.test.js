import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { canAccessAdminRoute, canAccessOperationalWorkspace, customerRouteRedirect, isStaffWorkspacePath } from './roleAccess'

const srcRoot = fileURLToPath(new URL('../', import.meta.url))

describe('role route guard', () => {
  it('allows managers to supervise every operational workspace', () => {
    const roles = ['ROLE_MANAGER']

    expect(canAccessOperationalWorkspace('/kitchen', roles)).toBe(true)
    expect(canAccessOperationalWorkspace('/waiter', roles)).toBe(true)
    expect(canAccessOperationalWorkspace('/cashier', roles)).toBe(true)
  })

  it('redirects cashier accounts away from customer reservation pages', () => {
    expect(customerRouteRedirect('/reservation', ['ROLE_CASHIER'])).toBe('/cashier')
    expect(customerRouteRedirect('/reservation', ['ROLE_MANAGER'])).toBeNull()
  })

  it('aligns operational admin pages with backend role permissions', () => {
    expect(canAccessAdminRoute('/admin/orders', ['ROLE_KITCHEN'])).toBe(true)
    expect(canAccessAdminRoute('/admin/orders', ['ROLE_WAITER'])).toBe(true)
    expect(canAccessAdminRoute('/admin/orders', ['ROLE_CASHIER'])).toBe(true)
    expect(canAccessAdminRoute('/admin/popular-items', ['ROLE_KITCHEN'])).toBe(true)
    expect(canAccessAdminRoute('/admin/ingredients', ['ROLE_KITCHEN'])).toBe(true)
    expect(canAccessAdminRoute('/admin/staff', ['ROLE_KITCHEN'])).toBe(false)
  })

  it('treats the dedicated staff profile as staff context without matching staff login', () => {
    expect(isStaffWorkspacePath('/staff/profile')).toBe(true)
    expect(isStaffWorkspacePath('/staff')).toBe(true)
    expect(isStaffWorkspacePath('/staff-login')).toBe(false)
    expect(customerRouteRedirect('/profile', ['ROLE_KITCHEN'])).toBe('/kitchen')
  })

  it('keeps every staff profile action out of the customer profile route', () => {
    for (const file of ['Kitchen.vue', 'Waiter.vue', 'CashierView.vue']) {
      const source = readFileSync(`${srcRoot}/views/${file}`, 'utf8')
      expect(source).toContain("$router.push('/staff/profile')")
    }

    const adminLayout = readFileSync(`${srcRoot}/components/AdminLayout.vue`, 'utf8')
    expect(adminLayout).toContain('to="/staff/profile"')
  })
})
