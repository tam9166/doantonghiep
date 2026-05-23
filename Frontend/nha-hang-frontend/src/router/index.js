import { createRouter, createWebHistory } from 'vue-router'
import Home from '@/views/Home.vue'
import Login from '@/views/Login.vue'
import Register from '@/views/Register.vue'
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

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: Home },
    { path: '/login', name: 'login', component: Login },
    { path: '/register', name: 'register', component: Register },
    { path: '/menu', name: 'menu', component: ProductMenu },
    { path: '/history', name: 'history', component: OrderHistory },
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
    { path: '/staff', name: 'Staff', component: () => import('../views/Staff.vue') }
  ]
})

// NGƯỜI GÁC CỔNG (Chuẩn cú pháp Vue Router 4 + Logic Doanh nghiệp)
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

  // 1. NGĂN NHÂN VIÊN LÀM VIỆC RIÊNG (Chỉ Admin/Manager mới được xem trang khách)
  const customerRoutes = ['/', '/menu', '/reservation', '/dine-in', '/history']
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
  if (to.path.startsWith('/admin')) {
    if (!token || (!userRoles.includes('ROLE_ADMIN') && !userRoles.includes('ROLE_MANAGER'))) {
      alert('Cảnh báo: Bạn không có quyền truy cập khu vực Quản trị!')
      return '/'
    }
  }

  // 3. KHU VỰC BẾP (Cho phép Bếp + Quản Lý + Admin)
  if (to.path.startsWith('/kitchen')) {
    if (!token || (!userRoles.includes('ROLE_KITCHEN') && !userRoles.includes('ROLE_ADMIN') && !userRoles.includes('ROLE_MANAGER'))) {
      alert('Khu vực hạn chế: Chỉ dành cho bộ phận Bếp và Ban Quản Lý!')
      return '/'
    }
  }

  // 4. KHU VỰC PHỤC VỤ (Cho phép Phục vụ + Quản Lý + Admin)
  if (to.path.startsWith('/waiter')) {
    if (!token || (!userRoles.includes('ROLE_WAITER') && !userRoles.includes('ROLE_ADMIN') && !userRoles.includes('ROLE_MANAGER'))) {
      alert('Khu vực hạn chế: Chỉ dành cho bộ phận Phục vụ và Ban Quản Lý!')
      return '/'
    }
  }

  // Nếu qua được hết các trạm kiểm soát thì cho phép đi tiếp
  return true
})

export default router