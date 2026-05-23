<template>
  <div class="waiter-wrapper">
    <header class="waiter-header">
      <div class="header-left">
        <div class="brand">
          <span class="brand-icon">🏃‍♂️</span>
          <div>
            <h2>ĐIỀU PHỐI PHỤC VỤ</h2>
            <p>Waiter Dashboard</p>
          </div>
        </div>
      </div>

      <div class="header-right">
        <div v-if="readyOrders.length > 0" class="alert-chip">
          <span class="alert-dot"></span>
          {{ readyOrders.length }} món cần bưng
        </div>
        <div class="live-indicator">
          <span class="live-dot"></span>
          <span>LIVE</span>
        </div>
        <button @click="handleLogout" class="btn-logout">🚪 Tan Ca</button>
      </div>
    </header>

    <main class="waiter-content">
      <!-- Món cần bưng ngay -->
      <section class="section">
        <div class="section-header">
          <h3 class="section-title">🔔 Món Đã Xong — Cần Bưng Ngay</h3>
          <span class="count-badge" :class="{ 'count-pulse': readyOrders.length > 0 }">
            {{ readyOrders.length }}
          </span>
        </div>

        <div v-if="readyOrders.length > 0" class="serve-grid">
          <div v-for="order in readyOrders" :key="order.id" class="serve-card">
            <div class="serve-card-glow"></div>
            <div class="serve-info">
              <h2 class="table-name">{{ getTableName(order.address) }}</h2>
              <p class="order-code">Mã đơn: <span>#{{ String(order.id).padStart(4, '0') }}</span></p>
            </div>
            <button @click="markAsServed(order.id)" class="btn-served">
              ✔ ĐÃ BƯNG RA BÀN
            </button>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon">🍽️</div>
          <p>Chưa có món nào từ bếp truyền ra</p>
        </div>
      </section>

      <!-- Tình trạng bàn -->
      <section class="section">
        <div class="section-header">
          <h3 class="section-title">📍 Tình Trạng Bàn</h3>
          <div class="legend">
            <span class="legend-item empty">🟢 Trống</span>
            <span class="legend-item booked">🟡 Đặt cọc</span>
            <span class="legend-item occupied">🔴 Có khách</span>
          </div>
        </div>

        <div class="table-grid">
          <div
            v-for="table in tables"
            :key="table.id"
            :class="['table-box', getTableClass(table.isOccupied)]"
          >
            <div class="table-status-dot"></div>
            <h4>{{ table.name }}</h4>
            <span class="table-status-text">
              {{ table.isOccupied === 0 ? 'Trống' : table.isOccupied === 1 ? 'Đã Cọc' : 'Có Khách' }}
            </span>

            <!-- Hành động Bàn -->
            <div class="table-actions" v-if="table.isOccupied === 2">
              <button @click="openInvoice(table)" class="btn-print">🖨️ In Bill</button>
              <button @click="openMoveTable(table)" class="btn-move">🔄 Chuyển Bàn</button>
              <button @click="checkoutTable(table)" class="btn-checkout">🏠 Khách Về</button>
            </div>
            <div class="table-actions" v-if="table.isOccupied === 2" style="margin-top: 5px;">
               <button @click="$router.push('/dine-in')" class="btn-add-item">➕ Gọi Thêm Món</button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Invoice Modal -->
    <div v-if="selectedOrder" class="modal-overlay" @click.self="closeModal">
      <div class="invoice-modal printable-area">
        <div class="modal-header hide-on-print">
          <h2>Hóa Đơn Tạm Tính - Bàn {{ selectedTableName }}</h2>
          <button @click="closeModal" class="btn-close">✖</button>
        </div>

        <div class="invoice-content">
          <div class="invoice-brand">
            <h1>FPOLY <span>RESTAURANT</span></h1>
            <p>Hóa Đơn Tạm Tính</p>
            <div class="brand-address">Bàn: {{ selectedTableName }}</div>
          </div>

          <table class="print-table">
            <thead>
              <tr>
                <th style="width:10%">STT</th>
                <th style="width:10%">Ảnh</th>
                <th style="width:30%">Tên Món</th>
                <th style="width:18%; text-align:right">Đơn Giá</th>
                <th style="width:12%; text-align:center">SL</th>
                <th style="width:20%; text-align:right">Thành Tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(detail, index) in selectedOrder.orderDetails" :key="index">
                <td style="text-align:center">{{ index + 1 }}</td>
                <td>
                  <img v-if="detail.product?.image" :src="detail.product.image" class="bill-thumb" />
                  <span v-else class="no-img-icon">🍽️</span>
                </td>
                <td><strong>{{ detail.product?.name }}</strong></td>
                <td style="text-align:right">{{ (detail.price / detail.quantity).toLocaleString() }}đ</td>
                <td style="text-align:center">{{ detail.quantity }}</td>
                <td style="text-align:right; font-weight: bold;">{{ detail.price.toLocaleString() }}đ</td>
              </tr>
            </tbody>
          </table>

          <div class="invoice-total">
            <div class="total-row">
              <span>TỔNG CỘNG:</span>
              <span>{{ calculateTotal(selectedOrder).toLocaleString() }} đ</span>
            </div>
          </div>

          <div class="qr-payment">
            <img :src="`https://img.vietqr.io/image/vietcombank-1047187126-compact2.png?amount=${calculateTotal(selectedOrder)}&addInfo=Thanh toan ban ${selectedTableName}&accountName=NGUYEN QUANG NHAT`" alt="QR Code" />
            <p>Quét QR để thanh toán</p>
          </div>

          <div class="invoice-footer">
            <p>Cảm ơn quý khách!</p>
          </div>
        </div>

        <div class="modal-actions hide-on-print">
          <button @click="printInvoice" class="btn-export">🖨️ IN HÓA ĐƠN NÀY</button>
        </div>
      </div>
    </div>

    <!-- Modal Chuyển Bàn -->
    <div v-if="showMoveModal" class="modal-overlay" @click.self="showMoveModal = false">
      <div class="move-modal">
        <div class="modal-header">
          <h2>Chuyển Khách Tự Động</h2>
          <button @click="showMoveModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <p>Từ: <strong>Bàn {{ movingTable?.name }}</strong></p>
          <label>Chọn bàn mới (Trống):</label>
          <select v-model="targetTableId" class="select-table">
            <option value="" disabled>-- Vui lòng chọn bàn trống --</option>
            <option v-for="t in emptyTables" :key="t.id" :value="t.id">
              Bàn {{ t.name }}
            </option>
          </select>
          <div class="move-actions">
            <button @click="confirmMoveTable" class="btn-confirm-move" :disabled="!targetTableId">Xác Nhận Chuyển</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div v-if="toastMsg" class="toast-notification">
      {{ toastMsg }}
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import axios from 'axios';

