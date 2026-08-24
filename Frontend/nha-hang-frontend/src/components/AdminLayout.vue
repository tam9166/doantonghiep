<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside
      id="admin-mobile-sidebar"
      class="admin-sidebar"
      :class="{ collapsed: sidebarCollapsed, 'mobile-open': mobileSidebarOpen }"
    >
      <div class="sidebar-header">
        <router-link to="/admin" class="sidebar-brand">
          <span class="sidebar-brand-icon"><UiIcon name="restaurant" /></span>
          <transition name="fade">
            <div v-if="!sidebarCollapsed" class="sidebar-brand-text">
              <h3>MỘC VỊ</h3>
              <p>ADMIN PANEL</p>
            </div>
          </transition>
        </router-link>
        <button class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? '→' : '←' }}
        </button>
      </div>
      <nav class="sidebar-nav" @click="handleSidebarNavigation">
        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">TỔNG QUAN</p>
          <router-link to="/admin/analytics" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="analytics" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Thống kê</span>
          </router-link>
          <router-link to="/admin/ai-knowledge" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="ai" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Tri thức AI</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">QUẢN LÝ</p>
          <router-link to="/admin" class="nav-item" exact-active-class="active">
            <span class="nav-icon"><AdminNavIcon name="product" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Sản phẩm</span>
          </router-link>
          <router-link to="/admin/categories" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="category" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Danh mục</span>
          </router-link>
          <router-link to="/admin/orders" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="orders" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Đơn hàng</span>
          </router-link>
          <router-link to="/admin/reservations" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="reservations" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Đặt bàn</span>
          </router-link>
          <router-link to="/admin/reservation-cancellations" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="cancellation" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Yêu cầu hủy</span>
          </router-link>
          <router-link to="/admin/reservation-reviews" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="reviews" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Đánh giá đặt bàn</span>
          </router-link>
          <router-link to="/admin/customer-history" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="customers" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Lịch sử khách</span>
          </router-link>
          <router-link to="/admin/deposit-policies" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="deposit" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Chính sách cọc</span>
          </router-link>
          <router-link to="/admin/tables" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="tables" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Sơ đồ bàn</span>
          </router-link>
          <router-link to="/admin/table-areas" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="areas" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Khu vực bàn</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">KHO & NGUYÊN LIỆU</p>
          <router-link to="/admin/ingredients" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="ingredients" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Nguyên liệu</span>
          </router-link>
          <router-link to="/admin/popular-items" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="popular" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Món hay dùng</span>
          </router-link>
          <router-link to="/admin/purchase-suggestions" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="purchase" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Đề xuất mua hàng</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">NHÂN SỰ & MARKETING</p>
          <router-link to="/admin/staff" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="staff" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Nhân viên</span>
          </router-link>
          <router-link to="/admin/posts" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="posts" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Tin tức</span>
          </router-link>
          <router-link to="/admin/vouchers" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="vouchers" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Voucher</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">HỆ THỐNG</p>
          <router-link to="/admin/activity-log" class="nav-item" active-class="active">
            <span class="nav-icon"><AdminNavIcon name="activity" /></span>
            <span class="nav-label" v-if="!sidebarCollapsed">Nhật ký thao tác</span>
          </router-link>
        </div>

      </nav>

      <!-- Sidebar Footer -->
      <div class="sidebar-footer" @click="handleSidebarNavigation">
        <router-link to="/staff/profile" class="nav-item">
          <span class="nav-icon"><AdminNavIcon name="profile" /></span>
          <span class="nav-label" v-if="!sidebarCollapsed">Hồ sơ cá nhân</span>
        </router-link>
        <router-link to="/" class="nav-item">
          <span class="nav-icon"><AdminNavIcon name="home" /></span>
          <span class="nav-label" v-if="!sidebarCollapsed">Về Trang Chủ</span>
        </router-link>
        <button @click="handleLogout" class="nav-item nav-item-danger">
          <span class="nav-icon"><AdminNavIcon name="logout" /></span>
          <span class="nav-label" v-if="!sidebarCollapsed">Đăng xuất</span>
        </button>
      </div>
    </aside>

    <button
      v-if="mobileSidebarOpen"
      type="button"
      class="mobile-sidebar-overlay"
      aria-label="Đóng menu quản trị"
      @click="closeMobileSidebar"
    ><UiIcon name="x" /></button>

    <!-- Main Content -->
    <div class="admin-main" :class="{ 'main-expanded': sidebarCollapsed }">
      <!-- Top Bar -->
      <header class="admin-topbar">
        <div class="topbar-left">
          <button
            type="button"
            class="mobile-menu-button"
            aria-label="Mở menu quản trị"
            aria-controls="admin-mobile-sidebar"
            :aria-expanded="mobileSidebarOpen"
            @click="openMobileSidebar"
          ><UiIcon name="menu" /></button>
          <h2 class="page-heading">
            <slot name="title">Quản Trị</slot>
          </h2>
          <form class="admin-module-search" role="search" @submit.prevent="goToSearchResult">
            <label class="sr-only" for="admin-module-search">Tìm chức năng quản trị</label>
            <span aria-hidden="true">⌕</span>
            <input id="admin-module-search" v-model.trim="moduleSearch" type="search" placeholder="Tìm đơn hàng, món ăn, khách hàng..." @keydown.esc="moduleSearch = ''" />
          </form>
        </div>
        <div class="topbar-right">
          <!--  Notification Bell -->
          <div class="notif-wrapper" @click="toggleNotifPanel">
            <div class="notif-bell" :class="{ 'bell-swing': bellAnimating }">
              <UiIcon name="bell" />
              <span v-if="unreadCount > 0" class="notif-badge ring-pulse">{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
            </div>

            <!-- Notification Dropdown -->
            <div v-if="showNotifPanel" class="notif-panel">
              <div class="notif-panel-header">
                <h4>Thông Báo</h4>
                <button v-if="unreadCount > 0" @click.stop="markAllRead" class="notif-mark-all">Đọc tất cả</button>
              </div>
              <div class="notif-list">
                <div v-for="n in notifications" :key="n.id"
                     class="notif-item" :class="{ unread: !n.isRead }"
                     @click.stop="readNotification(n)">
                  <div class="notif-icon-dot" :class="'severity-' + (n.severity || 'info')"></div>
                  <div class="notif-content">
                    <p class="notif-title">{{ n.title }}</p>
                    <p class="notif-msg">{{ n.message }}</p>
                    <span class="notif-time">{{ formatNotifTime(n.createdAt) }}</span>
                  </div>
                </div>
                <div v-if="notifications.length === 0" class="notif-empty">
                  <UiIcon name="check" /> Không có thông báo mới
                </div>
              </div>
            </div>
          </div>

          <div class="topbar-user" v-if="user">
            <span class="user-avatar">{{ user.username?.charAt(0)?.toUpperCase() }}</span>
            <div class="user-info">
              <p class="user-name">{{ user.username }}</p>
              <p class="user-role">{{ user.roles?.[0]?.replace('ROLE_', '') }}</p>
            </div>
          </div>
        </div>
      </header>

      <!-- Page Content -->
      <div class="admin-content">
        <slot />
      </div>
    </div>

    <StaffOperationsAssistant />
  </div>
</template>

<script setup>
import StaffOperationsAssistant from './StaffOperationsAssistant.vue'
import AdminNavIcon from './AdminNavIcon.vue'
import UiIcon from './UiIcon.vue'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { clearStaffSession, getStaffToken, getStaffUser } from '@/services/session'

const router = useRouter()
const sidebarCollapsed = ref(false)
const mobileSidebarOpen = ref(false)
const user = ref(null)
const moduleSearch = ref('')
const adminModules = [
  { keywords: 'sản phẩm món ăn menu', route: '/admin' },
  { keywords: 'danh mục category', route: '/admin/categories' },
  { keywords: 'đơn hàng order hóa đơn lịch sử khách đặt hàng', route: '/admin/orders' },
  { keywords: 'đặt bàn reservation booking khách hàng', route: '/admin/reservations' },
  { keywords: 'yêu cầu hủy đặt bàn hoàn cọc refund cancellation', route: '/admin/reservation-cancellations' },
  { keywords: 'lịch sử khách đặt bàn customer history', route: '/admin/customer-history' },
  { keywords: 'bàn sơ đồ table', route: '/admin/tables' },
  { keywords: 'khu vực bàn tầng phòng sảnh table area', route: '/admin/table-areas' },
  { keywords: 'nhân viên staff tài khoản nhân sự ca làm lương', route: '/admin/staff' },
  { keywords: 'nguyên liệu kho inventory tồn kho nhập hàng', route: '/admin/ingredients' },
  { keywords: 'món hay dùng món bán chạy popular items', route: '/admin/popular-items' },
  { keywords: 'đề xuất mua hàng nhập kho purchase suggestions', route: '/admin/purchase-suggestions' },
  { keywords: 'thống kê analytics doanh thu lợi nhuận', route: '/admin/analytics' }
]
const normalizeSearch = (value) => value
  .toLocaleLowerCase('vi-VN')
  .normalize('NFD')
  .replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd')
const matchingModule = computed(() => {
  const query = normalizeSearch(moduleSearch.value)
  return query ? adminModules.find(item => normalizeSearch(item.keywords).includes(query)) : null
})

// === NOTIFICATIONS ===
const notifications = ref([])
const unreadCount = ref(0)
const showNotifPanel = ref(false)
const bellAnimating = ref(false)
let notifInterval = null

const getToken = getStaffToken
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } })

const fetchNotifications = async () => {
  try {
    const [notifRes, countRes] = await Promise.all([
      api.get('/api/admin/notifications', configHeader()),
      api.get('/api/admin/notifications/unread-count', configHeader())
    ])
    notifications.value = notifRes.data.slice(0, 20)
    const newCount = countRes.data.count || 0
    if (newCount > unreadCount.value && unreadCount.value > 0) {
      bellAnimating.value = true
      setTimeout(() => bellAnimating.value = false, 1000)
    }
    unreadCount.value = newCount
  } catch (err) { /* silent */ }
}

const checkAlerts = async () => {
  try {
    await api.post('/api/admin/notifications/check-alerts', {}, configHeader())
    await fetchNotifications()
  } catch (err) { /* silent */ }
}

const toggleNotifPanel = () => {
  showNotifPanel.value = !showNotifPanel.value
}

const readNotification = async (n) => {
  if (!n.isRead) {
    try {
      await api.put(`/api/admin/notifications/${n.id}/read`, {}, configHeader())
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (err) { /* silent */ }
  }
  // Navigate based on type
  if (n.relatedEntity === 'ingredient' || n.type === 'LOW_STOCK') {
    router.push('/admin/ingredients')
  } else if (n.type === 'EXPIRING_BATCH') {
    router.push('/admin/ingredients')
  }
  showNotifPanel.value = false
}

const markAllRead = async () => {
  try {
    await api.put('/api/admin/notifications/read-all', {}, configHeader())
    notifications.value.forEach(n => n.isRead = true)
    unreadCount.value = 0
  } catch (err) { /* silent */ }
}

const formatNotifTime = (ts) => {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return 'Vừa xong'
  if (diff < 3600) return Math.floor(diff / 60) + ' phút trước'
  if (diff < 86400) return Math.floor(diff / 3600) + ' giờ trước'
  return d.toLocaleDateString('vi-VN')
}

// Close notification panel when clicking outside
const handleClickOutside = (e) => {
  if (showNotifPanel.value && !e.target.closest('.notif-wrapper')) {
    showNotifPanel.value = false
  }
}

const openMobileSidebar = () => {
  sidebarCollapsed.value = false
  mobileSidebarOpen.value = true
}

const closeMobileSidebar = () => {
  mobileSidebarOpen.value = false
}

const handleSidebarNavigation = (event) => {
  if (event.target.closest('a')) closeMobileSidebar()
}

const handleEscape = (event) => {
  if (event.key === 'Escape') closeMobileSidebar()
}

function goToSearchResult() {
  if (!matchingModule.value) return
  router.push(matchingModule.value.route)
  moduleSearch.value = ''
}

function handleLogout() {
  if (confirm('Bạn có chắc muốn đăng xuất?')) {
    clearStaffSession()
    router.push('/staff-login')
  }
}

onMounted(() => {
  user.value = getStaffUser()
  // Auto-collapse on small screens
  if (window.innerWidth < 1024) {
    sidebarCollapsed.value = true
  }
  // Fetch notifications
  if (getToken()) {
    checkAlerts()
    notifInterval = setInterval(checkAlerts, 30000) // Polling mỗi 30 giây
  }
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleEscape)
})

