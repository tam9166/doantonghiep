<template>
  <div class="kitchen-wrapper">
    <header class="kitchen-header">
      <div class="header-left">
        <span class="header-icon">👨‍🍳</span>
        <div>
          <h1>BẾP — MỘC VỊ RESTAURANT</h1>
          <p class="header-sub">Đơn cần nấu: <strong>{{ pendingOrders.length }}</strong> | Tổng món: <strong>{{ totalDishes }}</strong></p>
        </div>
      </div>
      <div class="header-right">
        <button @click="activeTab = 'orders'" :class="['tab-btn', { active: activeTab === 'orders' }]">📋 Đơn Hàng</button>
        <button @click="activeTab = 'inventory'" :class="['tab-btn', { active: activeTab === 'inventory' }]">
          📦 Tồn Kho
          <span v-if="lowStockCount > 0" class="badge-warn">{{ lowStockCount }}</span>
        </button>
        <button @click="activeTab = 'menu'" :class="['tab-btn', { active: activeTab === 'menu' }]">🍽️ Báo Hết Món</button>
        <button @click="fetchOrders" class="btn-refresh">🔄</button>
        <button @click="handleLogout" class="btn-logout">🚪 Đăng Xuất</button>
      </div>
    </header>

    <!-- Stats Bar -->
    <div class="stats-bar">
      <div class="stat-item">
        <span class="stat-value urgent">{{ pendingOrders.length }}</span>
        <span class="stat-label">Đơn Chờ Nấu</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ totalDishes }}</span>
        <span class="stat-label">Tổng Món Cần Nấu</span>
      </div>
      <div class="stat-item">
        <span class="stat-value done">{{ todayCompleted }}</span>
        <span class="stat-label">Đã Nấu Hôm Nay</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ todayDishes }}</span>
        <span class="stat-label">Tổng Món Đã Xong</span>
      </div>
      <div class="stat-item" :class="{ 'stat-warn': lowStockCount > 0 }">
        <span class="stat-value" :class="{ urgent: lowStockCount > 0 }">{{ lowStockCount }}</span>
        <span class="stat-label">NL Sắp Hết</span>
      </div>
    </div>

    <main class="kitchen-main">
      <!-- ========== TAB 1: ĐƠN HÀNG ========== -->
      <div v-if="activeTab === 'orders'">
        <div v-if="pendingOrders.length === 0" class="empty-state">
          <div class="empty-icon">✅</div>
          <h2>Không có đơn nào cần nấu!</h2>
          <p>Hệ thống sẽ tự động cập nhật khi có đơn mới.</p>
        </div>

        <div v-if="pendingOrders.length > 0" class="kitchen-ai-action">
          <button @click="getAiKitchenAdvice" class="btn-ai-analyze">🤖 AI Gợi Ý Nấu</button>
        </div>

        <div class="orders-grid">
          <div v-for="order in sortedOrders" :key="order.id" :class="['order-card', getUrgencyClass(order), { 'cooking': order.status === 6 }]">
            <div class="card-header">
              <span class="order-id">#{{ order.id }}</span>
              <span class="order-table">{{ getTableName(order) }}</span>
              <span :class="['order-timer', getTimerClass(order)]">{{ getElapsedTime(order.createDate) }}</span>
            </div>
            <div v-if="getNote(order)" class="order-note">📝 {{ getNote(order) }}</div>
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
            <div class="card-footer">
              <span class="dish-count">{{ getDishCount(order) }} món</span>
              <div style="display: flex; gap: 8px;">
                <button v-if="order.status === 1" @click="startCooking(order.id)" class="btn-start">🔥 Làm</button>
                <button @click="markReady(order.id)" class="btn-done">✅ Xong</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB 2: TỒN KHO ========== -->
      <div v-if="activeTab === 'inventory'">
        <div class="inv-header" style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h2>📦 Tồn Kho Nguyên Liệu</h2>
            <p class="inv-sub">Danh sách nguyên liệu hiện có trong kho.</p>
          </div>
          <button @click="$router.push('/admin/ingredients')" class="btn-restock" style="padding: 10px 20px; border-radius: 8px;">
            ⚙️ Quản Lý Kho & Công Thức
          </button>
        </div>

        <div v-if="ingredients.length === 0" class="empty-state">
          <div class="empty-icon">📦</div>
          <h2>Chưa có nguyên liệu nào</h2>
          <p>Quản lý cần thêm nguyên liệu từ trang Admin.</p>
        </div>

        <div class="inv-grid" v-else>
          <div v-for="ing in ingredients" :key="ing.id" :class="['inv-card', getStockClass(ing)]">
            <div class="inv-card-top">
              <div class="inv-icon">{{ getStockIcon(ing) }}</div>
              <div class="inv-info">
                <h4>{{ ing.name }}</h4>
                <span class="inv-unit">Đơn vị: {{ ing.unit }}</span>
              </div>
            </div>
            <div class="inv-card-bottom">
              <div class="inv-qty">
                <span class="inv-qty-value">{{ ing.quantity?.toFixed(1) || '0' }}</span>
                <span class="inv-qty-unit">{{ ing.unit }}</span>
              </div>
              <div class="inv-status">
                <span v-if="ing.quantity <= 0" class="inv-badge inv-out">HẾT</span>
                <span v-else-if="ing.quantity <= ing.minStock" class="inv-badge inv-low">SẮP HẾT</span>
                <span v-else class="inv-badge inv-ok">ĐỦ</span>
              </div>
            </div>
            <div class="inv-bar-wrap">
              <div class="inv-bar" :style="{ width: getStockPercent(ing) + '%' }" :class="getStockBarClass(ing)"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB 3: BÁO HẾT MÓN ========== -->
      <div v-if="activeTab === 'menu'">
        <div class="inv-header">
          <h2>🍽️ Quản Lý Trạng Thái Món Ăn</h2>
          <p class="inv-sub">Bấm để báo hết hoặc mở bán lại món. Cập nhật ngay trên Menu khách.</p>
        </div>

        <div class="menu-grid">
          <div v-for="product in products" :key="product.id" :class="['menu-card', { 'menu-disabled': !product.available }]">
            <img :src="product.image || 'https://placehold.co/80x80/0d1b2a/00d4aa?text=🍽'" class="menu-img" />
            <div class="menu-info">
              <h4>{{ product.name }}</h4>
              <span class="menu-price">{{ product.price?.toLocaleString() }}đ</span>
              <span class="menu-cat">{{ product.category?.name || '' }}</span>
            </div>
            <div class="menu-action">
              <span :class="['menu-status', product.available ? 'status-on' : 'status-off']">
                {{ product.available ? '✅ Đang bán' : '❌ Hết món' }}
              </span>
              <button @click="toggleAvailable(product)" :class="['btn-toggle-menu', product.available ? 'btn-off' : 'btn-on']">
                {{ product.available ? '⏸ Báo Hết' : '▶ Mở Bán' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- AI Modal -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
      <div class="ai-modal">
        <div class="modal-header">
          <h2>🤖 Bếp Trưởng AI</h2>
          <button @click="showAiModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <div v-if="aiLoading" class="ai-loading">
            <div class="spinner"></div>
            <p>AI đang phân tích các đơn cần nấu...</p>
          </div>
          <div v-else class="ai-result">
            <p>{{ aiResponse }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <div v-if="toastMsg" class="toast-notification">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const router = useRouter();
const orders = ref([]);
const pendingOrders = ref([]);
const allOrders = ref([]);
const ingredients = ref([]);
const products = ref([]);
const toastMsg = ref('');
const now = ref(new Date());
const activeTab = ref('orders');
let timerInterval = null;
let previousPendingIds = [];
let stompClient = null;

const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

// === AUDIO NOTIFICATION ===
const playNotificationSound = () => {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    [0, 0.15].forEach((delay, i) => {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.type = 'sine';
      osc.frequency.value = i === 0 ? 880 : 1100;
      gain.gain.setValueAtTime(0.3, audioCtx.currentTime + delay);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + delay + 0.3);
      osc.start(audioCtx.currentTime + delay);
      osc.stop(audioCtx.currentTime + delay + 0.3);
    });
  } catch (e) { /* silent */ }
};

// === FETCH DATA ===
const fetchOrders = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/admin/orders', configHeader());
    allOrders.value = res.data;
    const newPending = res.data.filter(o => o.status === 1 || o.status === 6);
    const newIds = newPending.map(o => o.id);
    const hasNewOrder = newIds.some(id => !previousPendingIds.includes(id));
    if (hasNewOrder && previousPendingIds.length > 0) {
      playNotificationSound();
      toastMsg.value = '🔔 Có đơn mới từ khách!';
      setTimeout(() => { toastMsg.value = ''; }, 3000);
    }
    previousPendingIds = newIds;
    orders.value = res.data;
    pendingOrders.value = newPending;
  } catch (err) { console.error('Lỗi lấy đơn bếp:', err); }
};

