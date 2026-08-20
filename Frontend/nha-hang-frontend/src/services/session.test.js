// @vitest-environment jsdom
import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearCustomerSession,
  clearStaffSession,
  getCustomerToken,
  getCustomerUser,
  getStaffToken,
  getStaffUser
} from './session'

describe('separate customer and staff sessions', () => {
  beforeEach(() => localStorage.clear())

  function storeBothSessions() {
    localStorage.setItem('token', 'customer-token')
    localStorage.setItem('user', JSON.stringify({ username: 'customer' }))
    localStorage.setItem('staff_token', 'staff-token')
    localStorage.setItem('staff_user', JSON.stringify({ username: 'manager' }))
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
})
