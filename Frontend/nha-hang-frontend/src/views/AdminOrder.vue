<template>
  <AdminLayout>
  <div class="admin-wrapper luxury-theme">
    

    <main class="admin-content">
      <!-- Dashboard Stats -->
      <div class="stats-grid hide-on-print">
        <div class="stat-card gold clickable" @click="$router.push('/admin/analytics')">
          <div class="stat-icon">💰</div>
          <div class="stat-info">
            <h3>{{ dynamicStats.totalRevenue ? dynamicStats.totalRevenue.toLocaleString() : 0 }} VNĐ</h3>
            <p>TỔNG DOANH THU</p>
          </div>
        </div>
        <div class="stat-card teal">
          <div class="stat-icon">🧾</div>
          <div class="stat-info">
            <h3>{{ dynamicStats.completedOrdersCount || 0 }} Đơn</h3>
            <p>HÓA ĐƠN HOÀN THÀNH</p>
          </div>
        </div>
        <div class="stat-card blue">
          <div class="stat-icon">🍽️</div>
          <div class="stat-info">
            <h3>{{ dynamicStats.totalItemsSold || 0 }} Đĩa</h3>
            <p>MÓN ĂN ĐÃ PHỤC VỤ</p>
          </div>
        </div>
        <div class="stat-card red">
          <div class="stat-icon">⏳</div>
          <div class="stat-info">
            <h3>{{ dynamicStats.pendingOrdersCount || 0 }} Đơn</h3>
            <p>ĐƠN ĐANG CHỜ DUYỆT</p>
          </div>
        </div>
      </div>

      <!-- Filter & Search -->
      <div class="filter-bar g-card hide-on-print">
        <div class="filter-item">
          <span class="filter-label">⏱️ Lọc thời gian</span>
          <select v-model="timeFilter" class="g-form-control" style="width: 200px;">
            <option value="all">Tất cả thời gian</option>
            <option value="today">Hôm nay</option>
            <option value="week">7 Ngày qua</option>
            <option value="month">Tháng này</option>
            <option value="year">Năm nay</option>
          </select>
        </div>
        <div class="filter-item flex-1">
          <span class="filter-label">🔍 Tìm hóa đơn</span>
          <input
            v-model="searchCode"
            type="text"
            maxlength="5"
            placeholder="Nhập mã hóa đơn (VD: 0011)..."
            class="g-form-control"
          />
        </div>
        <button v-if="searchCode || timeFilter !== 'all'" @click="resetFilters" class="btn-clear">
          ✕ Xóa bộ lọc
        </button>
      </div>

      <!-- Invoice Table -->
      <div class="g-card invoice-card">
        <h3 class="section-title hide-on-print">📋 Danh Sách Nhật Ký Hóa Đơn</h3>
        <div class="table-responsive">
          <table class="g-table">
            <colgroup>
              <col style="width: 100px" />
              <col style="width: 160px" />
              <col style="width: auto" />
              <col style="width: 160px" />
              <col style="width: 130px" />
              <col style="width: 170px" />
            </colgroup>
            <thead>
              <tr>
                <th>MÃ ĐƠN</th>
                <th>KHÁCH HÀNG</th>
                <th>CHI TIẾT ĐƠN HÀNG</th>
                <th>THỜI GIAN</th>
                <th>TRẠNG THÁI</th>
                <th class="hide-on-print">HÀNH ĐỘNG</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in filteredOrders" :key="order.id" class="invoice-row">
                <td class="col-code">
                  <span class="code-badge">#{{ getOrderCode(order) }}</span>
                </td>
                <td class="customer-cell">👤 {{ order.account?.fullname || order.username || 'Khách Vãng Lai' }}</td>
                <td class="col-detail">
                  <div class="address-text">{{ cleanAddress(order.address) }}</div>
                  <div v-if="order.status === 5" class="scheduled-badge">
                    ⏰ {{ getCountdown(order.address) }}
                  </div>
                  <div class="food-tags">
                    <span v-for="detail in order.orderDetails" :key="detail.id" class="food-tag">
                      <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="food-thumb" @error="replaceFoodImage" />
                      ×{{ detail.quantity }} {{ detail.product?.name }} ({{ detail.price.toLocaleString() }}đ)
                    </span>
                  </div>
                </td>
                <td class="date-cell">📅 {{ formatDate(order.createDate) }}</td>
                <td>
                  <span :class="['g-badge', getStatusClass(order.status)]">
                    {{ getStatusText(order.status) }}
                  </span>
                </td>
                <td class="hide-on-print">
                  <div class="action-row">
                    <button @click="viewInvoice(order)" class="btn-view">👁 Xem</button>
                    <button
                      v-if="order.status === 0 || order.status === 5"
                      @click="approveOrderToKitchen(order.id)"
                      class="btn-approve"
                    >👨‍🍳 Chuyển Bếp</button>
                  </div>
                </td>
              </tr>
              <tr v-if="filteredOrders.length === 0">
                <td colspan="6" class="empty-row">⚠️ Không tìm thấy hóa đơn nào!</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Invoice Modal -->
      <div v-if="selectedOrder" class="modal-overlay" @click.self="closeModal">
        <div class="invoice-modal printable-area">
          <div class="modal-header hide-on-print">
            <h2>Chi Tiết Hóa Đơn</h2>
            <button @click="closeModal" class="btn-close">✖</button>
          </div>

          <div class="invoice-content">
            <div class="invoice-brand">
              <h1>Mộc Vị <span>RESTAURANT</span></h1>
              <p>Luxury Dining Experience</p>
              <div class="brand-address">
                Địa chỉ: 137 Nguyễn Thị Thập, Hòa Minh, Liên Chiểu, Đà Nẵng<br>
                Hotline: 0905.XXX.XXX | Email: contact@Mộc Vịrestaurant.vn
              </div>
            </div>

            <div class="invoice-meta">
              <div class="meta-left">
                <p><strong>Khách hàng:</strong> {{ selectedOrder.account?.fullname || selectedOrder.username || 'Khách Vãng Lai' }}</p>
                <p><strong>Vị trí:</strong> {{ cleanAddress(selectedOrder.address) }}</p>
                <p><strong>Ngày lập:</strong> {{ formatDate(selectedOrder.createDate) }}</p>
              </div>
              <div class="meta-right">
                <h3>HÓA ĐƠN THANH TOÁN</h3>
                <span class="code-badge-lg">#{{ getOrderCode(selectedOrder) }}</span>
              </div>
            </div>

            <table class="print-table">
              <thead>
                <tr>
                  <th style="width:10%">STT</th>
                  <th style="width:10%">Ảnh</th>
                  <th style="width:35%">Tên Món Ăn</th>
                  <th style="width:15%; text-align:right">Đơn Giá</th>
                  <th style="width:10%; text-align:center">SL</th>
                  <th style="width:20%; text-align:right">Thành Tiền</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(detail, index) in selectedOrder.orderDetails" :key="detail.id">
                  <td style="text-align:center">{{ index + 1 }}</td>
                  <td>
                    <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="invoice-thumb" @error="replaceFoodImage" />
                    <span v-else class="no-img">🍽️</span>
                  </td>
                  <td><strong>{{ detail.product?.name }}</strong></td>
                  <td style="text-align:right">{{ (detail.price / detail.quantity).toLocaleString() }}đ</td>
                  <td style="text-align:center">{{ detail.quantity }}</td>
                  <td style="text-align:right; color: var(--primary); font-weight:700">{{ detail.price.toLocaleString() }}đ</td>
                </tr>
              </tbody>
            </table>

            <div class="invoice-total">
              <table class="total-table">
                <tr>
                  <td>Tạm tính:</td>
                  <td>{{ calculateTotal(selectedOrder).toLocaleString() }} đ</td>
                </tr>
                <tr>
                  <td>Thuế VAT (0%):</td>
                  <td>0 đ</td>
                </tr>
                <tr class="total-row">
                  <td>TỔNG CỘNG:</td>
                  <td>{{ calculateTotal(selectedOrder).toLocaleString() }} đ</td>
                </tr>
              </table>
            </div>

            <div class="invoice-footer">
              <p class="thanks-msg">Thank you for dining with us!</p>
              <p class="system-msg">Hóa đơn được tạo tự động bởi hệ thống MỘC VỊ RESTAURANT System</p>
            </div>
          </div>

          <div class="modal-actions hide-on-print">
            <button @click="exportToPDF" class="btn-export">📥 Xuất Hóa Đơn PDF</button>
          </div>
        </div>
      </div>
    </main>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, computed, onMounted } from 'vue';
