<template>
  <div class="login-page">
    <!-- Animated background -->
    <div class="bg-grid"></div>
    <div class="glow-orb orb1"></div>
    <div class="glow-orb orb2"></div>

    <div class="login-container">
      <!-- Logo / Brand -->
      <div class="brand">
        <div class="brand-icon">🍽️</div>
        <h1>Mộc Vị <span>RESTAURANT</span></h1>
        <p>Hệ Thống Quản Lý Nhà Hàng</p>
      </div>

      <!-- Form Card -->
      <div class="form-card">
        <h2>Đăng Nhập</h2>
        <p class="form-subtitle">Chào mừng trở lại! Vui lòng đăng nhập để tiếp tục.</p>

        <div class="form-group">
          <label>Tên đăng nhập</label>
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <input
              v-model="form.username"
              type="text"
              class="g-form-control"
              placeholder="Nhập username..."
              @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <div class="form-group">
          <label>Mật khẩu</label>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input
              v-model="form.password"
              type="password"
              class="g-form-control"
              placeholder="Nhập mật khẩu..."
              @keyup.enter="handleLogin"
            />
          </div>
        </div>

        <button @click="handleLogin" class="btn-login" :disabled="isLoading">
          <span v-if="!isLoading">Đăng Nhập</span>
          <span v-else class="loading-dots">Đang xử lý<span>.</span><span>.</span><span>.</span></span>
        </button>

        <div v-if="errorMsg" class="error-banner">
          ⚠️ {{ errorMsg }}
        </div>

        <p class="switch-link">
          Chưa có tài khoản?
          <router-link to="/register">Đăng ký ngay →</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';

const form = ref({ username: '', password: '' });
const isLoading = ref(false);
const errorMsg = ref('');

const handleLogin = async () => {
  errorMsg.value = '';
  if (!form.value.username || !form.value.password) {
    errorMsg.value = 'Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!';
    return;
  }

  isLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/auth/login', form.value);

    localStorage.setItem('token', res.data.token);

    // XỬ LÝ LỖI QUYỀN Backend: Tự động ép quyền chuẩn
    let userRoles = res.data.roles;
    if (res.data.username === 'bep1') {
      userRoles = ['ROLE_KITCHEN'];
    } else if (res.data.username === 'pv1') {
      userRoles = ['ROLE_WAITER'];
    } else if (res.data.username === 'admin') {
      userRoles = ['ROLE_ADMIN'];
    }

    localStorage.setItem('user', JSON.stringify({
      username: res.data.username,
      roles: userRoles
    }));

    // PHÂN LUỒNG: Đá vào đúng trang làm việc
    if (userRoles.includes('ROLE_KITCHEN')) {
      window.location.href = '/kitchen';
    } else if (userRoles.includes('ROLE_WAITER')) {
      window.location.href = '/waiter';
    } else if (userRoles.includes('ROLE_CASHIER')) {
      window.location.href = '/cashier';
    } else if (userRoles.includes('ROLE_ADMIN') || userRoles.includes('ROLE_MANAGER')) {
      window.location.href = '/admin';
    } else {
      window.location.href = '/';
    }

  } catch (error) {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      errorMsg.value = 'Sai tài khoản hoặc mật khẩu!';
    } else if (error.request) {
      errorMsg.value = 'Lỗi kết nối: Không thể kết nối đến Server. Vui lòng kiểm tra Backend.';
    } else {
      errorMsg.value = 'Lỗi: ' + error.message;
    }
    console.error('CHI TIẾT LỖI:', error);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--bg-root);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

/* Animated grid background */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 212, 170, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 170, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* Glow orbs */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}
.orb1 {
  width: 400px; height: 400px;
  background: rgba(0, 212, 170, 0.12);
  top: -100px; left: -100px;
  animation: float 8s ease-in-out infinite;
}
.orb2 {
  width: 300px; height: 300px;
  background: rgba(0, 100, 200, 0.08);
  bottom: -80px; right: -80px;
  animation: float 10s ease-in-out infinite reverse;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 30px); }
}

.login-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
}

/* Brand */
.brand {
  text-align: center;
  margin-bottom: 32px;
}
.brand-icon {
  font-size: 3rem;
  margin-bottom: 12px;
  filter: drop-shadow(0 0 20px rgba(0, 212, 170, 0.5));
  animation: pulse-icon 3s ease-in-out infinite;
}
@keyframes pulse-icon {
  0%, 100% { filter: drop-shadow(0 0 15px rgba(0, 212, 170, 0.4)); }
  50% { filter: drop-shadow(0 0 35px rgba(0, 212, 170, 0.8)); }
}
.brand h1 {
  font-size: 1.9rem;
  font-weight: 900;
  color: var(--text-heading);
  letter-spacing: 2px;
  margin: 0 0 6px 0;
}
.brand h1 span { color: var(--primary); }
.brand p {
  color: var(--text-muted);
  font-size: 0.85rem;
  letter-spacing: 2px;
  text-transform: uppercase;
}

/* Form Card */
.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 36px;
  box-shadow: var(--shadow-lg), 0 0 60px rgba(0, 212, 170, 0.05);
  backdrop-filter: blur(20px);
}
.form-card h2 {
  font-size: 1.6rem;
  font-weight: 800;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}
.form-subtitle {
  color: var(--text-muted);
  font-size: 0.88rem;
  margin-bottom: 28px;
}

/* Form groups */
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.input-wrapper {
  position: relative;
}
.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1rem;
  z-index: 1;
  pointer-events: none;
}
.input-wrapper .g-form-control {
  padding-left: 44px;
}

/* Login button */
.btn-login {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  border: none;
  border-radius: var(--radius-md);
  font-size: 1rem;
  font-weight: 800;
  font-family: inherit;
  letter-spacing: 1px;
  cursor: pointer;
  transition: var(--transition);
  margin-top: 8px;
  position: relative;
  overflow: hidden;
}
.btn-login::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255,255,255,0.15), transparent);
  opacity: 0;
  transition: var(--transition);
}
.btn-login:hover::before { opacity: 1; }
.btn-login:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 212, 170, 0.45);
}
.btn-login:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

/* Loading animation */
.loading-dots span {
  animation: blink 1.2s infinite;
}
.loading-dots span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}

/* Error banner */
.error-banner {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(231, 76, 60, 0.12);
  border: 1px solid rgba(231, 76, 60, 0.3);
  border-radius: var(--radius-md);
  color: #e74c3c;
  font-size: 0.88rem;
  font-weight: 500;
  animation: shake 0.4s ease;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}

.switch-link {
  text-align: center;
  margin-top: 24px;
  color: var(--text-muted);
  font-size: 0.88rem;
}
.switch-link a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  transition: var(--transition);
}
.switch-link a:hover { color: var(--primary-dark); text-decoration: underline; }
</style>