const fetchIngredients = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/admin/ingredients', configHeader());
    ingredients.value = res.data;
  } catch (err) { console.error('Lỗi lấy nguyên liệu:', err); }
};

const fetchProducts = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/products');
    products.value = res.data;
  } catch (err) { console.error('Lỗi lấy sản phẩm:', err); }
};

// === COMPUTED ===
const totalDishes = computed(() => pendingOrders.value.reduce((sum, o) => sum + (o.orderDetails?.reduce((s, d) => s + d.quantity, 0) || 0), 0));
const sortedOrders = computed(() => [...pendingOrders.value].sort((a, b) => new Date(a.createDate) - new Date(b.createDate)));
const lowStockCount = computed(() => ingredients.value.filter(i => i.quantity <= i.minStock).length);

const todayOrders = computed(() => {
  const today = new Date().toDateString();
  return allOrders.value.filter(o => o.createDate && new Date(o.createDate).toDateString() === today);
});
const todayCompleted = computed(() => todayOrders.value.filter(o => o.status >= 2).length);
const todayDishes = computed(() => todayOrders.value.filter(o => o.status >= 2).reduce((sum, o) => sum + (o.orderDetails?.reduce((s, d) => s + d.quantity, 0) || 0), 0));

// === HELPERS ===
const getDishCount = (order) => order.orderDetails?.reduce((s, d) => s + d.quantity, 0) || 0;

