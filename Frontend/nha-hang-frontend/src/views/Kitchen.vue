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
        <button @click="activeTab = 'menu'" :class="['tab-btn', { active: activeTab === 'menu' }]">🍽️ Thực Đơn</button>
        <button @click="activeTab = 'ai-kitchen'" :class="['tab-btn', { active: activeTab === 'ai-kitchen' }]">🤖 Gom Món (AI)</button>
        <button @click="$router.push('/profile')" class="btn-profile">👤 Cá Nhân</button>
        <button @click="fetchOrders" class="btn-refresh">🔄</button>
        <button @click="handleLogout" class="btn-logout">🚪 Đăng Xuất</button>
      </div>
    </header>

    <TimekeepingWidget />

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

    <div v-if="isLoading" class="kitchen-main empty-state">
      <div class="empty-icon">⏳</div>
      <h2>Đang tải dữ liệu bếp...</h2>
    </div>

    <main v-else class="kitchen-main">
      <div v-if="loadError" class="empty-state">
        <div class="empty-icon">⚠️</div>
        <h2>Không thể tải dữ liệu bếp</h2>
        <p>{{ loadError }}</p>
        <button class="btn-refresh" @click="fetchOrders">Thử lại</button>
      </div>
      <!-- ========== TAB 1: ĐƠN HÀNG ========== -->
      <div v-if="activeTab === 'orders'">
        <div v-if="pendingOrders.length === 0" class="empty-state">
          <div class="empty-icon">✅</div>
          <h2>Không có đơn nào cần nấu!</h2>
          <p>Hệ thống sẽ tự động cập nhật khi có đơn mới.</p>
        </div>

        <div v-if="pendingOrders.length === 0" class="empty-state">
          <div class="empty-icon">✅</div>
          <h2>Không có đơn nào cần nấu!</h2>
          <p>Hệ thống sẽ tự động cập nhật khi có đơn mới.</p>
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
              <div v-for="detail in order.orderDetails" :key="detail.id" class="dish-item" :class="{ 'dish-done': detail.status >= 1 }">
                <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="dish-thumb" @error="replaceFoodImage" />
                <span v-else class="dish-thumb-placeholder">🍽️</span>
                <div class="dish-info" style="flex:1;">
                  <strong>{{ detail.product?.name || 'Món ăn' }}</strong>
                  <span class="dish-qty">x{{ detail.quantity }}</span>
                  <p v-if="detail.note" class="dish-note">Ghi chú: {{ detail.note }}</p>
                  <p v-if="detail.allergyNote" class="dish-allergy">Cảnh báo dị ứng: {{ detail.allergyNote }}</p>
                </div>
                <div class="dish-action">
                  <button v-if="(!detail.status || detail.status === 0) && !detail.startedAt" @click="startDish(detail.id)" class="btn-dish-start" title="Bắt đầu chế biến món này">🔥</button>
                  <button v-else-if="!detail.status || detail.status === 0" @click="markDishReady(detail.id)" class="btn-dish-done" title="Xong món này">✅</button>
                  <button v-if="!detail.status || detail.status === 0" @click="cancelDish(detail.id)" class="btn-dish-cancel" title="Hủy món">✖</button>
                  <span v-else style="color: #2F8F5B; font-size:1.2rem; font-weight:bold;" title="Đã báo phục vụ bưng">✔️ Xong</span>
                </div>
              </div>
            </div>
            <div class="card-footer">
              <span class="dish-count">{{ getDishCount(order) }} món</span>
              <div style="display: flex; gap: 8px;">
                <button v-if="order.status === 1" @click="startCooking(order.id)" class="btn-start">🔥 Bắt đầu làm</button>
                <button
                  v-if="order.status === 6"
                  @click="markReady(order.id)"
                  class="btn-done"
                  :disabled="!canCompleteOrder(order)"
                  :title="completionTitle(order)"
                >{{ canCompleteOrder(order) ? '✅ Hoàn thành toàn bộ' : `⏳ Còn ${unfinishedDishCount(order)} món chưa xong` }}</button>
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

        <div v-if="expiringBatches.length > 0" class="expiring-alert">
          <h3 style="color: #B23B2E; margin-top: 0;">⚠️ Cảnh báo: Sắp hết hạn sử dụng</h3>
          <ul style="margin: 0; padding-left: 20px;">
            <li v-for="b in expiringBatches" :key="b.id">
              <strong>{{ b.ingredient.name }}</strong> - Còn <strong>{{ b.quantity }} {{ b.ingredient.unit }}</strong> 
              (Hết hạn: {{ new Date(b.expirationDate).toLocaleDateString('vi-VN') }})
            </li>
          </ul>
        </div>

        <div class="inv-grid" v-else-if="ingredients.length > 0">
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

      <!-- ========== TAB 3: THỰC ĐƠN & CHI PHÍ ========== -->
      <div v-if="activeTab === 'menu'">
        <div class="inv-header">
          <h2>🍽️ Quản Lý Thực Đơn & Chi Phí</h2>
          <p class="inv-sub">Xem chi phí nguyên liệu của món và quản lý trạng thái bán (Báo hết/Mở bán).</p>
        </div>

        <div class="menu-grid">
          <div v-for="product in products" :key="product.id" :class="['menu-card', { 'menu-disabled': !product.available }]">
            <img :src="foodImage(product.image)" class="menu-img" @error="replaceFoodImage" />
            <div class="menu-info">
              <h4>{{ product.name }}</h4>
              <span class="menu-price">Bán: {{ product.price?.toLocaleString() }}đ</span>
              <span class="menu-cost" v-if="product.costPrice > 0">Vốn: {{ product.costPrice?.toLocaleString() }}đ</span>
              <span class="menu-cost" v-else>Vốn: Chưa tính</span>
            </div>
            <div class="menu-action">
              <span :class="['menu-status', product.available ? 'status-on' : 'status-off']">
                {{ product.available ? '✅ Đang bán' : '❌ Hết món' }}
              </span>
              <button @click="viewRecipeDetails(product)" class="btn-toggle-menu" style="border-color: #5A6E45; color: #5A6E45; background: rgba(90, 110, 69, 0.1)">👁️ Công thức</button>
              <button @click="toggleAvailable(product)" :class="['btn-toggle-menu', product.available ? 'btn-off' : 'btn-on']">
                {{ product.available ? '⏸ Báo Hết' : '▶ Mở Bán' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== TAB 4: NẤU GOM MÓN (AI) ========== -->
      <div v-if="activeTab === 'ai-kitchen'">
        <div class="inv-header">
          <h2>🤖 Phân Tích Gom Món (AI)</h2>
          <p class="inv-sub">Tự động gộp các món giống nhau từ nhiều bàn để nấu chung 1 mẻ, tiết kiệm thời gian.</p>
        </div>
        
        <div style="margin-bottom: 20px;">
          <button @click="analyzeDishes" class="btn-ai-analyze" :disabled="aiLoading">
            {{ aiLoading ? '⏳ Đang phân tích...' : '🧠 Phân Tích Ngay' }}
          </button>
        </div>
        
        <div v-if="aiResponse" class="ai-result" style="margin-bottom: 20px; font-size: 1.1rem; background: #FFFFFF; padding: 20px; border-radius: 8px; border-left: 5px solid #8A641F; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
          <strong style="color: #8A641F;">💡 AI Bếp Trưởng Gợi Ý:</strong>
          <pre style="white-space: pre-wrap; font-family: inherit; margin-top:10px; line-height: 1.5;">{{ aiResponse }}</pre>
        </div>

        <div class="menu-grid">
          <div v-for="(group, key) in aggregatedDishes" :key="key" class="menu-card" style="position:relative; flex-direction: column; text-align:center; align-items: stretch; padding: 20px;">
            <div class="menu-info" style="text-align: center; width: 100%;">
              <h3 style="font-size: 1.4rem; color: #8A641F;">{{ group.name }}</h3>
              <div style="font-size: 2rem; font-weight: bold; margin: 10px 0; color: var(--primary);">Tổng số lượng: {{ group.totalQuantity }}</div>
              <p style="color: #7A7460; font-size: 0.95rem; margin-bottom: 10px;">Gộp từ các bàn: <strong>{{ group.tables.join(', ') }}</strong></p>
            </div>
            <div class="menu-action" style="justify-content: center; width: 100%; border-top: 1px dashed var(--border-light); padding-top: 15px;">
              <button @click="markGroupReady(group.details)" class="btn-done" style="width: 100%; font-size: 1.1rem; padding: 12px; background: #2F8F5B; color: #FFFFFF;">✅ Đã Nấu Xong {{ group.totalQuantity }} Phần</button>
            </div>
          </div>
        </div>
        <div v-if="Object.keys(aggregatedDishes).length === 0" class="empty-state">
          <p>Tất cả các món đã được nấu xong!</p>
        </div>
      </div>
    </main>

    <!-- Recipe Breakdown Modal -->
    <div v-if="showRecipeModal" class="modal-overlay" @click.self="showRecipeModal = false">
      <div class="ai-modal" style="width: 600px;">
        <div class="modal-header">
          <h2>🍳 Chi phí nguyên liệu: {{ selectedProductForRecipe?.name }}</h2>
          <button @click="showRecipeModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body" style="max-height: 60vh; overflow-y: auto; padding: 0 20px 20px 20px;">
          <table class="g-table" style="width: 100%; border-collapse: collapse; margin-top: 10px;">
            <thead>
              <tr style="border-bottom: 1px solid var(--border-light); text-align: left;">
                <th style="padding: 10px;">Nguyên liệu</th>
                <th style="padding: 10px;">Định lượng</th>
                <th style="padding: 10px;">Đơn giá nhập</th>
                <th style="padding: 10px;">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rec in currentProductRecipes" :key="rec.id" style="border-bottom: 1px solid var(--border-light);">
                <td style="padding: 10px;"><strong>{{ rec.ingredient?.name }}</strong></td>
                <td style="padding: 10px;">{{ rec.amountRequired }} {{ rec.ingredient?.unit }}</td>
                <td style="padding: 10px;">{{ (rec.ingredient?.unitPrice || 0).toLocaleString() }}đ</td>
                <td style="padding: 10px; color: #B23B2E; font-weight: bold;">
                  {{ ((rec.ingredient?.unitPrice || 0) * rec.amountRequired).toLocaleString() }}đ
                </td>
              </tr>
              <tr v-if="currentProductRecipes.length === 0">
                <td colspan="4" style="text-align: center; padding: 20px; color: var(--text-muted)">Chưa có công thức cho món này.</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3" style="padding: 15px 10px; text-align: right; font-weight: bold; font-size: 1.1rem;">Tổng chi phí vốn:</td>
                <td style="padding: 15px 10px; color: #B23B2E; font-weight: 900; font-size: 1.2rem;">
                  {{ selectedProductForRecipe?.costPrice?.toLocaleString() || 0 }}đ
                </td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>
    </div>

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
    <StaffOperationsAssistant />
  </div>
</template>

<script setup>
import StaffOperationsAssistant from '@/components/StaffOperationsAssistant.vue'
import { ref, computed, onMounted, onUnmounted } from 'vue';
import api from '@/services/api';
import { useRouter } from 'vue-router';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import TimekeepingWidget from '../components/TimekeepingWidget.vue';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';

const router = useRouter();
const orders = ref([]);
const pendingOrders = ref([]);
const allOrders = ref([]);
const ingredients = ref([]);
const expiringBatches = ref([]);
const products = ref([]);
const isLoading = ref(true);
const loadError = ref('');
const toastMsg = ref('');
const now = ref(new Date());
const activeTab = ref('orders');
let timerInterval = null;
let syncInterval = null;
let previousPendingIds = [];
let stompClient = null;

const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

// Recipe Modal state
const showRecipeModal = ref(false);
const selectedProductForRecipe = ref(null);
const currentProductRecipes = ref([]);

const getToken = () => localStorage.getItem('staff_token');
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
    loadError.value = '';
    const res = await api.get('/api/admin/orders', configHeader());
    const normalizedOrders = Array.isArray(res.data)
      ? res.data.map(order => ({ ...order, orderDetails: Array.isArray(order.orderDetails) ? order.orderDetails : [] }))
      : [];
    allOrders.value = normalizedOrders;
    const newPending = normalizedOrders.filter(o => o.status === 1 || o.status === 6);
    const newIds = newPending.map(o => o.id);
    const hasNewOrder = newIds.some(id => !previousPendingIds.includes(id));
    if (hasNewOrder && previousPendingIds.length > 0) {
      playNotificationSound();
      toastMsg.value = '🔔 Có đơn mới từ khách!';
      setTimeout(() => { toastMsg.value = ''; }, 3000);
    }
    previousPendingIds = newIds;
    orders.value = normalizedOrders;
    pendingOrders.value = newPending;
  } catch (err) {
    console.error('Kitchen orders request failed:', err);
    loadError.value = err.response?.data?.message || 'Không thể tải dữ liệu bếp. Vui lòng thử lại.';
    allOrders.value = [];
    orders.value = [];
    pendingOrders.value = [];
  } finally {
    isLoading.value = false;
  }
};

