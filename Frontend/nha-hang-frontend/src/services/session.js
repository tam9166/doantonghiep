const STAFF_TOKEN_KEY = 'staff_token'
const STAFF_USER_KEY = 'staff_user'
const CUSTOMER_TOKEN_KEY = 'token'
const CUSTOMER_USER_KEY = 'user'

function readJson(key) {
  const value = localStorage.getItem(key)
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export const getStaffToken = () => localStorage.getItem(STAFF_TOKEN_KEY)
export const getStaffUser = () => readJson(STAFF_USER_KEY)
export const isStaffAuthenticated = () => Boolean(getStaffToken())

export const getCustomerToken = () => localStorage.getItem(CUSTOMER_TOKEN_KEY)
export const getCustomerUser = () => readJson(CUSTOMER_USER_KEY)
export const isCustomerAuthenticated = () => Boolean(getCustomerToken())

export function clearStaffSession() {
  localStorage.removeItem(STAFF_TOKEN_KEY)
  localStorage.removeItem(STAFF_USER_KEY)
}

export function clearCustomerSession() {
  localStorage.removeItem(CUSTOMER_TOKEN_KEY)
  localStorage.removeItem(CUSTOMER_USER_KEY)
}
