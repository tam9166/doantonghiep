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

async function attachAuthAndCaptcha(config) {
  config.headers = config.headers || {}
  if (!isInternalRequest(config)) {
    return config
  }

  const token = localStorage.getItem('token')
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
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

      // Token hết hạn hoặc không hợp lệ
      if (status === 401) {
        // Kiểm tra role trước khi xóa để redirect đúng trang
        const storedUser = localStorage.getItem('user')
        let isStaff = false
        if (storedUser) {
          try {
            const staffRoles = ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER']
            const roles = JSON.parse(storedUser).roles || []
            isStaff = roles.some(r => staffRoles.includes(r))
          } catch (e) { /* ignore */ }
        }

        localStorage.removeItem('token')
        localStorage.removeItem('user')

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
  const stored = localStorage.getItem('user')
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
  return !!localStorage.getItem('token')
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
