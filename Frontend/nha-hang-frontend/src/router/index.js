import { createRouter, createWebHistory } from 'vue-router'
import { routeLoading } from './loadingState'
import i18n from '@/i18n'
import { canAccessAdminRoute, canAccessOperationalWorkspace, customerRouteRedirect } from './roleAccess'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue') },
    { path: '/staff-login', name: 'staff-login', component: () => import('@/views/StaffLogin.vue') },
    { path: '/change-password', name: 'change-password', component: () => import('@/views/FirstPasswordChange.vue') },
    { path: '/menu', name: 'menu', component: () => import('@/views/ProductMenu.vue') },
    { path: '/history', name: 'history', component: () => import('@/views/OrderHistory.vue') },
    { path: '/profile', name: 'profile', component: () => import('@/views/CustomerProfile.vue') },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminProduct.vue') },
    { path: '/admin/orders', name: 'admin-orders', component: () => import('@/views/AdminOrder.vue') },
    { path: '/admin/reservations', name: 'admin-reservations', component: () => import('@/views/AdminReservation.vue') },
    { path: '/admin/reservation-reviews', name: 'admin-reservation-reviews', component: () => import('@/views/AdminReservationReview.vue') },
    { path: '/admin/customer-history', name: 'admin-customer-history', component: () => import('@/views/AdminCustomerHistory.vue') },
    { path: '/admin/deposit-policies', name: 'admin-deposit-policies', component: () => import('@/views/AdminDepositPolicy.vue') },
    { path: '/admin/analytics', name: 'admin-analytics', component: () => import('@/views/AdminAnalytics.vue') },
    { path: '/admin/ai-knowledge', name: 'admin-ai-knowledge', component: () => import('@/views/AdminAiKnowledge.vue') },
    { path: '/reservation', name: 'reservation', component: () => import('@/views/Reservation.vue') },
    { path: '/dat-su-kien', name: 'event-booking', component: () => import('@/views/EventBooking.vue') },
    { path: '/reservation-lookup', name: 'reservation-lookup', component: () => import('@/views/ReservationLookup.vue') },
    { path: '/admin/categories', name: 'admin-categories', component: () => import('@/views/AdminCategory.vue') },
    { path: '/admin/tables', name: 'admin-tables', component: () => import('@/views/AdminTable.vue') },
    { path: '/admin/table-areas', name: 'admin-table-areas', component: () => import('@/views/AdminTableArea.vue') },
    { path: '/admin/staff', name: 'AdminStaff', component: () => import('../views/AdminStaff.vue') },
    { path: '/admin/posts', name: 'AdminPost', component: () => import('../views/AdminPost.vue') },
    { path: '/dine-in', name: 'DineInOrder', component: () => import('../views/DineInOrder.vue') },
    { path: '/kitchen', name: 'Kitchen', component: () => import('../views/Kitchen.vue') },
    { path: '/waiter', name: 'Waiter', component: () => import('../views/Waiter.vue') },
    { path: '/staff', name: 'Staff', component: () => import('../views/Staff.vue') },
    { path: '/admin/ingredients', name: 'AdminIngredient', component: () => import('../views/AdminIngredient.vue') },
    { path: '/admin/activity-log', name: 'AdminActivityLog', component: () => import('../views/AdminActivityLog.vue') },
    { path: '/admin/popular-items', name: 'AdminPopularItems', component: () => import('../views/AdminPopularItems.vue') },
    { path: '/admin/purchase-suggestions', name: 'AdminPurchaseSuggestion', component: () => import('../views/AdminPurchaseSuggestion.vue') },
    { path: '/admin/vouchers', name: 'AdminVoucher', component: () => import('../views/AdminVoucher.vue') },
    { path: '/cashier', name: 'Cashier', component: () => import('../views/CashierView.vue') },
    // P0-07: Catch-all route for SPA fallback - handles all unmatched paths
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