import api from '@/services/api';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';

const orders = ref([]);
const searchCode = ref('');
const timeFilter = ref('all');
const selectedOrder = ref(null);

const configHeader = () => {
  const token = localStorage.getItem('staff_token');
  return { headers: { 'Authorization': `Bearer ${token}` } };
};

const loadData = async () => {
  try {
    const resOrders = await api.get('/api/admin/orders', configHeader());
    orders.value = resOrders.data;
  } catch (err) { console.error('Lỗi tải dữ liệu', err); }
};

const approveOrderToKitchen = async (orderId) => {
  if (confirm('Xác nhận chuyển đơn hàng này xuống bếp để chuẩn bị?')) {
    try {
      await api.put(`/api/admin/orders/${orderId}/status?status=1`, {}, configHeader());
      alert('Đã chuyển đơn xuống Bếp thành công!');
      loadData();
    } catch (error) {
      alert('Lỗi! Không thể chuyển đơn xuống bếp. Vui lòng kiểm tra quyền.');
    }
  }
};

const filteredOrders = computed(() => {
  let result = orders.value;
  if (timeFilter.value !== 'all') {
    const now = new Date();
    result = result.filter(order => {
      if (!order.createDate) return true;
      const orderDate = new Date(order.createDate);
      if (timeFilter.value === 'today') return orderDate.toDateString() === now.toDateString();
      if (timeFilter.value === 'week') {
        const diffDays = Math.ceil(Math.abs(now - orderDate) / (1000 * 60 * 60 * 24));
        return diffDays <= 7;
      }
      if (timeFilter.value === 'month') return orderDate.getMonth() === now.getMonth() && orderDate.getFullYear() === now.getFullYear();
      if (timeFilter.value === 'year') return orderDate.getFullYear() === now.getFullYear();
      return true;
    });
  }
  if (searchCode.value.trim()) {
    result = result.filter(o => getOrderCode(o).includes(searchCode.value.trim()));
  }
  return result;
});

