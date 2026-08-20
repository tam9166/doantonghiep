<template>
  <CustomerLayout>
    <div class="history-wrapper">


    <div class="history-container">
      <div style="margin-bottom: 20px;">
        <button @click="$router.back()" class="g-btn-outline" style="border-radius: 100px; padding: 8px 20px; border-color: rgba(255,255,255,0.2);">
          ← Quay Lại
        </button>
      </div>
      
      <!-- VIP CARD SECTION -->
      <div v-if="userProfile" class="vip-card-wrapper">
        <div :class="['vip-card', getTierClass(userProfile.membershipTier)]">
          <div class="vip-top">
            <span class="vip-logo">👑 Mộc Vị VIP</span>
            <span class="vip-tier">{{ userProfile.membershipTier }}</span>
          </div>
          <div class="vip-chip">💳</div>
          <div class="vip-number">{{ userProfile.username.toUpperCase() }} - {{ String(userProfile.points).padStart(4, '0') }} PT</div>
          <div class="vip-bottom">
            <div class="vip-name">{{ userProfile.fullname }}</div>
            <div class="vip-discount">Giảm: {{ getDiscountByTier(userProfile.membershipTier) }}%</div>
          </div>
        </div>
      </div>

    <h1>Lịch Sử Đặt Món</h1>
    
    <div v-if="orders.length > 0">
      <table class="history-table">
        <thead>
          <tr>
            <th>Mã Đơn</th>
            <th>Ngày Đặt</th>
            <th>Địa Chỉ</th>
            <th>Trạng Thái</th>
            <th>Hành Động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td>#{{ order.id }}</td>
            <td>{{ new Date(order.createDate).toLocaleString() }}</td>
            <td>{{ order.address }}</td>
            <td>
              <span :class="order.status === 0 ? 'status-pending' : (order.status === 4 ? 'status-done' : 'status-pending')">
                {{ order.status === 0 ? 'Đang xử lý' : (order.status === 4 ? 'Đã hoàn thành' : 'Đang thực hiện') }}
              </span>
              <small class="payment-summary">{{ paymentSummary(order) }}</small>
            </td>
            <td>
              <button v-if="order.isPaid" @click="selectedInvoice = order" class="btn-review">Xem hóa đơn</button>
              <button v-if="order.isPaid" @click="requestInvoice(order)" class="btn-review" :disabled="invoiceRequesting === order.id">
                {{ order.invoiceRequested ? 'Đã yêu cầu xuất hóa đơn' : invoiceRequesting === order.id ? 'Đang gửi yêu cầu...' : 'Yêu cầu xuất hóa đơn' }}
              </button>
              <button v-if="order.status === 4 && !reviewedOrders.includes(order.id)" @click="openReviewModal(order)" class="btn-review">⭐ Đánh giá (Nhận +2 Điểm)</button>
              <span v-if="order.status === 4 && reviewedOrders.includes(order.id)" class="text-success">✅ Đã đánh giá</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <section v-if="reservations.length > 0" class="reservation-history">
      <h2>Lịch sử đặt bàn</h2>
      <table class="history-table">
        <thead>
          <tr>
            <th>Mã đặt bàn</th>
            <th>Thời gian</th>
            <th>Bàn</th>
            <th>Món đặt trước</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="reservation in reservations" :key="reservation.id">
            <td>{{ reservation.reservationCode }}</td>
            <td>{{ reservation.reservationDate }} {{ reservation.arrivalTime }}</td>
            <td>{{ reservation.tableName || '-' }}</td>
            <td>{{ reservation.preorderItems?.length || 0 }} món</td>
            <td>{{ reservationStatusText(reservation.reservationStatus) }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <div v-if="selectedInvoice" class="modal-overlay" @click.self="selectedInvoice = null">
      <div class="review-modal invoice-modal">
        <h3>Hóa đơn #{{ selectedInvoice.id }}</h3>
        <p>{{ new Date(selectedInvoice.createDate).toLocaleString('vi-VN') }}</p>
        <div v-for="detail in selectedInvoice.orderDetails" :key="detail.id" class="invoice-row">
          <span>{{ detail.product?.name }} x{{ detail.quantity }}</span>
          <strong>{{ Number(detail.price || 0).toLocaleString('vi-VN') }} đ</strong>
        </div>
        <div class="invoice-row invoice-total-row">
          <span>Tổng thanh toán</span>
          <strong>{{ Number(selectedInvoice.totalAmount || 0).toLocaleString('vi-VN') }} đ</strong>
        </div>
        <p class="text-success">{{ paymentSummary(selectedInvoice) }}</p>
        <button @click="printInvoice" class="btn-submit-review">In hóa đơn</button>
        <button @click="selectedInvoice = null" class="btn-close-review">Đóng</button>
      </div>
    </div>
    <p v-if="orders.length === 0 && reservations.length === 0" class="no-order">Bạn chưa có đơn hàng hoặc đặt bàn nào.</p>
    </div>
    
    <!-- Modal Đánh Giá -->
    <div v-if="showReview" class="modal-overlay" @click.self="showReview = false">
      <div class="review-modal">
        <h3>Đánh giá món ăn - Đơn #{{ currentReviewOrder?.id }}</h3>
        <div class="review-list">
          <div v-for="detail in currentReviewOrder?.orderDetails" :key="detail.id" class="review-item">
            <p><strong>{{ detail.product.name }}</strong></p>
            <select v-model="reviewData[detail.product.id].rating" class="r-select">
              <option value="5">⭐⭐⭐⭐⭐ Tuyệt vời</option>
              <option value="4">⭐⭐⭐⭐ Tốt</option>
              <option value="3">⭐⭐⭐ Bình thường</option>
              <option value="2">⭐⭐ Tệ</option>
              <option value="1">⭐ Rất tệ</option>
            </select>
            <input v-model="reviewData[detail.product.id].comment" placeholder="Chia sẻ cảm nhận của bạn..." class="r-input" />
            <button @click="submitReview(detail.product.id)" class="btn-submit-review">Gửi</button>
          </div>
        </div>
        <button @click="closeReviewModal" class="btn-close-review">Đóng & Hoàn tất</button>
      </div>
    </div>
  </div>
  </CustomerLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '@/services/api';
import CustomerLayout from '@/components/CustomerLayout.vue';

const orders = ref([]);
const reservations = ref([]);
const showReview = ref(false);
const currentReviewOrder = ref(null);
const reviewData = ref({});
const reviewedOrders = ref(JSON.parse(localStorage.getItem('reviewedOrders')) || []);
const userProfile = ref(null);
const selectedInvoice = ref(null);
const invoiceRequesting = ref(null);

const paymentSummary = (order) => {
  if (order.isPaid || order.paymentStatus === 'PAID' || order.paymentStatus === 'OVERPAID') {
    return 'Đã thanh toán đủ 100%';
  }
  const paid = Number(order.paidAmount || order.deposit || 0);
  if (paid > 0) return `Đã thanh toán trước ${paid.toLocaleString('vi-VN')} đ`;
  return 'Chưa thanh toán';
};

const requestInvoice = async (order) => {
  if (invoiceRequesting.value || order.invoiceRequested) return;
  invoiceRequesting.value = order.id;
  try {
    const token = localStorage.getItem('token');
    const response = await api.post(`/api/orders/${order.id}/invoice-request`, {}, {
      headers: { Authorization: `Bearer ${token}` },
    });
    order.invoiceRequested = true;
    alert(response.data?.message || 'Đã ghi nhận yêu cầu xuất hóa đơn.');
  } catch (error) {
    alert(error.response?.data?.message || error.response?.data || 'Không thể gửi yêu cầu xuất hóa đơn.');
  } finally {
    invoiceRequesting.value = null;
  }
};

const printInvoice = () => {
  window.print();
};

const fetchHistory = async () => {
  const token = localStorage.getItem('token');
  if (!token) return;
  try {
    const res = await api.get('/api/orders/history', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    orders.value = res.data.sort((a, b) => b.id - a.id);
  } catch (error) {
    console.error("Lỗi lấy lịch sử", error);
  }
};

const fetchReservationHistory = async () => {
  const token = localStorage.getItem('token');
  if (!token) return;
  try {
    const res = await api.get('/api/reservations/history', {
      headers: { Authorization: `Bearer ${token}` },
    });
    reservations.value = res.data || [];
  } catch (error) {
    console.error('Không thể tải lịch sử đặt bàn', error);
  }
};

const reservationStatusText = (status) => ({
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  DEPOSIT_REQUIRED: 'Chờ đặt cọc',
  DEPOSIT_PAID: 'Đã đặt cọc',
  FULLY_PAID: 'Đã thanh toán',
  CHECKED_IN: 'Đã nhận bàn',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  REJECTED: 'Từ chối',
}[status] || status || '-');

const fetchProfile = async () => {
  const token = localStorage.getItem('token');
  if (!token) return;
  try {
    const res = await api.get('/api/auth/profile', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    userProfile.value = res.data;
  } catch (error) {
    console.error("Lỗi lấy profile", error);
  }
};

const getTierClass = (tier) => {
  if (tier === 'Kim Cương') return 'tier-diamond';
  if (tier === 'Vàng') return 'tier-gold';
  if (tier === 'Bạc') return 'tier-silver';
  return 'tier-bronze';
};

const getDiscountByTier = (tier) => {
  if (tier === 'Kim Cương') return 15;
  if (tier === 'Vàng') return 10;
  if (tier === 'Bạc') return 5;
  return 0;
};

const openReviewModal = (order) => {
  currentReviewOrder.value = order;
  reviewData.value = {};
  if (order.orderDetails) {
    order.orderDetails.forEach(d => {
      reviewData.value[d.product.id] = { rating: 5, comment: '' };
    });
  }
  showReview.value = true;
};

const submitReview = async (productId) => {
  const token = localStorage.getItem('token');
  const data = reviewData.value[productId];
  try {
    const res = await api.post(`/api/reviews/product/${productId}`, {
      rating: parseInt(data.rating),
      comment: data.comment
    }, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert(res.data.message || "Cảm ơn bạn đã đánh giá món ăn này!");
    fetchProfile(); // Cập nhật lại thẻ VIP
  } catch (err) {
    alert("Có lỗi khi gửi đánh giá.");
  }
};

const closeReviewModal = () => {
  if (currentReviewOrder.value) {
    reviewedOrders.value.push(currentReviewOrder.value.id);
    localStorage.setItem('reviewedOrders', JSON.stringify(reviewedOrders.value));
  }
  showReview.value = false;
};

onMounted(() => {
  fetchHistory();
  fetchReservationHistory();
  fetchProfile();
});
</script>

<style scoped>
.history-wrapper { background: var(--bg-root); min-height: 100vh; font-family: var(--font-primary); }
.reservation-history { margin-top: 28px; }
.reservation-history h2 { color: var(--text-heading); font-size: 1.25rem; margin-bottom: 12px; }

/* Navbar */
.history-navbar {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  position: sticky; top: 0; z-index: 100;
  padding: 12px 40px;
}
.nav-container { max-width: 1400px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 12px; cursor: pointer; text-decoration: none; }
.logo-icon { font-size: 2rem; filter: drop-shadow(0 0 10px var(--primary-glow)); }
.logo h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--text-heading); letter-spacing: 1px; }
.logo p { margin: 0; font-size: 0.7rem; color: var(--text-muted); letter-spacing: 3px; font-weight: 700; text-transform: uppercase; }

.nav-links { display: flex; gap: 6px; }
.nav-links a {
  text-decoration: none; color: var(--text-secondary);
  font-weight: 600; font-size: 0.95rem; padding: 10px 20px;
  border-radius: 100px; transition: var(--transition);
}
.nav-links a:hover, .nav-links a.router-link-active, .nav-links a.active {
  color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent);
}

.nav-right { display: flex; align-items: center; gap: 10px; }
.nav-badge {
  background: transparent; border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-secondary); padding: 10px 24px;
  border-radius: 100px; font-weight: 700; cursor: pointer; transition: var(--transition);
}
.nav-badge:hover { border-color: var(--primary); color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent); }

