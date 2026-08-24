<template>
  <CustomerLayout>
  <div class="profile-wrapper">
    <!-- Navbar -->
    

    <main class="profile-content">
      <div style="margin-bottom: 20px;">
        <button @click="$router.back()" class="g-btn-outline" style="border-radius: 100px; padding: 8px 20px; border-color: rgba(255,255,255,0.2);">
          ← Quay Lại
        </button>
      </div>
      <div class="profile-card">
        <div class="profile-header">
          <div class="avatar-circle">
            {{ userProfile.fullname ? userProfile.fullname.charAt(0).toUpperCase() : 'U' }}
          </div>
          <div class="header-info">
            <h2>{{ userProfile.fullname || 'Người dùng' }}</h2>
            <p>{{ userProfile.username }}</p>
          </div>
          <div class="tier-badge" :class="getTierClass(userProfile.membershipTier)">
            <span class="tier-icon">{{ getTierIcon(userProfile.membershipTier) }}</span>
            <div class="tier-info">
              <span class="tier-name">Thành viên {{ userProfile.membershipTier }}</span>
              <span class="tier-points">{{ userProfile.points }} điểm</span>
            </div>
          </div>
        </div>

        <div class="profile-tabs">
          <button :class="{ active: currentTab === 'info' }" @click="currentTab = 'info'">Thông tin cá nhân</button>
          <button :class="{ active: currentTab === 'password' }" @click="currentTab = 'password'">Đổi mật khẩu</button>
        </div>

        <!-- Tab Thông tin -->
        <div v-if="currentTab === 'info'" class="tab-content">
          <div class="form-group">
            <label>Họ và Tên</label>
            <input type="text" v-model="editProfile.fullname" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Email</label>
            <input type="email" v-model="editProfile.email" class="g-form-control" />
          </div>
          <button class="g-btn-primary mt-4" @click="saveProfile">Lưu thay đổi</button>
        </div>

        <!-- Tab Đổi mật khẩu -->
        <div v-if="currentTab === 'password'" class="tab-content">
          <div class="form-group">
            <label>Mật khẩu cũ</label>
            <input type="password" v-model="passwordForm.oldPassword" class="g-form-control" placeholder="Nhập mật khẩu hiện tại" />
          </div>
          <div class="form-group">
            <label>Mật khẩu mới</label>
            <input type="password" v-model="passwordForm.newPassword" class="g-form-control" placeholder="Nhập mật khẩu mới" />
          </div>
          <div class="form-group">
            <label>Xác nhận mật khẩu mới</label>
            <input type="password" v-model="passwordForm.confirmPassword" class="g-form-control" placeholder="Nhập lại mật khẩu mới" />
          </div>
          <button class="g-btn-primary mt-4" @click="changePassword">Đổi mật khẩu</button>
        </div>

      </div>
    </main>
  </div>
  </CustomerLayout>
</template>

<script setup>
import CustomerLayout from '@/components/CustomerLayout.vue';

import { ref, onMounted } from 'vue';
import api from '@/services/api';
import { useRouter } from 'vue-router';

const router = useRouter();
const currentTab = ref('info');

const userProfile = ref({
  username: '',
  fullname: '',
  email: '',
  points: 0,
  membershipTier: 'Đồng'
});