const toastMsg = ref('');

const orders = ref([]);
const tables = ref([]);

// FIX LỖI ÉP KIỂU: Dùng Number() để đảm bảo lọc đúng số 2
const readyOrders = computed(() => orders.value.filter(o => Number(o.status) === 2));

const getTableName = (address) => {
  if (!address) return 'Ship / Mang về';
  const match = address.match(/Bàn:\s*([^|]+)/);
  return match ? match[1].trim() : 'Ship / Mang về';
};

const getTableClass = (status) => {
  if (status === 0) return 'table-empty';
  if (status === 1) return 'table-booked';
  return 'table-occupied';
};

const fetchData = async () => {
  try {
    const token = localStorage.getItem('token');

    const resOrders = await axios.get('http://localhost:8080/api/admin/orders', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    orders.value = resOrders.data;

    const resTables = await axios.get('http://localhost:8080/api/tables');
    tables.value = resTables.data;

  } catch (error) {
    if (error.response && error.response.status === 403) {
      alert('❌ LỖI 403: Spring Security ở Backend đang CHẶN không cho Phục vụ lấy đơn!');
    }
    console.error('Lỗi lấy dữ liệu phục vụ:', error);
  }
};

const markAsServed = async (id) => {
  try {
    await axios.put(`http://localhost:8080/api/admin/orders/${id}/status?status=3`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    fetchData();
  } catch (error) { alert('Lỗi hệ thống!'); }
};

// Nút KHÁCH VỀ: dọn bàn + tự động chốt đơn hàng của bàn đó
const checkoutTable = async (table) => {
  const confirmed = confirm(
    `🏠 Xác nhận Khách Về tại "${table.name}"?\n\n` +
    `✅ Hệ thống sẽ:\n` +
    `  • Đổi trạng thái bàn → Trống\n` +
    `  • Tự động chốt tất cả đơn hàng tại bàn này → Hoàn thành\n\n` +
    `Đã thu tiền xong chưa?`
  );
  if (!confirmed) return;

  try {
    await axios.put(`http://localhost:8080/api/tables/${table.id}/status?status=0`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    showCheckoutToast(table.name);
    fetchData();
  } catch (error) {
    if (error.response?.status === 403) {
      alert('❌ Lỗi 403: Tài khoản Phục vụ chưa được cấp quyền dọn bàn!');
    } else {
      alert('Lỗi khi dọn bàn: ' + (error.response?.data || error.message));
    }
  }
};

// In Hóa Đơn Tạm Tính
const selectedOrder = ref(null);
const selectedTableName = ref('');

const getActiveOrderForTable = (tableName) => {
  return orders.value.find(o => 
    o.status !== 4 && 
    o.address && 
    o.address.includes(`Bàn: ${tableName}`)
  );
};

const openInvoice = (table) => {
  const activeOrder = getActiveOrderForTable(table.name);
  if (activeOrder) {
    selectedOrder.value = activeOrder;
    selectedTableName.value = table.name;
  } else {
    alert('Không tìm thấy đơn hàng nào đang mở cho bàn này!');
  }
};

const closeModal = () => { selectedOrder.value = null; };

const calculateTotal = (order) => {
  if (!order?.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + item.price, 0);
};

const printInvoice = () => { window.print(); };

// Chuyển Bàn Logic
const showMoveModal = ref(false);
const movingTable = ref(null);
const targetTableId = ref("");

const emptyTables = computed(() => {
  return tables.value.filter(t => t.isOccupied === 0);
});

const openMoveTable = (table) => {
  movingTable.value = table;
  targetTableId.value = "";
  showMoveModal.value = true;
};

const confirmMoveTable = async () => {
  if (!targetTableId.value || !movingTable.value) return;

  const activeOrder = getActiveOrderForTable(movingTable.value.name);
  if (!activeOrder) {
    alert("Không tìm thấy đơn hàng của bàn này!");
    return;
  }

  const newTable = tables.value.find(t => t.id === targetTableId.value);
  const token = localStorage.getItem('token');
  
  try {
    // 1. Cập nhật địa chỉ đơn hàng sang bàn mới
    const oldAddress = activeOrder.address;
    const newAddress = oldAddress.replace(`Bàn: ${movingTable.value.name}`, `Bàn: ${newTable.name}`);
    
    await axios.put(`http://localhost:8080/api/admin/orders/${activeOrder.id}/address?newAddress=${encodeURIComponent(newAddress)}`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // 2. Set bàn mới thành Có Khách (2)
    await axios.put(`http://localhost:8080/api/tables/${newTable.id}/status?status=2`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    // 3. Set bàn cũ thành Trống (0)
    await axios.put(`http://localhost:8080/api/tables/${movingTable.value.id}/status?status=0`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    showMoveModal.value = false;
    toastMsg.value = `✅ Đã chuyển khách sang Bàn ${newTable.name} thành công!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    fetchData();
  } catch (error) {
    console.error("Lỗi chuyển bàn", error);
    alert("Có lỗi xảy ra khi chuyển bàn!");
  }
};

// Hiệu ứng toast thông báo dọn bàn thành công
const showCheckoutToast = (tableName) => {
  toastMsg.value = `✅ Bàn "${tableName}" đã dọn xong!`;
  setTimeout(() => { toastMsg.value = ''; }, 3500);
};

const handleLogout = () => {
  if (confirm('Bạn có chắc chắn muốn đăng xuất tan ca không?')) {
    localStorage.clear();
    window.location.href = '/login';
  }
};

onMounted(() => {
  fetchData();
  setInterval(fetchData, 4000);
});
</script>

<style scoped>
.waiter-wrapper {
  background: var(--bg-root);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* HEADER */
.waiter-header {
  background: var(--bg-nav);
  border-bottom: 1px solid var(--border);
  padding: 0 24px;
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 2px 20px rgba(0,0,0,0.5);
}
.header-left { display: flex; align-items: center; }
.header-right { display: flex; align-items: center; gap: 16px; }

.brand { display: flex; align-items: center; gap: 14px; }
.brand-icon { font-size: 2rem; filter: drop-shadow(0 0 10px rgba(0, 212, 170, 0.5)); }
.brand h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--primary); letter-spacing: 1px; }
.brand p { margin: 0; font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 2px; }

.alert-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: rgba(231, 76, 60, 0.15);
  border: 1px solid rgba(231, 76, 60, 0.3);
  color: #e74c3c;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.83rem;
  font-weight: 700;
  animation: fadeInAlert 0.4s ease;
}
.alert-dot {
  width: 7px; height: 7px;
  background: #e74c3c;
  border-radius: 50%;
  animation: pulse-red 1.2s ease-in-out infinite;
}
@keyframes pulse-red {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.6; }
}

.live-indicator {
  display: flex; align-items: center; gap: 6px;
  color: var(--primary); font-size: 0.8rem; font-weight: 700; letter-spacing: 1px;
}
.live-dot {
  width: 8px; height: 8px;
  background: var(--primary); border-radius: 50%;
  animation: pulse-dot 1.5s ease-in-out infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

.btn-logout {
  background: rgba(231, 76, 60, 0.15);
  border: 1px solid rgba(231, 76, 60, 0.3);
  color: #e74c3c;
  padding: 8px 18px;
  border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.88rem; font-family: inherit;
  transition: var(--transition);
}
.btn-logout:hover { background: rgba(231,76,60,0.3); }

/* CONTENT */
.waiter-content {
  flex: 1;
  padding: 28px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* Section */
.section { margin-bottom: 40px; }
.section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}
.section-title {
  font-size: 1.1rem; font-weight: 700;
  color: var(--text-heading);
}
.count-badge {
  background: var(--primary-glow);
  color: var(--primary);
  border: 1px solid var(--border);
  padding: 4px 14px;
  border-radius: 20px;
  font-weight: 800;
  font-size: 1rem;
}
.count-pulse { animation: pop 0.3s ease; }
@keyframes pop {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}
.legend { display: flex; gap: 12px; }
.legend-item { font-size: 0.8rem; font-weight: 600; padding: 4px 12px; border-radius: 20px; }
.legend-item.empty { background: rgba(0,212,170,0.1); color: var(--primary); }
.legend-item.booked { background: rgba(241,196,15,0.1); color: #f1c40f; }
.legend-item.occupied { background: rgba(231,76,60,0.1); color: #e74c3c; }

/* Serve Grid */
.serve-grid { display: flex; flex-direction: column; gap: 14px; }
.serve-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 4px solid #e74c3c;
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  animation: slideIn 0.4s ease;
  transition: var(--transition);
}
.serve-card:hover { box-shadow: 0 0 30px rgba(231,76,60,0.15), var(--shadow-md); }
.serve-card-glow {
  position: absolute;
  left: 0; top: 0; bottom: 0; width: 200px;
  background: linear-gradient(90deg, rgba(231,76,60,0.05), transparent);
  pointer-events: none;
}
@keyframes slideIn {
  from { transform: translateX(-20px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

.table-name { margin: 0 0 6px 0; font-size: 1.9rem; font-weight: 900; color: var(--text-heading); }
.order-code { margin: 0; color: var(--text-muted); font-size: 0.9rem; }
.order-code span { color: var(--primary); font-weight: 700; }

.btn-served {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  border: none;
  padding: 14px 28px;
  font-weight: 800; font-size: 0.95rem; font-family: inherit;
  border-radius: 30px; cursor: pointer;
  transition: var(--transition);
  white-space: nowrap;
}
.btn-served:hover { transform: scale(1.05); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }

/* Empty */
.empty-state { text-align: center; padding: 40px; color: var(--text-muted); }
.empty-icon { font-size: 3rem; margin-bottom: 10px; }
.empty-state p { font-size: 0.95rem; }

/* Table Grid */
.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 14px;
}
.table-box {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 18px 12px;
  text-align: center;
  position: relative;
  transition: var(--transition);
}
.table-box:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }
.table-status-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  margin: 0 auto 10px auto;
}
.table-box h4 { margin: 0 0 6px 0; font-size: 0.95rem; color: var(--text-primary); }
.table-status-text { font-size: 0.8rem; font-weight: 600; }

/* Table Status Colors */
.table-empty { border-color: rgba(0,212,170,0.3); }
.table-empty .table-status-dot { background: var(--primary); box-shadow: 0 0 8px rgba(0,212,170,0.6); }
.table-empty .table-status-text { color: var(--primary); }

.table-booked { border-color: rgba(241,196,15,0.3); }
.table-booked .table-status-dot { background: #f1c40f; box-shadow: 0 0 8px rgba(241,196,15,0.6); }
.table-booked .table-status-text { color: #f1c40f; }

.table-occupied { border-color: rgba(231,76,60,0.3); }
.table-occupied .table-status-dot { background: #e74c3c; box-shadow: 0 0 8px rgba(231,76,60,0.6); }
.table-occupied .table-status-text { color: #e74c3c; }

/* Table Actions */
.table-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.btn-print {
  flex: 1;
  background: rgba(52, 152, 219, 0.1);
  color: #3498db;
  border: 1px solid rgba(52, 152, 219, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-print:hover { background: #3498db; color: white; }

.btn-move {
  flex: 1;
  background: rgba(243, 156, 18, 0.1);
  color: #f39c12;
  border: 1px solid rgba(243, 156, 18, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-move:hover { background: #f39c12; color: white; }

.btn-checkout {
  flex: 1;
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  border: 1px solid rgba(231, 76, 60, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-checkout:hover { background: #e74c3c; color: white; }

.btn-add-item {
  width: 100%;
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
  border: 1px solid rgba(46, 204, 113, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-add-item:hover { background: #2ecc71; color: white; }

/* Toast */
.toast-notification {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--bg-card);
  color: var(--primary);
  padding: 12px 24px;
  border-radius: 30px;
  border: 1px solid var(--primary);
  box-shadow: 0 0 20px rgba(0,212,170,0.3);
  font-weight: bold;
  z-index: 1000;
  animation: slideUp 0.3s ease;
}
@keyframes slideUp {
  from { transform: translate(-50%, 20px); opacity: 0; }
  to { transform: translate(-50%, 0); opacity: 1; }
}

/* Modal In Hóa Đơn Nhiệt */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.invoice-modal {
  background: white; color: #000; width: 320px; border-radius: 8px; overflow: hidden;
}
.modal-header {
  background: var(--bg-nav); color: white; display: flex; justify-content: space-between; padding: 10px;
}
.modal-header h2 { margin: 0; font-size: 1rem; color: var(--primary); }
.btn-close { background: none; border: none; color: white; cursor: pointer; font-size: 1.2rem; }
.invoice-content { padding: 20px; font-family: monospace; font-size: 0.9rem; }
.invoice-brand { text-align: center; border-bottom: 1px dashed #000; padding-bottom: 10px; margin-bottom: 10px; }
.invoice-brand h1 { margin: 0; font-size: 1.2rem; }
.print-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }
.print-table th, .print-table td { padding: 4px 2px; border-bottom: 1px dashed #ccc; vertical-align: middle; font-size: 0.8rem; }
.bill-thumb {
  width: 32px; height: 32px; border-radius: 4px; object-fit: cover;
  border: 1px solid #ddd;
}
.no-img-icon { font-size: 1.2rem; }
.total-row { display: flex; justify-content: space-between; font-weight: bold; font-size: 1.1rem; border-top: 1px dashed #000; padding-top: 10px; }
.qr-payment { text-align: center; margin-top: 20px; border-top: 1px dashed #000; padding-top: 10px; }
.qr-payment img { width: 150px; }
.invoice-footer { text-align: center; margin-top: 10px; font-size: 0.8rem; }
.modal-actions { padding: 10px; text-align: center; background: #f1f1f1; }
.btn-export { background: #3498db; color: white; border: none; padding: 10px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; width: 100%; }

@media print {
  body * { visibility: hidden; }
  .printable-area, .printable-area * { visibility: visible; }
  .printable-area { position: absolute; left: 0; top: 0; width: 80mm; }
  .hide-on-print { display: none !important; }
}

/* Modal Chuyển Bàn */
.move-modal {
  background: var(--bg-card);
  border-radius: 12px;
  width: 350px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
  animation: slideDown 0.3s ease;
}
.move-modal .modal-header {
  background: var(--bg-nav);
  padding: 15px 20px;
}
.move-modal .modal-body {
  padding: 20px;
  color: var(--text-primary);
}
.select-table {
  width: 100%;
  padding: 10px;
  margin-top: 10px;
  margin-bottom: 20px;
  border: 1px solid var(--border);
  background: var(--bg-input);
  color: var(--text-primary);
  border-radius: 6px;
}
.btn-confirm-move {
  width: 100%;
  padding: 12px;
  background: #f39c12;
  color: white;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-confirm-move:disabled {
  background: #7f8c8d;
  cursor: not-allowed;
}
.btn-confirm-move:not(:disabled):hover {
  background: #e67e22;
}

@keyframes slideDown {
  from { transform: translateY(-30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>