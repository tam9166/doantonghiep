/**
 * ============================================================
 * API SERVICE - Tập trung hóa tất cả HTTP calls
 * ============================================================
 * Thay vì hardcode http://localhost:8080 ở mọi nơi,
 * import { api } from '@/services/api' rồi dùng api.get(), api.post()...
 */
import axios from 'axios'
import router from '@/router'

// Base URL - dễ dàng thay đổi khi deploy
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// Tạo Axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ========== REQUEST INTERCEPTOR ==========
// Tự động gắn JWT token vào mọi request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
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

export { api, API_BASE_URL }
export default api
