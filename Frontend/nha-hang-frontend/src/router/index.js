import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
import StaffLogin from '@/views/StaffLogin.vue'
import ProductMenu from '@/views/ProductMenu.vue'
import OrderHistory from '@/views/OrderHistory.vue'
import AdminProduct from '@/views/AdminProduct.vue'
import AdminOrder from '@/views/AdminOrder.vue'
import Reservation from '@/views/Reservation.vue'
import AdminCategory from '@/views/AdminCategory.vue'
import AdminTable from '@/views/AdminTable.vue'
import DineInOrder from '../views/DineInOrder.vue'
import Kitchen from '../views/Kitchen.vue'
import Waiter from '../views/Waiter.vue'
import AdminAnalytics from '@/views/AdminAnalytics.vue'
import CustomerProfile from '@/views/CustomerProfile.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: Home },
    { path: '/login', name: 'login', component: Login },
    { path: '/register', name: 'register', component: Register },
    { path: '/staff-login', name: 'staff-login', component: StaffLogin },
    { path: '/menu', name: 'menu', component: ProductMenu },
    { path: '/history', name: 'history', component: OrderHistory },
    { path: '/profile', name: 'profile', component: CustomerProfile },
    { path: '/admin', name: 'admin', component: AdminProduct },
    { path: '/admin/orders', name: 'admin-orders', component: AdminOrder },
    { path: '/admin/analytics', name: 'admin-analytics', component: AdminAnalytics },
    { path: '/reservation', name: 'reservation', component: Reservation },
    { path: '/admin/categories', name: 'admin-categories', component: AdminCategory },
    { path: '/admin/tables', name: 'admin-tables', component: AdminTable },
    { path: '/admin/staff', name: 'AdminStaff', component: () => import('../views/AdminStaff.vue') },
    { path: '/admin/posts', name: 'AdminPost', component: () => import('../views/AdminPost.vue') },
    { path: '/dine-in', name: 'DineInOrder', component: DineInOrder },
    { path: '/kitchen', name: 'Kitchen', component: Kitchen },
    { path: '/waiter', name: 'Waiter', component: Waiter },
    { path: '/staff', name: 'Staff', component: () => import('../views/Staff.vue') },
    { path: '/admin/ingredients', name: 'AdminIngredient', component: () => import('../views/AdminIngredient.vue') },
    { path: '/admin/activity-log', name: 'AdminActivityLog', component: () => import('../views/AdminActivityLog.vue') },
    { path: '/admin/popular-items', name: 'AdminPopularItems', component: () => import('../views/AdminPopularItems.vue') },
    { path: '/admin/purchase-suggestions', name: 'AdminPurchaseSuggestion', component: () => import('../views/AdminPurchaseSuggestion.vue') },
    { path: '/admin/vouchers', name: 'AdminVoucher', component: () => import('../views/AdminVoucher.vue') },
    { path: '/cashier', name: 'Cashier', component: () => import('../views/CashierView.vue') }
  ]
})

// NGƯỜI GÁC CỔNG (Tách biệt Khách hàng và Nhân sự)
router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const storedUser = localStorage.getItem('user')
  let userRoles = []

  if (storedUser) {
    try {
      userRoles = JSON.parse(storedUser).roles || []
    } catch (e) {
      userRoles = []
    }
  }

  // Danh sách role nhân sự
  const staffRoles = ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER']
  const isStaff = userRoles.some(r => staffRoles.includes(r))

  // 1. NGĂN NHÂN VIÊN LÀM VIỆC RIÊNG (Chỉ Admin/Manager mới được xem trang khách)
  const customerRoutes = ['/', '/reservation', '/history', '/profile']
  if (customerRoutes.includes(to.path)) {
    if (userRoles.includes('ROLE_KITCHEN') && !userRoles.includes('ROLE_ADMIN')) {
      alert('Bạn là nhân viên Bếp, vui lòng làm việc tại khu vực Bếp!')
      return '/kitchen'
    }
    if (userRoles.includes('ROLE_WAITER') && !userRoles.includes('ROLE_ADMIN')) {
      alert('Bạn là nhân viên Phục vụ, vui lòng làm việc tại khu vực Phục vụ!')
      return '/waiter'
    }
  }

  // 2. BẢO VỆ KHU VỰC QUẢN TRỊ CAO CẤP (Chỉ Admin / Manager)
  if (to.path.startsWith('/admin') && to.path !== '/admin/ingredients') {
    if (!token || (!userRoles.includes('ROLE_ADMIN') && !userRoles.includes('ROLE_MANAGER'))) {
      if (!token) {
        // Chưa đăng nhập → chuyển về trang đăng nhập nhân sự
        return '/staff-login'
      }
      alert('Cảnh báo: Bạn không có quyền truy cập khu vực Quản trị!')
      return '/'
    }
  }

  // 2.5 KHU VỰC NGUYÊN LIỆU (Cho phép Admin / Manager / Kitchen)
  if (to.path === '/admin/ingredients') {
    if (!token || (!userRoles.includes('ROLE_ADMIN') && !userRoles.includes('ROLE_MANAGER') && !userRoles.includes('ROLE_KITCHEN'))) {
      if (!token) {
        return '/staff-login'
      }
      alert('Cảnh báo: Bạn không có quyền truy cập trang Quản lý nguyên liệu!')
      return '/'
    }
  }

  // 3. KHU VỰC BẾP (Chỉ cho phép Bếp)
  if (to.path.startsWith('/kitchen')) {
    if (!token || !userRoles.includes('ROLE_KITCHEN')) {
      if (!token) {
        return '/staff-login'
      }
      alert('Khu vực hạn chế: Chỉ dành cho bộ phận Bếp!')
      return '/'
    }
  }

  // 4. KHU VỰC PHỤC VỤ (Chỉ cho phép Phục vụ)
  if (to.path.startsWith('/waiter')) {
    if (!token || !userRoles.includes('ROLE_WAITER')) {
      if (!token) {
        return '/staff-login'
      }
      alert('Khu vực hạn chế: Chỉ dành cho bộ phận Phục vụ!')
      return '/'
    }
  }

  // 5. KHU VỰC THU NGÂN (Chỉ cho phép Thu ngân)
  if (to.path.startsWith('/cashier')) {
    if (!token || !userRoles.includes('ROLE_CASHIER')) {
      if (!token) {
        return '/staff-login'
      }
      alert('Khu vực hạn chế: Chỉ dành cho bộ phận Thu ngân!')
      return '/'
    }
  }

  // 6. KHU VỰC NHÂN VIÊN CHUNG
  if (to.path === '/staff') {
    if (!token || !isStaff) {
      if (!token) {
        return '/staff-login'
      }
      alert('Khu vực hạn chế: Chỉ dành cho nhân viên!')
      return '/'
    }
  }

  // Nếu qua được hết các trạm kiểm soát thì cho phép đi tiếp
  return true
})

export default router

