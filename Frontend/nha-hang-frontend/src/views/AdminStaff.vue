<template>
  <div class="admin-wrapper">
    <header class="g-navbar">
      <div class="g-logo">
        <h2>FPOLY <span>RESTAURANT</span></h2>
        <p>HR Management System</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/admin">Thực Đơn</router-link>
        <router-link to="/admin/categories">Danh Mục</router-link>
        <router-link to="/admin/ingredients">Nguyên Liệu</router-link>
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/vouchers">Khuyến Mãi</router-link>
        <router-link to="/admin/staff" class="active">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
        <router-link to="/admin/analytics">Thống Kê</router-link>
      </nav>
      <button @click="$router.push('/')" class="g-btn-nav">🏠 Trang Khách</button>
    </header>

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Lý Nhân Sự & Phân Quyền</h1>
        <p class="page-subtitle">Thêm mới tài khoản cho Bếp, Phục vụ và Quản lý</p>
      </div>

      <div class="content-grid">
        <!-- Form thêm nhân viên -->
        <div class="form-card">
          <h3>👤 Cấp Tài Khoản Mới</h3>

          <div class="form-group">
            <label>Tên đăng nhập (Username)</label>
            <input v-model="newStaff.username" type="text" class="g-form-control" placeholder="VD: bep_chinh" />
          </div>
          <div class="form-group">
            <label>Mật khẩu</label>
            <input v-model="newStaff.password" type="password" class="g-form-control" placeholder="Nhập mật khẩu..." />
          </div>
          <div class="form-group">
            <label>Họ và Tên Nhân Viên</label>
            <input v-model="newStaff.fullname" type="text" class="g-form-control" placeholder="VD: Nguyễn Quang Nhật" />
          </div>
          <div class="form-group">
            <label>Email liên hệ</label>
            <input v-model="newStaff.email" type="email" class="g-form-control" placeholder="nhat@example.com" />
          </div>
          <div class="form-group">
            <label>Phân Quyền (Role)</label>
            <select v-model="selectedRole" class="g-form-control">
              <option value="ROLE_KITCHEN">👨‍🍳 Nhân Viên Bếp (Kitchen)</option>
              <option value="ROLE_WAITER">🤵 Nhân Viên Phục Vụ (Waiter)</option>
              <option value="ROLE_MANAGER">👔 Quản Lý Nhà Hàng (Manager)</option>
              <option value="ROLE_USER">👥 Khách Hàng (User)</option>
            </select>
          </div>

          <div class="role-preview" :class="getRoleClass(selectedRole)">
            <span class="role-icon">{{ getRoleIcon(selectedRole) }}</span>
            <div>
              <div class="role-name">{{ getRoleName(selectedRole) }}</div>
              <div class="role-desc">{{ getRoleDesc(selectedRole) }}</div>
            </div>
          </div>

          <button @click="handleAddStaff" class="g-btn-primary" style="width:100%; margin-top: 20px;">
            CẤP TÀI KHOẢN
          </button>
        </div>

        <!-- Danh sách nhân viên -->
        <div class="table-card">
          <h3>📋 Danh Sách Tài Khoản Hệ Thống <span class="count-chip">{{ staffList.length }}</span></h3>
          <div class="table-responsive">
            <table class="g-table">
              <thead>
                <tr>
                  <th>USERNAME</th>
                  <th>VAI TRÒ</th>
                  <th>HỌ VÀ TÊN</th>
                  <th>EMAIL</th>
                  <th>HÀNH ĐỘNG</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="staff in staffList" :key="staff.username" class="staff-row">
                  <td>
                    <span class="username-badge">{{ staff.username }}</span>
                  </td>
                  <td>
                    <div class="role-badge-small" :class="getRoleClass(staff.role)">
                      {{ getRoleIcon(staff.role) }} {{ getRoleName(staff.role) }}
                    </div>
                  </td>
                  <td class="fullname-cell">{{ staff.fullname || 'Chưa cập nhật' }}</td>
                  <td class="email-cell">{{ staff.email || '---' }}</td>
                  <td>
                    <button @click="deleteStaff(staff.username)" class="g-btn-danger" title="Sa thải nhân viên này">
                      🗑 Xóa
                    </button>
                  </td>
                </tr>
                <tr v-if="staffList.length === 0">
                  <td colspan="5" class="empty-row">Chưa có tài khoản nào.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const staffList = ref([]);
const newStaff = ref({ username: '', password: '', fullname: '', email: '' });
const selectedRole = ref('ROLE_WAITER');