// NGƯỜI GÁC CỔNG (Tách biệt Khách hàng và Nhân sự)
router.beforeEach((to, from) => {
  routeLoading.value = to.path.startsWith('/admin') && from.path !== to.path

  const hasStaffSession = Boolean(localStorage.getItem('staff_token'))
  const isStaffWorkspace = to.path.startsWith('/admin')
    || to.path.startsWith('/kitchen')
    || to.path.startsWith('/waiter')
    || to.path.startsWith('/cashier')
    || to.path === '/staff'
    || (to.path === '/change-password' && hasStaffSession)
  const token = localStorage.getItem(isStaffWorkspace ? 'staff_token' : 'token')
  const storedUser = localStorage.getItem(isStaffWorkspace ? 'staff_user' : 'user')
  let userRoles = []
  let mustChangePassword = false

  if (storedUser) {
    try {
      const user = JSON.parse(storedUser)
      userRoles = user.roles || []
      mustChangePassword = Boolean(user.mustChangePassword)
    } catch {
      userRoles = []
    }
  }

  // Danh sách role nhân sự
  const staffRoles = ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER']
  const isStaff = userRoles.some(r => staffRoles.includes(r))

  if (token && mustChangePassword && to.path !== '/change-password') {
    return '/change-password'
  }
  if (to.path === '/change-password' && !token) {
    return isStaff ? '/staff-login' : '/login'
  }

  // 1. NGĂN NHÂN VIÊN LÀM VIỆC RIÊNG (Chỉ Admin/Manager mới được xem trang khách)
  const customerRedirect = customerRouteRedirect(to.path, userRoles)
  if (customerRedirect) {
    if (customerRedirect === '/kitchen') {
      alert(i18n.global.t('access.kitchenRedirect'))
    }
    if (customerRedirect === '/waiter') {
      alert(i18n.global.t('access.waiterRedirect'))
    }
    if (customerRedirect === '/cashier') {
      alert(i18n.global.t('access.cashierOnly'))
    }
    return customerRedirect
  }

  // 2. BẢO VỆ KHU VỰC QUẢN TRỊ THEO CÙNG MA TRẬN QUYỀN VỚI BACKEND
  if (to.path.startsWith('/admin')) {
    if (!token || !canAccessAdminRoute(to.path, userRoles)) {
      if (!token) {
        // Chưa đăng nhập → chuyển về trang đăng nhập nhân sự
        return '/staff-login'
      }
      alert(i18n.global.t('access.adminDenied'))
      return '/'
    }
  }

  // 3. KHU VỰC BẾP (Bếp và cấp giám sát)
  if (to.path.startsWith('/kitchen')) {
    if (!token || !canAccessOperationalWorkspace(to.path, userRoles)) {
      if (!token) {
        return '/staff-login'
      }
      alert(i18n.global.t('access.kitchenOnly'))
      return '/'
    }
  }

  // 4. KHU VỰC PHỤC VỤ (Phục vụ và cấp giám sát)
  if (to.path.startsWith('/waiter')) {
    if (!token || !canAccessOperationalWorkspace(to.path, userRoles)) {
      if (!token) {
        return '/staff-login'
      }
      alert(i18n.global.t('access.waiterOnly'))
      return '/'
    }
  }

  // 5. KHU VỰC THU NGÂN (Thu ngân và cấp giám sát)
  if (to.path.startsWith('/cashier')) {
    if (!token || !canAccessOperationalWorkspace(to.path, userRoles)) {
      if (!token) {
        return '/staff-login'
      }
      alert(i18n.global.t('access.cashierOnly'))
      return '/'
    }
  }

  // 6. KHU VỰC NHÂN VIÊN CHUNG
  if (to.path === '/staff') {
    if (!token || !isStaff) {
      if (!token) {
        return '/staff-login'
      }
      alert(i18n.global.t('access.staffOnly'))
      return '/'
    }
  }

  // Nếu qua được hết các trạm kiểm soát thì cho phép đi tiếp
  return true
})

router.afterEach(() => {
  window.setTimeout(() => {
    routeLoading.value = false
  }, 160)
})

router.onError(() => {
  routeLoading.value = false
})

export default router