onUnmounted(() => {
  if (notifInterval) clearInterval(notifInterval)
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-root);
}

/* ===== SIDEBAR ===== */
.admin-sidebar {
  width: 260px;
  background: color-mix(in srgb, var(--secondary) 95%, transparent);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  transition: width 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow-y: auto;
  overflow-x: hidden;
}

/* GustoPro administration surface: low-contrast pink workspace, red navigation state. */
.admin-sidebar { width: 280px; background: var(--color-surface-container-high); border-right-color: var(--color-outline-variant); }
.admin-sidebar.collapsed { width: 72px; }
.sidebar-header, .sidebar-footer { border-color: var(--color-outline-variant); }
.sidebar-brand-text h3 { color: var(--primary); }
.sidebar-brand-text p, .nav-section-title { color: var(--text-muted); }
.sidebar-toggle { background: var(--bg-card); border-color: var(--border); color: var(--text-secondary); }
.sidebar-toggle:hover { background: var(--bg-hover); border-color: var(--primary); color: var(--primary); }
.nav-item { border-radius: var(--radius-md); color: var(--text-secondary); }
.nav-item:hover { background: var(--color-surface-container); color: var(--primary); }
.nav-item.active { position: relative; background: #f6cfd0; color: var(--primary); }
.nav-item.active::before { content: ''; position: absolute; left: -10px; top: 0; bottom: 0; width: 4px; background: var(--primary); }
.nav-item.active .nav-icon { filter: none; }
.nav-item-danger { color: var(--danger); }
.nav-item-danger:hover { background: var(--color-error-container); color: var(--danger); }
.admin-main { margin-left: 280px; }
.admin-main.main-expanded { margin-left: 72px; }
.admin-topbar { min-height: 72px; background: rgba(255, 248, 247, 0.94); border-bottom-color: var(--color-outline-variant); box-shadow: none; }
.page-heading { font-family: var(--font-display); }
.admin-module-search { position: relative; display: flex; align-items: center; width: min(34vw, 420px); }
.admin-module-search > span { position: absolute; left: 12px; color: var(--text-muted); font-size: 1.2rem; pointer-events: none; }
.admin-module-search input { width: 100%; min-height: 42px; padding: 0 14px 0 38px; border: 1px solid var(--color-outline-variant); border-radius: 999px; background: var(--color-surface-container-low); color: var(--text-primary); font: inherit; }
.admin-module-search input:focus { outline: 2px solid var(--primary-glow); border-color: var(--primary); }
.topbar-user { background: var(--color-surface-container-low); border-color: var(--color-outline-variant); }
.user-avatar { background: var(--primary); color: var(--color-on-primary); }
.admin-content { background: var(--bg-root); }
.admin-content :deep(h1),
.admin-content :deep(h2),
.admin-content :deep(h3),
.admin-content :deep(h4),
.admin-content :deep(th),
.admin-content :deep(td),
.admin-content :deep(label),
.admin-content :deep(.page-title),
.admin-content :deep(.page-subtitle),
.admin-content :deep(.toolbar p),
.admin-content :deep(.section-heading p),
.admin-content :deep(.form-group label),
.admin-content :deep(.form-card),
.admin-content :deep(.table-card),
.admin-content :deep(.floor-card) { color: var(--text-primary) !important; }
.admin-content :deep(input),
.admin-content :deep(select),
.admin-content :deep(textarea) { color: var(--text-primary) !important; }
.admin-content :deep(input::placeholder),
.admin-content :deep(textarea::placeholder) { color: #5b403f !important; opacity: 1; }
.admin-content :deep(.table-wrap td span),
.admin-content :deep(.table-wrap td small),
.admin-content :deep(.empty),
.admin-content :deep(.muted) { color: #3e2c2b !important; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.admin-sidebar.collapsed {
  width: 72px;
}

/* Sidebar Header */
.sidebar-header {
  padding: 20px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  min-height: 72px;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  overflow: hidden;
}
.sidebar-brand-icon {
  font-size: 1.6rem;
  flex-shrink: 0;
  filter: drop-shadow(0 0 8px color-mix(in srgb, var(--secondary) 40%, transparent));
}
.sidebar-brand-text h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: #F2C46D;
  letter-spacing: 1px;
  white-space: nowrap;
}
.sidebar-brand-text p {
  margin: 0;
  font-size: 0.6rem;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 2px;
  white-space: nowrap;
}

.sidebar-toggle {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #FFFFFF;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  transition: var(--transition);
  flex-shrink: 0;
}
.sidebar-toggle:hover {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.35);
  color: #FFFFFF;
}

