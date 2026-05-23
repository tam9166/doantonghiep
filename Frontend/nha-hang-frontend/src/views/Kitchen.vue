<template>
  <div class="kitchen-wrapper">
    <header class="kitchen-header">
      <div class="header-left">
        <span class="header-icon">👨‍🍳</span>
        <div>
          <h1>BẾP — FPOLY RESTAURANT</h1>
          <p class="header-sub">Đơn cần nấu: <strong>{{ pendingOrders.length }}</strong></p>
        </div>
      </div>
      <div class="header-right">
        <button @click="fetchOrders" class="btn-refresh">🔄 Làm Mới</button>
        <button @click="handleLogout" class="btn-logout">🚪 Đăng Xuất</button>
      </div>
    </header>

    <main class="kitchen-main">
      <div v-if="pendingOrders.length === 0" class="empty-state">
        <div class="empty-icon">✅</div>
        <h2>Không có đơn nào cần nấu!</h2>
        <p>Hệ thống sẽ tự động cập nhật khi có đơn mới.</p>
      </div>

      <div class="orders-grid">
        <div v-for="order in pendingOrders" :key="order.id" class="order-card">
          <div class="card-header">
            <span class="order-id">#{{ order.id }}</span>
            <span class="order-table">{{ getTableName(order) }}</span>
            <span class="order-time">{{ formatTime(order.createDate) }}</span>
          </div>

          <div class="dish-list">
            <div v-for="(detail, idx) in order.orderDetails" :key="idx" class="dish-item">
              <img v-if="detail.product?.image" :src="detail.product.image" class="dish-thumb" />
              <span v-else class="dish-thumb-placeholder">🍽️</span>
              <div class="dish-info">
                <strong>{{ detail.product?.name || 'Món ăn' }}</strong>
                <span class="dish-qty">x{{ detail.quantity }}</span>
              </div>
            </div>
          </div>

          <div class="card-actions">
            <button @click="markReady(order.id)" class="btn-done">
              ✅ Đã Nấu Xong
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';

const router = useRouter();
const orders = ref([]);
const pendingOrders = ref([]);
let interval = null;

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

const fetchOrders = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/admin/orders', configHeader());
    orders.value = res.data;
    // status 1 = đang nấu (Kitchen cần xử lý)
    pendingOrders.value = res.data.filter(o => o.status === 1);
  } catch (err) {
    console.error('Lỗi lấy đơn bếp:', err);
  }
};

const markReady = async (id) => {
  try {
    await axios.put(`http://localhost:8080/api/admin/orders/${id}/status?status=2`, {}, configHeader());
    fetchOrders();
  } catch (err) { alert('Lỗi cập nhật trạng thái!'); }
};

const getTableName = (order) => {
  if (!order.address) return '🛵 Giao hàng';
  const match = order.address.match(/Bàn\s+(\S+)/);
  return match ? `🪑 Bàn ${match[1]}` : order.address;
};

const formatTime = (d) => d ? new Date(d).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  router.push('/login');
};

onMounted(() => {
  fetchOrders();
  interval = setInterval(fetchOrders, 15000); // Auto-refresh 15s
});

onUnmounted(() => { if (interval) clearInterval(interval); });
</script>

<style scoped>
.kitchen-wrapper { background: var(--bg-root); min-height: 100vh; font-family: 'Inter', sans-serif; }

/* Header */
.kitchen-header {
  background: rgba(6,13,26,0.95); backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-light);
  padding: 16px 24px; display: flex; justify-content: space-between; align-items: center;
  position: sticky; top: 0; z-index: 100;
  box-shadow: 0 2px 30px rgba(0,0,0,0.5);
}
.header-left { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 2rem; filter: drop-shadow(0 0 8px rgba(0,212,170,0.5)); }
.header-left h1 { margin: 0; font-size: 1.2rem; font-weight: 900; color: var(--primary); letter-spacing: 1px; }
.header-sub { margin: 2px 0 0 0; font-size: 0.85rem; color: var(--text-muted); }
.header-sub strong { color: #f39c12; }
.header-right { display: flex; gap: 10px; }

.btn-refresh, .btn-logout {
  background: transparent; border: 1px solid var(--border);
  color: var(--text-secondary); padding: 8px 16px; border-radius: 20px;
  cursor: pointer; font-weight: 600; font-size: 0.85rem; font-family: inherit;
  transition: 0.3s;
}
.btn-refresh:hover { border-color: var(--primary); color: var(--primary); }
.btn-logout:hover { border-color: #e74c3c; color: #e74c3c; }

/* Main */
.kitchen-main { padding: 24px; max-width: 1400px; margin: 0 auto; }

.empty-state {
  text-align: center; padding: 80px 20px;
  background: var(--bg-card); border: 1px dashed var(--border);
  border-radius: 16px; margin-top: 40px;
}
.empty-icon { font-size: 4rem; margin-bottom: 16px; }
.empty-state h2 { color: var(--primary); margin: 0 0 8px 0; font-weight: 800; }
.empty-state p { color: var(--text-muted); margin: 0; }

/* Orders Grid */
.orders-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px;
}

.order-card {
  background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: 16px; overflow: hidden;
  transition: 0.3s; border-left: 4px solid #f39c12;
}
.order-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); }

.card-header {
  padding: 14px 18px; display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid var(--border-light); background: rgba(0,0,0,0.15);
}
.order-id { font-weight: 900; color: var(--primary); font-size: 1rem; }
.order-table { font-weight: 700; color: var(--text-heading); font-size: 0.88rem; }
.order-time { font-size: 0.8rem; color: var(--text-muted); }

.dish-list { padding: 12px 18px; }
.dish-item {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid var(--border-light);
}
.dish-item:last-child { border-bottom: none; }
.dish-thumb { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border); }
.dish-thumb-placeholder { font-size: 1.5rem; width: 40px; text-align: center; }
.dish-info { flex: 1; }
.dish-info strong { font-size: 0.9rem; color: var(--text-heading); }
.dish-qty {
  margin-left: 8px; background: rgba(0,212,170,0.15); color: var(--primary);
  padding: 2px 8px; border-radius: 10px; font-size: 0.78rem; font-weight: 700;
}

.card-actions { padding: 12px 18px; }
.btn-done {
  width: 100%; padding: 12px;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; border-radius: 10px;
  font-weight: 800; font-size: 0.95rem; cursor: pointer;
  font-family: inherit; transition: 0.3s;
}
.btn-done:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }
</style>