const fetchIngredients = async () => {
  try {
    const res = await api.get('/api/admin/ingredients', configHeader());
    ingredients.value = res.data;
    
    // Lấy lô hàng sắp hết hạn
    const resExp = await api.get('/api/admin/ingredients/expiring-batches', configHeader());
    expiringBatches.value = resExp.data;
  } catch (err) { console.error('Lỗi lấy nguyên liệu:', err); }
};

const fetchProducts = async () => {
  try {
    const res = await api.get('/api/admin/products', configHeader());
products.value = res.data;
  } catch (err) { console.error('Lỗi lấy sản phẩm:', err); }
};

// === COMPUTED ===
const totalDishes = computed(() => {
  return pendingOrders.value.reduce((total, order) => {
    return total + (order.orderDetails || []).filter(d => !d.status || d.status === 0)
      .reduce((sum, d) => sum + (d.quantity || 0), 0);
  }, 0);
});
const sortedOrders = computed(() => [...pendingOrders.value].sort((a, b) => new Date(a.createDate) - new Date(b.createDate)));
const lowStockCount = computed(() => ingredients.value.filter(i => i.quantity <= i.minStock).length);

const todayOrders = computed(() => {
  const today = new Date().toDateString();
  return allOrders.value.filter(o => o.createDate && new Date(o.createDate).toDateString() === today);
});
const todayCompleted = computed(() => todayOrders.value.filter(o => o.status >= 2).length);
const todayDishes = computed(() => todayOrders.value.filter(o => o.status >= 2).reduce((sum, o) => sum + (o.orderDetails?.reduce((s, d) => s + d.quantity, 0) || 0), 0));

