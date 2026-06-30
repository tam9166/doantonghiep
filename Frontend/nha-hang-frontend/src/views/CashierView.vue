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
        <button @click="$router.push('/staff')" class="btn-profile" style="background:#8e44ad; color:white; padding:10px 20px; border:none; border-radius:6px; font-weight:bold; cursor:pointer;">👤 Cá Nhân</button>
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
            :class="['table-box', getTableClass(table.isOccupied), { 'selected-table': selectedOrder && getTableName(selectedOrder.address) === table.name }]"
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
                <span v-if="table.isOccupied >= 2" style="color:#f1c40f; font-weight: bold;">
                  {{ getPendingTotalForTable(table.name).toLocaleString() }}đ
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
                  <img v-if="item.product.image" :src="item.product.image" style="width: 40px; height: 40px; border-radius: 4px; object-fit: cover; display: inline-block;" />
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
          <div v-if="(calculateTotal(selectedOrder) - (selectedOrder.deposit || 0)) < 0" style="text-align: right; color: var(--primary); font-style: italic;">
            (Thu ngân thối lại: {{ Math.abs(calculateTotal(selectedOrder) - (selectedOrder.deposit || 0)).toLocaleString() }} VNĐ)
          </div>

          <!-- Khu vực QR Code Thanh Toán -->
          <div class="qr-payment-section hide-on-print" v-if="!selectedOrder.isPaid" style="margin-top: 20px; padding: 15px; background: rgba(0, 212, 170, 0.05); border: 1px dashed var(--primary); border-radius: 8px;">
            <h4 style="text-align: center; margin-top: 0; margin-bottom: 10px; color: var(--primary); font-size: 1.1rem;">Quét Mã QR Để Thanh Toán</h4>
            <div style="text-align: center;">
              <img :src="vietQrUrl" alt="QR Thanh Toán" style="width: 180px; height: 180px; border-radius: 8px; padding: 5px; background: #fff;" />
              <p style="margin-top: 10px; margin-bottom: 2px; font-weight: bold; color: #000;">Chủ TK: NGUYEN QUANG NHAT</p>
              <p style="color: #555; font-size: 0.85rem; margin: 0;">Ngân hàng: Vietcombank</p>
            </div>
            <p style="text-align: center; margin-top: 10px; font-size: 0.8rem; color: #e74c3c; font-style: italic;">Khách chuyển khoản xong, vui lòng bấm "Xác Nhận Thanh Toán"</p>
          </div>

          <div class="invoice-footer" style="margin-top: 20px;">
            <p>Cảm ơn quý khách & Hẹn gặp lại!</p>
            <p>Hóa đơn được in từ hệ thống Mộc Vị RESTAURANT System</p>
          </div>
        </div>
        
        <div class="action-buttons">
          <button class="btn-print" @click="printInvoice">🖨️ In Hóa Đơn</button>
          <button v-if="!selectedOrder.isPaid && (selectedOrder.status === 0 || selectedOrder.status === 1 || selectedOrder.status === 5)" class="btn-print" style="background: var(--danger); border-color: var(--danger);" @click="cancelOrderAndRefund(selectedOrder)">❌ Hủy & Hoàn Cọc</button>
          <button class="btn-pay" @click="payOrder" :disabled="selectedOrder.isPaid">
            {{ selectedOrder.isPaid ? '✅ Đã Thanh Toán' : '💰 Xác Nhận Thanh Toán' }}
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
          <p v-if="shiftStats.unpaidCount > 0" class="text-danger" style="margin-top: 15px; font-weight: bold; text-align: center; border: 1px dashed #e74c3c; padding: 10px;">
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
          <button @click="printShiftReport" class="btn-primary" style="background: var(--primary); color: #000; border: none; padding: 10px 20px; border-radius: 6px;">🖨️ In Báo Cáo</button>
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
        <div class="modal-body" id="printable-history-invoice" style="background: white; color: black; padding: 20px; border-radius: 8px;">
          <div style="text-align: center; border-bottom: 2px dashed #ccc; padding-bottom: 10px; margin-bottom: 15px;">
            <h2 style="margin: 0; font-size: 1.5rem;">Mộc Vị RESTAURANT</h2>
            <p style="margin: 5px 0 0; font-size: 0.9rem;">HÓA ĐƠN ĐÃ THANH TOÁN</p>
            <p style="margin: 5px 0 0; font-size: 0.8rem;">Ngày: {{ new Date(historySelectedOrder.createDate).toLocaleString('vi-VN') }}</p>
            <p style="margin: 5px 0 0; font-size: 0.8rem;">Bàn: {{ getTableName(historySelectedOrder.address) }} | Mã HĐ: #{{ historySelectedOrder.id }}</p>
          </div>
          <table style="width: 100%; border-collapse: collapse; font-size: 0.9rem;">
            <thead>
              <tr style="border-bottom: 1px solid #ddd;">
                <th style="text-align: left; padding: 8px 0;">Món</th>
                <th style="text-align: center; padding: 8px 0;">SL</th>
                <th style="text-align: right; padding: 8px 0;">TTiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in historySelectedOrder.orderDetails" :key="item.id" style="border-bottom: 1px solid #f0f0f0;">
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import TimekeepingWidget from '../components/TimekeepingWidget.vue';