const fetchStaff = async () => {
  const token = localStorage.getItem('token');
  try {
    const res = await axios.get('http://localhost:8080/api/admin/staff', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    staffList.value = res.data;
  } catch (error) { console.error('Lỗi tải danh sách nhân sự', error); }
};

const handleAddStaff = async () => {
  if (!newStaff.value.username || !newStaff.value.password) return alert('Vui lòng nhập Username và Password!');
  const token = localStorage.getItem('token');
  try {
    await axios.post(`http://localhost:8080/api/admin/staff?roleId=${selectedRole.value}`, newStaff.value, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert('Cấp tài khoản nhân viên thành công!');
    newStaff.value = { username: '', password: '', fullname: '', email: '' };
    fetchStaff();
  } catch (error) {
    alert(error.response?.data || 'Lỗi tạo tài khoản! Có thể Username đã tồn tại.');
  }
};

const deleteStaff = async (username) => {
  if (username === 'admin') return alert('Không được phép xóa tài khoản Super Admin!');
  if (!confirm(`Bạn có chắc chắn muốn xóa tài khoản [${username}] không?`)) return;
  const token = localStorage.getItem('token');
  try {
    await axios.delete(`http://localhost:8080/api/admin/staff/${username}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert('Đã xóa tài khoản thành công!');
    fetchStaff();
  } catch (error) { alert('Lỗi khi xóa tài khoản!'); }
};

const getRoleClass = (role) => {
  if (role === 'ROLE_KITCHEN') return 'role-kitchen';
  if (role === 'ROLE_WAITER') return 'role-waiter';
  if (role === 'ROLE_MANAGER' || role === 'ADMIN' || role === 'ROLE_ADMIN') return 'role-manager';
  return 'role-user';
};
const getRoleIcon = (role) => {
  if (role === 'ROLE_KITCHEN') return '👨‍🍳';
  if (role === 'ROLE_WAITER') return '🤵';
  if (role === 'ROLE_MANAGER' || role === 'ADMIN' || role === 'ROLE_ADMIN') return '👑';
  return '👥';
};
const getRoleName = (role) => {
  if (role === 'ROLE_KITCHEN') return 'Nhân Viên Bếp';
  if (role === 'ROLE_WAITER') return 'Nhân Viên Phục Vụ';
  if (role === 'ROLE_MANAGER' || role === 'ADMIN' || role === 'ROLE_ADMIN') return 'Quản Trị Viên';
  return 'Khách Hàng';
};
const getRoleDesc = (role) => {
  if (role === 'ROLE_KITCHEN') return 'Truy cập: Trang bếp KDS';
  if (role === 'ROLE_WAITER') return 'Truy cập: Dashboard phục vụ';
  if (role === 'ROLE_MANAGER' || role === 'ADMIN' || role === 'ROLE_ADMIN') return 'Truy cập: Toàn bộ trang Admin';
  return 'Truy cập: Trang đặt hàng, menu';
};

onMounted(fetchStaff);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }

.page-header { margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }

.content-grid { display: grid; grid-template-columns: 380px 1fr; gap: 28px; }

.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
  height: fit-content;
}
.form-card h3 {
  margin: 0 0 24px 0; font-size: 1.1rem; font-weight: 700; color: var(--text-heading);
  padding-bottom: 16px; border-bottom: 1px solid var(--border-light);
}
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block; font-size: 0.83rem; font-weight: 600; color: var(--text-muted);
  margin-bottom: 7px; text-transform: uppercase; letter-spacing: 0.5px;
}

/* Role Preview */
.role-preview {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-light);
  margin-top: 8px;
  transition: var(--transition);
}
.role-icon { font-size: 1.8rem; }
.role-name { font-weight: 700; color: var(--text-heading); font-size: 0.95rem; }
.role-desc { font-size: 0.8rem; color: var(--text-muted); margin-top: 2px; }

.role-kitchen { border-color: rgba(0,212,170,0.3); background: rgba(0,212,170,0.05); }
.role-waiter  { border-color: rgba(52,152,219,0.3); background: rgba(52,152,219,0.05); }
.role-manager { border-color: rgba(241,196,15,0.3); background: rgba(241,196,15,0.05); }
.role-user    { border-color: rgba(90,122,138,0.3); background: rgba(90,122,138,0.05); }

/* Table Card */
.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
}
.table-card h3 {
  margin: 0 0 24px 0; font-size: 1.1rem; font-weight: 700; color: var(--text-heading);
  padding-bottom: 16px; border-bottom: 1px solid var(--border-light);
  display: flex; align-items: center; gap: 10px;
}
.count-chip {
  background: var(--primary-glow); color: var(--primary);
  padding: 3px 10px; border-radius: 20px; font-size: 0.85rem; font-weight: 800;
}
.table-responsive { overflow-x: auto; }

.username-badge {
  background: var(--bg-input); color: var(--primary);
  padding: 4px 12px; border-radius: 6px;
  font-family: monospace; font-size: 0.95rem; font-weight: 700;
  border: 1px solid var(--border-light);
}
.fullname-cell { color: var(--text-primary); font-weight: 600; }
.email-cell { color: var(--text-muted); font-size: 0.88rem; }
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }

.role-badge-small {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px; border-radius: 6px; font-size: 0.85rem; font-weight: 700;
  border: 1px solid; white-space: nowrap;
}
</style>