// === HELPERS ===
const showToast = (msg) => { toastMsg.value = msg; setTimeout(() => { toastMsg.value = ''; }, 3000); };
const getDishCount = (order) => order.orderDetails?.reduce((s, d) => s + d.quantity, 0) || 0;
const unfinishedDishCount = (order) => (order.orderDetails || [])
  .filter(detail => Number(detail.status) < 1 || detail.status == null)
  .length;
const canCompleteOrder = (order) => unfinishedDishCount(order) === 0;
const completionTitle = (order) => canCompleteOrder(order)
  ? 'Báo phục vụ khi tất cả món đã nấu xong'
  : `Còn ${unfinishedDishCount(order)} món chưa nấu xong`;
const apiErrorMessage = (err, fallback) => {
  const body = err.response?.data;
  const message = typeof body === 'string' ? body : body?.message;
  return message || fallback;
};

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
  const match = order.address.match(/Bàn:\s*(.*?)\s*\|/);
  return match ? `🪑 ${match[1].trim()}` : order.address;
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
    const token = localStorage.getItem('staff_token');
    await api.put(`/api/admin/orders/${id}/status?status=2`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    fetchOrders();
    showToast('✅ Đã báo phục vụ: Bàn #' + id);
  } catch (err) {
    alert(apiErrorMessage(err, 'Không thể hoàn thành đơn lúc này.'));
  }
};

