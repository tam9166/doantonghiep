<template>
  <div class="admin-layout">
    <!-- Sidebar -->
    <aside class="admin-sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <router-link to="/admin" class="sidebar-brand">
          <span class="sidebar-brand-icon">🍽️</span>
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

      <nav class="sidebar-nav">
        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">TỔNG QUAN</p>
          <router-link to="/admin/analytics" class="nav-item" active-class="active">
            <span class="nav-icon">📊</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Thống kê</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">QUẢN LÝ</p>
          <router-link to="/admin" class="nav-item" exact-active-class="active">
            <span class="nav-icon">🍔</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Sản phẩm</span>
          </router-link>
          <router-link to="/admin/categories" class="nav-item" active-class="active">
            <span class="nav-icon">📂</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Danh mục</span>
          </router-link>
          <router-link to="/admin/orders" class="nav-item" active-class="active">
            <span class="nav-icon">📋</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Đơn hàng</span>
          </router-link>
          <router-link to="/admin/tables" class="nav-item" active-class="active">
            <span class="nav-icon">🪑</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Sơ đồ bàn</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">KHO & NGUYÊN LIỆU</p>
          <router-link to="/admin/ingredients" class="nav-item" active-class="active">
            <span class="nav-icon">🧅</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Nguyên liệu</span>
          </router-link>
          <router-link to="/admin/popular-items" class="nav-item" active-class="active">
            <span class="nav-icon">🔥</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Món hay dùng</span>
          </router-link>
          <router-link to="/admin/purchase-suggestions" class="nav-item" active-class="active">
            <span class="nav-icon">🛒</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Đề xuất mua hàng</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">NHÂN SỰ & MARKETING</p>
          <router-link to="/admin/staff" class="nav-item" active-class="active">
            <span class="nav-icon">👥</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Nhân viên</span>
          </router-link>
          <router-link to="/admin/posts" class="nav-item" active-class="active">
            <span class="nav-icon">📰</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Tin tức</span>
          </router-link>
          <router-link to="/admin/vouchers" class="nav-item" active-class="active">
            <span class="nav-icon">🎟️</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Voucher</span>
          </router-link>
        </div>

        <div class="nav-section">
          <p class="nav-section-title" v-if="!sidebarCollapsed">HỆ THỐNG</p>
          <router-link to="/admin/activity-log" class="nav-item" active-class="active">
            <span class="nav-icon">📋</span>
            <span class="nav-label" v-if="!sidebarCollapsed">Nhật ký thao tác</span>
          </router-link>
        </div>

      </nav>

      <!-- Sidebar Footer -->
      <div class="sidebar-footer">
        <router-link to="/" class="nav-item">
          <span class="nav-icon">🏠</span>
          <span class="nav-label" v-if="!sidebarCollapsed">Về Trang Chủ</span>
        </router-link>
        <button @click="handleLogout" class="nav-item nav-item-danger">
          <span class="nav-icon">🚪</span>
          <span class="nav-label" v-if="!sidebarCollapsed">Đăng xuất</span>
        </button>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="admin-main" :class="{ 'main-expanded': sidebarCollapsed }">
      <!-- Top Bar -->
      <header class="admin-topbar">
        <div class="topbar-left">
          <h2 class="page-heading">
            <slot name="title">Quản Trị</slot>
          </h2>
        </div>
        <div class="topbar-right">
          <!-- 🔔 Notification Bell -->
          <div class="notif-wrapper" @click="toggleNotifPanel">
            <div class="notif-bell" :class="{ 'bell-swing': bellAnimating }">
              🔔
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
                  ✅ Không có thông báo mới
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

    <!-- Global Toast -->
    <ToastGlobal />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '@/services/api'
import ToastGlobal from '@/components/ToastGlobal.vue'
import axios from 'axios'

const router = useRouter()
const sidebarCollapsed = ref(false)
const user = ref(null)

// === NOTIFICATIONS ===
const notifications = ref([])
const unreadCount = ref(0)
const showNotifPanel = ref(false)
const bellAnimating = ref(false)
let notifInterval = null

