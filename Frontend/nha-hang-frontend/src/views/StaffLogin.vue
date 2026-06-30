<template>
  <div class="staff-login-page">
    <!-- Left Panel - System Visual -->
    <div class="staff-visual">
      <div class="visual-overlay"></div>
      <div class="visual-content">
        <div class="system-badge">🔐 HỆ THỐNG QUẢN TRỊ</div>
        <h1>Mộc Vị<br><span>Management</span></h1>
        <p>Đăng nhập vào hệ thống quản trị nhà hàng. Chỉ dành cho nhân sự được cấp quyền.</p>
        
        <!-- Role indicators -->
        <div class="role-list">
          <div class="role-item">
            <span class="role-icon">👨‍💼</span>
            <div>
              <strong>Admin / Manager</strong>
              <p>Quản lý toàn bộ hệ thống</p>
            </div>
          </div>
          <div class="role-item">
            <span class="role-icon">👨‍🍳</span>
            <div>
              <strong>Bếp</strong>
              <p>Quản lý đơn món & nguyên liệu</p>
            </div>
          </div>
          <div class="role-item">
            <span class="role-icon">🧑‍🍽️</span>
            <div>
              <strong>Phục vụ & Thu ngân</strong>
              <p>Phục vụ khách & thanh toán</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Decorative -->
      <div class="deco-hex hex-1"></div>
      <div class="deco-hex hex-2"></div>
      <div class="deco-line line-1"></div>
      <div class="deco-line line-2"></div>
    </div>

    <!-- Right Panel - Login Form -->
    <div class="staff-form-panel">
      <div class="form-wrapper">
        <!-- System Logo -->
        <div class="system-logo">
          <div class="logo-icon">
            <span>⚙️</span>
          </div>
          <div>
            <h2>MỘC VỊ POS</h2>
            <p class="logo-sub">Hệ thống quản trị nhà hàng</p>
          </div>
        </div>

        <!-- Form Header -->
        <div class="form-header">
          <h2>Đăng Nhập Nhân Viên</h2>
          <p>Sử dụng tài khoản được cấp bởi quản lý</p>
        </div>

        <!-- Login Form -->
        <div class="form-body">
          <div class="input-group">
            <label>Tài khoản nhân viên</label>
            <div class="input-field">
              <span class="field-icon">👤</span>
              <input
                v-model="form.username"
                type="text"
                placeholder="Nhập tài khoản..."
                @keyup.enter="handleStaffLogin"
                autocomplete="username"
              />
            </div>
          </div>

          <div class="input-group">
            <label>Mật khẩu</label>
            <div class="input-field">
              <span class="field-icon">🔒</span>
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Nhập mật khẩu..."
                @keyup.enter="handleStaffLogin"
                autocomplete="current-password"
              />
              <button class="toggle-pw" @click="showPassword = !showPassword" type="button">
                {{ showPassword ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>

          <!-- Login Button -->
          <button @click="handleStaffLogin" class="btn-staff-login" :disabled="isLoading">
            <span v-if="!isLoading">🔐 Đăng Nhập Hệ Thống</span>
            <span v-else class="btn-loading">
              <span class="spinner"></span>
              Đang xác thực...
            </span>
          </button>

          <!-- Error -->
          <Transition name="shake">
            <div v-if="errorMsg" class="error-alert">
              <span>⚠️</span>
              <p>{{ errorMsg }}</p>
            </div>
          </Transition>

          <!-- Info -->
          <div class="staff-info">
            <span>ℹ️</span>
            <p>Nếu chưa có tài khoản, vui lòng liên hệ quản lý để được cấp quyền truy cập.</p>
          </div>
        </div>

        <!-- Footer -->
        <div class="form-footer">
          <router-link to="/" class="back-home">← Về trang chủ nhà hàng</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()
const form = ref({ username: '', password: '' })
const isLoading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)

const handleStaffLogin = async () => {
  errorMsg.value = ''
  if (!form.value.username || !form.value.password) {
    errorMsg.value = 'Vui lòng nhập đầy đủ tài khoản và mật khẩu!'
    return
  }

  isLoading.value = true
  try {
    // Gọi endpoint riêng dành cho nhân sự
    const res = await api.post('/api/auth/staff/login', form.value)

    localStorage.setItem('token', res.data.token)
    localStorage.setItem('user', JSON.stringify({
      username: res.data.username,
      roles: res.data.roles,
      assignedArea: res.data.assignedArea,
      shift: res.data.shift
    }))

    toast.success(`Chào mừng ${res.data.username}!`, 'Đăng nhập hệ thống thành công')

    // Redirect theo vai trò nhân sự
    setTimeout(() => {
      const roles = res.data.roles
      if (roles.includes('ROLE_ADMIN') || roles.includes('ROLE_MANAGER')) {
        window.location.href = '/admin'
      } else if (roles.includes('ROLE_KITCHEN')) {
        window.location.href = '/kitchen'
      } else if (roles.includes('ROLE_WAITER')) {
        window.location.href = '/waiter'
      } else if (roles.includes('ROLE_CASHIER')) {
        window.location.href = '/cashier'
      } else {
        window.location.href = '/staff'
      }
    }, 800)

  } catch (error) {
    if (error.response && error.response.status === 403) {
      // Khách hàng nhầm trang đăng nhập
      errorMsg.value = error.response.data || 'Tài khoản không có quyền truy cập hệ thống quản trị.'
    } else if (error.response && error.response.status === 401) {
      errorMsg.value = 'Sai tài khoản hoặc mật khẩu!'
    } else if (error.request) {
      errorMsg.value = 'Không thể kết nối Server. Vui lòng kiểm tra Backend.'
    } else {
      errorMsg.value = 'Lỗi: ' + error.message
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.staff-login-page {
  min-height: 100vh;
  display: flex;
  background: #0a0e1a;
}

/* ===== LEFT PANEL ===== */
.staff-visual {
  flex: 1;
  position: relative;
  background: linear-gradient(145deg, #0a0e1a 0%, #111832 50%, #0d1225 100%);
  display: flex;
  align-items: center;
  overflow: hidden;
}
.visual-overlay {
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse at 20% 50%, rgba(99, 102, 241, 0.08) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(139, 92, 246, 0.06) 0%, transparent 50%);
}
.visual-content {
  position: relative;
  z-index: 1;
  padding: 60px;
  max-width: 540px;
}
.system-badge {
  display: inline-block;
  background: rgba(99, 102, 241, 0.12);
  border: 1px solid rgba(99, 102, 241, 0.25);
  color: #818cf8;
  padding: 8px 20px;
  border-radius: 100px;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 3px;
  margin-bottom: 32px;
  text-transform: uppercase;
}
.visual-content h1 {
  font-family: var(--font-display, 'Inter', sans-serif);
  font-size: 3.2rem;
  font-weight: 900;
  color: #fff;
  line-height: 1.15;
  margin: 0 0 20px 0;
}
.visual-content h1 span {
  background: linear-gradient(135deg, #818cf8, #a78bfa);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.visual-content > p {
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.45);
  line-height: 1.7;
  margin: 0 0 40px 0;
}

/* Role List */
.role-list { display: flex; flex-direction: column; gap: 12px; }
.role-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 14px;
  max-width: 320px;
  transition: all 0.3s ease;
}
.role-item:hover {
  background: rgba(99, 102, 241, 0.06);
  border-color: rgba(99, 102, 241, 0.2);
}
.role-icon { font-size: 1.6rem; flex-shrink: 0; }
.role-item strong { display: block; color: #e2e8f0; font-size: 0.88rem; }
.role-item p { margin: 0; color: rgba(255,255,255,0.35); font-size: 0.76rem; }

/* Decorative */
.deco-hex {
  position: absolute;
  width: 200px;
  height: 200px;
  border: 1px solid rgba(99, 102, 241, 0.08);
  border-radius: 20px;
  transform: rotate(45deg);
}
.hex-1 { top: -60px; right: -40px; animation: floatSlow 10s ease-in-out infinite; }
.hex-2 { bottom: -30px; left: -50px; width: 150px; height: 150px; animation: floatSlow 12s ease-in-out infinite 2s; }
.deco-line {
  position: absolute;
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(99, 102, 241, 0.15), transparent);
}
.line-1 { height: 300px; top: 20%; right: 15%; }
.line-2 { height: 200px; bottom: 10%; right: 30%; }

@keyframes floatSlow {
  0%, 100% { transform: rotate(45deg) translateY(0); }
  50% { transform: rotate(45deg) translateY(-15px); }
}

/* ===== RIGHT PANEL ===== */
.staff-form-panel {
  width: 520px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #0a0e1a;
  position: relative;
}
.staff-form-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(99, 102, 241, 0.25), transparent);
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
}

/* System Logo */
.system-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 40px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.logo-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(139, 92, 246, 0.15));
  border: 1px solid rgba(99, 102, 241, 0.25);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.4rem;
}
.system-logo h2 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 900;
  color: #818cf8;
  letter-spacing: 2px;
}
.logo-sub {
  margin: 2px 0 0 0;
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.35);
}

