<template>
  <div class="history-wrapper">
    <header class="history-navbar">
      <div class="nav-container">
        <div class="logo" @click="$router.push('/')">
          <span class="logo-icon">🍽️</span>
          <div>
            <h2>NHÀ HÀNG FPOLY</h2>
            <p>ĐÀ NẴNG</p>
          </div>
        </div>
        <nav class="nav-links">
          <router-link to="/">Trang chủ</router-link>
          <router-link to="/menu">Thực đơn</router-link>
          <router-link to="/reservation">Đặt chỗ</router-link>
          <router-link to="/dine-in">Tại bàn</router-link>
        </nav>
        <div class="nav-right">
          <span class="nav-badge">📜 Lịch Sử Đơn Hàng</span>
        </div>
      </div>
    </header>

    <div class="history-container">
    <h1>Lịch Sử Đặt Món</h1>
    
    <div v-if="orders.length > 0">
      <table class="history-table">
        <thead>
          <tr>
            <th>Mã Đơn</th>
            <th>Ngày Đặt</th>
            <th>Địa Chỉ</th>
            <th>Trạng Thái</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>#{{ order.id }}</td>
            <td>{{ new Date(order.createDate).toLocaleString() }}</td>
            <td>{{ order.address }}</td>
            <td>
              <span :class="order.status === 0 ? 'status-pending' : 'status-done'">
                {{ order.status === 0 ? 'Đang xử lý' : 'Đã hoàn thành' }}
              </span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="no-order">Bạn chưa có đơn hàng nào.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const orders = ref([]);

const fetchHistory = async () => {
  const token = localStorage.getItem('token');
  try {
    const response = await axios.get('http://localhost:8080/api/orders/history', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    orders.value = response.data;
  } catch (error) {
    console.error("Lỗi khi lấy lịch sử:", error);
  }
};

onMounted(fetchHistory);
</script>

<style scoped>
.history-wrapper { background: var(--bg-root); min-height: 100vh; font-family: 'Inter', sans-serif; }

/* Navbar */
.history-navbar {
  background: rgba(6, 13, 26, 0.95); backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light); position: sticky; top: 0; z-index: 100;
  box-shadow: 0 2px 30px rgba(0,0,0,0.5);
}
.nav-container {
  max-width: 1400px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 24px; height: 68px;
}
.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo-icon { font-size: 1.5rem; filter: drop-shadow(0 0 8px rgba(0,212,170,0.5)); }
.logo h2 { margin: 0; font-size: 1.2rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo p { margin: 0; font-size: 0.65rem; color: var(--primary); letter-spacing: 3px; text-transform: uppercase; }
.nav-links { display: flex; gap: 4px; align-items: center; }
.nav-links a {
  color: var(--text-muted); text-decoration: none; font-size: 0.88rem; font-weight: 500;
  padding: 8px 14px; border-radius: 8px; transition: 0.3s;
}
.nav-links a:hover, .nav-links a.router-link-active { color: var(--primary); background: rgba(0,212,170,0.08); }
.nav-right { display: flex; align-items: center; gap: 8px; }
.nav-badge {
  background: rgba(0,212,170,0.1); border: 1px solid var(--border);
  color: var(--primary); padding: 7px 14px; border-radius: 20px;
  font-size: 0.83rem; font-weight: 700;
}

/* Content */
.history-container { padding: 40px 20px; max-width: 900px; margin: 0 auto; color: var(--text-primary); }
h1 { color: var(--primary); border-bottom: 1px solid var(--border); padding-bottom: 10px; margin-bottom: 20px; font-weight: 900; }

.history-table { width: 100%; border-collapse: collapse; background: var(--bg-card); box-shadow: var(--shadow-md); border-radius: 8px; overflow: hidden; border: 1px solid var(--border-light); }
.history-table th, .history-table td { padding: 15px; border-bottom: 1px solid var(--border); text-align: left; }
.history-table th { background-color: var(--bg-nav); color: var(--primary); font-weight: bold; }
.history-table tr:hover { background-color: rgba(255,255,255,0.02); }
.status-pending { color: #f39c12; font-weight: bold; background: rgba(243, 156, 18, 0.1); padding: 5px 10px; border-radius: 12px; font-size: 0.85rem; }
.status-done { color: #2ecc71; font-weight: bold; background: rgba(46, 204, 113, 0.1); padding: 5px 10px; border-radius: 12px; font-size: 0.85rem; }
.no-order { text-align: center; color: var(--text-muted); margin-top: 50px; font-style: italic; background: var(--bg-card); padding: 30px; border-radius: 8px; border: 1px dashed var(--border); }
</style>