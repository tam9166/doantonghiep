<template>
  <div class="admin-wrapper">
    <header class="g-navbar">
      <div class="g-logo">
        <h2>Mộc Vị <span>RESTAURANT</span></h2>
        <p>Admin Dashboard</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/admin">Thực Đơn</router-link>
        <router-link to="/admin/categories">Danh Mục</router-link>
        <router-link to="/admin/ingredients">Nguyên Liệu</router-link>
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/vouchers" class="active">Khuyến Mãi</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
        <router-link to="/admin/analytics">Thống Kê</router-link>
      </nav>
      <button @click="$router.push('/')" class="g-btn-nav">🏠 Trang Khách</button>
    </header>

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Lý Voucher Khuyến Mãi</h1>
        <button @click="showAddModal = true" class="g-btn-primary">+ Tạo Voucher Mới</button>
      </div>

      <div class="g-table-container">
        <table class="g-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Mã Code</th>
              <th>Giảm Giá (%)</th>
              <th>Khách Hàng Áp Dụng</th>
              <th>Trạng Thái</th>
              <th>Ngày Tạo</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in vouchers" :key="v.id">
              <td>#{{ v.id }}</td>
              <td><span class="code-badge">{{ v.code }}</span></td>
              <td style="color: #e74c3c; font-weight: bold;">{{ v.discountPercent }}%</td>
              <td>{{ v.account ? v.account.username : 'Tất cả khách hàng' }}</td>
              <td>
                <span class="status-badge" :class="{'used': v.isUsed}">
                  {{ v.isUsed ? 'Đã dùng' : 'Chưa dùng' }}
                </span>
              </td>
              <td>{{ formatDate(v.createDate) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>

    <!-- Modal Tạo Voucher -->
    <div v-if="showAddModal" class="g-modal-overlay" @click.self="showAddModal = false">
      <div class="g-modal-box">
        <h3>Tạo Voucher Mới</h3>
        
        <div class="form-group mt-3">
          <label>Mã Code (Để trống sẽ tự tạo)</label>
          <input v-model="newVoucher.code" class="g-form-control" placeholder="VD: TET2025" />
        </div>
        
        <div class="form-group mt-3">
          <label>Mức Giảm Giá (%) *</label>
          <input type="number" v-model="newVoucher.discountPercent" class="g-form-control" min="1" max="100" />
        </div>

        <div class="form-group mt-3">
          <label>Tài Khoản Khách (Chỉ người này được dùng)</label>
          <input v-model="newVoucher.accountUsername" class="g-form-control" placeholder="Để trống nếu áp dụng cho mọi khách" />
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="createVoucher" class="g-btn-primary" style="flex:1;">Tạo Ngay</button>
          <button @click="showAddModal = false" class="g-btn-outline" style="flex:1;">Hủy</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const vouchers = ref([]);
const showAddModal = ref(false);
const newVoucher = ref({ code: '', discountPercent: 10, accountUsername: '' });

const fetchVouchers = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/vouchers/admin', {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    vouchers.value = res.data;
  } catch (error) {
    console.error('Lỗi lấy danh sách voucher:', error);
  }
};

const createVoucher = async () => {
  if (!newVoucher.value.discountPercent) return alert("Vui lòng nhập phần trăm giảm giá!");
  try {
    const payload = {
      code: newVoucher.value.code,
      discountPercent: newVoucher.value.discountPercent,
      account: newVoucher.value.accountUsername ? { username: newVoucher.value.accountUsername } : null
    };
    await axios.post('http://localhost:8080/api/vouchers/admin/create', payload, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    alert('Tạo Voucher thành công!');
    showAddModal.value = false;
    newVoucher.value = { code: '', discountPercent: 10, accountUsername: '' };
    fetchVouchers();
  } catch (error) {
    alert("Lỗi khi tạo Voucher. Kiểm tra lại username khách nếu có nhập!");
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return d.toLocaleString('vi-VN');
};

onMounted(fetchVouchers);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.code-badge { background: #f1c40f; color: #000; padding: 4px 10px; border-radius: 4px; font-weight: bold; font-family: monospace; letter-spacing: 1px;}
.status-badge { background: rgba(46, 204, 113, 0.2); color: #2ecc71; padding: 4px 10px; border-radius: 20px; font-weight: bold; font-size: 0.85rem;}
.status-badge.used { background: rgba(149, 165, 166, 0.2); color: #7f8c8d; }
.mt-3 { margin-top: 15px; }
.mt-4 { margin-top: 20px; }
</style>