const router = useRouter();
const pendingOrders = ref([]);
const allOrders = ref([]);
const selectedOrder = ref(null);
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
  
  document.body.innerHTML = `<div style="padding: 20px; font-family: 'Courier New', Courier, monospace; color: #000; background: #fff;">${printContents}</div>`;
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
  if (!order || !order.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + item.price, 0);
};

const tables = ref([]);

const fetchTables = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/tables', configHeader());
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
  const order = pendingOrders.value.find(o => o.address && getTableName(o.address) === table.name);
  if (order) {
    selectedOrder.value = order;
  } else {
    selectedOrder.value = null;
    alert("Bàn này chưa gọi món hoặc đã thanh toán xong!");
  }
};

const getPendingTotalForTable = (tableName) => {
  const order = pendingOrders.value.find(o => o.address && getTableName(o.address) === tableName);
  return order ? calculateTotal(order) : 0;
};

const fetchOrders = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/admin/orders', configHeader());
    allOrders.value = res.data;
    // Thu ngân chỉ quan tâm đơn ăn tại quán, chưa thanh toán (hoặc đã giao = cần thanh toán)
    // address chứa "Bàn", isPaid == false hoặc null, status != 3 (Đã hủy)
    pendingOrders.value = res.data.filter(o => 
      o.address && o.address.includes('Bàn') && 
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
  if (!selectedOrder.value) return;
  if (!confirm('Xác nhận khách đã thanh toán tiền cho đơn hàng này?')) return;
  
  const token = localStorage.getItem('token');
  try {
    // Update order status
    await axios.put(`http://localhost:8080/api/admin/orders/${selectedOrder.value.id}/pay`, {}, configHeader());
    
    // Update table status to 3 (Cleaning) if it was a table order
    const tableName = getTableName(selectedOrder.value.address);
    const table = tables.value.find(t => t.name === tableName);
    if (table) {
      await axios.put(`http://localhost:8080/api/tables/${table.id}/status?status=3`, {}, { headers: { 'Authorization': `Bearer ${token}` } });
    }

    alert('Thanh toán thành công! Bàn đang chờ dọn dẹp.');
    selectedOrder.value.isPaid = true;
    selectedOrder.value = null;
    
    fetchOrders();
  } catch (err) {
    alert('Lỗi khi thanh toán');
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

const vietQrUrl = computed(() => {
  if (!selectedOrder.value) return '';
  const bank = 'vietcombank';
  const accountNo = '1047187126';
  const accountName = 'NGUYEN QUANG NHAT';
  const amount = Math.max(0, calculateTotal(selectedOrder.value) - (selectedOrder.value.deposit || 0));
  const tableName = getTableName(selectedOrder.value.address).replace(/\s/g, '');
  const addInfo = encodeURIComponent(`Thanh toan ${tableName}`);
  return `https://img.vietqr.io/image/${bank}-${accountNo}-compact2.png?amount=${amount}&addInfo=${addInfo}&accountName=${encodeURIComponent(accountName)}`;
});

const cancelOrderAndRefund = async (order) => {
  if (!confirm('Bạn có chắc muốn HỦY BÀN này? Nếu hủy, khách sẽ được hoàn lại 50% tiền cọc (Thu ngân tự chuyển khoản ngoài).')) return;
  const token = localStorage.getItem('token');
  try {
    const res = await axios.put(`http://localhost:8080/api/admin/orders/${order.id}/cancel-with-refund`, {}, configHeader());
    
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
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {};
  stompClient.connect({}, () => {
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
.btn-logout { background: transparent; border: 1px solid rgba(231,76,60,0.5); color: #e74c3c; padding: 10px 20px; border-radius: 6px; cursor: pointer; font-weight: bold; font-size: 1rem; transition: 0.3s; }
.btn-logout:hover { background: rgba(231,76,60,0.1); border-color: #e74c3c; transform: translateY(-2px); }

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
  border-color: #3498db !important;
  box-shadow: 0 0 15px rgba(52,152,219,0.5) !important;
  transform: scale(1.02);
}
.table-box.table-empty { border-color: #2ecc71; box-shadow: 0 0 10px rgba(46, 204, 113, 0.1); opacity: 0.7; }
.table-box.table-booked { border-color: #f1c40f; box-shadow: 0 0 10px rgba(241, 196, 15, 0.1); }
.table-box.table-occupied { border-color: #e74c3c; box-shadow: 0 0 10px rgba(231, 76, 60, 0.1); }
.table-box.table-cleaning { border-color: #9b59b6; box-shadow: 0 0 10px rgba(155, 89, 182, 0.1); }

.tc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.tc-capacity {
  background: rgba(255,255,255,0.05);
  color: #3498db;
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
.table-empty .tc-dot { background: #2ecc71; box-shadow: 0 0 8px #2ecc71; }
.table-booked .tc-dot { background: #f1c40f; box-shadow: 0 0 8px #f1c40f; }
.table-occupied .tc-dot { background: #e74c3c; box-shadow: 0 0 8px #e74c3c; }
.table-cleaning .tc-dot { background: #9b59b6; box-shadow: 0 0 8px #9b59b6; }

.tc-center h4 {
  margin: 0 0 5px 0;
  font-size: 1.2rem;
  font-weight: 900;
  color: #fff;
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
  background: #fff;
  padding: 20px;
  font-family: 'Courier New', Courier, monospace;
  color: #000;
  overflow-y: auto;
  border: 1px dashed #bdc3c7;
  margin-bottom: 20px;
  border-radius: var(--radius-sm);
}
.invoice-header { text-align: center; margin-bottom: 20px; }
.invoice-header h2 { margin: 0 0 5px 0; font-size: 1.4rem; }
.invoice-header p { margin: 2px 0; font-size: 0.85rem; }
.invoice-header h3 { margin: 15px 0; font-size: 1.2rem; }

.invoice-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 0.85rem; }
.invoice-table th, .invoice-table td { padding: 8px 0; text-align: right; border-bottom: 1px dotted #ccc; }
.invoice-table th:first-child, .invoice-table td:first-child { text-align: left; }
.invoice-table th { font-weight: bold; border-bottom: 1px solid #000; }

.invoice-total { display: flex; justify-content: space-between; font-weight: bold; font-size: 1.1rem; margin: 10px 0; }
.invoice-footer { text-align: center; font-size: 0.8rem; margin-top: 30px; font-style: italic; }

.action-buttons { display: flex; gap: 10px; }
.action-buttons button {
  flex: 1; padding: 15px; font-size: 1.1rem; font-weight: bold; border-radius: 8px; border: none; cursor: pointer; transition: 0.3s;
}
.btn-print { background: var(--bg-card); color: var(--text-primary); border: 1px solid var(--border) !important; }
.btn-print:hover { background: rgba(255,255,255,0.05); }
.btn-pay { background: var(--primary); color: #000; }
.btn-pay:hover:not(:disabled) { background: #00b38f; transform: translateY(-2px); box-shadow: 0 4px 15px rgba(0,212,170,0.4); }
.btn-pay:disabled { background: #555; color: #999; cursor: not-allowed; }
.btn-primary { background: var(--primary); color: #000; border: none; cursor: pointer; transition: 0.3s; }
.btn-primary:hover { background: #00b38f; transform: translateY(-2px); }

/* Modal Styles */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(5px); }
.shift-modal { width: 550px; padding: 30px; background: var(--bg-card); color: var(--text-primary); border-radius: var(--radius-lg); border: 1px solid var(--border); box-shadow: 0 20px 50px rgba(0,0,0,0.5); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; border-bottom: 1px solid var(--border); padding-bottom: 15px; }
.modal-header h2 { margin: 0; color: var(--primary); }
.btn-close-modal { background: none; border: none; color: var(--text-muted); font-size: 1.5rem; cursor: pointer; transition: 0.3s; }
.btn-close-modal:hover { color: #e74c3c; transform: rotate(90deg); }
.stats-box { display: flex; flex-direction: column; gap: 15px; margin-top: 20px; }
.stat-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 20px; background: rgba(255,255,255,0.03); border-radius: 8px; font-size: 1.1rem; border: 1px solid var(--border-light); }
.stat-label { color: var(--text-secondary); }
.stat-value { font-weight: bold; font-size: 1.3rem; }
.text-primary { color: var(--primary); }
.text-danger { color: #e74c3c; }

/* Tabs */
.tab-btn { background: transparent; color: var(--text-muted); border: none; padding: 10px 20px; font-size: 1.1rem; font-weight: bold; cursor: pointer; transition: 0.3s; position: relative; }
.tab-btn:hover { color: var(--primary); }
.tab-btn.active { color: var(--primary); }
.tab-btn.active::after { content: ''; position: absolute; bottom: -10px; left: 0; width: 100%; height: 3px; background: var(--primary); border-radius: 3px 3px 0 0; }

@media print {
  .hide-on-print { display: none !important; }
}
</style>
