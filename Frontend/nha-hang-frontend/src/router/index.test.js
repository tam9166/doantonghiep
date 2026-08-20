import { describe, expect, it } from 'vitest'
import { canAccessAdminRoute, canAccessOperationalWorkspace, customerRouteRedirect } from './roleAccess'

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
})