const getElapsedTime = (createDate) => {
  if (!createDate) return '';
  const elapsed = Math.floor((now.value - new Date(createDate)) / 1000);
  const mins = Math.floor(elapsed / 60);
  const secs = elapsed % 60;
  if (mins >= 60) return `${Math.floor(mins / 60)}h ${mins % 60}p`;
  return `${mins}:${String(secs).padStart(2, '0')}`;
};

const getElapsedMinutes = (d) => d ? Math.floor((now.value - new Date(d)) / 60000) : 0;
const getTimerClass = (o) => { const m = getElapsedMinutes(o.createDate); return m >= 15 ? 'timer-critical late-warning-text' : m >= 10 ? 'timer-warning' : 'timer-normal'; };
const getUrgencyClass = (o) => { const m = getElapsedMinutes(o.createDate); return m >= 15 ? 'urgency-critical late-warning-box' : m >= 10 ? 'urgency-warning' : ''; };

const getTableName = (order) => {
  if (!order.address) return '🛵 Giao hàng';
  const match = order.address.match(/Bàn[:\s]+([^\s|]+)/);
  return match ? `🪑 Bàn ${match[1]}` : order.address;
};
const getNote = (order) => { if (!order.address) return ''; const m = order.address.match(/GhiChú:\s*([^|]+)/i); return m ? m[1].trim() : ''; };

// Inventory helpers
const getStockClass = (ing) => {
  if (ing.quantity <= 0) return 'inv-card-out';
  if (ing.quantity <= ing.minStock) return 'inv-card-low';
  return '';
};
const getStockIcon = (ing) => {
  if (ing.quantity <= 0) return '🚫';
  if (ing.quantity <= ing.minStock) return '⚠️';
  return '✅';
};
const getStockPercent = (ing) => {
  if (!ing.minStock || ing.minStock <= 0) return ing.quantity > 0 ? 100 : 0;
  return Math.min(100, (ing.quantity / (ing.minStock * 3)) * 100);
};
const getStockBarClass = (ing) => {
  if (ing.quantity <= 0) return 'bar-out';
  if (ing.quantity <= ing.minStock) return 'bar-low';
  return 'bar-ok';
};

