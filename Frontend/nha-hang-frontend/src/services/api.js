/**
 * ============================================================
 * API SERVICE - Tập trung hóa tất cả HTTP calls
 * ============================================================
 * Thay vì hardcode  ở mọi nơi,
 * import { api } from '@/services/api' rồi dùng api.get(), api.post()...
 */
import axios from 'axios'
import router from '@/router'
import { captchaActionForRequest, executeCaptcha } from './captcha'

// Only this client is allowed to talk to the restaurant backend. Keeping the
// base URL explicit prevents the authenticated interceptor from being reused
// accidentally for a third-party origin.
const API_BASE_URL = import.meta.env.VITE_API_URL || '/'

// Tạo Axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// External integrations must use this client. It deliberately has no request
// interceptor, so application credentials cannot leave the backend origin.
const externalApi = axios.create({
  timeout: 15000
})

function isInternalRequest(config) {
  const requestUrl = config.url || ''
  if (requestUrl.startsWith('/') && !requestUrl.startsWith('//')) {
    return true
  }

  if (typeof window === 'undefined') {
    return false
  }

  try {
    const backendOrigin = new URL(API_BASE_URL, window.location.origin).origin
    return new URL(requestUrl, backendOrigin).origin === backendOrigin
  } catch {
    return false
  }
}

function isStaffRequest(config) {
  if (config.authType === 'staff') return true
  if (config.authType === 'customer') return false

  const requestUrl = config.url || ''
  if (/^\/api\/(admin|staff|schedules|timekeeping|cashier|waiter|kitchen)(\/|$)/.test(requestUrl)) {
    return true
  }

  const currentPath = router.currentRoute.value.path
  return currentPath.startsWith('/admin')
    || currentPath.startsWith('/kitchen')
    || currentPath.startsWith('/waiter')
    || currentPath.startsWith('/cashier')
    || currentPath === '/staff'
    || (currentPath === '/change-password' && Boolean(localStorage.getItem('staff_token')))
}

async function attachAuthAndCaptcha(config) {
  config.headers = config.headers || {}
  if (!isInternalRequest(config)) {
    return config
  }

  const token = localStorage.getItem(isStaffRequest(config) ? 'staff_token' : 'token')
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
    // Used by the response interceptor to distinguish a rejected session from
    // a public/login request that happens to return 401.
    config.__sessionTokenAttached = true
  }

  const action = captchaActionForRequest(config)
  if (action && !config.headers['X-Captcha-Token']) {
    const captchaToken = await executeCaptcha(action)
    if (captchaToken) {
      config.headers['X-Captcha-Token'] = captchaToken
    }
  }
  return config
}

// ========== REQUEST INTERCEPTOR ==========
// Only the dedicated internal client can attach application credentials.
api.interceptors.request.use(
  attachAuthAndCaptcha,
  (error) => Promise.reject(error)
)

// ========== RESPONSE INTERCEPTOR ==========
// Xử lý lỗi tập trung
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status } = error.response

      // Only an authenticated request rejected with 401 proves that the
      // locally stored session is no longer accepted. A 403 means the session
      // is valid but lacks permission and must never force logout.
      if (status === 401 && error.config?.__sessionTokenAttached && !error.config?.preserveSessionOn401) {
        const isStaff = isStaffRequest(error.config || {})
        if (isStaff) {
          localStorage.removeItem('staff_token')
          localStorage.removeItem('staff_user')
        } else {
          localStorage.removeItem('token')
          localStorage.removeItem('user')
        }

        // Redirect về đúng trang đăng nhập
        const currentPath = router.currentRoute.value.path
        const loginPath = isStaff ? '/staff-login' : '/login'
        if (currentPath !== '/login' && currentPath !== '/staff-login') {
          router.push(loginPath)
        }
      }
    }
    return Promise.reject(error)
  }
)

// ========== HELPER FUNCTIONS ==========

/**
 * Lấy thông tin user hiện tại từ localStorage
 */
export function getCurrentUser() {
  const stored = localStorage.getItem('staff_user') || localStorage.getItem('user')
  if (!stored) return null
  try {
    return JSON.parse(stored)
  } catch {
    return null
  }
}

/**
 * Kiểm tra user đã đăng nhập chưa
 */
export function isAuthenticated() {
  return !!(localStorage.getItem('staff_token') || localStorage.getItem('token'))
}

/**
 * Kiểm tra user có role cụ thể
 */
export function hasRole(role) {
  const user = getCurrentUser()
  return user?.roles?.includes(role) || false
}

/**
 * Kiểm tra user có bất kỳ role nào trong danh sách
 */
export function hasAnyRole(...roles) {
  const user = getCurrentUser()
  if (!user?.roles) return false
  return roles.some(r => user.roles.includes(r))
}

/**
 * Logout - xóa token và redirect
 */
export function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  window.location.href = '/'
}

export { api, externalApi, API_BASE_URL }
export default api