/* Nav */
.sidebar-nav {
  flex: 1;
  padding: 16px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.nav-section {
  margin-bottom: 8px;
}
.nav-section-title {
  margin: 0 0 6px 8px;
  font-size: 0.65rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.64);
  letter-spacing: 2px;
  text-transform: uppercase;
  white-space: nowrap;
  overflow: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  color: rgba(255, 255, 255, 0.88);
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 600;
  transition: var(--transition);
  cursor: pointer;
  background: transparent;
  border: none;
  width: 100%;
  text-align: left;
  font-family: inherit;
  white-space: nowrap;
}
.nav-item:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #FFFFFF;
}
.nav-item.active {
  background: color-mix(in srgb, var(--color-on-secondary-container) 72%, transparent);
  color: #FFFFFF;
  font-weight: 700;
}
.nav-item.active .nav-icon {
  filter: drop-shadow(0 0 6px rgba(242, 196, 109, 0.55));
}
.nav-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
  width: 24px;
  text-align: center;
}
.nav-label {
  overflow: hidden;
  text-overflow: ellipsis;
}
.nav-item-danger { color: #FFD5D0; }
.nav-item-danger:hover { background: color-mix(in srgb, var(--primary) 42%, transparent); color: #FFFFFF; }

/* Sidebar Footer */
.sidebar-footer {
  padding: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* ===== MAIN ===== */
.admin-main {
  flex: 1;
  min-width: 0;
  margin-left: 260px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.admin-main.main-expanded {
  margin-left: 72px;
}

.mobile-sidebar-overlay,
.mobile-menu-button {
  display: none;
}

/* Topbar */
.admin-topbar {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 16px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 50;
}
.page-heading {
  margin: 0;
  font-size: 1.3rem;
  font-weight: 800;
  color: var(--text-heading);
}
.topbar-left { display: flex; align-items: center; gap: 12px; min-width: 0; }
.topbar-right { display: flex; align-items: center; gap: 16px; }
.topbar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 14px 6px 6px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 100px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}
.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  font-size: 0.9rem;
}
.user-info { display: flex; flex-direction: column; }
.user-name { margin: 0; font-size: 0.85rem; font-weight: 700; color: var(--text-heading); }
.user-role { margin: 0; font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }

/* Content */
.admin-content {
  flex: 1;
  padding: 28px 32px;
}

/* Transitions */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* Responsive */
@media (max-width: 1024px) {
  .admin-sidebar { width: 72px; }
  .admin-main { margin-left: 72px; }
  .nav-section-title, .nav-label, .sidebar-brand-text { display: none !important; }
  .admin-module-search { width: min(42vw, 360px); }
}
@media (max-width: 640px) {
  .admin-sidebar,
  .admin-sidebar.collapsed {
    display: flex;
    width: min(300px, 86vw);
    transform: translateX(-100%);
  box-shadow: 18px 0 40px rgba(39, 23, 23, 0.28);
    transition: transform 0.25s ease;
  }
  .admin-sidebar.mobile-open { transform: translateX(0); }
  .admin-sidebar .nav-section-title,
  .admin-sidebar .nav-label,
  .admin-sidebar .sidebar-brand-text { display: block !important; }
  .admin-sidebar .sidebar-toggle { display: none; }
  .mobile-sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 90;
    width: 100%;
    height: 100%;
    padding: 0;
    border: 0;
    border-radius: 0;
  background: var(--overlay-dark);
    cursor: pointer;
  }
  .mobile-menu-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    flex: 0 0 44px;
    padding: 0;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--bg-card);
    color: var(--text-heading);
    font-size: 1.35rem;
    cursor: pointer;
  }
  .admin-main { margin-left: 0; }
  .admin-content { padding: 16px; }
  .admin-topbar { padding: 12px 16px; }
  .admin-module-search { display: none; }
  .page-heading { overflow: hidden; font-size: 1.1rem; text-overflow: ellipsis; white-space: nowrap; }
  .topbar-right { gap: 8px; }
  .topbar-user { padding-right: 6px; }
  .user-info { display: none; }
  .admin-content :deep(.content-grid),
  .admin-content :deep(.stats-grid),
  .admin-content :deep(.stats-container) {
    grid-template-columns: minmax(0, 1fr) !important;
  }
  .admin-content :deep(.content-grid > *),
  .admin-content :deep(.stats-grid > *),
  .admin-content :deep(.stats-container > *),
  .admin-content :deep(.form-card),
  .admin-content :deep(.table-card),
  .admin-content :deep(.floor-card) {
    min-width: 0;
    max-width: 100%;
  }
  .admin-content :deep(.table-responsive),
  .admin-content :deep(.g-table-container),
  .admin-content :deep(.table-card),
  .admin-content :deep(.data-table) {
    max-width: 100%;
    overflow-x: auto;
  }
  .admin-content :deep(table) {
    display: block;
    max-width: 100%;
    overflow-x: auto;
  }
  .admin-content :deep(table th),
  .admin-content :deep(table td) {
    white-space: nowrap;
  }
  .admin-content :deep(.filter-tabs) {
    max-width: 100%;
    overflow-x: auto;
  }
  .admin-content :deep(.tabs) {
    max-width: 100%;
    overflow-x: auto;
  }
  .admin-content :deep(.tabs > *) { flex: 0 0 auto; }
  .admin-content :deep(.panel-header) {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }
  .admin-content :deep(.panel-header > div) {
    flex-wrap: wrap;
  }
  .admin-content :deep(.g-form-control) { max-width: 100%; }
  .admin-content :deep(.stats-row) {
    flex-wrap: wrap;
  }
  .admin-content :deep(.stats-row > *) {
    flex: 1 1 100%;
    min-width: 0;
  }
  .admin-content :deep(.tabs-header),
  .admin-content :deep(.legend-box),
  .admin-content :deep(.legend-box > div) {
    flex-wrap: wrap;
  }
  .admin-content :deep(.tabs-header > div) {
    width: 100%;
    margin-left: 0 !important;
    flex-wrap: wrap;
  }
  .admin-content :deep(.legend-box) {
    align-items: stretch !important;
    flex-direction: column;
  }
  .admin-content :deep(.g-card) {
    min-width: 0;
    max-width: 100%;
    overflow-x: auto;
  }
  .admin-content :deep(.data-table) {
    display: block !important;
    width: 100% !important;
    overflow-x: auto !important;
  }
  .admin-content :deep(.recipe-layout) {
    grid-template-columns: minmax(0, 1fr) !important;
    height: auto !important;
  }
}
/* === NOTIFICATION BELL === */
.notif-wrapper {
  position: relative;
  cursor: pointer;
}
.notif-bell {
  font-size: 1.4rem;
  padding: 6px;
  border-radius: 10px;
  transition: all 0.3s;
  position: relative;
}
.notif-bell:hover {
  background: rgba(255,255,255,0.05);
  transform: scale(1.1);
}
.notif-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  background: var(--primary);
  color: #FFFFFF;
  font-size: 0.6rem;
  font-weight: 900;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid color-mix(in srgb, var(--secondary) 95%, transparent);
}

