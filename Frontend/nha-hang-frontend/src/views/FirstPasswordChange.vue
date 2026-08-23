<template>
  <main class="password-page">
    <section class="password-panel" aria-labelledby="password-title">
      <p class="eyebrow">Bảo mật tài khoản</p>
      <h1 id="password-title">Đổi mật khẩu lần đầu</h1>
      <p class="description">Tài khoản chỉ được sử dụng sau khi mật khẩu tạm thời được thay đổi.</p>

      <form @submit.prevent="submitPasswordChange">
        <label for="old-password">Mật khẩu hiện tại</label>
        <input
          id="old-password"
          v-model="form.oldPassword"
          type="password"
          autocomplete="current-password"
          required
        />

        <label for="new-password">Mật khẩu mới</label>
        <input
          id="new-password"
          v-model="form.newPassword"
          type="password"
          autocomplete="new-password"
          minlength="10"
          maxlength="72"
          required
        />

        <label for="confirm-password">Xác nhận mật khẩu mới</label>
        <input
          id="confirm-password"
          v-model="form.confirmPassword"
          type="password"
          autocomplete="new-password"
          minlength="10"
          maxlength="72"
          required
        />

        <p v-if="errorMessage" class="error-message" role="alert">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">
          {{ submitting ? 'Đang cập nhật...' : 'Đổi mật khẩu' }}
        </button>
      </form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import api from '@/services/api'
import {
  AUTH_CONTEXT,
  clearCustomerSession,
  clearStaffSession,
  getActiveAuthContext,
  getCustomerUser,
  getStaffUser
} from '@/services/session'

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const submitting = ref(false)
const errorMessage = ref('')

const isStaffSession = () => getActiveAuthContext() === AUTH_CONTEXT.STAFF

const loginPath = () => {
  try {
    const roles = (isStaffSession() ? getStaffUser() : getCustomerUser())?.roles || []
    return roles.some(role => ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_KITCHEN', 'ROLE_WAITER', 'ROLE_CASHIER'].includes(role))
      ? '/staff-login'
      : '/login'
  } catch {
    return '/login'
  }
}

const submitPasswordChange = async () => {
  errorMessage.value = ''
  if (form.newPassword.length < 10) {
    errorMessage.value = 'Mật khẩu mới phải có ít nhất 10 ký tự.'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = 'Xác nhận mật khẩu mới không khớp.'
    return
  }

  submitting.value = true
  try {
    await api.put('/api/auth/password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    const destination = loginPath()
    if (isStaffSession()) {
      clearStaffSession()
    } else {
      clearCustomerSession()
    }
    window.location.href = destination
  } catch (error) {
    const payload = error.response?.data
    errorMessage.value = typeof payload === 'string'
      ? payload
      : payload?.message || 'Không thể đổi mật khẩu. Vui lòng kiểm tra lại mật khẩu hiện tại.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.password-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--bg-root);
  color: var(--text-primary);
}

.password-panel {
  width: min(100%, 460px);
  padding: 32px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow-lg);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--secondary);
  font-weight: 700;
}

h1 {
  margin: 0 0 10px;
  font-size: 28px;
  letter-spacing: 0;
}

.description {
  margin: 0 0 24px;
  color: var(--text-secondary);
  line-height: 1.6;
}

form {
  display: grid;
  gap: 10px;
}

label {
  margin-top: 6px;
  font-weight: 700;
}

input {
  min-height: 46px;
  padding: 10px 12px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-root);
  color: var(--text-primary);
  font: inherit;
}

input:focus {
  border-color: var(--primary);
  outline: 3px solid color-mix(in srgb, var(--primary) 20%, transparent);
}

button {
  min-height: 46px;
  margin-top: 12px;
  border: 0;
  border-radius: 6px;
  background: var(--primary);
  color: #fff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.error-message {
  margin: 6px 0 0;
  color: var(--danger, #a32020);
  line-height: 1.5;
}

@media (max-width: 640px) {
  .password-page {
    padding: 16px;
  }

  .password-panel {
    padding: 24px 18px;
  }

  h1 {
    font-size: 24px;
  }
}
</style>
