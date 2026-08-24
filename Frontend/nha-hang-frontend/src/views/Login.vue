<template>
  <div class="login-page">
    <router-link to="/" class="g-back-btn-floating">
      <span>←</span> Về Trang Chủ
    </router-link>
    
    <!-- Left Panel - Visual -->
    <div class="login-visual">
      <div class="visual-overlay"></div>
      <div class="visual-content">
        <div class="visual-badge">✦ NHÀ HÀNG MỘC VỊ — ĐÀ NẴNG</div>
        <h1 class="text-gradient">Chào mừng<br>trở lại<span>.</span></h1>
        <p>Đăng nhập để trải nghiệm ẩm thực tuyệt vời và quản lý đơn hàng của bạn.</p>
        
        <!-- Floating cards -->
        <div class="floating-cards">
          <div class="float-card fc-1">
            <span></span>
            <div>
              <strong>100+ Món Ăn</strong>
              <p>Đa dạng ẩm thực 3 miền</p>
            </div>
          </div>
          <div class="float-card fc-2">
            <span></span>
            <div>
              <strong>4.9/5 Đánh Giá</strong>
              <p>Từ 2000+ khách hàng</p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Decorative elements -->
      <div class="deco-circle c1"></div>
      <div class="deco-circle c2"></div>
      <div class="deco-circle c3"></div>
    </div>

    <!-- Right Panel - Form -->
    <div class="login-form-panel">
      <div class="form-wrapper">
        <!-- Mobile Brand -->
        <div class="mobile-brand">
          <span></span>
          <h2>MỘC VỊ</h2>
        </div>

        <!-- Form Header -->
        <div class="form-header">
          <h2>Đăng Nhập</h2>
          <p>Vui lòng đăng nhập để tiếp tục sử dụng dịch vụ</p>
        </div>

        <!-- Login Form -->
        <div class="form-body">
          <div class="input-group">
            <label>Tên đăng nhập</label>
            <div class="input-field">
              <span class="field-icon"><UiIcon name="user" /></span>
              <input
                v-model="form.username"
                type="text"
                placeholder="Nhập username..."
                @keyup.enter="handleLogin"
                autocomplete="username"
              />
            </div>
          </div>

          <div class="input-group">
            <label>Mật khẩu</label>
            <div class="input-field">
              <span class="field-icon"><UiIcon name="settings" /></span>
              <input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Nhập mật khẩu..."
                @keyup.enter="handleLogin"
                autocomplete="current-password"
              />
              <button class="toggle-pw" @click="showPassword = !showPassword" type="button">
                {{ showPassword ? '' : '' }}
              </button>
            </div>
          </div>

          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="rememberMe" />
              <span class="checkmark"></span>
              Ghi nhớ đăng nhập
            </label>
          </div>

          <!-- Login Button -->
          <button @click="handleLogin" class="btn-login" :disabled="isLoading">
            <span v-if="!isLoading">Đăng Nhập →</span>
            <span v-else class="btn-loading">
              <span class="spinner"></span>
              Đang xử lý...
            </span>
          </button>

          <!-- Error -->
          <Transition name="shake">
            <div v-if="errorMsg" class="error-alert">
              <span></span>
              <p>{{ errorMsg }}</p>
            </div>
          </Transition>
        </div>

        <!-- Form Footer -->
        <div class="form-footer">
          <p>
            Chưa có tài khoản?
            <router-link to="/register">Đăng ký ngay →</router-link>
          </p>
          <router-link to="/" class="back-home">← Về trang chủ</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import api from '@/services/api'
import { setCustomerSession } from '@/services/session'
import { getApiErrorMessage } from '@/services/errorMessage'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const form = ref({ username: '', password: '' })
const isLoading = ref(false)
const errorMsg = ref('')
const showPassword = ref(false)
const rememberMe = ref(false)

const handleLogin = async () => {
  if (isLoading.value) return
  errorMsg.value = ''
  if (!form.value.username || !form.value.password) {
    errorMsg.value = 'Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!'
    return
  }

  isLoading.value = true
  try {
    const res = await api.post('/api/auth/login', form.value)

    setCustomerSession(res.data.token, {
      username: res.data.username,
      roles: res.data.roles,
      mustChangePassword: res.data.mustChangePassword
    })

    toast.success(`Chào mừng ${res.data.username}!`, 'Đăng nhập thành công')

    // Khách hàng luôn redirect về trang chủ
    setTimeout(() => {
      window.location.href = res.data.mustChangePassword ? '/change-password' : '/'
    }, 800)

  } catch (error) {
    if (error.response && error.response.status === 403) {
      // Nhân viên nhầm trang đăng nhập
      errorMsg.value = getApiErrorMessage(error, 'Vui lòng sử dụng trang đăng nhập dành cho nhân viên.')
    } else if (error.response && error.response.status === 401) {
      errorMsg.value = 'Sai tài khoản hoặc mật khẩu!'
    } else if (error.response) {
      errorMsg.value = getApiErrorMessage(error, 'Đăng nhập không thành công.')
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
.login-page {
  min-height: 100vh;
  display: flex;
  background: var(--bg-root);
}

/* ===== LEFT PANEL ===== */
.login-visual {
  flex: 1;
  position: relative;
  background-image: url('https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=2000&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  overflow: hidden;
}
.visual-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(20, 28, 15, 0.85) 0%,
    color-mix(in srgb, var(--secondary) 40%, transparent) 50%,
    rgba(20, 28, 15, 0.9) 100%
  );
}
.visual-content {
  position: relative;
  z-index: 1;
  padding: 60px;
  max-width: 560px;
}
.visual-badge {
  display: inline-block;
  background: color-mix(in srgb, var(--secondary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--secondary) 30%, transparent);
  color: var(--secondary);
  padding: 8px 20px;
  border-radius: 100px;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: 32px;
}
.visual-content h1 {
  font-family: var(--font-display);
  font-size: 3.5rem;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1.15;
  margin: 0 0 20px 0;
}
.visual-content h1 span { color: var(--secondary); }
.visual-content > p {
  font-size: 1.05rem;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.7;
  margin: 0 0 40px 0;
}

/* Floating Cards */
.floating-cards { display: flex; flex-direction: column; gap: 16px; }
.float-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  max-width: 300px;
}
.float-card span { font-size: 1.8rem; flex-shrink: 0; }
.float-card strong { display: block; color: #FFFFFF; font-size: 0.9rem; }
.float-card p { margin: 0; color: rgba(255,255,255,0.5); font-size: 0.78rem; }
.fc-1 { animation: float 6s ease-in-out infinite; }
.fc-2 { animation: float 8s ease-in-out infinite 1s; margin-left: 40px; }

/* Decorative circles */
.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid color-mix(in srgb, var(--secondary) 15%, transparent);
}
.c1 { width: 400px; height: 400px; top: -100px; right: -100px; }
.c2 { width: 250px; height: 250px; bottom: 50px; left: -80px; }
.c3 { width: 150px; height: 150px; bottom: -30px; right: 100px; background: color-mix(in srgb, var(--secondary) 5%, transparent); }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

/* ===== RIGHT PANEL ===== */
.login-form-panel {
  width: 520px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: var(--bg-root);
  position: relative;
}
.login-form-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, transparent, color-mix(in srgb, var(--secondary) 30%, transparent), transparent);
}

