<template>
  <AdminLayout>
    <div class="history-page">
      <div class="page-header">
        <div>
          <h1>Lịch Sử Khách Đặt Bàn</h1>
          <p>Theo dõi khách quay lại, tổng chi tiêu, số lần hủy và no-show theo số điện thoại.</p>
        </div>
        <button class="g-btn-primary" @click="fetchHistory">Tải lại</button>
      </div>

      <div class="toolbar">
        <input v-model.trim="keyword" placeholder="Tìm theo tên hoặc số điện thoại" />
      </div>

      <div class="history-layout">
        <section class="history-list">
          <article
            v-for="item in filteredHistory"
            :key="item.customerPhone"
            class="customer-card"
            :class="{ active: selectedPhone === item.customerPhone }"
            @click="selectCustomer(item)"
          >
            <div>
              <h3>{{ item.latestCustomerName || 'Khách hàng' }}</h3>
              <p>{{ item.customerPhone }}</p>
            </div>
            <div class="customer-metrics">
              <span>{{ item.reservationCount || 0 }} lượt</span>
              <span>{{ money(item.totalAmount) }}</span>
            </div>
            <div class="mini-stats">
              <span>Hoàn tất: {{ item.completedCount || 0 }}</span>
              <span>Hủy: {{ item.cancelledCount || 0 }}</span>
              <span>No-show: {{ item.noShowCount || 0 }}</span>
            </div>
          </article>
          <div v-if="!loading && filteredHistory.length === 0" class="empty-state">Không có dữ liệu phù hợp.</div>
        </section>

        <section class="detail-panel">
          <div v-if="!selectedPhone" class="empty-state">Chọn một khách để xem chi tiết lịch sử đặt bàn.</div>
          <template v-else>
            <div class="detail-head">
              <div>
                <h2>{{ selectedCustomer?.latestCustomerName || selectedPhone }}</h2>
                <p>{{ selectedPhone }}</p>
              </div>
              <strong>{{ selectedReservations.length }} booking</strong>
            </div>
            <div v-if="detailLoading" class="empty-state">Đang tải chi tiết...</div>
            <div v-else class="reservation-list">
              <article v-for="reservation in selectedReservations" :key="reservation.id" class="reservation-row">
                <div>
                  <strong>{{ reservation.reservationCode }}</strong>
                  <span>{{ reservation.reservationDate }} {{ reservation.arrivalTime }}</span>
                </div>
                <div>
                  <span>{{ reservation.guestCount }} khách</span>
                  <strong>{{ money(reservation.totalAmount) }}</strong>
                </div>
                <span class="status-pill" :class="reservation.reservationStatus">{{ statusText(reservation.reservationStatus) }}</span>
              </article>
              <div v-if="selectedReservations.length === 0" class="empty-state">Khách này chưa có booking chi tiết.</div>
            </div>
          </template>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import api from '@/services/api';
import AdminLayout from '@/components/AdminLayout.vue';

const history = ref([]);
const selectedReservations = ref([]);
const selectedPhone = ref('');
const selectedCustomer = ref(null);
const keyword = ref('');
const loading = ref(false);
const detailLoading = ref(false);

const authHeader = () => ({
  headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
});

const filteredHistory = computed(() => {
  const q = keyword.value.toLowerCase();
  if (!q) return history.value;
  return history.value.filter(item =>
    String(item.customerPhone || '').toLowerCase().includes(q)
    || String(item.latestCustomerName || '').toLowerCase().includes(q)
  );
});

const money = value => Number(value || 0).toLocaleString('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0
});

const statusText = status => ({
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  DEPOSIT_PAID: 'Đã cọc',
  FULLY_PAID: 'Đã thanh toán',
  CHECKED_IN: 'Đã đến',
  IN_SERVICE: 'Đang phục vụ',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'No-show',
  EXPIRED: 'Quá hạn',
  REJECTED: 'Từ chối'
}[status] || status || '-');

const fetchHistory = async () => {
  loading.value = true;
  try {
    const res = await api.get('/api/admin/customer-reservation-history', authHeader());
    history.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    alert('Không thể tải lịch sử khách đặt bàn.');
  } finally {
    loading.value = false;
  }
};

const selectCustomer = async (item) => {
  selectedCustomer.value = item;
  selectedPhone.value = item.customerPhone;
  selectedReservations.value = [];
  detailLoading.value = true;
  try {
    const res = await api.get(`/api/admin/customer-reservation-history/${encodeURIComponent(item.customerPhone)}/reservations`, authHeader());
    selectedReservations.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    alert('Không thể tải chi tiết đặt bàn của khách.');
  } finally {
    detailLoading.value = false;
  }
};

onMounted(fetchHistory);
</script>

<style scoped>
.history-page { max-width: 1320px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin: 0 0 6px; color: var(--text-heading); font-weight: 900; font-size: 1.9rem; }
.page-header p { margin: 0; color: var(--text-muted); }
.toolbar { margin-bottom: 16px; }
.toolbar input { width: 100%; max-width: 420px; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 11px 12px; background: var(--bg-input); color: var(--text-primary); font: inherit; }
.history-layout { display: grid; grid-template-columns: 420px minmax(0, 1fr); gap: 18px; align-items: start; }
.history-list, .detail-panel { background: var(--bg-card); border: 1px solid var(--border-light); border-radius: var(--radius-lg); padding: 16px; box-shadow: var(--shadow-md); }
.history-list { display: grid; gap: 10px; max-height: 760px; overflow-y: auto; }
.customer-card { border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 14px; cursor: pointer; background: var(--bg-card2); transition: var(--transition); }
.customer-card:hover, .customer-card.active { border-color: var(--primary); transform: translateY(-2px); }
.customer-card h3, .customer-card p { margin: 0; }
.customer-card h3 { color: var(--text-heading); }
.customer-card p, .mini-stats, .detail-head p, .reservation-row span { color: var(--text-muted); }
.customer-metrics, .mini-stats { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; font-size: 0.84rem; font-weight: 800; }
.customer-metrics span { color: var(--primary); }
.detail-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 14px; }
.detail-head h2 { margin: 0; color: var(--text-heading); }
.reservation-list { display: grid; gap: 10px; }
.reservation-row { display: grid; grid-template-columns: 1fr 160px auto; gap: 12px; align-items: center; border: 1px solid var(--border-light); border-radius: var(--radius-md); padding: 12px; background: var(--bg-card2); }
.reservation-row div { display: grid; gap: 4px; }
.status-pill { border-radius: 999px; padding: 6px 10px; font-weight: 900; background: rgba(90, 110, 69, 0.12); color: var(--primary); white-space: nowrap; }
.status-pill.CANCELLED, .status-pill.NO_SHOW, .status-pill.EXPIRED, .status-pill.REJECTED { background: rgba(178, 59, 46, 0.12); color: #B23B2E; }
.empty-state { border: 1px dashed var(--border-light); border-radius: var(--radius-md); padding: 34px; text-align: center; color: var(--text-muted); }
@media (max-width: 980px) {
  .history-layout, .reservation-row { grid-template-columns: 1fr; }
  .page-header, .detail-head { display: grid; }
}
</style>
