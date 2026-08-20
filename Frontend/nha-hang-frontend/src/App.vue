<template>
  <div id="app">
    <div
      v-if="routeLoading"
      class="admin-route-skeleton"
      role="status"
      aria-live="polite"
      aria-label="Đang tải trang quản trị"
    >
      <aside class="admin-skeleton-sidebar">
        <div class="skeleton-logo"></div>
        <div v-for="item in 8" :key="item" class="skeleton-nav-line"></div>
      </aside>
      <main class="admin-skeleton-main">
        <div class="skeleton-toolbar">
          <div class="skeleton-title"></div>
          <div class="skeleton-action"></div>
        </div>
        <div class="skeleton-grid">
          <div v-for="card in 4" :key="card" class="skeleton-card"></div>
        </div>
        <div class="skeleton-table">
          <div v-for="row in 7" :key="row" class="skeleton-row"></div>
        </div>
      </main>
    </div>

    <router-view v-slot="{ Component }">
      <Transition name="page" mode="out-in">
        <component :is="Component" />
      </Transition>
    </router-view>
    
    <!-- Global Toast (luôn hiển thị) -->
    <ToastGlobal />
  </div>
</template>

<script setup>
import ToastGlobal from '@/components/ToastGlobal.vue'
import { routeLoading } from '@/router/loadingState'
</script>

<style>
#app {
  min-height: 100vh;
}

.admin-route-skeleton {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  background: #f6f1df;
  color: #1f2b1a;
}

.admin-skeleton-sidebar {
  background: #24331f;
  padding: 24px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: 12px 0 28px rgba(31, 43, 26, 0.14);
}

.admin-skeleton-main {
  padding: 28px;
  overflow: hidden;
}

.skeleton-logo,
.skeleton-nav-line,
.skeleton-title,
.skeleton-action,
.skeleton-card,
.skeleton-row {
  position: relative;
  overflow: hidden;
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(255,255,255,0.18), rgba(255,255,255,0.34), rgba(255,255,255,0.18));
  background-size: 220% 100%;
  animation: skeletonPulse 1.15s ease-in-out infinite;
}

.skeleton-logo {
  width: 78%;
  height: 42px;
  margin-bottom: 14px;
}

.skeleton-nav-line {
  height: 38px;
}

.skeleton-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

.skeleton-title {
  width: 320px;
  height: 42px;
  background-color: rgba(36, 51, 31, 0.12);
}

.skeleton-action {
  width: 160px;
  height: 42px;
  background-color: rgba(36, 51, 31, 0.12);
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.skeleton-card {
  height: 104px;
  background-color: rgba(36, 51, 31, 0.1);
}

.skeleton-table {
  padding: 18px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid color-mix(in srgb, var(--secondary) 18%, transparent);
}

.skeleton-row {
  height: 38px;
  margin-bottom: 12px;
  background-color: rgba(36, 51, 31, 0.1);
}

.skeleton-row:last-child {
  margin-bottom: 0;
}

@keyframes skeletonPulse {
  0% { background-position: 120% 0; }
  100% { background-position: -120% 0; }
}

@media (max-width: 1024px) {
  .admin-route-skeleton {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .admin-skeleton-main {
    padding: 20px;
  }

  .skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-route-skeleton {
    grid-template-columns: 1fr;
  }

  .admin-skeleton-sidebar {
    display: none;
  }

  .admin-skeleton-main {
    padding: 18px;
  }

  .skeleton-toolbar {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
  }

  .skeleton-title,
  .skeleton-action {
    max-width: 100%;
  }

  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}

/* Page Transitions */
.page-enter-active {
  animation: pageIn 0.35s ease-out;
}
.page-leave-active {
  animation: pageOut 0.2s ease-in;
}

@keyframes pageIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
@keyframes pageOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(-8px);
  }
}
</style>
