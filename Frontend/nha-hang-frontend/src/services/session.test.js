// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from 'vitest'
import {
  AUTH_CONTEXT,
  clearCustomerSession,
  clearStaffSession,
  getActiveAuthContext,
  getCustomerToken,
  getCustomerUser,
  getStaffToken,
  getStaffUser,
  setCustomerSession,
  setStaffSession
} from './session'

describe('separate customer and staff sessions', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  function storeBothSessions() {
    sessionStorage.setItem('token', 'customer-token')
    sessionStorage.setItem('user', JSON.stringify({ username: 'customer' }))
    sessionStorage.setItem('staff_token', 'staff-token')
    sessionStorage.setItem('staff_user', JSON.stringify({ username: 'manager' }))
  }

  it('reads each namespace without falling back to the other', () => {
    storeBothSessions()
    expect(getCustomerToken()).toBe('customer-token')
    expect(getCustomerUser().username).toBe('customer')
    expect(getStaffToken()).toBe('staff-token')
    expect(getStaffUser().username).toBe('manager')
  })

  it('staff logout preserves an active customer session', () => {
    storeBothSessions()
    clearStaffSession()
    expect(getStaffToken()).toBeNull()
    expect(getStaffUser()).toBeNull()
    expect(getCustomerToken()).toBe('customer-token')
    expect(getCustomerUser().username).toBe('customer')
  })

  it('customer logout preserves an active staff session', () => {
    storeBothSessions()
    clearCustomerSession()
    expect(getCustomerToken()).toBeNull()
    expect(getCustomerUser()).toBeNull()
    expect(getStaffToken()).toBe('staff-token')
    expect(getStaffUser().username).toBe('manager')
  })

  it('ignores browser-wide auth values from localStorage', () => {
    localStorage.setItem('token', 'shared-customer-token')
    localStorage.setItem('staff_token', 'shared-admin-token')

    expect(getCustomerToken()).toBeNull()
    expect(getStaffToken()).toBeNull()
  })

  it.each([
    ['ROLE_WAITER', 'waiter-token'],
    ['ROLE_KITCHEN', 'kitchen-token'],
    ['ROLE_CASHIER', 'cashier-token'],
    ['ROLE_ADMIN', 'admin-token']
  ])('keeps a %s identity in the current window only', (role, token) => {
    setStaffSession(token, { username: role, roles: [role] })

    expect(getActiveAuthContext()).toBe(AUTH_CONTEXT.STAFF)
    expect(getStaffToken()).toBe(token)
    expect(getStaffUser().roles).toEqual([role])
    expect(localStorage.getItem('staff_token')).toBeNull()
  })

  it('switches the active identity only inside the current window', () => {
    setStaffSession('admin-token', { username: 'admin', roles: ['ROLE_ADMIN'] })
    setCustomerSession('customer-token', { username: 'customer', roles: ['ROLE_CUSTOMER'] })

    expect(getActiveAuthContext()).toBe(AUTH_CONTEXT.CUSTOMER)
    expect(getCustomerToken()).toBe('customer-token')
    expect(getStaffToken()).toBeNull()
  })
})
