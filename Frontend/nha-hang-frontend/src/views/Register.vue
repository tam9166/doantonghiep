<template>
  <div class="register-page">
    <div class="bg-grid"></div>
    <div class="glow-orb orb1"></div>
    <div class="glow-orb orb2"></div>

    <div class="register-container">
      <div class="brand">
        <div class="brand-icon">🍽️</div>
        <h1>Mộc Vị <span>RESTAURANT</span></h1>
        <p>Tạo Tài Khoản Mới</p>
      </div>

      <div class="form-card">
        <h2>Đăng Ký</h2>
        <p class="form-subtitle">Điền thông tin để tạo tài khoản của bạn.</p>

        <div class="form-row">
          <div class="form-group">
            <label>Tên đăng nhập (*)</label>
            <div class="input-wrapper">
              <span class="input-icon">👤</span>
              <input v-model="form.username" type="text" class="g-form-control" placeholder="username..." />
            </div>
          </div>
          <div class="form-group">
            <label>Họ và Tên (*)</label>
            <div class="input-wrapper">
              <span class="input-icon">📛</span>
              <input v-model="form.fullname" type="text" class="g-form-control" placeholder="Nguyễn Văn A..." />
            </div>
          </div>
        </div>

        <div class="form-group">
          <label>Email</label>
          <div class="input-wrapper">
            <span class="input-icon">📧</span>
            <input v-model="form.email" type="email" class="g-form-control" placeholder="email@gmail.com" />
          </div>
        </div>

        <div class="form-group">
          <label>Mật khẩu (*)</label>
          <div class="input-wrapper">
            <span class="input-icon">🔒</span>
            <input v-model="form.password" type="password" class="g-form-control" placeholder="Nhập mật khẩu..." @keyup.enter="handleRegister" />
          </div>
        </div>

        <button @click="handleRegister" class="btn-register" :disabled="isLoading">
          <span v-if="!isLoading">🚀 Đăng Ký Ngay</span>
          <span v-else>Đang xử lý...</span>
        </button>

        <div v-if="errorMsg" class="error-banner">⚠️ {{ errorMsg }}</div>

        <p class="switch-link">
          Đã có tài khoản?
          <router-link to="/login">Đăng nhập →</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const router = useRouter();
const form = ref({ username: '', password: '', fullname: '', email: '' });
const isLoading = ref(false);
const errorMsg = ref('');

const handleRegister = async () => {
  errorMsg.value = '';
  if (!form.value.username || !form.value.password || !form.value.fullname) {
    errorMsg.value = 'Vui lòng điền đầy đủ thông tin bắt buộc!'; return;
  }
  isLoading.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/auth/signup', form.value);
    alert(res.data);
    router.push('/login');
  } catch (error) {
    errorMsg.value = error.response?.data || 'Đăng ký thất bại! Vui lòng thử lại.';
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
.register-page {
  min-height: 100vh; background: var(--bg-root);
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden; padding: 20px;
}
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(0, 212, 170, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 170, 0.04) 1px, transparent 1px);
  background-size: 40px 40px;
}
.glow-orb { position: absolute; border-radius: 50%; filter: blur(80px); pointer-events: none; }
.orb1 { width: 400px; height: 400px; background: rgba(0,212,170,0.1); top: -100px; right: -100px; }
.orb2 { width: 300px; height: 300px; background: rgba(0,100,200,0.08); bottom: -80px; left: -80px; }

.register-container { position: relative; z-index: 1; width: 100%; max-width: 520px; }

.brand { text-align: center; margin-bottom: 28px; }
.brand-icon { font-size: 2.5rem; margin-bottom: 10px; display: block; filter: drop-shadow(0 0 20px rgba(0,212,170,0.5)); }
.brand h1 { font-size: 1.7rem; font-weight: 900; color: var(--text-heading); letter-spacing: 2px; margin: 0 0 4px 0; }
.brand h1 span { color: var(--primary); }
.brand p { color: var(--text-muted); font-size: 0.83rem; letter-spacing: 2px; text-transform: uppercase; margin: 0; }

.form-card {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius-xl); padding: 36px;
  box-shadow: var(--shadow-lg), 0 0 60px rgba(0,212,170,0.05);
}
.form-card h2 { font-size: 1.5rem; font-weight: 800; color: var(--text-heading); margin: 0 0 6px 0; }
.form-subtitle { color: var(--text-muted); font-size: 0.88rem; margin: 0 0 24px 0; }

.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 0.82rem; font-weight: 600; color: var(--text-muted); margin-bottom: 7px; }
.input-wrapper { position: relative; }
.input-icon { position: absolute; left: 14px; top: 50%; transform: translateY(-50%); pointer-events: none; }
.input-wrapper .g-form-control { padding-left: 44px; }

.btn-register {
  width: 100%; padding: 14px; margin-top: 8px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; border-radius: var(--radius-md);
  font-size: 1rem; font-weight: 800; font-family: inherit;
  letter-spacing: 1px; cursor: pointer; transition: var(--transition);
}
.btn-register:hover { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(0,212,170,0.4); }
.btn-register:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.error-banner {
  margin-top: 16px; padding: 12px 16px;
  background: rgba(231,76,60,0.12); border: 1px solid rgba(231,76,60,0.3);
  border-radius: var(--radius-md); color: #e74c3c; font-size: 0.88rem;
}

.switch-link { text-align: center; margin-top: 22px; color: var(--text-muted); font-size: 0.88rem; }
.switch-link a { color: var(--primary); text-decoration: none; font-weight: 600; }
.switch-link a:hover { text-decoration: underline; }
</style>