// === ACTIONS ===
const markReady = async (id) => {
  try {
    await axios.put(`http://localhost:8080/api/admin/orders/${id}/status?status=2`, {}, configHeader());
    toastMsg.value = '✅ Đã chuyển món cho phục vụ!';
    setTimeout(() => { toastMsg.value = ''; }, 2500);
    fetchOrders();
  } catch (err) { alert('Lỗi cập nhật trạng thái!'); }
};

const startCooking = async (id) => {
  try {
    await axios.put(`http://localhost:8080/api/admin/orders/${id}/status?status=6`, {}, configHeader());
    toastMsg.value = '🔥 Đang nấu...';
    setTimeout(() => { toastMsg.value = ''; }, 2500);
    fetchOrders();
  } catch (err) { alert('Lỗi cập nhật trạng thái!'); }
};

const toggleAvailable = async (product) => {
  const originalState = product.available;
  product.available = !product.available;
  try {
    const res = await axios.put(`http://localhost:8080/api/admin/products/${product.id}/toggle-available`, {}, configHeader());
    toastMsg.value = typeof res.data === 'string' ? res.data : '✅ Đã cập nhật!';
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchProducts();
    } catch (error) {
      alert("Lỗi khi cập nhật trạng thái món ăn!");
      product.available = originalState; // revert if error
    }
};

// === AI ASSISTANT ===
const getAiKitchenAdvice = async () => {
  showAiModal.value = true;
  aiLoading.value = true;
  aiResponse.value = '';

  // Gom nhóm tên món và tổng số lượng
  const dishCounts = {};
  pendingOrders.value.forEach(order => {
    order.orderDetails?.forEach(d => {
      const name = d.product?.name;
      if (name) {
        dishCounts[name] = (dishCounts[name] || 0) + d.quantity;
      }
    });
  });

  const dishListText = Object.entries(dishCounts)
    .map(([name, qty]) => `${name}: ${qty} phần`)
    .join(', ');

  if (!dishListText) {
    aiResponse.value = "Chưa có món nào để nấu!";
    aiLoading.value = false;
    return;
  }

  try {
    const res = await axios.post('http://localhost:8080/api/chatbot/chat', {
      type: 'KITCHEN_SORT',
      message: JSON.stringify(dishListText)
    });
    aiResponse.value = res.data.reply;
  } catch (error) {
    aiResponse.value = "Xin lỗi, AI đang bận nấu cơm rùi!";
  } finally {
    aiLoading.value = false;
  }
};

const handleLogout = () => { localStorage.removeItem('token'); localStorage.removeItem('user'); router.push('/login'); };

// === WEBSOCKET ===
const connectWebSocket = () => {
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {}; // Tắt log debug
  stompClient.connect({}, (frame) => {
    stompClient.subscribe('/topic/kitchen', (message) => {
      if (message.body === 'NEW_ORDER') {
        fetchOrders();
      }
    });
  });
};

const disconnectWebSocket = () => {
  if (stompClient) stompClient.disconnect();
};

// === LIFECYCLE ===
onMounted(() => {
  fetchOrders();
  fetchIngredients();
  fetchProducts();
  connectWebSocket();
  timerInterval = setInterval(() => { now.value = new Date(); }, 1000);
});

onUnmounted(() => {
  disconnectWebSocket();
  if (timerInterval) clearInterval(timerInterval);
});
</script>

<style scoped>
.kitchen-wrapper { background: var(--bg-root); min-height: 100vh; font-family: 'Outfit', -apple-system, sans-serif; }