const getToken = () => localStorage.getItem('token')
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } })

const fetchNotifications = async () => {
  try {
    const [notifRes, countRes] = await Promise.all([
      axios.get('http://localhost:8080/api/admin/notifications', configHeader()),
      axios.get('http://localhost:8080/api/admin/notifications/unread-count', configHeader())
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
    await axios.post('http://localhost:8080/api/admin/notifications/check-alerts', {}, configHeader())
    await fetchNotifications()
  } catch (err) { /* silent */ }
}

const toggleNotifPanel = () => {
  showNotifPanel.value = !showNotifPanel.value
}

const readNotification = async (n) => {
  if (!n.isRead) {
    try {
      await axios.put(`http://localhost:8080/api/admin/notifications/${n.id}/read`, {}, configHeader())
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
    await axios.put('http://localhost:8080/api/admin/notifications/read-all', {}, configHeader())
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

function handleLogout() {
  if (confirm('Bạn có chắc muốn đăng xuất?')) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/staff-login')
  }
}

onMounted(() => {
  user.value = getCurrentUser()
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
})

onUnmounted(() => {
  if (notifInterval) clearInterval(notifInterval)
  document.removeEventListener('click', handleClickOutside)
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
  background: rgba(8, 16, 31, 0.95);
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
  filter: drop-shadow(0 0 8px rgba(0, 212, 170, 0.4));
}
.sidebar-brand-text h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 900;
  color: var(--primary);
  letter-spacing: 1px;
  white-space: nowrap;
}
.sidebar-brand-text p {
  margin: 0;
  font-size: 0.6rem;
  color: var(--text-muted);
  letter-spacing: 2px;
  white-space: nowrap;
}

.sidebar-toggle {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--text-muted);
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
  background: rgba(0, 212, 170, 0.1);
  border-color: rgba(0, 212, 170, 0.3);
  color: var(--primary);
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
  color: var(--text-muted);
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
  color: var(--text-secondary);
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
  background: rgba(0, 212, 170, 0.06);
  color: var(--text-primary);
}
.nav-item.active {
  background: rgba(0, 212, 170, 0.12);
  color: var(--primary);
  font-weight: 700;
}
.nav-item.active .nav-icon {
  filter: drop-shadow(0 0 6px rgba(0, 212, 170, 0.4));
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
.nav-item-danger { color: #e74c3c; }
.nav-item-danger:hover { background: rgba(231, 76, 60, 0.1); }

/* Sidebar Footer */
.sidebar-footer {
  padding: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* ===== MAIN ===== */
.admin-main {
  flex: 1;
  margin-left: 260px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  transition: margin-left 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
.admin-main.main-expanded {
  margin-left: 72px;
}

/* Topbar */
.admin-topbar {
  background: rgba(8, 16, 31, 0.7);
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
}
@media (max-width: 640px) {
  .admin-sidebar { display: none; }
  .admin-main { margin-left: 0; }
  .admin-content { padding: 16px; }
  .admin-topbar { padding: 12px 16px; }
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
  background: #e74c3c;
  color: white;
  font-size: 0.6rem;
  font-weight: 900;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid rgba(8, 16, 31, 0.95);
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
  0% { box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.6); }
  70% { box-shadow: 0 0 0 8px rgba(231, 76, 60, 0); }
  100% { box-shadow: 0 0 0 0 rgba(231, 76, 60, 0); }
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
  background: rgba(12, 20, 38, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255,255,255,0.08);
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
  color: var(--primary, #00d4aa);
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
.notif-item:hover { background: rgba(0, 212, 170, 0.05); }
.notif-item.unread { background: rgba(0, 212, 170, 0.03); border-left: 3px solid var(--primary, #00d4aa); }

.notif-icon-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}
.severity-critical { background: #e74c3c; box-shadow: 0 0 8px rgba(231, 76, 60, 0.5); }
.severity-warning { background: #f1c40f; box-shadow: 0 0 8px rgba(241, 196, 15, 0.5); }
.severity-info { background: #3498db; }

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

</style>