const dynamicStats = computed(() => {
  let revenue = 0, completed = 0, items = 0, pending = 0;
  filteredOrders.value.forEach(order => {
    if (order.status === 4) {
      completed++;
      if (order.orderDetails?.length > 0) {
        order.orderDetails.forEach(d => { revenue += d.price; items += d.quantity; });
      }
    }
    if (order.status === 0) pending++;
  });
  return { totalRevenue: revenue, completedOrdersCount: completed, totalItemsSold: items, pendingOrdersCount: pending };
});

const resetFilters = () => { searchCode.value = ''; timeFilter.value = 'all'; };

const getOrderCode = (order) => {
  if (!order) return '----';
  if (order.address) {
    const matchHash = order.address.match(/#(\d{4})/);
    if (matchHash) return matchHash[1];
    const matchGD = order.address.match(/MãGD:\s*(\d+)/);
    if (matchGD) return matchGD[1].padStart(4, '0');
  }
  if (order.id) return String(order.id).padStart(4, '0');
  return '0000';
};

const cleanAddress = (address) => {
  if (!address) return '';
  return address.replace(/MÃ ĐƠN: #\d{4} \| /, '');
};

const getStatusText = (status) => {
  const map = { 0: 'Chờ xử lý', 1: 'Đang nấu', 2: 'Đã lên món', 4: 'Hoàn thành', 5: '⏰ Chờ hẹn giờ' };
  return map[status] || 'Đang phục vụ';
};

const getStatusClass = (status) => {
  if (status === 0) return 'g-badge-warning';
  if (status === 1) return 'g-badge-info';
  if (status === 2) return 'g-badge-info';
  if (status === 4) return 'g-badge-success';
  if (status === 5) return 'g-badge-scheduled';
  return 'g-badge-info';
};

const formatDate = (dateStr) => {
  if (!dateStr) return '---';
  return new Date(dateStr).toLocaleString('vi-VN');
};

const viewInvoice = (order) => { selectedOrder.value = order; };
const closeModal = () => { selectedOrder.value = null; };
const calculateTotal = (order) => {
  if (!order?.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + item.price, 0);
};
const exportToPDF = () => { window.print(); };

// 🌟 Parse giờ hẹn từ address và hiện countdown
const getCountdown = (address) => {
  if (!address) return '';
  const match = address.match(/Lúc:\s*(\d{2}:\d{2})\s*ngày\s*(\d{4}-\d{2}-\d{2})/);
  if (!match) return '';
  const arrivalTime = new Date(`${match[2]}T${match[1]}:00`);
  const now = new Date();
  const diffMs = arrivalTime.getTime() - now.getTime();
  const diffMin = Math.round(diffMs / 60000);
  if (diffMin <= 0) return '🔥 Đã tới giờ hẹn!';
  if (diffMin <= 15) return `🔔 Còn ${diffMin} phút → Đang chuyển bếp...`;
  return `⏳ Còn ${diffMin} phút nữa tới giờ hẹn (bếp nhận trước 15p)`;
};

// 🌟 Auto-activate scheduled reservation orders every 30s
const activateScheduled = async () => {
  try {
    const token = localStorage.getItem('staff_token');
    if (!token || token === 'null' || token === 'undefined') return; // Skip if no valid token
    await api.put('/api/admin/orders/activate-scheduled', {}, configHeader());
    loadData(); // Refresh list
  } catch (err) { /* silent */ }
};

onMounted(() => {
  loadData();
  setInterval(activateScheduled, 30000); // Mỗi 30 giây check 1 lần
});
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1500px; margin: 0 auto; padding: 36px 24px; }

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}
.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 4px solid;
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  box-shadow: var(--shadow-sm);
  transition: var(--transition);
}
.stat-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
.stat-card.gold { border-left-color: var(--color-tertiary); }
.stat-card.clickable { cursor: pointer; }
.stat-card.clickable:hover { box-shadow: 0 0 20px color-mix(in srgb, var(--color-tertiary) 40%, transparent); transform: translateY(-5px) scale(1.02); }
.stat-card.teal { border-left-color: var(--primary); }
.stat-card.blue { border-left-color: var(--secondary); }
.stat-card.red  { border-left-color: var(--primary); }