/* Bell Animation */
@keyframes bellSwing {
  0% { transform: rotate(0deg); }
  10% { transform: rotate(15deg); }
  20% { transform: rotate(-13deg); }
  30% { transform: rotate(10deg); }
  40% { transform: rotate(-8deg); }
  50% { transform: rotate(5deg); }
  100% { transform: rotate(0deg); }
}
.bell-swing { animation: bellSwing 0.8s ease-in-out; transform-origin: top center; }

/* Ring Pulse */
@keyframes ringPulse {
  0% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--primary) 60%, transparent); }
  70% { box-shadow: 0 0 0 8px transparent; }
  100% { box-shadow: 0 0 0 0 transparent; }
}
.ring-pulse { animation: ringPulse 2s ease-in-out infinite; }

/* Notification Panel */
.notif-panel {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  width: 380px;
  max-height: 480px;
  background: rgba(247, 243, 230, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.5);
  overflow: hidden;
  z-index: 200;
}
.notif-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.notif-panel-header h4 { margin: 0; font-size: 1rem; font-weight: 800; color: var(--text-heading); }
.notif-mark-all {
  background: none;
  border: none;
  color: var(--primary, var(--secondary));
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
}
.notif-mark-all:hover { text-decoration: underline; }

.notif-list {
  max-height: 380px;
  overflow-y: auto;
}
.notif-item {
  display: flex;
  gap: 12px;
  padding: 14px 18px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid rgba(255,255,255,0.03);
}
.notif-item:hover { background: color-mix(in srgb, var(--secondary) 5%, transparent); }
.notif-item.unread { background: color-mix(in srgb, var(--secondary) 3%, transparent); border-left: 3px solid var(--primary, var(--secondary)); }