/* Header */
.kitchen-header {
  background: rgba(13, 27, 42, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 14px 40px; display: flex; justify-content: space-between; align-items: center;
  position: sticky; top: 0; z-index: 100;
  flex-wrap: wrap; gap: 10px;
}
.header-left { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.header-left h1 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.header-sub { margin: 2px 0 0 0; font-size: 0.85rem; color: var(--text-muted); }
.header-sub strong { color: var(--primary); }
.header-right { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }

.tab-btn {
  background: transparent; border: 1px solid var(--border); color: var(--text-muted);
  padding: 8px 16px; border-radius: 20px; cursor: pointer; font-weight: 600;
  font-size: 0.83rem; font-family: inherit; transition: 0.3s; position: relative;
}
.tab-btn.active { border-color: var(--primary); color: var(--primary); background: var(--primary-glow); }
.tab-btn:hover { border-color: var(--primary); color: var(--primary); }
.badge-warn {
  position: absolute; top: -6px; right: -6px;
  background: #e74c3c; color: white; font-size: 0.65rem; font-weight: 800;
  width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
}

.btn-refresh, .btn-logout {
  background: transparent; border: 1px solid var(--border); color: var(--text-secondary);
  padding: 8px 14px; border-radius: 20px; cursor: pointer; font-weight: 600;
  font-size: 0.83rem; font-family: inherit; transition: 0.3s;
}
.btn-refresh:hover { border-color: var(--primary); color: var(--primary); }
.btn-logout:hover { border-color: #e74c3c; color: #e74c3c; }

/* Stats Bar */
.stats-bar { display: flex; justify-content: center; gap: 6px; padding: 14px 24px; background: rgba(6,13,26,0.8); border-bottom: 1px solid var(--border-light); flex-wrap: wrap; }
.stat-item { display: flex; flex-direction: column; align-items: center; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 12px; padding: 10px 18px; min-width: 110px; }
.stat-item.stat-warn { border-color: rgba(231,76,60,0.3); }
.stat-value { font-size: 1.5rem; font-weight: 900; color: var(--text-heading); }
.stat-value.urgent { color: #f39c12; }
.stat-value.done { color: var(--primary); }
.stat-label { font-size: 0.65rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-top: 4px; font-weight: 600; text-align: center; }

/* Main */
.kitchen-main { padding: 24px; max-width: 1400px; margin: 0 auto; }
.empty-state { text-align: center; padding: 60px 20px; background: var(--bg-card); border: 1px dashed var(--border); border-radius: 16px; margin-top: 20px; }
.empty-icon { font-size: 4rem; margin-bottom: 16px; }
.empty-state h2 { color: var(--primary); margin: 0 0 8px 0; font-weight: 800; }
.empty-state p { color: var(--text-muted); margin: 0; }

/* Orders Grid */
.orders-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }
.order-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 16px; overflow: hidden; transition: 0.3s; border-left: 4px solid #f39c12; }
.order-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); }
.order-card.urgency-warning { border-left-color: #e67e22; box-shadow: 0 0 15px rgba(230,126,34,0.15); }
.order-card.urgency-critical { border-left-color: #e74c3c; animation: pulse-urgent 1.5s ease-in-out infinite; }
@keyframes pulse-urgent { 0%,100%{box-shadow:0 0 20px rgba(231,76,60,0.2)}50%{box-shadow:0 0 30px rgba(231,76,60,0.4)} }

.card-header { padding: 14px 18px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-light); background: rgba(0,0,0,0.15); }
.order-id { font-weight: 900; color: var(--primary); font-size: 1rem; }
.order-table { font-weight: 700; color: var(--text-heading); font-size: 0.88rem; }
.order-timer { font-size: 0.85rem; font-weight: 800; padding: 4px 10px; border-radius: 20px; font-family: 'Courier New', monospace; }
.timer-normal { background: rgba(0,212,170,0.15); color: var(--primary); }
.timer-warning { background: rgba(243,156,18,0.2); color: #f39c12; }
.timer-critical { background: rgba(231,76,60,0.2); color: #e74c3c; animation: blink-timer 0.8s ease-in-out infinite; }
@keyframes blink-timer { 0%,100%{opacity:1}50%{opacity:0.5} }

.order-note { padding: 8px 18px; background: rgba(155,89,182,0.1); border-bottom: 1px solid var(--border-light); color: #9b59b6; font-weight: 600; font-size: 0.85rem; }
.dish-list { padding: 12px 18px; }
.dish-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.dish-item:last-child { border-bottom: none; }
.dish-thumb { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border); }
.dish-thumb-placeholder { font-size: 1.5rem; width: 40px; text-align: center; }
.dish-info { flex: 1; }
.dish-info strong { font-size: 0.9rem; color: var(--text-heading); }
.dish-qty { margin-left: 8px; background: rgba(0,212,170,0.15); color: var(--primary); padding: 2px 8px; border-radius: 10px; font-size: 0.78rem; font-weight: 700; }
.card-footer { padding: 12px 18px; display: flex; align-items: center; justify-content: space-between; border-top: 1px solid var(--border-light); background: rgba(0,0,0,0.08); }
.dish-count { font-size: 0.82rem; font-weight: 700; color: var(--text-muted); background: var(--bg-card); padding: 4px 12px; border-radius: 20px; border: 1px solid var(--border-light); }
.btn-done { padding: 10px 15px; background: linear-gradient(135deg, var(--primary), var(--primary-dark)); color: var(--bg-dark); border: none; border-radius: 10px; font-weight: 800; font-size: 0.85rem; cursor: pointer; font-family: inherit; transition: 0.3s; }
.btn-done:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }
.btn-start { padding: 10px 15px; background: rgba(243,156,18,0.15); color: #f39c12; border: 1px solid rgba(243,156,18,0.3); border-radius: 10px; font-weight: 800; font-size: 0.85rem; cursor: pointer; transition: 0.3s; font-family: inherit; }
.btn-start:hover { background: #f39c12; color: white; }
.order-card.cooking { border-left-color: #f39c12; box-shadow: 0 0 15px rgba(243,156,18,0.15); animation: pulse-cooking 2s infinite; }
@keyframes pulse-cooking { 0%,100%{box-shadow:0 0 10px rgba(243,156,18,0.15)}50%{box-shadow:0 0 25px rgba(243,156,18,0.3)} }

/* ========== INVENTORY TAB ========== */
.inv-header { margin-bottom: 24px; }
.inv-header h2 { color: var(--text-heading); font-size: 1.4rem; font-weight: 800; margin: 0 0 6px 0; }
.inv-sub { color: var(--text-muted); font-size: 0.9rem; margin: 0; }

.inv-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.inv-card {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 14px;
  padding: 18px; transition: 0.3s; position: relative; overflow: hidden;
}
.inv-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
.inv-card-low { border-color: rgba(243,156,18,0.3); }
.inv-card-out { border-color: rgba(231,76,60,0.3); background: rgba(231,76,60,0.03); }

.inv-card-top { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.inv-icon { font-size: 1.8rem; }
.inv-info h4 { margin: 0; color: var(--text-heading); font-size: 1rem; font-weight: 700; }
.inv-unit { font-size: 0.78rem; color: var(--text-muted); }

.inv-card-bottom { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.inv-qty { display: flex; align-items: baseline; gap: 4px; }
.inv-qty-value { font-size: 1.6rem; font-weight: 900; color: var(--text-heading); }
.inv-qty-unit { font-size: 0.8rem; color: var(--text-muted); }
.inv-badge { padding: 3px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
.inv-ok { background: rgba(0,212,170,0.15); color: var(--primary); }
.inv-low { background: rgba(243,156,18,0.15); color: #f39c12; }
.inv-out { background: rgba(231,76,60,0.15); color: #e74c3c; }

.inv-bar-wrap { height: 4px; background: var(--bg-input); border-radius: 4px; overflow: hidden; }
.inv-bar { height: 100%; border-radius: 4px; transition: width 0.5s ease; }
.bar-ok { background: var(--primary); }
.bar-low { background: #f39c12; }
.bar-out { background: #e74c3c; }

/* ========== MENU TAB ========== */
.menu-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 14px; }
.menu-card {
  display: flex; align-items: center; gap: 14px;
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 14px;
  padding: 14px 18px; transition: 0.3s;
}
.menu-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); }
.menu-card.menu-disabled { opacity: 0.55; border-color: rgba(231,76,60,0.2); }
.menu-img { width: 56px; height: 56px; border-radius: 10px; object-fit: cover; border: 1px solid var(--border); flex-shrink: 0; }
.menu-info { flex: 1; }
.menu-info h4 { margin: 0 0 4px 0; color: var(--text-heading); font-size: 0.95rem; }
.menu-price { color: var(--primary); font-weight: 700; font-size: 0.88rem; }
.menu-cat { margin-left: 8px; color: var(--text-muted); font-size: 0.78rem; }
.menu-action { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; flex-shrink: 0; }
.menu-status { font-size: 0.78rem; font-weight: 700; }
.status-on { color: var(--primary); }
.status-off { color: #e74c3c; }
.btn-toggle-menu { padding: 6px 14px; border-radius: 20px; border: 1px solid; font-size: 0.78rem; font-weight: 700; cursor: pointer; transition: 0.3s; font-family: inherit; }
.btn-off { background: rgba(231,76,60,0.1); color: #e74c3c; border-color: rgba(231,76,60,0.3); }
.btn-off:hover { background: #e74c3c; color: white; }
.btn-on { background: rgba(0,212,170,0.1); color: var(--primary); border-color: rgba(0,212,170,0.3); }
.btn-on:hover { background: var(--primary); color: var(--bg-dark); }

/* Toast */
.toast-notification { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--bg-card); color: var(--primary); padding: 14px 28px; border-radius: 30px; border: 1px solid var(--primary); box-shadow: 0 0 30px rgba(0,212,170,0.3); font-weight: 700; z-index: 1000; animation: slideUp 0.3s ease; }
@keyframes slideUp { from{transform:translate(-50%,20px);opacity:0}to{transform:translate(-50%,0);opacity:1} }

/* Responsive */
@media (max-width: 1024px) {
  .orders-grid, .inv-grid, .menu-grid { grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); }
  .stats-bar { gap: 4px; padding: 10px 16px; }
  .stat-item { min-width: 90px; padding: 8px 12px; }
}
@media (max-width: 768px) {
  .kitchen-header { padding: 10px 14px; }
  .header-left h1 { font-size: 1rem; }
  .orders-grid, .inv-grid, .menu-grid { grid-template-columns: 1fr; }
  .stats-bar { gap: 4px; padding: 8px; }
  .stat-item { min-width: 70px; padding: 6px 8px; flex: 1; }
  .stat-value { font-size: 1.1rem; }
  .stat-label { font-size: 0.55rem; }
  .kitchen-main { padding: 14px; }
}

/* AI Elements */
.kitchen-ai-action { text-align: right; margin-bottom: 15px; }
.btn-ai-analyze {
  background: linear-gradient(135deg, #e67e22, #d35400);
  color: white; border: none; padding: 10px 20px; border-radius: 8px;
  font-weight: bold; cursor: pointer; transition: 0.3s;
  box-shadow: 0 4px 15px rgba(230, 126, 34, 0.4); font-family: inherit;
}
.btn-ai-analyze:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(230, 126, 34, 0.6); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(5px); }
.ai-modal { background: var(--bg-card); width: 450px; max-width: 90%; border-radius: 12px; padding: 20px; border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h2 { margin: 0; color: #e67e22; font-size: 1.2rem; font-weight: 800; }
.btn-close { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted); }
.ai-loading { text-align: center; padding: 30px; color: #e67e22; font-weight: bold; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(230, 126, 34, 0.2); border-top-color: #e67e22; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-result { padding: 20px; font-size: 1.05rem; line-height: 1.6; color: var(--text-primary); border-left: 4px solid #e67e22; background: rgba(230, 126, 34, 0.05); border-radius: 0 8px 8px 0; white-space: pre-line; font-weight: 600; }

/* SLA Warning CSS */
.late-warning-box {
  animation: blinkRedBox 1s infinite alternate;
}
@keyframes blinkRedBox {
  from { border-color: #e74c3c; box-shadow: 0 0 10px rgba(231,76,60,0.4); }
  to { border-color: #ff0000; box-shadow: 0 0 20px rgba(255,0,0,0.8); }
}

.late-warning-text {
  animation: blinkRedText 1s infinite alternate;
}
@keyframes blinkRedText {
  from { color: #ff0000; text-shadow: 0 0 5px rgba(255,0,0,0.5); }
  to { color: #ffffff; background: #ff0000; }
}
</style>