.stat-icon {
  font-size: 2rem;
  background: var(--bg-card2);
  width: 56px; height: 56px;
  display: flex; align-items: center; justify-content: center;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}
.stat-info h3 { margin: 0 0 4px 0; font-size: 1.5rem; font-weight: 900; color: var(--text-heading); }
.stat-info p { margin: 0; font-size: 0.72rem; color: var(--text-muted); font-weight: 700; letter-spacing: 1px; }

/* Filter Bar */
.filter-bar {
  display: flex;
  gap: 16px;
  align-items: flex-end;
  flex-wrap: wrap;
  margin-bottom: 28px;
}
.filter-item { display: flex; flex-direction: column; gap: 6px; }
.filter-item.flex-1 { flex: 1; min-width: 280px; }
.filter-label { font-size: 0.8rem; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }
.btn-clear {
  background: rgba(92,107,101,0.15); border: 1px solid var(--border);
  color: var(--text-muted); padding: 12px 20px;
  border-radius: var(--radius-md); cursor: pointer; font-weight: 600;
  font-family: inherit; font-size: 0.88rem; transition: var(--transition);
  white-space: nowrap;
}
.btn-clear:hover { background: rgba(92,107,101,0.3); color: var(--text-secondary); }

/* Invoice Card */
.invoice-card { margin-bottom: 28px; }
.section-title {
  margin: 0 0 24px 0; font-size: 1.1rem; font-weight: 700;
  color: var(--text-heading); padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light);
}
.table-responsive { overflow-x: auto; }

