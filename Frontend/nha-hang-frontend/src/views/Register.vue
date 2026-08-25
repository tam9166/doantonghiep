<template>
  <div class="register-page">
    <router-link to="/" class="g-back-btn-floating">
      <span>←</span> Về Trang Chủ
    </router-link>
    
    <!-- Left Panel -->
    <div class="register-visual">
      <div class="visual-overlay"></div>
      <div class="visual-content">
        <div class="visual-badge">✦ GIA NHẬP MỘC VỊ</div>
        <h1 class="text-gradient">Tạo tài khoản<br><span>miễn phí</span></h1>
        <p>Đăng ký để đặt bàn, gọi món, tích điểm và nhận nhiều ưu đãi hấp dẫn.</p>
        
        <div class="benefits-list">
          <div class="benefit-item">
            <span class="benefit-icon"><UiIcon name="sparkles" /></span>
            <div>
              <strong>Tích điểm VIP</strong>
              <p>Mỗi đơn hàng đều được tích điểm đổi quà</p>
            </div>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon"><UiIcon name="note" /></span>
            <div>
              <strong>Voucher độc quyền</strong>
              <p>Nhận mã giảm giá dành riêng cho thành viên</p>
            </div>
          </div>
          <div class="benefit-item">
            <span class="benefit-icon"><UiIcon name="clock" /></span>
            <div>
              <strong>Đặt món nhanh</strong>
              <p>Lưu lịch sử, đặt lại chỉ 1 click</p>
            </div>
          </div>
        </div>
      </div>
      
      <div class="deco-circle c1"></div>
      <div class="deco-circle c2"></div>
    </div>

    <!-- Right Panel - Form -->
    <div class="register-form-panel">
      <div class="form-wrapper">
        <!-- Mobile Brand -->
        <div class="mobile-brand">
          <span></span>
          <h2>MỘC VỊ</h2>
        </div>

        <!-- Progress Steps -->
        <div class="steps-bar">
          <div class="step" :class="{ active: step >= 1, done: step > 1 }">
            <div class="step-circle">{{ step > 1 ? '✓' : '1' }}</div>
            <span>Thông tin</span>
          </div>
          <div class="step-line" :class="{ active: step > 1 }"></div>
          <div class="step" :class="{ active: step >= 2 }">
            <div class="step-circle">2</div>
            <span>Xác nhận</span>
          </div>
        </div>

        <!-- Step 1: Information -->
        <Transition name="step" mode="out-in">
          <div v-if="step === 1" key="step1" class="step-content">
            <div class="form-header">
              <h2>Thông tin tài khoản</h2>
              <p>Điền thông tin cơ bản để tạo tài khoản</p>
            </div>

            <div class="form-grid">
              <div class="input-group">
                <label>Tên đăng nhập *</label>
                <div class="input-field">
                  <span class="field-icon"><UiIcon name="user" /></span>
                  <input v-model="form.username" type="text" required minlength="4" maxlength="50" pattern="[a-zA-Z0-9._-]+" autocomplete="username" placeholder="username..." />
                </div>
              </div>
              <div class="input-group">
                <label>Họ và Tên *</label>
                <div class="input-field">
                  <span class="field-icon"><UiIcon name="profile" /></span>
                  <input v-model="form.fullname" type="text" required maxlength="100" autocomplete="name" placeholder="Nguyễn Văn A..." />
                </div>
              </div>
            </div>

            <div class="input-group">
              <label>Email *</label>
              <div class="input-field">
                <span class="field-icon"><UiIcon name="mail" /></span>
                <input v-model="form.email" type="email" required maxlength="100" autocomplete="email" placeholder="email@gmail.com" />
              </div>
            </div>

            <div class="input-group">
              <label>Mật khẩu *</label>
              <div class="input-field">
                <span class="field-icon"><UiIcon name="settings" /></span>
                <input v-model="form.password" :type="showPw ? 'text' : 'password'" required minlength="10" maxlength="72" autocomplete="new-password" placeholder="Từ 10 đến 72 ký tự..." />
                <button class="toggle-pw" @click="showPw = !showPw" type="button" :aria-label="showPw ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"><UiIcon name="eye" /></button>
              </div>
              <!-- Password Strength -->
              <div class="pw-strength" v-if="form.password">
                <div class="pw-bar">
                  <div class="pw-fill" :class="pwStrengthClass" :style="{ width: pwStrengthPercent + '%' }"></div>
                </div>
                <span class="pw-label" :class="pwStrengthClass">{{ pwStrengthLabel }}</span>
              </div>
            </div>

            <button @click="goStep2" class="btn-next">Tiếp tục →</button>
          </div>

          <!-- Step 2: Confirm -->
          <div v-else-if="step === 2" key="step2" class="step-content">
            <div class="form-header">
              <h2>Xác nhận thông tin</h2>
              <p>Kiểm tra lại thông tin trước khi đăng ký</p>
            </div>

            <div class="confirm-card">
              <div class="confirm-row">
                <span class="confirm-label"> Username</span>
                <span class="confirm-value">{{ form.username }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label"> Họ và Tên</span>
                <span class="confirm-value">{{ form.fullname }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label"> Email</span>
                <span class="confirm-value">{{ form.email }}</span>
              </div>
              <div class="confirm-row">
                <span class="confirm-label"> Mật khẩu</span>
                <span class="confirm-value">••••••••</span>
              </div>
            </div>

            <label class="terms-check">
              <input type="checkbox" v-model="termsAccepted" />
              <span class="checkmark"></span>
              Tôi đồng ý với <a href="#">Điều khoản sử dụng</a> và <a href="#">Chính sách bảo mật</a>
            </label>

            <div class="btn-row">
              <button @click="step = 1" class="btn-back">← Quay lại</button>
              <button @click="handleRegister" class="btn-register" :disabled="isLoading || !termsAccepted">
                <span v-if="!isLoading"> Đăng Ký</span>
                <span v-else class="btn-loading"><span class="spinner"></span> Đang xử lý...</span>
              </button>
            </div>
          </div>
        </Transition>

        <!-- Error -->
        <Transition name="shake">
          <div v-if="errorMsg" class="error-alert">
            <span></span>
            <p>{{ errorMsg }}</p>
          </div>
        </Transition>

        <!-- Footer -->
        <div class="form-footer">
          <p>Đã có tài khoản? <router-link to="/login">Đăng nhập →</router-link></p>
          <router-link to="/" class="back-home">← Về trang chủ</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/services/api'
import { getApiErrorMessage } from '@/services/errorMessage'
import { useToast } from '@/composables/useToast'
import { getRegistrationValidationError, normalizeRegistration } from '@/utils/registrationValidation'

const router = useRouter()
const toast = useToast()
const form = ref({ username: '', password: '', fullname: '', email: '' })
const isLoading = ref(false)
const errorMsg = ref('')
const step = ref(1)
const showPw = ref(false)
const termsAccepted = ref(false)

// Password strength
const pwStrength = computed(() => {
  const pw = form.value.password
  if (!pw) return 0
  let score = 0
  if (pw.length >= 10) score++
  if (pw.length >= 12) score++
  if (/[A-Z]/.test(pw)) score++
  if (/[0-9]/.test(pw)) score++
  if (/[^A-Za-z0-9]/.test(pw)) score++
  return score
})
const pwStrengthPercent = computed(() => (pwStrength.value / 5) * 100)
const pwStrengthClass = computed(() => {
  if (pwStrength.value <= 1) return 'pw-weak'
  if (pwStrength.value <= 3) return 'pw-medium'
  return 'pw-strong'
})
const pwStrengthLabel = computed(() => {
  if (pwStrength.value <= 1) return 'Yếu'
  if (pwStrength.value <= 3) return 'Trung bình'
  return 'Mạnh'
})

function goStep2() {
  errorMsg.value = ''
  const validationError = getRegistrationValidationError(form.value)
  if (validationError) {
    errorMsg.value = validationError
    return
  }
  form.value = normalizeRegistration(form.value)
  step.value = 2
}

async function handleRegister() {
  errorMsg.value = ''
  if (!termsAccepted.value) {
    errorMsg.value = 'Bạn cần đồng ý với Điều khoản sử dụng và Chính sách bảo mật.'
    return
  }
  const validationError = getRegistrationValidationError(form.value)
  if (validationError) {
    errorMsg.value = validationError
    step.value = 1
    return
  }
  form.value = normalizeRegistration(form.value)
  isLoading.value = true
  try {
    await api.post('/api/auth/signup', { ...form.value, termsAccepted: true })
    toast.success('Tài khoản đã được tạo. Hãy đăng nhập!', 'Đăng ký thành công')
    router.push('/login')
  } catch (error) {
    errorMsg.value = getApiErrorMessage(error, 'Đăng ký thất bại! Vui lòng thử lại.')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  background: var(--bg-root);
}

/* ===== LEFT PANEL ===== */
.register-visual {
  flex: 1;
  position: relative;
  background-image: url('https://images.unsplash.com/photo-1414235077428-338989a2e8c0?q=80&w=2000&auto=format&fit=crop');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  overflow: hidden;
}
.visual-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(135deg, rgba(20,28,15,0.88) 0%, color-mix(in srgb, var(--color-on-secondary-container) 40%, transparent) 50%, rgba(20,28,15,0.92) 100%);
}
.visual-content { position: relative; z-index: 1; padding: 60px; max-width: 520px; }
.visual-badge {
  display: inline-block; background: color-mix(in srgb, var(--secondary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent); color: var(--secondary);
  padding: 8px 20px; border-radius: 100px; font-size: 0.75rem;
  font-weight: 700; letter-spacing: 2px; margin-bottom: 32px;
}
.visual-content h1 {
  font-family: var(--font-display); font-size: 3rem; font-weight: 800;
  color: #FFFFFF; line-height: 1.15; margin: 0 0 20px 0;
}
.visual-content h1 span { color: var(--secondary); }
.visual-content > p {
  font-size: 1rem; color: rgba(255,255,255,0.6); line-height: 1.7; margin: 0 0 40px 0;
}

.benefits-list { display: flex; flex-direction: column; gap: 16px; }
.benefit-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px; background: rgba(255,255,255,0.05);
  backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 14px; transition: var(--transition);
}
.benefit-item:hover { background: color-mix(in srgb, var(--secondary) 8%, transparent); border-color: color-mix(in srgb, var(--secondary) 20%, transparent); }
.benefit-icon { font-size: 1.6rem; flex-shrink: 0; }
.benefit-item strong { display: block; color: #FFFFFF; font-size: 0.88rem; }
.benefit-item p { margin: 0; color: rgba(255,255,255,0.5); font-size: 0.78rem; }

.deco-circle { position: absolute; border-radius: 50%; border: 1px solid color-mix(in srgb, var(--secondary) 12%, transparent); }
.c1 { width: 350px; height: 350px; top: -80px; right: -100px; }
.c2 { width: 200px; height: 200px; bottom: -40px; left: -60px; background: color-mix(in srgb, var(--secondary) 4%, transparent); }

/* ===== RIGHT PANEL ===== */
.register-form-panel {
  width: 560px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  padding: 40px; background: var(--bg-root); position: relative;
}
.register-form-panel::before {
  content: ''; position: absolute; top: 0; left: 0; bottom: 0; width: 1px;
  background: linear-gradient(180deg, transparent, color-mix(in srgb, var(--secondary) 30%, transparent), transparent);
}
.form-wrapper { width: 100%; max-width: 440px; }

.mobile-brand { display: none; align-items: center; gap: 10px; margin-bottom: 28px; }
.mobile-brand span { font-size: 2rem; }
.mobile-brand h2 { margin: 0; font-weight: 900; color: var(--primary); letter-spacing: 2px; }

/* Steps Bar */
.steps-bar {
  display: flex; align-items: center; justify-content: center;
  gap: 0; margin-bottom: 36px;
}
.step {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.step-circle {
  width: 36px; height: 36px; border-radius: 50%;
  background: rgba(255,255,255,0.05); border: 2px solid rgba(255,255,255,0.1);
  display: flex; align-items: center; justify-content: center;
  font-size: 0.85rem; font-weight: 800; color: var(--text-muted);
  transition: var(--transition);
}
.step.active .step-circle { background: color-mix(in srgb, var(--secondary) 15%, transparent); border-color: var(--primary); color: var(--primary); }
.step.done .step-circle { background: var(--primary); border-color: var(--primary); color: var(--bg-dark); }
.step span { font-size: 0.75rem; color: var(--text-muted); font-weight: 600; }
.step.active span { color: var(--primary); }
.step-line {
  width: 60px; height: 2px; background: rgba(255,255,255,0.08);
  margin: 0 12px; margin-bottom: 20px; transition: background 0.3s;
}
.step-line.active { background: var(--primary); }

/* Form */
.form-header { margin-bottom: 28px; }
.form-header h2 { font-size: 1.6rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.form-header p { color: var(--text-muted); font-size: 0.9rem; margin: 0; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }

.input-group { margin-bottom: 18px; }
.input-group label { display: block; font-size: 0.82rem; font-weight: 700; color: var(--text-secondary); margin-bottom: 7px; }
.input-field { position: relative; display: flex; align-items: center; }
.field-icon { position: absolute; left: 16px; font-size: 1rem; z-index: 1; pointer-events: none; }
.input-field input {
  width: 100%; padding: 14px 48px 14px 48px;
  background: var(--overlay-dark); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 14px; color: var(--text-primary); font-size: 0.95rem;
  font-family: inherit; transition: var(--transition);
}
.input-field input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px color-mix(in srgb, var(--secondary) 15%, transparent); }
.input-field input::placeholder { color: var(--text-muted); }
.toggle-pw { position: absolute; right: 14px; background: none; border: none; cursor: pointer; font-size: 1rem; }

/* Password Strength */
.pw-strength { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.pw-bar { flex: 1; height: 4px; background: rgba(255,255,255,0.06); border-radius: 4px; overflow: hidden; }
.pw-fill { height: 100%; border-radius: 4px; transition: width 0.4s ease, background 0.4s ease; }
.pw-weak .pw-fill, .pw-weak { color: var(--primary); }
.pw-weak .pw-fill { background: var(--primary); }
.pw-medium .pw-fill, .pw-medium { color: var(--color-tertiary); }
.pw-medium .pw-fill { background: var(--color-tertiary); }
.pw-strong .pw-fill, .pw-strong { color: var(--primary); }
.pw-strong .pw-fill { background: var(--primary); }
.pw-label { font-size: 0.75rem; font-weight: 700; }

/* Confirm Card */
.confirm-card {
  background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06);
  border-radius: 16px; padding: 20px; margin-bottom: 24px;
}
.confirm-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 0; border-bottom: 1px solid rgba(255,255,255,0.04);
}
.confirm-row:last-child { border-bottom: none; }
.confirm-label { color: var(--text-muted); font-size: 0.85rem; }
.confirm-value { color: var(--text-heading); font-weight: 700; font-size: 0.9rem; }

/* Terms */
.terms-check {
  display: flex; align-items: flex-start; gap: 10px;
  color: var(--text-muted); font-size: 0.82rem; cursor: pointer;
  user-select: none; margin-bottom: 24px; line-height: 1.5;
}
.terms-check input { position: absolute; width: 1px; height: 1px; opacity: 0; }
.terms-check .checkmark {
  width: 18px; height: 18px; border: 2px solid var(--primary);
  border-radius: 5px; flex-shrink: 0; margin-top: 2px;
  background: var(--bg-input); transition: var(--transition); position: relative;
}
.terms-check input:focus-visible + .checkmark { outline: 3px solid color-mix(in srgb, var(--primary) 30%, transparent); outline-offset: 2px; }
.terms-check input:checked + .checkmark { background: var(--primary); border-color: var(--primary); }
.terms-check input:checked + .checkmark::after {
  content: '✓'; position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  color: var(--bg-dark); font-size: 0.7rem; font-weight: 900;
}
.terms-check a { color: var(--primary); text-decoration: none; }
.terms-check a:hover { text-decoration: underline; }

/* Buttons */
.btn-next, .btn-register {
  width: 100%; padding: 16px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; border-radius: 14px;
  font-size: 1rem; font-weight: 800; font-family: inherit;
  cursor: pointer; transition: var(--transition); position: relative; overflow: hidden;
}
.btn-next::after, .btn-register::after {
  content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s ease;
}
.btn-next:hover::after, .btn-register:hover::after { left: 100%; }
.btn-next:hover, .btn-register:hover { transform: translateY(-2px); box-shadow: 0 8px 30px color-mix(in srgb, var(--secondary) 40%, transparent); }
.btn-register:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.btn-register:disabled::after { display: none; }

.btn-row { display: flex; gap: 12px; }
.btn-back {
  flex: 0 0 auto; padding: 16px 24px;
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); border-radius: 14px;
  font-weight: 700; font-family: inherit; cursor: pointer; transition: var(--transition);
}
.btn-back:hover { border-color: var(--primary); color: var(--primary); }
.btn-row .btn-register { flex: 1; }

.btn-loading { display: flex; align-items: center; justify-content: center; gap: 8px; }
.spinner {
  width: 18px; height: 18px; border: 2px solid rgba(39, 23, 23, 0.3);
  border-top-color: var(--bg-dark); border-radius: 50%; animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Error */
.error-alert {
  display: flex; align-items: flex-start; gap: 10px;
  margin-top: 16px; padding: 14px 16px;
  background: color-mix(in srgb, var(--primary) 10%, transparent); border: 1px solid color-mix(in srgb, var(--primary) 25%, transparent);
  border-radius: 12px;
}
.error-alert span { font-size: 1.1rem; flex-shrink: 0; }
.error-alert p { margin: 0; color: var(--primary); font-size: 0.88rem; }
.shake-enter-active { animation: shakeX 0.4s ease; }
@keyframes shakeX {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-8px); }
  75% { transform: translateX(8px); }
}

/* Step Transition */
.step-enter-active { animation: stepIn 0.35s ease-out; }
.step-leave-active { animation: stepOut 0.2s ease-in; }
@keyframes stepIn { from { opacity: 0; transform: translateX(20px); } to { opacity: 1; transform: translateX(0); } }
@keyframes stepOut { from { opacity: 1; transform: translateX(0); } to { opacity: 0; transform: translateX(-20px); } }

/* Footer */
.form-footer { margin-top: 28px; text-align: center; }
.form-footer p { color: var(--text-muted); font-size: 0.88rem; margin: 0 0 8px 0; }
.form-footer a { color: var(--primary); text-decoration: none; font-weight: 700; }
.form-footer a:hover { text-decoration: underline; }
.back-home { font-size: 0.85rem; color: var(--text-muted) !important; font-weight: 500 !important; }

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .register-visual { display: none; }
  .register-form-panel { width: 100%; }
  .register-form-panel::before { display: none; }
  .mobile-brand { display: flex; }
}
@media (max-width: 640px) {
  .register-page,
  .register-page * { box-sizing: border-box; }
  .register-page { overflow-x: hidden; }
  .form-grid { grid-template-columns: 1fr; }
  .register-form-panel {
    min-height: 100vh;
    align-items: flex-start;
    padding: 20px 16px;
    overflow-y: auto;
  }
  .form-wrapper { max-width: 100%; }
  .mobile-brand { margin: 68px 0 20px; }
  .steps-bar { margin-bottom: 28px; }
  .step-line { width: 32px; margin-right: 6px; margin-left: 6px; }
  .input-field input { min-height: 48px; }
  .toggle-pw { width: 44px; height: 44px; right: 4px; }
  .confirm-row { align-items: flex-start; flex-wrap: wrap; gap: 6px 12px; }
  .confirm-value { max-width: 100%; overflow-wrap: anywhere; }
  .terms-check { min-height: 44px; }
  .btn-next,
  .btn-register,
  .btn-back { min-height: 48px; }
  .btn-row { flex-direction: column; }
  .btn-back { width: 100%; }
}
</style>