/* Form Header */
.form-header {
  margin-bottom: 32px;
}
.form-header h2 {
  font-size: 1.8rem;
  font-weight: 900;
  color: #e2e8f0;
  margin: 0 0 8px 0;
}
.form-header p {
  color: rgba(255, 255, 255, 0.4);
  font-size: 0.9rem;
  margin: 0;
}

/* Input Groups */
.input-group {
  margin-bottom: 22px;
}
.input-group label {
  display: block;
  font-size: 0.82rem;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.input-field {
  position: relative;
  display: flex;
  align-items: center;
}
.field-icon {
  position: absolute;
  left: 16px;
  font-size: 1rem;
  z-index: 1;
  pointer-events: none;
}
.input-field input {
  width: 100%;
  padding: 14px 48px 14px 48px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(99, 102, 241, 0.15);
  border-radius: 12px;
  color: #e2e8f0;
  font-size: 0.95rem;
  font-family: inherit;
  transition: all 0.3s ease;
}
.input-field input:focus {
  outline: none;
  border-color: #818cf8;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
  background: rgba(15, 23, 42, 1);
}
.input-field input::placeholder { color: rgba(255, 255, 255, 0.25); }
.toggle-pw {
  position: absolute;
  right: 14px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 4px;
}

/* Login Button */
.btn-staff-login {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 800;
  font-family: inherit;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  margin-top: 4px;
}
.btn-staff-login::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  transition: left 0.5s ease;
}
.btn-staff-login:hover::after { left: 100%; }
.btn-staff-login:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.4);
}
.btn-staff-login:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.btn-staff-login:disabled::after { display: none; }

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Error */
.error-alert {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 16px;
  padding: 14px 16px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 12px;
  animation: shakeX 0.4s ease;
}
.error-alert span { font-size: 1.1rem; flex-shrink: 0; }
.error-alert p { margin: 0; color: #f87171; font-size: 0.88rem; font-weight: 500; line-height: 1.4; }

@keyframes shakeX {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}
.shake-enter-active { animation: shakeX 0.4s ease; }

/* Staff Info */
.staff-info {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 20px;
  padding: 14px 16px;
  background: rgba(99, 102, 241, 0.06);
  border: 1px solid rgba(99, 102, 241, 0.12);
  border-radius: 12px;
}
.staff-info span { font-size: 1rem; flex-shrink: 0; }
.staff-info p { margin: 0; color: rgba(255,255,255,0.4); font-size: 0.82rem; line-height: 1.5; }

/* Footer */
.form-footer {
  margin-top: 32px;
  text-align: center;
}
.back-home {
  font-size: 0.85rem;
  color: rgba(255,255,255,0.35);
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
}
.back-home:hover { color: #818cf8; }

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .staff-visual { display: none; }
  .staff-form-panel {
    width: 100%;
    padding: 24px;
  }
  .staff-form-panel::before { display: none; }
  .form-wrapper { max-width: 420px; }
}

@media (max-width: 480px) {
  .form-header h2 { font-size: 1.4rem; }
  .staff-form-panel { padding: 20px; }
}
</style>