/* Fix table layout for uniform rows */
.g-table {
  table-layout: fixed;
  width: 100%;
}
.g-table th, .g-table td {
  vertical-align: middle;
  padding: 14px 12px;
}
.g-table tbody tr {
  border-bottom: 1px solid var(--border-light);
}
.g-table tbody tr:hover {
  background: rgba(255,255,255,0.03);
}
.col-code { text-align: center; }
.col-detail {
  padding-top: 10px !important;
  padding-bottom: 10px !important;
}

.code-badge {
  background: var(--primary-glow); color: var(--primary);
  border: 1px solid var(--border);
  padding: 5px 12px; border-radius: 6px;
  font-family: var(--font-primary); font-size: 0.95rem; font-weight: 800;
}
.customer-cell { font-weight: 600; color: var(--text-primary); }
.address-text {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}
.food-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  max-height: 80px;
  overflow-y: auto;
}
.food-tag {
  background: var(--bg-input); border: 1px solid var(--border-light);
  color: var(--text-secondary); padding: 3px 8px;
  border-radius: 4px; font-size: 0.78rem; font-weight: 500;
  display: inline-flex; align-items: center; gap: 5px;
}
.food-thumb {
  width: 22px; height: 22px; border-radius: 4px; object-fit: cover;
  border: 1px solid var(--border);
}
.scheduled-badge {
  background: color-mix(in srgb, var(--color-tertiary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  color: var(--color-tertiary);
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 0.78rem;
  font-weight: 700;
  margin: 6px 0;
  display: inline-block;
  animation: pulse-scheduled 2s ease-in-out infinite;
}
@keyframes pulse-scheduled {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
.g-badge-scheduled {
  background: color-mix(in srgb, var(--color-tertiary) 15%, transparent) !important;
  color: var(--color-tertiary) !important;
  border-color: color-mix(in srgb, var(--color-tertiary) 30%, transparent) !important;
}
.date-cell { color: var(--text-muted); font-size: 0.88rem; white-space: nowrap; }
.action-row { display: flex; gap: 6px; }
.btn-view {
  background: color-mix(in srgb, var(--secondary) 10%, transparent); border: 1px solid var(--border);
  color: var(--primary); padding: 7px 12px;
  border-radius: var(--radius-sm); cursor: pointer; font-size: 0.82rem; font-weight: 600;
  transition: var(--transition);
}
.btn-view:hover { background: var(--primary-glow); }
.btn-approve {
  background: color-mix(in srgb, var(--color-tertiary) 15%, transparent); border: 1px solid color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  color: var(--color-tertiary); padding: 7px 12px;
  border-radius: var(--radius-sm); cursor: pointer; font-size: 0.82rem; font-weight: 600;
  white-space: nowrap; transition: var(--transition);
}
.btn-approve:hover { background: color-mix(in srgb, var(--color-tertiary) 30%, transparent); }
.empty-row { text-align: center; color: var(--text-muted); padding: 50px; font-style: italic; }

/* ===== MODAL ===== */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.75);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; backdrop-filter: blur(6px);
  padding: 20px;
}
.invoice-modal {
  background: #FFFFFF; color: var(--text-primary);
  width: 100%; max-width: 860px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 25px 60px rgba(0,0,0,0.5);
  max-height: 90vh; overflow-y: auto;
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--bg-nav); color: #FFFFFF; padding: 16px 24px;
  border-bottom: 1px solid var(--border);
}
.modal-header h2 { margin: 0; color: var(--primary); font-size: 1.1rem; }
.btn-close {
  background: transparent; border: none; color: var(--text-muted);
  font-size: 1.3rem; cursor: pointer; transition: var(--transition);
}
.btn-close:hover { color: var(--primary); transform: scale(1.1); }

.invoice-content { padding: 40px; }
.invoice-brand { text-align: center; border-bottom: 2px solid var(--text-primary); padding-bottom: 20px; margin-bottom: 28px; }
.invoice-brand h1 { margin: 0; font-size: 2rem; color: var(--text-primary); letter-spacing: 2px; }
.invoice-brand h1 span { color: var(--success); }
.invoice-brand p { margin: 4px 0 0 0; color: var(--text-muted); text-transform: uppercase; font-size: 0.8rem; letter-spacing: 3px; }
.brand-address { font-size: 0.82rem; color: var(--text-muted); margin-top: 10px; }

