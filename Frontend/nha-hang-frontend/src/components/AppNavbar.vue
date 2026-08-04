<template>
  <header class="app-navbar" :class="{ 'navbar-scrolled': isScrolled, 'navbar-transparent': transparent && !isScrolled }">
    <div class="navbar-inner">
      <!-- Logo -->
      <router-link to="/" class="navbar-brand">
        <span class="brand-icon">🍽️</span>
        <div class="brand-text">
          <h2>NHÀ HÀNG <span>MỘC VỊ</span></h2>
          <p>ĐÀ NẴNG</p>
        </div>
      </router-link>

      <!-- Desktop Navigation -->
      <nav class="navbar-nav" :class="{ 'nav-open': mobileMenuOpen }">
        <router-link to="/" exact-active-class="active">{{ $t('nav.home') }}</router-link>
        <router-link to="/menu" active-class="active">{{ $t('nav.menu') }}</router-link>
        <router-link to="/reservation" active-class="active">{{ $t('nav.booking') }}</router-link>
        <router-link to="/reservation-lookup" active-class="active">{{ $t('nav.lookup') }}</router-link>
        <router-link to="/dine-in" active-class="active">{{ $t('nav.dine_in') }}</router-link>
      </nav>

      <!-- Right Actions -->
      <div class="navbar-actions">
        <!-- Language Switch -->
        <select v-model="currentLang" @change="changeLanguage" class="lang-select">
          <option value="vi">🇻🇳 VN</option>
          <option value="en">🇺🇸 EN</option>
        </select>

        <template v-if="!isLoggedIn">
          <router-link to="/login" class="nav-btn">{{ $t('nav.login') }}</router-link>
          <router-link to="/register" class="nav-btn nav-btn-primary">{{ $t('nav.register') }}</router-link>
        </template>

        <template v-else>
          <router-link to="/history" class="nav-btn">📜 {{ $t('nav.history') }}</router-link>
          <router-link to="/profile" class="nav-btn">👤 {{ $t('nav.profile') }}</router-link>
          
          <router-link
            v-if="hasRole('ROLE_ADMIN') || hasRole('ROLE_MANAGER')"
            to="/admin"
            class="nav-btn nav-btn-admin"
          >⚙️ {{ $t('nav.admin') }}</router-link>

          <router-link
            v-if="hasRole('ROLE_KITCHEN')"
            to="/kitchen"
            class="nav-btn nav-btn-kitchen"
          >👨‍🍳 {{ $t('nav.kitchen') }}</router-link>

          <router-link
            v-if="hasRole('ROLE_WAITER')"
            to="/waiter"
            class="nav-btn nav-btn-waiter"
          >🏃 {{ $t('nav.waiter') }}</router-link>

          <router-link
            v-if="hasRole('ROLE_CASHIER')"
            to="/cashier"
            class="nav-btn nav-btn-cashier"
          >💰 {{ $t('nav.cashier') }}</router-link>

          <button @click="handleLogout" class="nav-btn nav-btn-logout">{{ $t('nav.logout') }}</button>
        </template>

        <!-- Mobile hamburger -->
        <button class="hamburger" @click="mobileMenuOpen = !mobileMenuOpen" :class="{ 'is-active': mobileMenuOpen }">
          <span></span><span></span><span></span>
        </button>
      </div>
    </div>

    <!-- Mobile Menu Overlay -->
    <Transition name="mobile-menu">
      <div v-if="mobileMenuOpen" class="mobile-overlay" @click="mobileMenuOpen = false">
        <nav class="mobile-nav" @click.stop>
          <router-link to="/" @click="mobileMenuOpen = false">🏠 {{ $t('nav.home') }}</router-link>
          <router-link to="/menu" @click="mobileMenuOpen = false">📖 {{ $t('nav.menu') }}</router-link>
          <router-link to="/reservation" @click="mobileMenuOpen = false">📅 {{ $t('nav.booking') }}</router-link>
          <router-link to="/reservation-lookup" @click="mobileMenuOpen = false">{{ $t('nav.lookup') }}</router-link>
          <router-link to="/dine-in" @click="mobileMenuOpen = false">🍽️ {{ $t('nav.dine_in') }}</router-link>
          <div class="mobile-divider"></div>
          <template v-if="!isLoggedIn">
            <router-link to="/login" @click="mobileMenuOpen = false">🔑 {{ $t('nav.login') }}</router-link>
            <router-link to="/register" @click="mobileMenuOpen = false">📝 {{ $t('nav.register') }}</router-link>
          </template>
          <template v-else>
            <router-link to="/history" @click="mobileMenuOpen = false">📜 {{ $t('nav.history') }}</router-link>
            <router-link to="/profile" @click="mobileMenuOpen = false">👤 {{ $t('nav.profile') }}</router-link>
            <router-link v-if="hasRole('ROLE_ADMIN') || hasRole('ROLE_MANAGER')" to="/admin" @click="mobileMenuOpen = false">⚙️ {{ $t('nav.admin') }}</router-link>
            <a href="#" @click.prevent="handleLogout" class="mobile-logout">🚪 {{ $t('nav.logout') }}</a>
          </template>
        </nav>
      </div>
    </Transition>
  </header>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getCurrentUser, hasRole as checkRole, isAuthenticated } from '@/services/api'