const editProfile = ref({
  fullname: '',
  email: ''
});

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const loadProfile = async () => {
  const token = sessionStorage.getItem('token');
  if (!token) {
    alert("Vui lòng đăng nhập!");
    router.push('/login');
    return;
  }
  try {
    const res = await api.get('/api/auth/profile', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    userProfile.value = res.data;
    editProfile.value.fullname = res.data.fullname;
    editProfile.value.email = res.data.email;
  } catch (error) {
    console.error("Lỗi lấy thông tin:", error);
  }
};

const saveProfile = async () => {
  const token = sessionStorage.getItem('token');
  if (!editProfile.value.fullname) {
    alert("Họ tên không được để trống!");
    return;
  }
  try {
    await api.put('/api/auth/profile', {
      fullname: editProfile.value.fullname,
      email: editProfile.value.email
    }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert("Cập nhật thông tin thành công!");
    loadProfile();
  } catch (error) {
    alert("Lỗi cập nhật: " + (error.response?.data || "Vui lòng thử lại"));
  }
};

const changePassword = async () => {
  const token = sessionStorage.getItem('token');
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    alert("Vui lòng điền đủ mật khẩu cũ và mới!");
    return;
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert("Mật khẩu mới không khớp!");
    return;
  }
  try {
    await api.put('/api/auth/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert("Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
    handleLogout();
  } catch (error) {
    alert("Lỗi đổi mật khẩu: " + (error.response?.data || "Vui lòng thử lại"));
  }
};

const handleLogout = () => {
  sessionStorage.removeItem('token');
  localStorage.removeItem('roles');
  localStorage.removeItem('username');
  router.push('/login');
};

const getTierClass = (tier) => {
  if (tier === 'Kim Cương') return 'tier-diamond';
  if (tier === 'Vàng') return 'tier-gold';
  if (tier === 'Bạc') return 'tier-silver';
  return 'tier-bronze';
};

const getTierIcon = (tier) => {
  if (tier === 'Kim Cương') return '';
  if (tier === 'Vàng') return '';
  if (tier === 'Bạc') return '';
  return '';
};

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.profile-wrapper {
  background-color: var(--bg-root);
  min-height: 100vh;
  color: var(--text-primary);
}

/* NAVBAR TƯƠNG TỰ HOME VÀ MENU */
.navbar {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: sticky; top: 0; z-index: 100;
  padding: 12px 40px;
}
.nav-container { max-width: 1400px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; }
.logo-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.logo h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo p { margin: 0; font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; font-weight: 700; text-transform: uppercase; }

.nav-links { display: flex; gap: 6px; }
.nav-links a {
  text-decoration: none; color: var(--text-secondary);
  font-weight: 600; font-size: 0.95rem; padding: 10px 20px;
  border-radius: 100px; transition: var(--transition);
}
.nav-links a:hover { color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }

.nav-right { display: flex; gap: 10px; }
.btn-nav-outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-nav-outline:hover { border-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }
.btn-cart {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--text-primary); border: none; padding: 10px 24px; border-radius: 100px;
  font-weight: 800; cursor: pointer; transition: var(--transition);
}
.btn-logout {
  background: color-mix(in srgb, var(--primary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  color: var(--primary); padding: 10px 20px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.btn-logout:hover { background: color-mix(in srgb, var(--primary) 30%, transparent); }

/* MAIN CONTENT */
.profile-content {
  max-width: 800px;
  margin: 60px auto;
  padding: 0 20px;
}
.profile-card {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.4);
}

/* HEADER */
.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 40px;
  padding-bottom: 30px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.avatar-circle {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--text-primary);
  font-size: 2.5rem;
  font-weight: 900;
  display: flex; justify-content: center; align-items: center;
  box-shadow: 0 10px 20px color-mix(in srgb, var(--secondary) 30%, transparent);
}
.header-info h2 { margin: 0 0 5px 0; font-size: 1.8rem; color: var(--text-heading); }
.header-info p { margin: 0; color: var(--text-muted); font-size: 1rem; }

.tier-badge {
  margin-left: auto;
  display: flex; align-items: center; gap: 12px;
  padding: 12px 20px;
  border-radius: 16px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
}
.tier-icon { font-size: 2rem; }
.tier-info { display: flex; flex-direction: column; }
.tier-name { font-weight: 900; font-size: 1.1rem; text-transform: uppercase; letter-spacing: 1px; }
.tier-points { font-size: 0.85rem; opacity: 0.8; font-weight: 600; }

/* TIER COLORS */
.tier-diamond .tier-name { color: var(--tier-diamond); text-shadow: 0 0 10px color-mix(in srgb, var(--tier-diamond) 50%, transparent); }
.tier-gold .tier-name { color: var(--color-tertiary); text-shadow: 0 0 10px color-mix(in srgb, var(--color-tertiary) 50%, transparent); }
.tier-silver .tier-name { color: var(--color-outline); }
.tier-bronze .tier-name { color: var(--color-tertiary); }

/* TABS */
.profile-tabs {
  display: flex; gap: 20px; margin-bottom: 30px;
}
.profile-tabs button {
  background: none; border: none; border-bottom: 2px solid transparent;
  color: var(--text-secondary); font-size: 1.1rem; font-weight: 700;
  padding: 10px 20px; cursor: pointer; transition: 0.3s;
}
.profile-tabs button:hover { color: #FFFFFF; }
.profile-tabs button.active {
  color: var(--primary);
  border-bottom-color: var(--primary);
}

.tab-content {
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

.form-group { margin-bottom: 20px; text-align: left; }
.form-group label { display: block; margin-bottom: 8px; font-weight: 600; color: var(--text-secondary); }
.g-form-control {
  width: 100%; padding: 14px;
  background: rgba(0,0,0,0.2);
  border: 1px solid rgba(255,255,255,0.1);
  color: #FFFFFF; border-radius: 8px;
  font-size: 1rem;
}
.g-form-control:focus { border-color: var(--primary); outline: none; box-shadow: 0 0 10px color-mix(in srgb, var(--secondary) 20%, transparent); }
</style>