.notif-icon-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.severity-critical { background: var(--primary); box-shadow: 0 0 8px color-mix(in srgb, var(--primary) 50%, transparent); }
.severity-warning { background: var(--color-tertiary); box-shadow: 0 0 8px color-mix(in srgb, var(--color-tertiary) 50%, transparent); }
.severity-info { background: var(--secondary); }

.notif-content { flex: 1; min-width: 0; }
.notif-title { margin: 0; font-size: 0.85rem; font-weight: 700; color: var(--text-heading); line-height: 1.3; }
.notif-msg { margin: 4px 0 0; font-size: 0.78rem; color: var(--text-muted); line-height: 1.4; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.notif-time { font-size: 0.7rem; color: var(--text-muted); margin-top: 4px; display: block; }
.notif-empty { padding: 40px; text-align: center; color: var(--text-muted); font-size: 0.9rem; }

/* 3D Nav Item Hover */
.nav-item {
  transform-style: preserve-3d;
  perspective: 600px;
}
.nav-item:hover {
  transform: translateX(4px);
}
.nav-item.active {
  transform: translateX(2px);
}

/* Keep the pink administration sidebar readable in every navigation state. */
.admin-sidebar .sidebar-brand-text h3,
.admin-sidebar .sidebar-brand-text p,
.admin-sidebar .nav-section-title,
.admin-sidebar .sidebar-toggle {
  color: var(--text-primary);
}

.admin-sidebar .nav-item {
  color: var(--text-secondary);
}

.admin-sidebar .nav-icon {
  color: var(--text-muted);
}

.admin-sidebar .sidebar-toggle {
  background: var(--bg-card);
  border-color: var(--color-outline-variant);
}

.admin-sidebar .nav-item:hover {
  background: var(--color-surface-container);
  color: var(--primary);
}

.admin-sidebar .nav-item.active {
  background: var(--color-primary-fixed);
  color: var(--primary);
}

.admin-sidebar .nav-item:hover .nav-icon,
.admin-sidebar .nav-item.active .nav-icon {
  color: var(--primary);
}

.admin-sidebar .nav-item-danger {
  color: var(--danger);
}

.admin-layout :deep(button:focus-visible),
.admin-layout :deep(a:focus-visible),
.admin-layout :deep(input:focus-visible),
.admin-layout :deep(select:focus-visible),
.admin-layout :deep(textarea:focus-visible) {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

.admin-layout :deep(input:focus),
.admin-layout :deep(select:focus),
.admin-layout :deep(textarea:focus) {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-glow);
}

</style>