defineProps({
  transparent: { type: Boolean, default: false }
})

const router = useRouter()
const { locale, t } = useI18n()
const currentLang = computed({
  get: () => locale.value,
  set: value => { locale.value = value }
})
const isLoggedIn = ref(false)
const user = ref(null)
const isScrolled = ref(false)
const mobileMenuOpen = ref(false)

function hasRole(role) {
  return checkRole(role)
}

function changeLanguage() {
  locale.value = currentLang.value
}

function handleLogout() {
  if (confirm(t('auth.logoutConfirm'))) {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    isLoggedIn.value = false
    user.value = null
    mobileMenuOpen.value = false
    router.push('/')
  }
}

function onScroll() {
  isScrolled.value = window.scrollY > 50
}

onMounted(() => {
  if (isAuthenticated()) {
    isLoggedIn.value = true
    user.value = getCurrentUser()
  }
  window.addEventListener('scroll', onScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.app-navbar {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 1000;
  padding: 14px 32px;
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.navbar-scrolled {
  background: rgba(26, 23, 15, 0.92);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.4);
  padding: 8px 32px;
}

.navbar-transparent {
  background: rgba(26, 23, 15, 0.72);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.app-navbar:not(.navbar-transparent):not(.navbar-scrolled) {
  background: rgba(26, 23, 15, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.navbar-inner {
  max-width: 1440px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

/* Brand */
.navbar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-icon {
  font-size: 1.8rem;
  filter: drop-shadow(0 0 12px rgba(90, 110, 69, 0.4));
  transition: var(--transition);
}
.navbar-brand:hover .brand-icon {
  transform: rotate(-10deg) scale(1.1);
}
.brand-text h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 900;
  color: #fffaf0;
  letter-spacing: 1px;
  line-height: 1.2;
}
.brand-text h2 span { color: #f5d37a; }
.brand-text p {
  margin: 0;
  font-size: 0.6rem;
  color: rgba(255, 250, 240, 0.78);
  letter-spacing: 4px;
  font-weight: 700;
  text-transform: uppercase;
}

/* Nav Links */
.navbar-nav {
  display: flex;
  gap: 4px;
}
.navbar-nav a {
  text-decoration: none;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
  font-size: 0.9rem;
  padding: 10px 18px;
  border-radius: 100px;
  transition: var(--transition);
  white-space: nowrap;
}
.navbar-nav a:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}
.navbar-nav a.active {
  color: #fff;
  background: rgba(90, 110, 69, 0.42);
}

/* Actions */
.navbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.lang-select {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 6px 10px;
  border-radius: 20px;
  cursor: pointer;
  outline: none;
  font-family: inherit;
  font-weight: 600;
  font-size: 0.8rem;
  transition: var(--transition);
}
.lang-select:hover { border-color: var(--primary); }
.lang-select option { background: var(--bg-dark); }

.nav-btn {
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 7px 14px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.82rem;
  font-weight: 600;
  font-family: inherit;
  transition: var(--transition);
  text-decoration: none;
  white-space: nowrap;
}
.nav-btn:hover {
  border-color: var(--primary);
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
}
.nav-btn-primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  border-color: var(--primary);
  font-weight: 700;
}
.nav-btn-primary:hover {
  color: var(--bg-dark);
  box-shadow: 0 4px 15px rgba(90, 110, 69, 0.4);
  transform: translateY(-1px);
}
.nav-btn-admin { border-color: rgba(185, 130, 41, 0.3); color: #B98229; }
.nav-btn-admin:hover { background: rgba(185, 130, 41, 0.1); color: #B98229; border-color: rgba(185, 130, 41, 0.5); }
.nav-btn-kitchen { border-color: rgba(90, 110, 69, 0.3); color: var(--primary); }
.nav-btn-waiter { border-color: rgba(90, 110, 69, 0.3); color: #5A6E45; }
.nav-btn-waiter:hover { background: rgba(90, 110, 69, 0.1); color: #5A6E45; border-color: rgba(90, 110, 69, 0.5); }
.nav-btn-cashier { border-color: rgba(192, 138, 46, 0.3); color: #C08A2E; }
.nav-btn-cashier:hover { background: rgba(192, 138, 46, 0.1); color: #C08A2E; border-color: rgba(192, 138, 46, 0.5); }
.nav-btn-logout { border-color: rgba(178, 59, 46, 0.45); color: #fff; }
.nav-btn-logout:hover { background: rgba(178, 59, 46, 0.16); color: #fff; border-color: rgba(178, 59, 46, 0.75); }

/* Hamburger */
.hamburger {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  flex-direction: column;
  gap: 5px;
}
.hamburger span {
  display: block;
  width: 24px;
  height: 2px;
  background: #FFFFFF;
  border-radius: 2px;
  transition: all 0.3s ease;
}
.hamburger.is-active span:nth-child(1) { transform: rotate(45deg) translate(5px, 5px); }
.hamburger.is-active span:nth-child(2) { opacity: 0; }
.hamburger.is-active span:nth-child(3) { transform: rotate(-45deg) translate(5px, -5px); }

/* Mobile Overlay */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(26, 23, 15, 0.9);
  backdrop-filter: blur(20px);
  z-index: 999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 76px 16px 20px;
  overflow-y: auto;
}
.mobile-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 90%;
  max-width: 360px;
}
.mobile-nav a {
  display: block;
  color: #FFFFFF;
  text-decoration: none;
  font-size: 1.2rem;
  font-weight: 700;
  padding: 16px 24px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: var(--transition);
}
.mobile-nav a:hover, .mobile-nav a.router-link-active {
  background: rgba(90, 110, 69, 0.1);
  border-color: rgba(90, 110, 69, 0.3);
  color: var(--primary);
}
.mobile-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.06);
  margin: 8px 0;
}
.mobile-logout {
  color: #B23B2E !important;
}

/* Customer-facing GustoPro navigation. */
.app-navbar,
.navbar-transparent,
.app-navbar:not(.navbar-transparent):not(.navbar-scrolled),
.navbar-scrolled {
  padding: 8px 32px;
  background: rgba(255, 248, 247, 0.96);
  border-bottom: 1px solid var(--color-outline-variant);
  box-shadow: 0 2px 12px rgba(39, 23, 23, 0.06);
}
.navbar-inner { max-width: 1240px; }
.brand-icon { filter: none; font-size: 1.55rem; }
.brand-text h2 { color: var(--color-on-surface); font-family: var(--font-display); font-size: 0.95rem; letter-spacing: 0; }
.brand-text h2 span { color: var(--primary); }
.brand-text p { color: var(--text-muted); letter-spacing: 0.18em; }
.navbar-nav a { color: var(--text-secondary); border-radius: 0; font-size: 0.8rem; padding: 10px 13px; }
.navbar-nav a:hover { color: var(--primary); background: transparent; }
.navbar-nav a.active { color: var(--primary); background: transparent; box-shadow: inset 0 -2px 0 var(--primary); }
.lang-select, .nav-btn { background: var(--bg-card); color: var(--text-secondary); border-color: var(--color-outline-variant); border-radius: 999px; }
.lang-select option { background: var(--bg-card); color: var(--text-primary); }
.nav-btn:hover, .lang-select:hover { color: var(--primary); border-color: var(--primary); background: var(--color-surface-container-low); }
.nav-btn-primary { background: var(--primary); color: var(--color-on-primary); border-color: var(--primary); }
.nav-btn-primary:hover { background: var(--primary-dark); color: var(--color-on-primary); box-shadow: none; transform: none; }
.hamburger span { background: var(--text-primary); }
.mobile-overlay { background: rgba(62, 44, 43, 0.38); backdrop-filter: blur(6px); }
.mobile-nav { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 12px; }
.mobile-nav a { background: transparent; border-color: transparent; color: var(--text-primary); }
.mobile-nav a:hover, .mobile-nav a.router-link-active { background: var(--color-surface-container); border-color: var(--color-outline-variant); color: var(--primary); }

/* Transition */
.mobile-menu-enter-active { animation: fadeIn 0.3s ease; }
.mobile-menu-leave-active { animation: fadeIn 0.3s ease reverse; }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

/* Responsive */
@media (max-width: 1024px) {
  .navbar-nav { display: none; }
  .navbar-actions .nav-btn { display: none; }
  .navbar-actions .lang-select { display: none; }
  .hamburger { display: flex; }
}

@media (max-width: 640px) {
  .app-navbar { padding: 10px 16px; }
  .brand-text h2 { font-size: 1rem; }
  .brand-text p { font-size: 0.55rem; letter-spacing: 2px; }
  .hamburger { width: 44px; height: 44px; align-items: center; justify-content: center; }
  .mobile-overlay { align-items: flex-start; }
  .mobile-nav { width: 100%; }
  .mobile-nav a { min-height: 48px; padding: 13px 18px; font-size: 1.05rem; }
  .navbar-inner { gap: 10px; }
  .brand-icon { font-size: 1.35rem; }
  .brand-text h2 { max-width: 164px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .app-navbar, .navbar-transparent, .navbar-scrolled { padding-right: 12px; padding-left: 12px; }
}
</style>