.invoice-meta { display: flex; justify-content: space-between; margin-bottom: 28px; }
.meta-left p { margin: 6px 0; color: var(--text-secondary); }
.meta-right { text-align: right; }
.meta-right h3 { margin: 0 0 10px 0; color: var(--primary); font-size: 1.2rem; }
.code-badge-lg {
  background: var(--color-inverse-surface); color: var(--success);
  padding: 6px 14px; border-radius: 4px;
  font-size: 1.2rem; font-weight: 800; font-family: var(--font-primary);
}

.print-table { width: 100%; border-collapse: collapse; margin-bottom: 28px; }
.print-table th { background: var(--color-inverse-surface); color: #FFFFFF; padding: 12px; font-size: 0.88rem; text-transform: uppercase; }
.print-table td { border-bottom: 1px solid var(--border); padding: 14px 12px; color: var(--text-primary); vertical-align: middle; }
.invoice-thumb {
  width: 45px; height: 45px; border-radius: 6px; object-fit: cover;
  border: 1px solid var(--border);
}
.no-img { font-size: 1.5rem; display: inline-block; }

.invoice-total { margin-top: 20px; display: flex; justify-content: flex-end; }
.total-table { border-collapse: collapse; min-width: 300px; }
.total-table td { padding: 6px 10px; color: var(--text-secondary); }
.total-table td:last-child { text-align: right; font-weight: 700; }
.total-row td { border-top: 2px solid var(--text-primary); padding-top: 12px; font-size: 1.15rem; color: var(--primary); font-weight: 900; }

.invoice-footer { text-align: center; margin-top: 40px; border-top: 1px solid var(--border); padding-top: 20px; }
.thanks-msg { color: var(--success); font-size: 1.2rem; font-family: var(--font-primary); margin: 0 0 6px 0; }
.system-msg { font-size: 0.78rem; color: var(--text-muted); margin: 0; }

.modal-actions { padding: 20px; background: var(--bg-card2); text-align: center; border-top: 1px solid var(--border); }
.btn-export {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark); border: none; padding: 13px 32px;
  font-weight: 800; border-radius: var(--radius-md); cursor: pointer;
  font-size: 1rem; font-family: inherit; transition: var(--transition);
}
.btn-export:hover { transform: translateY(-2px); box-shadow: 0 6px 20px color-mix(in srgb, var(--secondary) 40%, transparent); }

@media print {
  @page {
    size: A4 portrait;
    margin: 10mm;
  }
  * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  body * { visibility: hidden !important; }
  .printable-area,
  .printable-area * { visibility: visible !important; }
  .printable-area {
    position: fixed !important;
    inset: 0 !important;
    width: 100% !important;
    max-width: 100% !important;
    max-height: none !important;
    overflow: visible !important;
    box-shadow: none !important;
    border-radius: 0 !important;
    background: #FFFFFF !important;
    color: var(--text-primary) !important;
    padding: 0 !important;
    z-index: 99999 !important;
  }
  /* Thu nhỏ nội dung bên trong hóa đơn khi in */
  .invoice-content { padding: 16px !important; }
  .invoice-brand { padding-bottom: 10px !important; margin-bottom: 12px !important; }
  .invoice-brand h1 { font-size: 1.3rem !important; }
  .brand-address { font-size: 0.7rem !important; }
  .invoice-meta { margin-bottom: 12px !important; }
  .meta-left p { margin: 3px 0 !important; font-size: 0.82rem !important; }
  .print-table th { padding: 7px 8px !important; font-size: 0.78rem !important; }
  .print-table td { padding: 7px 8px !important; font-size: 0.82rem !important; }
  .invoice-thumb { width: 32px !important; height: 32px !important; }
  .invoice-total { margin-top: 10px !important; }
  .total-table td { padding: 4px 8px !important; font-size: 0.88rem !important; }
  .invoice-footer { margin-top: 16px !important; padding-top: 10px !important; }
  .thanks-msg { font-size: 0.95rem !important; }
  .hide-on-print { display: none !important; }
}
</style>