/* Content */
.history-container { padding: 40px 20px; max-width: 900px; margin: 0 auto; color: var(--text-primary); }
h1 { color: var(--primary); border-bottom: 1px solid var(--border); padding-bottom: 10px; margin-bottom: 20px; font-weight: 900; }

.history-table { width: 100%; border-collapse: collapse; background: var(--bg-card); box-shadow: var(--shadow-md); border-radius: 8px; overflow: hidden; border: 1px solid var(--border-light); }
.history-table th, .history-table td { padding: 15px; border-bottom: 1px solid var(--border); text-align: left; }
.history-table th { background-color: var(--bg-nav); color: var(--primary); font-weight: bold; }
.history-table tr:hover { background-color: rgba(255,255,255,0.02); }
.status-pending { color: var(--color-tertiary); font-weight: bold; background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); padding: 5px 10px; border-radius: 12px; font-size: 0.85rem; }
.status-done { color: var(--success); font-weight: bold; background: color-mix(in srgb, var(--success) 10%, transparent); padding: 5px 10px; border-radius: 12px; font-size: 0.85rem; }
.payment-summary { display: block; margin-top: 6px; color: var(--text-secondary); font-weight: 600; }
.no-order { text-align: center; color: var(--text-muted); margin-top: 50px; font-style: italic; background: var(--bg-card); padding: 30px; border-radius: 8px; border: 1px dashed var(--border); }
.btn-review { background: var(--primary); color: var(--color-on-primary); border: none; padding: 5px 10px; border-radius: 6px; font-weight: bold; cursor: pointer; transition: 0.3s; }
.btn-review:hover { background: var(--primary-glow); }
.text-success { color: var(--success); font-weight: bold; font-style: italic; font-size: 0.9rem; }

/* Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.review-modal { background: var(--bg-card); padding: 20px; border-radius: 10px; width: 400px; max-width: 90%; border: 1px solid var(--border); }
.review-modal h3 { color: var(--primary); margin-top: 0; border-bottom: 1px solid var(--border); padding-bottom: 10px; }
.review-list { display: flex; flex-direction: column; gap: 15px; margin-bottom: 20px; max-height: 60vh; overflow-y: auto; }
.review-item { background: var(--bg-input); padding: 10px; border-radius: 6px; border: 1px solid var(--border); }
.r-select { width: 100%; padding: 5px; margin-bottom: 5px; border-radius: 4px; border: 1px solid var(--border); background: var(--bg-card); color: #FFFFFF; }
.r-input { width: 100%; padding: 8px; margin-bottom: 5px; border-radius: 4px; border: 1px solid var(--border); background: var(--bg-card); color: #FFFFFF; box-sizing: border-box; }
.btn-submit-review { background: var(--success); color: #FFFFFF; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-weight: bold; width: 100%; }
.btn-close-review { background: transparent; color: #FFFFFF; border: 1px solid var(--border); padding: 8px; border-radius: 4px; cursor: pointer; width: 100%; }
.invoice-modal { color: var(--text-primary); }
.invoice-row { display: flex; justify-content: space-between; gap: 16px; padding: 9px 0; border-bottom: 1px dashed var(--border); }
.invoice-total-row { margin-top: 10px; border-top: 2px solid var(--border); font-size: 1.05rem; }

/* VIP CARD CSS */
.vip-card-wrapper { display: flex; justify-content: center; margin-bottom: 30px; }
.vip-card { width: 350px; height: 200px; border-radius: 16px; padding: 20px; display: flex; flex-direction: column; justify-content: space-between; box-shadow: 0 10px 30px rgba(0,0,0,0.3); position: relative; overflow: hidden; color: #FFFFFF; font-family: var(--font-primary); }
.vip-card::before { content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: linear-gradient(45deg, transparent, rgba(255,255,255,0.1), transparent); transform: rotate(45deg); pointer-events: none; }
.vip-top { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.vip-logo { font-family: var(--font-primary); letter-spacing: 1px; }
.vip-tier { padding: 4px 10px; background: rgba(0,0,0,0.3); border-radius: 12px; font-size: 0.8rem; }
.vip-chip { font-size: 2rem; }
.vip-number { font-size: 1.2rem; letter-spacing: 2px; text-shadow: 1px 1px 2px rgba(0,0,0,0.5); }
.vip-bottom { display: flex; justify-content: space-between; align-items: flex-end; }
.vip-name { font-size: 1.1rem; text-transform: uppercase; font-family: var(--font-primary); font-weight: bold; }
.vip-discount { font-size: 0.9rem; font-family: var(--font-primary); background: rgba(255,255,255,0.2); padding: 4px 8px; border-radius: 6px; }

.tier-bronze { background: linear-gradient(135deg, var(--color-tertiary), var(--warning)); }
.tier-silver { background: linear-gradient(135deg, var(--color-outline), var(--text-muted)); color: var(--text-primary); }
.tier-gold { background: linear-gradient(135deg, var(--color-tertiary), var(--warning)); }
.tier-diamond { background: linear-gradient(135deg, var(--tier-diamond), var(--secondary)); }
</style>
