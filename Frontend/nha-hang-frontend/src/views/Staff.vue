<template>
  <div class="staff-wrapper">
    <div class="staff-content">
      <div class="staff-icon">👥</div>
      <h1>Trang Nhân Viên</h1>
      <p>Trang này đang được phát triển. Vui lòng sử dụng trang Quản lý Nhân Sự.</p>
      <div class="staff-actions">
        <button @click="$router.push('/admin/staff')" class="btn-primary">
          📋 Quản Lý Nhân Sự
        </button>
        <button @click="$router.push('/')" class="btn-secondary">
          🏠 Về Trang Chủ
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<style scoped>
.staff-wrapper {
  background: var(--bg-root);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.staff-content {
  text-align: center;
  padding: 60px 40px;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  max-width: 500px;
  box-shadow: var(--shadow-lg);
}
.staff-icon { font-size: 4rem; margin-bottom: 20px; }
h1 { font-size: 2rem; color: var(--text-heading); margin: 0 0 12px 0; font-weight: 800; }
p { color: var(--text-muted); margin: 0 0 32px 0; }
.staff-actions { display: flex; gap: 14px; justify-content: center; flex-wrap: wrap; }
.btn-primary {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; padding: 13px 24px;
  border-radius: var(--radius-md); cursor: pointer;
  font-weight: 700; font-size: 0.95rem; font-family: inherit;
  transition: var(--transition);
}
.btn-primary:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }
.btn-secondary {
  background: transparent; border: 1px solid var(--border);
  color: var(--text-secondary); padding: 13px 24px;
  border-radius: var(--radius-md); cursor: pointer;
  font-weight: 600; font-size: 0.95rem; font-family: inherit;
  transition: var(--transition);
}
.btn-secondary:hover { border-color: var(--primary); color: var(--primary); }
</style>
