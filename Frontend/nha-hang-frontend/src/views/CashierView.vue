<template>
  <div class="cashier-wrapper luxury-theme">
    <!-- Header -->
    <header class="cashier-header">
      <div class="header-left">
        <div class="brand">
          <span class="brand-icon">💰</span>
          <div>
            <h2>QUẦY THU NGÂN</h2>
            <p>Cashier Dashboard - {{ new Date().toLocaleDateString('vi-VN') }}</p>
          </div>
        </div>
      </div>
      <div class="header-right" style="display: flex; gap: 10px; align-items: center;">
        <div class="tabs" style="display: flex; gap: 10px; margin-right: 20px;">
          <button @click="activeTab = 'pending'" :class="['tab-btn', { active: activeTab === 'pending' }]">⏳ Chờ Thanh Toán</button>
          <button @click="activeTab = 'history'" :class="['tab-btn', { active: activeTab === 'history' }]">📜 Lịch Sử Hóa Đơn</button>
        </div>
        <button @click="openShiftModal" class="btn-primary" style="padding: 10px 20px; border-radius: 6px; font-weight: bold;">📋 Kết Ca</button>
        <button @click="$router.push('/staff')" class="btn-profile" style="background:#8A641F; color:#FFFFFF; padding:10px 20px; border:none; border-radius:6px; font-weight:bold; cursor:pointer;">👤 Cá Nhân</button>
        <button @click="logout" class="btn-logout">Đăng Xuất</button>
      </div>
    </header>

    <TimekeepingWidget />

    <div class="cashier-content" v-if="activeTab === 'pending'">
      <div class="orders-list-panel">
        <h3>Sơ Đồ Bàn Nhà Hàng</h3>

        <div class="table-grid">
          <div
            v-for="table in tables"
            :key="table.id"
            :class="['table-box', getTableClass(table.isOccupied), { 'selected-table': selectedOrder && orderMatchesTable(selectedOrder, table) }]"
            @click="selectOrderForTable(table)"
          >
            <div class="tc-top">
              <span class="tc-capacity">👥 {{ table.capacity || 4 }}</span>
              <span class="tc-icon">🧮</span>
            </div>
            <div class="tc-center">
              <div class="tc-dot"></div>
              <h4>{{ table.name }}</h4>
              <p class="tc-subtitle">
                <span v-if="getOpenOrderForTable(table)" style="color:#B98229; font-weight: bold;">
                  {{ getPendingTotalForTable(table).toLocaleString() }}đ
                </span>
                <span v-else>
                  {{ table.isOccupied === 0 ? 'Sẵn sàng phục vụ' : table.isOccupied === 1 ? 'Đã được đặt cọc' : table.isOccupied === 3 ? 'Đang dọn dẹp' : 'Khách đang ăn' }}
                </span>
              </p>
            </div>
            <div class="tc-bottom">
              <span class="tc-status">
                {{ table.isOccupied === 0 ? '🟢 Trống ⌄' : table.isOccupied === 1 ? '🟡 Đã Cọc ⌄' : table.isOccupied === 3 ? '🟣 Cần Dọn ⌄' : '🔴 Có Khách ⌄' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="invoice-panel" v-if="selectedOrder">
        <div class="invoice-paper" id="printable-invoice">
          <div class="invoice-header">
            <h2>Mộc Vị RESTAURANT</h2>
            <p>Luxury Dining Experience</p>
            <p>Địa chỉ: 137 Nguyễn Thị Thập, Hòa Minh, Liên Chiểu, Đà Nẵng</p>
            <p>Hotline: 0905.XXX.XXX | Email: contact@mocvirestaurant.vn</p>
            <hr />
            <h3>HÓA ĐƠN THANH TOÁN</h3>
            <p>Mã HĐ: #{{ selectedOrder.id }}</p>
            <p>Bàn: {{ getTableName(selectedOrder.address) }}</p>
            <p>Ngày: {{ new Date(selectedOrder.createDate).toLocaleString('vi-VN') }}</p>
          </div>
          <table class="invoice-table">
            <thead>
              <tr>
                <th style="width: 50px; text-align: center;">Ảnh</th>
                <th style="text-align: left;">Món</th>
                <th style="text-align: center;">SL</th>
                <th style="text-align: right;">Đơn Giá</th>
                <th style="text-align: right;">Thành Tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in selectedOrder.orderDetails" :key="item.id">
                <td style="text-align: center;">
                  <img v-if="item.product.image" :src="foodImage(item.product.image)" @error="replaceFoodImage" style="width: 40px; height: 40px; border-radius: 4px; object-fit: cover; display: inline-block;" />
                  <span v-else style="font-size: 1.5rem;">🍽️</span>
                </td>
                <td>{{ item.product.name }}</td>
                <td style="text-align: center;">{{ item.quantity }}</td>
                <td style="text-align: right;">{{ (item.price / item.quantity).toLocaleString() }}đ</td>
                <td style="text-align: right; font-weight: bold; color: var(--primary);">{{ item.price.toLocaleString() }}đ</td>
              </tr>
            </tbody>
          </table>
          <hr />
          <div class="invoice-total">
            <span>TỔNG ĐỒ ĂN:</span>
            <span class="total-amount">{{ calculateTotal(selectedOrder).toLocaleString() }} VNĐ</span>
          </div>
          <div class="invoice-total" v-if="selectedOrder.deposit > 0">
            <span>ĐÃ ĐẶT CỌC:</span>
            <span class="total-amount">- {{ selectedOrder.deposit.toLocaleString() }} VNĐ</span>
          </div>
          <hr v-if="selectedOrder.deposit > 0" />
          <div class="invoice-total" style="font-size: 1.3rem; margin-top: 10px;">
            <span>CẦN THANH TOÁN:</span>
            <span class="total-amount">{{ Math.max(0, calculateTotal(selectedOrder) - (selectedOrder.deposit || 0)).toLocaleString() }} VNĐ</span>
          </div>
          <div class="invoice-total" style="font-size: 1rem; margin-top: 10px; color: var(--text-primary);">
            <span>TRẠNG THÁI THANH TOÁN:</span>
            <span class="total-amount">{{ paymentSummary(selectedOrder) }}</span>
          </div>
          <div v-if="(calculateTotal(selectedOrder) - (selectedOrder.deposit || 0)) < 0" style="text-align: right; color: var(--primary); font-style: italic;">
            (Thu ngân thối lại: {{ Math.abs(calculateTotal(selectedOrder) - (selectedOrder.deposit || 0)).toLocaleString() }} VNĐ)
          </div>

          <!-- Khu vực QR Code Thanh Toán -->
          <div class="qr-payment-section hide-on-print" v-if="!selectedOrder.isPaid" style="margin-top: 20px; padding: 15px; background: rgba(90, 110, 69, 0.05); border: 1px dashed var(--primary); border-radius: 8px;">
            <h4 style="text-align: center; margin-top: 0; margin-bottom: 10px; color: var(--primary); font-size: 1.1rem;">QR Chuyển Khoản Theo Hóa Đơn</h4>
            <p v-if="paymentQrLoading" style="text-align: center; color: #55503E;">Đang tạo QR an toàn...</p>
            <div v-else-if="paymentQr" style="text-align: center;">
              <img :src="paymentQr.qrUrl" alt="QR chuyển khoản cho hóa đơn" style="width: 180px; height: 180px; border-radius: 8px; padding: 5px; background: #FFFFFF;" />
              <p style="margin-top: 10px; margin-bottom: 2px; font-weight: bold; color: #201D14;">Chủ TK: {{ paymentQr.accountHolder }}</p>
              <p style="color: #55503E; font-size: 0.85rem; margin: 0;">Ngân hàng: {{ paymentQr.bankCode }} · {{ paymentQr.accountNumber }}</p>
              <p style="color: #201D14; font-size: 0.85rem; margin: 4px 0 0;">Nội dung: <strong>{{ paymentQr.transferContent }}</strong></p>
              <button class="btn-print" style="margin-top: 10px;" :disabled="paymentQrLoading" @click="regeneratePaymentQr">Tạo lại QR</button>
            </div>
            <div v-else style="text-align: center;">
              <p style="color: #55503E;">QR được backend tạo riêng cho hóa đơn này và tự đối soát qua ngân hàng.</p>
              <button class="btn-pay" :disabled="paymentQrLoading" @click="createPaymentQr">Tạo QR chuyển khoản</button>
            </div>
            <p v-if="paymentQrError" style="text-align: center; margin-top: 10px; color: #B23B2E;">{{ paymentQrError }}</p>
            <p v-if="paymentQr" style="text-align: center; margin-top: 10px; font-size: 0.8rem; color: #B23B2E; font-style: italic;">Không xác nhận thủ công. Hệ thống sẽ tự ghi nhận khi ngân hàng báo giao dịch thành công.</p>
          </div>

          <div class="invoice-footer" style="margin-top: 20px;">
            <p>Cảm ơn quý khách & Hẹn gặp lại!</p>
            <p>Hóa đơn được in từ hệ thống Mộc Vị RESTAURANT System</p>
          </div>
        </div>
        
        <div class="action-buttons">
          <button class="btn-print" @click="printInvoice">🖨️ In Hóa Đơn</button>
          <button v-if="!selectedOrder.isPaid && (selectedOrder.status === 0 || selectedOrder.status === 1 || selectedOrder.status === 5)" class="btn-print" style="background: var(--danger); border-color: var(--danger);" @click="cancelOrderAndRefund(selectedOrder)">❌ Hủy & Hoàn Cọc</button>
          <button class="btn-pay" @click="payOrder" :disabled="paymentSubmitting || selectedOrder.isPaid || selectedOrder.paymentOption === 'PREPAID_TRANSFER'">
            {{ paymentSubmitting ? 'Đang xác nhận...' : selectedOrder.isPaid ? '✅ Đã Thanh Toán' : selectedOrder.paymentOption === 'PREPAID_TRANSFER' ? '⏳ Chờ ngân hàng xác nhận' : '💵 Xác nhận thanh toán' }}
          </button>
        </div>
      </div>
      
      <div v-else class="invoice-panel empty-invoice">
        <p>Vui lòng chọn một đơn hàng để xem hóa đơn.</p>
      </div>
    </div>

    <!-- TAB: HISTORY -->
    <div class="cashier-content" v-if="activeTab === 'history'" style="display: block;">
      <div class="history-panel g-card" style="padding: 20px; background: var(--bg-card); border-radius: var(--radius-lg); border: 1px solid var(--border);">
        <div class="history-header" style="display: flex; justify-content: space-between; margin-bottom: 20px; align-items: center;">
          <h3 style="margin: 0; color: var(--text-heading);">Lịch Sử Hóa Đơn Đã Thanh Toán</h3>
          <div class="filters" style="display: flex; gap: 15px;">
            <input type="text" v-model="searchQuery" placeholder="Tìm theo Mã HĐ hoặc Bàn..." style="padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border); background: rgba(0,0,0,0.2); color: var(--text-primary); outline: none;" />
            <select v-model="dateFilter" style="padding: 8px 12px; border-radius: 6px; border: 1px solid var(--border); background: var(--bg-card); color: var(--text-primary); outline: none;">
              <option value="today">Hôm nay</option>
              <option value="yesterday">Hôm qua</option>
              <option value="all">Tất cả thời gian</option>
            </select>
          </div>
        </div>

        <table style="width: 100%; text-align: left; border-collapse: collapse; color: var(--text-primary);">
          <thead>
            <tr style="border-bottom: 1px solid var(--border); background: rgba(255,255,255,0.05);">
              <th style="padding: 15px 10px;">Mã HĐ</th>
              <th style="padding: 15px 10px;">Ngày Giờ</th>
              <th style="padding: 15px 10px;">Bàn</th>
              <th style="padding: 15px 10px;">Tổng Tiền</th>
              <th style="padding: 15px 10px; text-align: center;">Thao Tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in filteredHistoryOrders" :key="order.id" style="border-bottom: 1px solid var(--border-light); transition: 0.2s;">
              <td style="padding: 15px 10px;">#{{ order.id }}</td>
              <td style="padding: 15px 10px;">{{ new Date(order.createDate).toLocaleString('vi-VN') }}</td>
              <td style="padding: 15px 10px;">{{ getTableName(order.address) }}</td>
              <td style="padding: 15px 10px; font-weight: bold; color: var(--primary);">{{ calculateTotal(order).toLocaleString() }}đ</td>
              <td style="padding: 15px 10px; text-align: center;">
                <button @click="viewHistoryInvoice(order)" style="padding: 6px 15px; border-radius: 6px; border: 1px solid var(--primary); background: transparent; color: var(--primary); cursor: pointer; font-weight: bold;">👁️ Xem Lại</button>
              </td>
            </tr>
            <tr v-if="filteredHistoryOrders.length === 0">
              <td colspan="5" style="text-align: center; padding: 30px; color: var(--text-muted); font-style: italic;">Không tìm thấy hóa đơn nào phù hợp.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Shift Modal -->
    <div v-if="showShiftModal" class="modal-overlay" @click.self="showShiftModal = false">
      <div class="shift-modal g-card">
        <div class="modal-header">
          <h2>📊 Báo Cáo Kết Ca Thu Ngân</h2>
          <button @click="showShiftModal = false" class="btn-close-modal">✖</button>
        </div>
        <div class="modal-body" id="printable-shift">
          <div style="text-align: center; margin-bottom: 20px;">
            <h3>Mộc Vị RESTAURANT</h3>
            <p><strong>BÁO CÁO KẾT CA DOANH THU</strong></p>
            <p>Ngày: {{ new Date().toLocaleDateString('vi-VN') }} | Giờ in: {{ new Date().toLocaleTimeString('vi-VN') }}</p>
            <p>Thu ngân: {{ currentUser?.fullname || currentUser?.username || 'Admin' }}</p>
          </div>
          
          <div class="stats-box">
            <div class="stat-item">
              <span class="stat-label">Tổng Hóa Đơn Đã Thu:</span>
              <span class="stat-value">{{ shiftStats.paidCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Doanh Thu Trong Ca:</span>
              <span class="stat-value text-primary">{{ shiftStats.revenue.toLocaleString() }} VNĐ</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">Bàn Chưa Thanh Toán:</span>
              <span class="stat-value text-danger">{{ shiftStats.unpaidCount }}</span>
            </div>
          </div>
          <p v-if="shiftStats.unpaidCount > 0" class="text-danger" style="margin-top: 15px; font-weight: bold; text-align: center; border: 1px dashed #B23B2E; padding: 10px;">
            ⚠️ Cảnh báo: Vẫn còn {{ shiftStats.unpaidCount }} bàn đang có khách chưa thanh toán! Không nên kết ca lúc này.
          </p>
          
          <div style="text-align: center; margin-top: 40px; display: flex; justify-content: space-around;">
            <div>
              <p><strong>Người nộp tiền</strong></p>
              <p style="margin-top: 50px;">(Ký, ghi rõ họ tên)</p>
            </div>
            <div>
              <p><strong>Người nhận tiền (Quản lý)</strong></p>
              <p style="margin-top: 50px;">(Ký, ghi rõ họ tên)</p>
            </div>
          </div>
        </div>
        <div class="modal-footer" style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
          <button @click="printShiftReport" class="btn-primary" style="background: var(--primary); color: #201D14; border: none; padding: 10px 20px; border-radius: 6px;">🖨️ In Báo Cáo</button>
          <button @click="showShiftModal = false" class="btn-logout" style="border: 1px solid var(--border); color: var(--text-primary); padding: 10px 20px;">Đóng</button>
        </div>
      </div>
    </div>

    <!-- History Invoice Modal -->
    <div v-if="historySelectedOrder" class="modal-overlay" @click.self="historySelectedOrder = null">
      <div class="shift-modal g-card" style="width: 480px;">
        <div class="modal-header">
          <h2>Chi Tiết Hóa Đơn #{{ historySelectedOrder.id }}</h2>
          <button @click="historySelectedOrder = null" class="btn-close-modal">✖</button>
        </div>
        <div class="modal-body" id="printable-history-invoice" style="background: #FFFFFF; color: black; padding: 20px; border-radius: 8px;">
          <div style="text-align: center; border-bottom: 2px dashed #A6B0AA; padding-bottom: 10px; margin-bottom: 15px;">
            <h2 style="margin: 0; font-size: 1.5rem;">Mộc Vị RESTAURANT</h2>
            <p style="margin: 5px 0 0; font-size: 0.9rem;">HÓA ĐƠN ĐÃ THANH TOÁN</p>
            <p style="margin: 5px 0 0; font-size: 0.8rem;">Ngày: {{ new Date(historySelectedOrder.createDate).toLocaleString('vi-VN') }}</p>
            <p style="margin: 5px 0 0; font-size: 0.8rem;">Bàn: {{ getTableName(historySelectedOrder.address) }} | Mã HĐ: #{{ historySelectedOrder.id }}</p>
          </div>
          <table style="width: 100%; border-collapse: collapse; font-size: 0.9rem;">
            <thead>
              <tr style="border-bottom: 1px solid #CFC7A8;">
                <th style="text-align: left; padding: 8px 0;">Món</th>
                <th style="text-align: center; padding: 8px 0;">SL</th>
                <th style="text-align: right; padding: 8px 0;">TTiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in historySelectedOrder.orderDetails" :key="item.id" style="border-bottom: 1px solid #E2DCC2;">
                <td style="padding: 8px 0;">{{ item.product.name }}</td>
                <td style="text-align: center; padding: 8px 0;">{{ item.quantity }}</td>
                <td style="text-align: right; padding: 8px 0;">{{ item.price.toLocaleString() }}đ</td>
              </tr>
            </tbody>
          </table>
          <div style="margin-top: 15px; text-align: right; font-size: 1.1rem; font-weight: bold;">
            TỔNG CỘNG: {{ calculateTotal(historySelectedOrder).toLocaleString() }}đ
          </div>
          <div style="margin-top: 20px; text-align: center; font-size: 0.8rem; font-style: italic;">
            Bản in lại (Reprint)
          </div>
        </div>
        <div class="modal-footer" style="display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px;">
          <button @click="printHistoryInvoice" class="btn-primary" style="padding: 10px 20px; border-radius: 6px;">🖨️ In Lại Hóa Đơn</button>
        </div>
      </div>
    </div>
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
const pendingOrders = ref([]);
const allOrders = ref([]);
const selectedOrder = ref(null);
const paymentQr = ref(null);
const paymentQrLoading = ref(false);
const paymentQrError = ref('');
const paymentSubmitting = ref(false);
let stompClient = null;

const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null);

const showShiftModal = ref(false);

const activeTab = ref('pending');
const searchQuery = ref('');
const dateFilter = ref('today');
const historySelectedOrder = ref(null);

const filteredHistoryOrders = computed(() => {
  let filtered = allOrders.value.filter(o => o.isPaid);

  if (dateFilter.value === 'today') {
    const today = new Date().toDateString();
    filtered = filtered.filter(o => new Date(o.createDate).toDateString() === today);
  } else if (dateFilter.value === 'yesterday') {
    const yesterday = new Date();
    yesterday.setDate(yesterday.getDate() - 1);
    const yesterdayStr = yesterday.toDateString();
    filtered = filtered.filter(o => new Date(o.createDate).toDateString() === yesterdayStr);
  }

  if (searchQuery.value) {
    const sq = searchQuery.value.toLowerCase();
    filtered = filtered.filter(o => 
      String(o.id).includes(sq) || 
      (o.address && getTableName(o.address).toLowerCase().includes(sq))
    );
  }

  return filtered.sort((a, b) => new Date(b.createDate) - new Date(a.createDate));
});

const viewHistoryInvoice = (order) => {
  historySelectedOrder.value = order;
};

const printHistoryInvoice = () => {
  const printContents = document.getElementById('printable-history-invoice').innerHTML;
  const originalContents = document.body.innerHTML;
  
  document.body.innerHTML = printContents;
  window.print();
  document.body.innerHTML = originalContents;
  window.location.reload(); 
};

const shiftStats = computed(() => {
  const todayStr = new Date().toDateString();
  let paidCount = 0;
  let revenue = 0;
  let unpaidCount = 0;

  allOrders.value.forEach(o => {
    if (new Date(o.createDate).toDateString() === todayStr && o.status !== 3) {
      if (o.isPaid) {
        paidCount++;
        revenue += calculateTotal(o);
      } else if (o.address && o.address.includes('Bàn')) {
        unpaidCount++;
      }
    }
  });

  return { paidCount, revenue, unpaidCount };
});

const openShiftModal = () => {
  showShiftModal.value = true;
};

const printShiftReport = () => {
  const printContents = document.getElementById('printable-shift').innerHTML;
  const originalContents = document.body.innerHTML;
  
  document.body.innerHTML = `<div style="padding: 20px; font-family: var(--font-primary); color: #201D14; background: #FFFFFF;">${printContents}</div>`;
  window.print();
  document.body.innerHTML = originalContents;
  window.location.reload(); 
};

const configHeader = () => {
  const token = localStorage.getItem('token');
  return { headers: { Authorization: 'Bearer ' + token } };
};

const logout = () => {
  localStorage.clear();
  router.push('/staff-login');
};

const getTableName = (address) => {
  if (!address) return "Mang đi";
  const match = address.match(/Bàn:\s*(.*?)\s*\|/);
  return match ? match[1].trim() : address;
};

const calculateTotal = (order) => {
  if (!order) return 0;
  if (Number(order.totalAmount) > 0) return Number(order.totalAmount);
  if (!order.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + Number(item.price || 0) + Number(item.taxAmount || 0), 0);
};

const paymentSummary = (order) => {
  if (order.isPaid || order.paymentStatus === 'PAID' || order.paymentStatus === 'OVERPAID') {
    return 'Đã thanh toán đủ 100%';
  }
  const paid = Number(order.paidAmount || order.deposit || 0);
  if (paid > 0) return `Đã thanh toán ${paid.toLocaleString('vi-VN')} VNĐ, còn ${Number(order.remainingAmount || 0).toLocaleString('vi-VN')} VNĐ`;
  return 'Chưa thanh toán';
};

const tables = ref([]);

const fetchTables = async () => {
  try {
    const res = await api.get('/api/tables', configHeader());
    tables.value = res.data;
  } catch (err) {
    console.error('Lỗi lấy bàn:', err);
  }
};

const getTableClass = (status) => {
  if (status === 0) return 'table-empty';
  if (status === 1) return 'table-booked';
  if (status === 3) return 'table-cleaning';
  return 'table-occupied';
};

const selectOrderForTable = (table) => {
  if (table.isOccupied === 0 || table.isOccupied === 1) return;
  const order = getOpenOrderForTable(table);
  if (order) {
    selectedOrder.value = order;
    paymentQr.value = null;
    paymentQrError.value = '';
  } else {
    selectedOrder.value = null;
    alert("Bàn này chưa gọi món hoặc đã thanh toán xong!");
  }
};

const orderMatchesTable = (order, table) => {
  if (!order || !table) return false;
  return Number(order.tableId) === Number(table.id)
    || (order.address && getTableName(order.address) === table.name);
};

const getOpenOrderForTable = (table) => pendingOrders.value.find(order => orderMatchesTable(order, table));

const getPendingTotalForTable = (table) => {
  const order = getOpenOrderForTable(table);
  return order ? calculateTotal(order) : 0;
};

const fetchOrders = async () => {
  try {
    const res = await api.get('/api/admin/orders', configHeader());
    allOrders.value = res.data;
    // Thu ngân chỉ quan tâm đơn ăn tại quán, chưa thanh toán (hoặc đã giao = cần thanh toán)
    // address chứa "Bàn", isPaid == false hoặc null, status != 3 (Đã hủy)
    pendingOrders.value = res.data.filter(o => 
      (o.tableId || (o.address && o.address.includes('Bàn'))) &&
      !o.isPaid && 
      Number(o.status) !== 3
    );
    if (selectedOrder.value && !pendingOrders.value.find(o => o.id === selectedOrder.value.id)) {
      selectedOrder.value = null;
    }
    
    // Đồng thời gọi bảng để cập nhật map bàn
    await fetchTables();
  } catch (err) {
    console.error('Lỗi lấy đơn hàng:', err);
  }
};

const payOrder = async () => {
  if (!selectedOrder.value || paymentSubmitting.value) return;
  if (!confirm('Xác nhận khách đã thanh toán tiền cho đơn hàng này?')) return;
  paymentSubmitting.value = true;
  try {
    await api.put(`/api/admin/orders/${selectedOrder.value.id}/pay`, {}, configHeader());
    const table = tables.value.find(t => orderMatchesTable(selectedOrder.value, t));
    if (table) {
      await api.put(`/api/tables/${table.id}/status?status=3`, {}, configHeader());
    }

    alert('Thanh toán thành công! Bàn đang chờ dọn dẹp.');
    selectedOrder.value.isPaid = true;
    selectedOrder.value = null;
    
    fetchOrders();
  } catch (err) {
    alert(err.response?.data?.message || err.response?.data || 'Không thể xác nhận thanh toán.');
  } finally {
    paymentSubmitting.value = false;
  }
};

const printInvoice = () => {
  const printContents = document.getElementById('printable-invoice').innerHTML;
  const originalContents = document.body.innerHTML;
  
  document.body.innerHTML = printContents;
  window.print();
  document.body.innerHTML = originalContents;
  window.location.reload(); // reload lại trang để khôi phục event listeners của vue
};

const createPaymentQr = async () => {
  if (!selectedOrder.value) return;
  paymentQrLoading.value = true;
  paymentQrError.value = '';
  try {
    const response = await api.post(
      `/api/admin/orders/${selectedOrder.value.id}/payment-qr`,
      {},
      configHeader(),
    );
    paymentQr.value = response.data;
    selectedOrder.value.paymentOption = 'PREPAID_TRANSFER';
  } catch (error) {
    paymentQrError.value = error.response?.data?.message || error.response?.data || 'Không thể tạo QR thanh toán.';
  } finally {
    paymentQrLoading.value = false;
  }
};

const regeneratePaymentQr = async () => {
  if (!selectedOrder.value || !paymentQr.value?.paymentCode) return;
  paymentQrLoading.value = true;
  paymentQrError.value = '';
  try {
    const idempotencyKey = `order-qr-${selectedOrder.value.id}-${Date.now()}`;
    const response = await api.post(
      `/api/admin/orders/${selectedOrder.value.id}/payment-qr/${paymentQr.value.paymentCode}/regenerate`,
      {},
      { ...configHeader(), headers: { ...configHeader().headers, 'X-Idempotency-Key': idempotencyKey } },
    );
    paymentQr.value = response.data;
  } catch (error) {
    paymentQrError.value = error.response?.data?.message || error.response?.data || 'Không thể tạo lại QR thanh toán.';
  } finally {
    paymentQrLoading.value = false;
  }
};

const cancelOrderAndRefund = async (order) => {
  if (!confirm('Bạn có chắc muốn HỦY BÀN này? Nếu hủy, khách sẽ được hoàn lại 50% tiền cọc (Thu ngân tự chuyển khoản ngoài).')) return;
  try {
    const res = await api.put(`/api/admin/orders/${order.id}/cancel-with-refund`, {}, configHeader());
    
    // Gửi thông báo cho Waiter update lại màu bàn
    if (stompClient) {
      stompClient.send("/app/order/cancel", {}, JSON.stringify({ message: "ORDER_CANCELLED" }));
    }

    alert(res.data.message || 'Hủy bàn thành công!');
    selectedOrder.value = null;
    fetchOrders();
  } catch (e) {
    alert('Lỗi hủy bàn: ' + (e.response?.data?.message || e.message));
  }
};

// Kết nối socket để nghe đơn hàng mới / update
const connectSocket = () => {
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {};
  const token = localStorage.getItem('token');
  stompClient.connect(token ? { Authorization: `Bearer ${token}` } : {}, () => {
    stompClient.subscribe('/topic/kitchen', () => {
      fetchOrders();
    });
    stompClient.subscribe('/topic/orders', () => {
      fetchOrders();
    });
  }, (err) => {
    console.error('Socket error', err);
    setTimeout(connectSocket, 5000);
  });
};

onMounted(() => {
  fetchOrders();
  connectSocket();
});

onUnmounted(() => {
  if (stompClient) stompClient.disconnect();
});
</script>

<style scoped>
.cashier-wrapper {
  background: var(--bg-root);
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.cashier-header {
  background: var(--bg-nav);
  color: var(--text-heading);
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(10px);
}
.brand { display: flex; align-items: center; gap: 15px; }
.brand-icon { font-size: 2.5rem; }
.brand h2 { margin: 0; font-size: 1.5rem; font-weight: 800; letter-spacing: 1px; color: var(--primary); }
.brand p { margin: 5px 0 0; font-size: 0.9rem; color: var(--text-muted); }
.btn-logout { background: transparent; border: 1px solid rgba(178,59,46,0.5); color: #B23B2E; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: bold; font-size: 1rem; transition: 0.3s; }
.btn-logout:hover { background: rgba(178,59,46,0.1); border-color: #B23B2E; transform: translateY(-2px); }

.cashier-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 20px;
  padding: 20px;
  flex: 1;
  overflow: hidden;
}

.orders-list-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-md);
  overflow-y: auto;
}
.orders-list-panel h3 { color: var(--text-heading); margin-top: 0; }
.empty-state { color: var(--text-muted); font-style: italic; }

/* Table Grid Redesign (Copied from Waiter) */
.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
  margin-top: 20px;
  padding: 20px 0;
}
.table-box {
  background: var(--bg-card);
  border-radius: 12px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid var(--border-light);
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
  transition: all 0.3s ease;
  cursor: pointer;
}
.table-box:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.3);
}
.table-box.selected-table {
  border-color: #5A6E45 !important;
  box-shadow: 0 0 15px rgba(90, 110, 69, 0.5) !important;
  transform: scale(1.02);
}
.table-box.table-empty { border-color: #2F8F5B; box-shadow: 0 0 10px rgba(47, 143, 91, 0.1); opacity: 0.7; }
.table-box.table-booked { border-color: #B98229; box-shadow: 0 0 10px rgba(185, 130, 41, 0.1); }
.table-box.table-occupied { border-color: #B23B2E; box-shadow: 0 0 10px rgba(178, 59, 46, 0.1); }
.table-box.table-cleaning { border-color: #C08A2E; box-shadow: 0 0 10px rgba(192, 138, 46, 0.1); }

.tc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.tc-capacity {
  background: rgba(255,255,255,0.05);
  color: #5A6E45;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: bold;
}
.tc-icon {
  font-size: 0.9rem;
  background: rgba(255,255,255,0.05);
  padding: 4px;
  border-radius: 6px;
}
.tc-center {
  text-align: center;
  margin-bottom: 15px;
  flex: 1;
}
.tc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin: 0 auto 8px auto;
}
.table-empty .tc-dot { background: #2F8F5B; box-shadow: 0 0 8px #2F8F5B; }
.table-booked .tc-dot { background: #B98229; box-shadow: 0 0 8px #B98229; }
.table-occupied .tc-dot { background: #B23B2E; box-shadow: 0 0 8px #B23B2E; }
.table-cleaning .tc-dot { background: #C08A2E; box-shadow: 0 0 8px #C08A2E; }

.tc-center h4 {
  margin: 0 0 5px 0;
  font-size: 1.2rem;
  font-weight: 900;
  color: #FFFFFF;
}
.tc-subtitle {
  margin: 0;
  font-size: 0.75rem;
  color: var(--text-muted);
  font-style: italic;
}
.tc-bottom {
  text-align: center;
  padding-top: 10px;
  border-top: 1px solid rgba(255,255,255,0.05);
}
.tc-status {
  font-size: 0.8rem;
  font-weight: bold;
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.invoice-panel {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  padding: 20px;
  border: 1px solid var(--border);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.empty-invoice { justify-content: center; align-items: center; color: var(--text-muted); font-style: italic; }

.invoice-paper {
  flex: 1;
  background: #FFFFFF;
  padding: 20px;
  font-family: var(--font-primary);
  color: #201D14;
  overflow-y: auto;
  border: 1px dashed #A6B0AA;
  margin-bottom: 20px;
  border-radius: var(--radius-sm);
}
.invoice-header { text-align: center; margin-bottom: 20px; }
.invoice-header h2 { margin: 0 0 5px 0; font-size: 1.4rem; }
.invoice-header p { margin: 2px 0; font-size: 0.85rem; }
.invoice-header h3 { margin: 15px 0; font-size: 1.2rem; }

.invoice-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 0.85rem; }
.invoice-table th, .invoice-table td { padding: 8px 0; text-align: right; border-bottom: 1px dotted #A6B0AA; }
.invoice-table th:first-child, .invoice-table td:first-child { text-align: left; }
.invoice-table th { font-weight: bold; border-bottom: 1px solid #201D14; }

.invoice-total { display: flex; justify-content: space-between; font-weight: bold; font-size: 1.1rem; margin: 10px 0; }
.invoice-footer { text-align: center; font-size: 0.8rem; margin-top: 30px; font-style: italic; }

.action-buttons { display: flex; gap: 10px; }
.action-buttons button {
  flex: 1; padding: 15px; font-size: 1.1rem; font-weight: bold; border-radius: 8px; border: none; cursor: pointer; transition: 0.3s;
}
.btn-print { background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border) !important; }
.btn-print:hover { background: rgba(255,255,255,0.05); }
.btn-pay { background: var(--primary); color: #201D14; }
.btn-pay:hover:not(:disabled) { background: #33422A; transform: translateY(-2px); box-shadow: 0 4px 15px rgba(90, 110, 69, 0.4); }
.btn-pay:disabled { background: #55503E; color: #7A7460; cursor: not-allowed; }
.btn-primary { background: var(--primary); color: #201D14; border: none; cursor: pointer; transition: 0.3s; }
.btn-primary:hover { background: #33422A; transform: translateY(-2px); }

/* Modal Styles */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(5px); }
.shift-modal { width: 550px; padding: 30px; background: var(--bg-card); color: var(--text-primary); border-radius: var(--radius-lg); border: 1px solid var(--border); box-shadow: 0 20px 50px rgba(0,0,0,0.5); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 15px; }
.modal-header h2 { margin: 0; color: var(--primary); }
.btn-close-modal { background: none; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; transition: 0.3s; }
.btn-close-modal:hover { color: #B23B2E; transform: rotate(90deg); }
.stats-box { display: flex; flex-direction: column; gap: 15px; margin-top: 20px; }
.stat-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: rgba(255,255,255,0.03); border-radius: 8px; font-size: 1.1rem; border: 1px solid var(--border-light); }
.stat-label { color: var(--text-secondary); }
.stat-value { font-weight: bold; font-size: 1.3rem; }
.text-primary { color: var(--primary); }
.text-danger { color: #B23B2E; }

/* Tabs */
.tab-btn { background: transparent; color: var(--text-muted); border: none; padding: 10px 20px; font-size: 1.1rem; font-weight: bold; cursor: pointer; transition: 0.3s; position: relative; }
.tab-btn:hover { color: var(--primary); }
.tab-btn.active { color: var(--primary); }
.tab-btn.active::after { content: ''; position: absolute; bottom: -10px; left: 0; width: 100%; height: 3px; background: var(--primary); border-radius: 3px 3px 0 0; }

@media print {
  .hide-on-print { display: none !important; }
}
</style>
