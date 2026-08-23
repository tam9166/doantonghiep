const STAFF_TOKEN_KEY = 'staff_token'
const STAFF_USER_KEY = 'staff_user'
const CUSTOMER_TOKEN_KEY = 'token'
const CUSTOMER_USER_KEY = 'user'
const AUTH_CONTEXT_KEY = 'auth_context'

export const AUTH_CONTEXT = Object.freeze({
  CUSTOMER: 'customer',
  STAFF: 'staff'
})

function readJson(key) {
  const value = sessionStorage.getItem(key)
  if (!value) return null
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

export const getStaffToken = () => sessionStorage.getItem(STAFF_TOKEN_KEY)
export const getStaffUser = () => readJson(STAFF_USER_KEY)
export const isStaffAuthenticated = () => Boolean(getStaffToken())

export const getCustomerToken = () => sessionStorage.getItem(CUSTOMER_TOKEN_KEY)
export const getCustomerUser = () => readJson(CUSTOMER_USER_KEY)
export const isCustomerAuthenticated = () => Boolean(getCustomerToken())

export const getActiveAuthContext = () => sessionStorage.getItem(AUTH_CONTEXT_KEY)

export function setStaffSession(token, user) {
  clearCustomerSession()
  sessionStorage.setItem(STAFF_TOKEN_KEY, token)
  sessionStorage.setItem(STAFF_USER_KEY, JSON.stringify(user))
  sessionStorage.setItem(AUTH_CONTEXT_KEY, AUTH_CONTEXT.STAFF)
}

export function setCustomerSession(token, user) {
  clearStaffSession()
  sessionStorage.setItem(CUSTOMER_TOKEN_KEY, token)
  sessionStorage.setItem(CUSTOMER_USER_KEY, JSON.stringify(user))
  sessionStorage.setItem(AUTH_CONTEXT_KEY, AUTH_CONTEXT.CUSTOMER)
}

export function clearStaffSession() {
  sessionStorage.removeItem(STAFF_TOKEN_KEY)
  sessionStorage.removeItem(STAFF_USER_KEY)
  if (getActiveAuthContext() === AUTH_CONTEXT.STAFF) {
    sessionStorage.removeItem(AUTH_CONTEXT_KEY)
  }
}

export function clearCustomerSession() {
  sessionStorage.removeItem(CUSTOMER_TOKEN_KEY)
  sessionStorage.removeItem(CUSTOMER_USER_KEY)
  if (getActiveAuthContext() === AUTH_CONTEXT.CUSTOMER) {
    sessionStorage.removeItem(AUTH_CONTEXT_KEY)
  }
}