.form-wrapper {
  width: 100%;
  max-width: 380px;
}

/* Mobile Brand */
.mobile-brand {
  display: none;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
}
.mobile-brand span { font-size: 2rem; }
.mobile-brand h2 { margin: 0; font-weight: 900; color: var(--primary); letter-spacing: 2px; }

/* Form Header */
.form-header {
  margin-bottom: 36px;
}
.form-header h2 {
  font-size: 2rem;
  font-weight: 900;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}
.form-header p {
  color: var(--text-muted);
  font-size: 0.92rem;
  margin: 0;
}

/* Input Groups */
.input-group {
  margin-bottom: 22px;
}
.input-group label {
  display: block;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: 8px;
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
  background: var(--overlay-dark);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  color: var(--text-primary);
  font-size: 0.95rem;
  font-family: inherit;
  transition: var(--transition);
}
.input-field input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--secondary) 15%, transparent);
  background: rgba(39, 23, 23, 0.7);
}
.input-field input::placeholder { color: var(--text-muted); }
.toggle-pw {
  position: absolute;
  right: 14px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 4px;
}

/* Form Options */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}
.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 0.85rem;
  cursor: pointer;
  user-select: none;
}
.remember-me input { display: none; }
.checkmark {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.15);
  border-radius: 5px;
  transition: var(--transition);
  position: relative;
}
.remember-me input:checked + .checkmark {
  background: var(--primary);
  border-color: var(--primary);
}
.remember-me input:checked + .checkmark::after {
  content: '✓';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--bg-dark);
  font-size: 0.7rem;
  font-weight: 900;
}

/* Login Button */
.btn-login {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  border: none;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 800;
  font-family: inherit;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  overflow: hidden;
}
.btn-login::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s ease;
}
.btn-login:hover::after { left: 100%; }
.btn-login:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px color-mix(in srgb, var(--secondary) 45%, transparent);
}
.btn-login:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.btn-login:disabled::after { display: none; }

.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(39, 23, 23, 0.3);
  border-top-color: var(--bg-dark);
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
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--primary) 25%, transparent);
  border-radius: 12px;
  animation: shakeX 0.4s ease;
}
.error-alert span { font-size: 1.1rem; flex-shrink: 0; }
.error-alert p { margin: 0; color: var(--primary); font-size: 0.88rem; font-weight: 500; line-height: 1.4; }

@keyframes shakeX {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}

.shake-enter-active { animation: shakeX 0.4s ease; }

/* Footer */
.form-footer {
  margin-top: 32px;
  text-align: center;
}
.form-footer p {
  color: var(--text-muted);
  font-size: 0.9rem;
  margin: 0 0 12px 0;
}
.form-footer a {
  color: var(--primary);
  text-decoration: none;
  font-weight: 700;
  transition: var(--transition);
}
.form-footer a:hover { color: var(--primary-dark); }
.back-home {
  font-size: 0.85rem;
  color: var(--text-muted) !important;
  font-weight: 500 !important;
}
.back-home:hover { color: var(--primary) !important; }

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .login-visual { display: none; }
  .login-form-panel {
    width: 100%;
    padding: 24px;
  }
  .login-form-panel::before { display: none; }
  .mobile-brand { display: flex; }
  .form-wrapper { max-width: 420px; }
}

@media (max-width: 640px) {
  .login-page,
  .login-page * { box-sizing: border-box; }
  .login-page { overflow-x: hidden; }
  .login-form-panel {
    min-height: 100vh;
    align-items: flex-start;
    padding: 24px 16px;
    overflow-y: auto;
  }
  .form-wrapper { max-width: 100%; }
  .mobile-brand { margin: 72px 0 24px; }
  .form-header { margin-bottom: 28px; }
  .form-header h2 { font-size: 1.6rem; }
  .input-field input { min-height: 48px; }
  .toggle-pw { width: 44px; height: 44px; right: 4px; }
  .form-options { flex-wrap: wrap; gap: 10px 16px; }
  .remember-me { min-height: 44px; }
  .btn-login { min-height: 48px; }
}
</style>