const markDishReady = async (detailId) => {
  try {
    const token = localStorage.getItem('staff_token');
    await api.put(`/api/orders/details/${detailId}/kitchen/complete`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    fetchOrders();
    showToast('✅ Món đã xong, báo phục vụ bưng!');
  } catch (err) {
    alert(apiErrorMessage(err, 'Không thể cập nhật món lúc này.'));
  }
};

const startDish = async (detailId) => {
  try {
    await api.put(`/api/orders/details/${detailId}/kitchen/start`, {}, configHeader());
    showToast('🔥 Đã bắt đầu chế biến món.');
    fetchOrders();
  } catch (err) {
    alert(err.response?.data?.message || 'Không thể bắt đầu chế biến món.');
  }
};

const cancelDish = async (detailId) => {
  const reason = window.prompt('Nhập lý do hủy món:');
  if (!reason?.trim()) return;
  try {
    await api.put(`/api/orders/details/${detailId}/kitchen/cancel`, { reason: reason.trim() }, configHeader());
    showToast('Đã hủy món và báo phục vụ.');
    fetchOrders();
  } catch (err) {
    alert(err.response?.data?.message || 'Không thể hủy món.');
  }
};

const aggregatedDishes = computed(() => {
  const groups = {};
  pendingOrders.value.forEach(order => {
    (order.orderDetails || []).forEach(detail => {
      if (!detail.status || detail.status === 0) {
        const prodName = detail.product?.name || 'Món ăn';
        if (!groups[prodName]) {
          groups[prodName] = { name: prodName, totalQuantity: 0, tables: [], details: [] };
        }
        groups[prodName].totalQuantity += detail.quantity;
        groups[prodName].details.push(detail);
        const tName = order.address ? order.address.replace('Bàn ', '').replace(' [TẠI QUÁN]', '') : order.id;
        if (!groups[prodName].tables.includes(tName)) {
          groups[prodName].tables.push(tName);
        }
      }
    });
  });
  return groups;
});

const analyzeDishes = async () => {
  const dishes = Object.values(aggregatedDishes.value).map(g => `${g.name} (SL: ${g.totalQuantity})`).join(', ');
  if (!dishes) {
    alert('Không có món nào chờ nấu!');
    return;
  }
  
  aiLoading.value = true;
  try {
    const token = localStorage.getItem('staff_token');
    const res = await api.post('/api/staff/ai/kitchen', { dishes }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    aiResponse.value = res.data.reply;
  } catch (err) {
    aiResponse.value = 'Lỗi kết nối AI!';
  } finally {
    aiLoading.value = false;
  }
};

const markGroupReady = async (details) => {
  if (!confirm(`Xác nhận đã nấu xong tất cả ${details.length} món đang chờ?`)) return;
  try {
    for (const detail of details) {
      if (!detail.startedAt) {
        await api.put(`/api/orders/details/${detail.id}/kitchen/start`, {}, configHeader());
      }
      await api.put(`/api/orders/details/${detail.id}/kitchen/complete`, {}, configHeader());
    }
    fetchOrders();
    aiResponse.value = '';
    showToast('✅ Đã báo phục vụ bưng các món gộp!');
  } catch (err) {
    alert('Lỗi cập nhật!');
  }
};

const startCooking = async (id) => {
  try {
    await api.put(`/api/admin/orders/${id}/status?status=6`, {}, configHeader());
    toastMsg.value = '🔥 Đang nấu...';
    setTimeout(() => { toastMsg.value = ''; }, 2500);
    fetchOrders();
  } catch (err) { alert('Lỗi cập nhật trạng thái!'); }
};

const toggleAvailable = async (product) => {
  const originalState = product.available;
  product.available = !product.available;
  try {
    const res = await api.put(`/api/admin/products/${product.id}/toggle-available`, {}, configHeader());
    toastMsg.value = typeof res.data === 'string' ? res.data : '✅ Đã cập nhật!';
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchProducts();
    } catch (error) {
      alert("Lỗi khi cập nhật trạng thái món ăn!");
      product.available = originalState; // revert if error
    }
};

const viewRecipeDetails = async (product) => {
  selectedProductForRecipe.value = product;
  try {
    const res = await api.get(`/api/admin/recipes/product/${product.id}`, configHeader());
    currentProductRecipes.value = res.data;
    showRecipeModal.value = true;
  } catch (err) {
    alert("Không thể tải công thức món ăn!");
  }
};

const handleLogout = () => { localStorage.removeItem('staff_token'); localStorage.removeItem('staff_user'); router.push('/staff-login'); };

// === WEBSOCKET ===
const connectWebSocket = () => {
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {}; // Tắt log debug
  const token = localStorage.getItem('staff_token');
  stompClient.connect(token ? { Authorization: `Bearer ${token}` } : {}, () => {
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
  // Refresh from SQL Server when Kitchen runs on a separate local port.
  syncInterval = setInterval(() => {
    fetchOrders();
    fetchIngredients();
    fetchProducts();
  }, 5000);
});

onUnmounted(() => {
  disconnectWebSocket();
  if (timerInterval) clearInterval(timerInterval);
  if (syncInterval) clearInterval(syncInterval);
});
</script>

<style scoped>
.kitchen-wrapper { background: var(--bg-root); min-height: 100vh; font-family: var(--font-primary); }

/* Header */
.kitchen-header {
  background: rgba(255, 255, 255, 0.72);
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
  background: #B23B2E; color: #FFFFFF; font-size: 0.65rem; font-weight: 800;
  width: 20px; height: 20px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
}

.btn-refresh, .btn-logout {
  background: transparent; border: 1px solid var(--border); color: var(--text-secondary);
  padding: 8px 14px; border-radius: 20px; cursor: pointer; font-weight: 600;
  font-size: 0.83rem; font-family: inherit; transition: 0.3s;
}
.btn-refresh:hover { border-color: var(--primary); color: var(--primary); }
.btn-logout:hover { border-color: #B23B2E; color: #B23B2E; }

/* Stats Bar */
.stats-bar { display: flex; justify-content: center; gap: 6px; padding: 14px 24px; background: rgba(43, 36, 32, 0.18); border-bottom: 1px solid var(--border-light); flex-wrap: wrap; }
.stat-item { display: flex; flex-direction: column; align-items: center; background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 12px; padding: 10px 18px; min-width: 110px; }
.stat-item.stat-warn { border-color: rgba(178,59,46,0.3); }
.stat-value { font-size: 1.5rem; font-weight: 900; color: var(--text-heading); }
.stat-value.urgent { color: #B98229; }
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
.order-card { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 16px; overflow: hidden; transition: 0.3s; border-left: 4px solid #B98229; }
.order-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); }
.order-card.urgency-warning { border-left-color: #C08A2E; box-shadow: 0 0 15px rgba(192, 138, 46, 0.15); }
.order-card.urgency-critical { border-left-color: #B23B2E; animation: pulse-urgent 1.5s ease-in-out infinite; }
@keyframes pulse-urgent { 0%,100%{box-shadow:0 0 20px rgba(178,59,46,0.2)}50%{box-shadow:0 0 30px rgba(178,59,46,0.4)} }

.card-header { padding: 14px 18px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border-light); background: rgba(0,0,0,0.15); }
.order-id { font-weight: 900; color: var(--primary); font-size: 1rem; }
.order-table { font-weight: 700; color: var(--text-heading); font-size: 0.88rem; }
.order-timer { font-size: 0.85rem; font-weight: 800; padding: 4px 10px; border-radius: 20px; font-family: var(--font-primary); }
.timer-normal { background: rgba(90, 110, 69, 0.15); color: var(--primary); }
.timer-warning { background: rgba(185,130,41,0.2); color: #B98229; }
.timer-critical { background: rgba(178,59,46,0.2); color: #B23B2E; animation: blink-timer 0.8s ease-in-out infinite; }
@keyframes blink-timer { 0%,100%{opacity:1}50%{opacity:0.5} }

.order-note { padding: 8px 18px; background: rgba(192, 138, 46, 0.1); border-bottom: 1px solid var(--border-light); color: #C08A2E; font-weight: 600; font-size: 0.85rem; }
.dish-list { padding: 12px 18px; }
.dish-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--border-light); }
.dish-item:last-child { border-bottom: none; }
.dish-item.dish-done {
  opacity: 0.6;
  background: #F3F7F0;
  border-color: #2F8F5B;
}
.dish-action {
  display: flex;
  align-items: center;
  gap: 6px;
}
.btn-dish-done, .btn-dish-start, .btn-dish-cancel {
  background: transparent;
  border: 1px solid #A6B0AA;
  border-radius: 4px;
  cursor: pointer;
  padding: 4px 8px;
  transition: 0.2s;
}
.btn-dish-done:hover {
  background: #2F8F5B;
  border-color: #2F8F5B;
}
.btn-dish-start:hover { background: #B98229; border-color: #B98229; }
.btn-dish-cancel { color: #9f2c20; }
.btn-dish-cancel:hover { color: #fff; background: #9f2c20; border-color: #9f2c20; }
.dish-thumb { width: 40px; height: 40px; border-radius: 8px; object-fit: cover; border: 1px solid var(--border); }
.dish-thumb-placeholder { font-size: 1.5rem; width: 40px; text-align: center; }
.dish-info { flex: 1; }
.dish-info strong { font-size: 0.9rem; color: var(--text-heading); }
.dish-qty { margin-left: 8px; background: rgba(90, 110, 69, 0.15); color: var(--primary); padding: 2px 8px; border-radius: 10px; font-size: 0.78rem; font-weight: 700; }
.dish-note, .dish-allergy { margin: 5px 0 0; font-size: 0.78rem; line-height: 1.35; }
.dish-note { color: var(--text-muted); }
.dish-allergy { color: #9f2c20; font-weight: 800; }
.card-footer { padding: 12px 18px; display: flex; align-items: center; justify-content: space-between; border-top: 1px solid var(--border-light); background: rgba(0,0,0,0.08); }
.dish-count { font-size: 0.82rem; font-weight: 700; color: var(--text-muted); background: var(--bg-card); padding: 4px 12px; border-radius: 20px; border: 1px solid var(--border-light); }
.btn-done { padding: 10px 15px; background: linear-gradient(135deg, var(--primary), var(--primary-dark)); color: var(--bg-dark); border: none; border-radius: 10px; font-weight: 800; font-size: 0.85rem; cursor: pointer; font-family: inherit; transition: 0.3s; }
.btn-done:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(90, 110, 69, 0.4); }
.btn-done:disabled { cursor: not-allowed; opacity: 0.55; transform: none; box-shadow: none; }
.btn-start { padding: 10px 15px; background: rgba(185,130,41,0.15); color: #B98229; border: 1px solid rgba(185,130,41,0.3); border-radius: 10px; font-weight: 800; font-size: 0.85rem; cursor: pointer; transition: 0.3s; font-family: inherit; }
.btn-start:hover { background: #B98229; color: #FFFFFF; }
.order-card.cooking { border-left-color: #B98229; box-shadow: 0 0 15px rgba(185,130,41,0.15); animation: pulse-cooking 2s infinite; }
@keyframes pulse-cooking { 0%,100%{box-shadow:0 0 10px rgba(185,130,41,0.15)}50%{box-shadow:0 0 25px rgba(185,130,41,0.3)} }

/* ========== INVENTORY TAB ========== */
.inv-header { margin-bottom: 24px; }
.inv-header h2 { color: var(--text-heading); font-size: 1.4rem; font-weight: 800; margin: 0 0 6px 0; }
.inv-sub { color: var(--text-muted); font-size: 0.9rem; margin: 0; }

.expiring-alert {
  background: rgba(178,59,46,0.1);
  border: 1px solid rgba(178,59,46,0.3);
  padding: 15px 20px;
  border-radius: 10px;
  margin-bottom: 20px;
  color: #B23B2E;
}
.expiring-alert li { margin-bottom: 5px; }

.inv-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.inv-card {
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 14px;
  padding: 18px; transition: 0.3s; position: relative; overflow: hidden;
}
.inv-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
.inv-card-low { border-color: rgba(185,130,41,0.3); }
.inv-card-out { border-color: rgba(178,59,46,0.3); background: rgba(178,59,46,0.03); }

.inv-card-top { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.inv-icon { font-size: 1.8rem; }
.inv-info h4 { margin: 0; color: var(--text-heading); font-size: 1rem; font-weight: 700; }
.inv-unit { font-size: 0.78rem; color: var(--text-muted); }

.inv-card-bottom { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.inv-qty { display: flex; align-items: baseline; gap: 4px; }
.inv-qty-value { font-size: 1.6rem; font-weight: 900; color: var(--text-heading); }
.inv-qty-unit { font-size: 0.8rem; color: var(--text-muted); }
.inv-badge { padding: 3px 10px; border-radius: 20px; font-size: 0.7rem; font-weight: 800; text-transform: uppercase; }
.inv-ok { background: rgba(90, 110, 69, 0.15); color: var(--primary); }
.inv-low { background: rgba(185,130,41,0.15); color: #B98229; }
.inv-out { background: rgba(178,59,46,0.15); color: #B23B2E; }

.inv-bar-wrap { height: 4px; background: var(--bg-input); border-radius: 4px; overflow: hidden; }
.inv-bar { height: 100%; border-radius: 4px; transition: width 0.5s ease; }
.bar-ok { background: var(--primary); }
.bar-low { background: #B98229; }
.bar-out { background: #B23B2E; }

/* ========== MENU TAB ========== */
.menu-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 14px; }
.menu-card {
  display: flex; align-items: center; gap: 14px;
  background: var(--bg-card); border: 1px solid var(--border-light); border-radius: 14px;
  padding: 14px 18px; transition: 0.3s;
}
.menu-card:hover { border-color: var(--primary); box-shadow: var(--shadow-md); }
.menu-card.menu-disabled { opacity: 0.55; border-color: rgba(178,59,46,0.2); }
.menu-img { width: 56px; height: 56px; border-radius: 10px; object-fit: cover; border: 1px solid var(--border); flex-shrink: 0; }
.menu-info { flex: 1; display: flex; flex-direction: column; }
.menu-info h4 { margin: 0 0 4px 0; color: var(--text-heading); font-size: 0.95rem; }
.menu-price { color: var(--primary); font-weight: 700; font-size: 0.88rem; }
.menu-cost { color: #B23B2E; font-weight: 700; font-size: 0.85rem; margin-top: 3px; }
.menu-cat { margin-left: 8px; color: var(--text-muted); font-size: 0.78rem; }
.menu-action { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; flex-shrink: 0; }
.menu-status { font-size: 0.78rem; font-weight: 700; }
.status-on { color: var(--primary); }
.status-off { color: #B23B2E; }
.btn-toggle-menu { padding: 6px 14px; border-radius: 20px; border: 1px solid; font-size: 0.78rem; font-weight: 700; cursor: pointer; transition: 0.3s; font-family: inherit; }
.btn-off { background: rgba(178,59,46,0.1); color: #B23B2E; border-color: rgba(178,59,46,0.3); }
.btn-off:hover { background: #B23B2E; color: #FFFFFF; }
.btn-on { background: rgba(90, 110, 69, 0.1); color: var(--primary); border-color: rgba(90, 110, 69, 0.3); }
.btn-on:hover { background: var(--primary); color: var(--bg-dark); }

/* Toast */
.toast-notification { position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); background: var(--bg-card); color: var(--primary); padding: 14px 28px; border-radius: 30px; border: 1px solid var(--primary); box-shadow: 0 0 30px rgba(90, 110, 69, 0.3); font-weight: 700; z-index: 1000; animation: slideUp 0.3s ease; }
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
  background: linear-gradient(135deg, #C08A2E, #8A641F);
  color: #FFFFFF; border: none; padding: 10px 20px; border-radius: 8px;
  font-weight: bold; cursor: pointer; transition: 0.3s;
  box-shadow: 0 4px 15px rgba(192, 138, 46, 0.4); font-family: inherit;
}
.btn-ai-analyze:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(192, 138, 46, 0.6); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.8); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(5px); }
.ai-modal { background: var(--bg-card); width: 450px; max-width: 90%; border-radius: 12px; padding: 20px; border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h2 { margin: 0; color: #C08A2E; font-size: 1.2rem; font-weight: 800; }
.btn-close { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted); }
.ai-loading { text-align: center; padding: 30px; color: #C08A2E; font-weight: bold; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(192, 138, 46, 0.2); border-top-color: #C08A2E; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-result { padding: 20px; font-size: 1.05rem; line-height: 1.6; color: var(--text-primary); border-left: 4px solid #C08A2E; background: rgba(192, 138, 46, 0.05); border-radius: 0 8px 8px 0; white-space: pre-line; font-weight: 600; }

/* SLA Warning CSS */
.late-warning-box {
  animation: blinkRedBox 1s infinite alternate;
}
@keyframes blinkRedBox {
  from { border-color: #B23B2E; box-shadow: 0 0 10px rgba(178,59,46,0.4); }
  to { border-color: #B23B2E; box-shadow: 0 0 20px rgba(255,0,0,0.8); }
}

.late-warning-text {
  animation: blinkRedText 1s infinite alternate;
}
@keyframes blinkRedText {
  from { color: #B23B2E; text-shadow: 0 0 5px rgba(255,0,0,0.5); }
  to { color: #FFFFFF; background: #B23B2E; }
}
</style>
