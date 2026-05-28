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
        <button @click="$router.push('/staff')" class="btn-profile" style="background:#8e44ad; color:white; padding:8px 15px; border:none; border-radius:6px; font-weight:bold; cursor:pointer;">👤 Cá Nhân</button>
        <button @click="handleLogout" class="btn-logout">🚪 Tan Ca</button>
      </div>
    </header>

    <TimekeepingWidget />

    <!-- Stats Bar -->
    <div class="stats-bar">
      <div class="stat-item stat-urgent">
        <span class="stat-value">{{ readyOrders.length }}</span>
        <span class="stat-label">Cần Bưng</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ cookingOrders.length }}</span>
        <span class="stat-label">Đang Nấu</span>
      </div>
      <div class="stat-item stat-active">
        <span class="stat-value">{{ occupiedTables.length }}</span>
        <span class="stat-label">Bàn Có Khách</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ emptyTables.length }}</span>
        <span class="stat-label">Bàn Trống</span>
      </div>
      <div class="stat-item stat-done">
        <span class="stat-value">{{ todayServed }}</span>
        <span class="stat-label">Đã Bưng Hôm Nay</span>
      </div>
    </div>

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
            <div class="serve-main">
              <div class="serve-top">
                <div class="serve-info">
                  <h2 class="table-name">{{ getTableName(order.address) }}</h2>
                  <p class="order-code">Mã đơn: <span>#{{ String(order.id).padStart(4, '0') }}</span></p>
                </div>
                <div class="serve-timer">
                  <span :class="['timer-badge', getServeTimerClass(order)]">
                    ⏱️ {{ getElapsedTime(order.createDate) }}
                  </span>
                </div>
              </div>
              <!-- Chi tiết món ăn -->
              <div class="serve-dishes">
                <template v-for="(detail, idx) in order.orderDetails" :key="idx">
                  <div class="serve-dish-item" v-if="Number(order.status) === 2 || detail.status === 1">
                    <img v-if="detail.product?.image" :src="detail.product.image" class="serve-dish-thumb" />
                    <span v-else class="serve-dish-icon">🍽️</span>
                    <span class="serve-dish-name">{{ detail.product?.name || 'Món ăn' }}</span>
                    <span class="serve-dish-qty">x{{ detail.quantity }}</span>
                    <button v-if="Number(order.status) === 6" @click="markDishServed(order, detail.id)" class="btn-dish-served" style="margin-left: auto;">✅ Đã Bưng</button>
                    <span v-else class="serve-dish-price">{{ detail.price?.toLocaleString() }}đ</span>
                  </div>
                </template>
              </div>
            </div>
            <button v-if="Number(order.status) === 2" @click="markAsServed(order.id)" class="btn-served">
              ✔ BƯNG TOÀN BỘ BÀN NÀY
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
            <span class="legend-item cleaning">🟣 Cần dọn</span>
          </div>
        </div>

        <div v-for="(tablesInFloor, floorName) in tablesByFloor" :key="floorName" class="floor-section">
          <div class="floor-header">
            <h3 class="floor-title">
              <span v-if="floorName.toLowerCase().includes('vip')">👑</span>
              <span v-else-if="floorName.toLowerCase().includes('thượng')">☁️</span>
              <span v-else>🏢</span>
              {{ floorName }}
            </h3>
            <div class="floor-divider"></div>
          </div>

          <div class="table-grid">
            <div
              v-for="table in tablesInFloor"
              :key="table.id"
              :class="['table-box', getTableClass(table.isOccupied), { 'vip-table': table.floor && table.floor.toLowerCase().includes('vip') }]"
              @click="openTableDetail(table)"
            >
              <div class="table-chairs"></div>
              <div class="table-content">
                <h4>{{ table.name }}</h4>
                <span class="table-status-text">
                  {{ table.isOccupied === 0 ? 'Trống' : table.isOccupied === 1 ? 'Đã Cọc' : table.isOccupied === 3 ? 'Cần Dọn' : 'Có Khách' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- Modal Chi Tiết Đơn Tại Bàn -->
    <div v-if="detailTable" class="modal-overlay" @click.self="detailTable = null">
      <div class="detail-modal">
        <div class="modal-header">
          <h2>📋 Chi Tiết — {{ detailTable.name }}</h2>
          <button @click="detailTable = null" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <div v-if="detailOrder">
            <div class="detail-meta">
              <span>Mã đơn: <strong>#{{ String(detailOrder.id).padStart(4, '0') }}</strong></span>
              <span :class="['g-badge', getDetailStatusClass(detailOrder.status)]">
                {{ getDetailStatusText(detailOrder.status) }}
              </span>
            </div>
            <div class="detail-dishes">
              <div v-for="(detail, idx) in detailOrder.orderDetails" :key="idx" class="detail-dish-row">
                <div class="detail-dish-left">
                  <img v-if="detail.product?.image" :src="detail.product.image" class="detail-dish-img" />
                  <span v-else class="detail-dish-placeholder">🍽️</span>
                  <div>
                    <strong>{{ detail.product?.name }}</strong>
                    <span class="detail-dish-qty">x{{ detail.quantity }}</span>
                  </div>
                </div>
                <span class="detail-dish-price">{{ detail.price?.toLocaleString() }}đ</span>
              </div>
            </div>
            <div class="detail-total">
              <span>TỔNG TẠM TÍNH:</span>
              <span>{{ calculateTotal(detailOrder).toLocaleString() }} đ</span>
            </div>
            
            <div class="ai-upsell-action">
              <button @click="getAiUpsellAdvice" class="btn-ai-analyze">💡 AI Gợi Ý Mời Món</button>
            </div>

            <!-- Hành động Bàn CÓ KHÁCH -->
            <div class="modal-table-actions" style="margin-top: 20px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,0.1); display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
               <button @click="openMoveTable(detailTable)" class="btn-action-large" style="background: rgba(243, 156, 18, 0.2); color: #f39c12; border: 1px solid #f39c12;">🔄 Chuyển Bàn</button>
               <button @click="goAddItem(detailTable)" class="btn-action-large" style="background: rgba(46, 204, 113, 0.2); color: #2ecc71; border: 1px solid #2ecc71;">➕ Gọi Thêm</button>
               <button @click="openMergeTable(detailTable)" class="btn-action-large" style="background: rgba(52, 152, 219, 0.2); color: #3498db; border: 1px solid #3498db;">🔗 Gộp Bàn</button>
               <button @click="openSplitTable(detailTable)" class="btn-action-large" style="background: rgba(230, 126, 34, 0.2); color: #e67e22; border: 1px solid #e67e22;">✂️ Tách Bàn</button>
               <button @click="markAsCleaning(detailTable)" class="btn-action-large" style="grid-column: span 2; background: rgba(155, 89, 182, 0.2); color: #9b59b6; border: 1px solid #9b59b6;">🏠 Khách Đã Về (Cần Dọn)</button>
            </div>
          </div>
          <div v-else-if="detailTable.isOccupied === 3" style="padding: 30px; text-align: center;">
            <p style="font-size: 1.2rem; margin-bottom: 20px;">Bàn này đang cần dọn dẹp</p>
            <button @click="checkoutTable(detailTable)" class="btn-action-large" style="width: 100%; background: rgba(46, 204, 113, 0.2); color: #2ecc71; border: 1px solid #2ecc71;">✅ Đã Dọn Xong</button>
          </div>
          <div v-else-if="detailTable.isOccupied === 1" style="padding: 30px; text-align: center;">
            <p style="font-size: 1.2rem; margin-bottom: 20px;">Bàn đang được khách đặt cọc trước</p>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
              <button @click="upgradeToOccupied(detailTable)" class="btn-action-large" style="background: rgba(46, 204, 113, 0.2); color: #2ecc71; border: 1px solid #2ecc71;">✅ Khách Đã Đến</button>
              <button @click="cancelBooking(detailTable)" class="btn-action-large" style="background: rgba(231, 76, 60, 0.2); color: #e74c3c; border: 1px solid #e74c3c;">❌ Khách Hủy</button>
            </div>
          </div>
          <div v-else class="empty-state" style="padding: 30px;">
            <p>Bàn này đang trống</p>
            <button @click="goAddItem(detailTable)" class="btn-action-large" style="width: 100%; margin-top: 15px; background: rgba(46, 204, 113, 0.2); color: #2ecc71; border: 1px solid #2ecc71;">👨‍👩‍👧 Đón Khách Mới</button>
          </div>
        </div>
      </div>
    </div>

    <!-- AI Modal -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
      <div class="ai-modal">
        <div class="modal-header">
          <h2>💡 Chuyên Gia Bán Chéo AI</h2>
          <button @click="showAiModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <div v-if="aiLoading" class="ai-loading">
            <div class="spinner"></div>
            <p>AI đang "liếc" xem khách đang ăn gì...</p>
          </div>
          <div v-else class="ai-result">
            <p>{{ aiResponse }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Invoice Modal -->
    <div v-if="selectedOrder" class="modal-overlay" @click.self="closeModal">
      <div class="invoice-modal printable-area">
        <div class="modal-header hide-on-print">
          <h2>Hóa Đơn Tạm Tính - Bàn {{ selectedTableName }}</h2>
          <button @click="closeModal" class="btn-close">✖</button>
        </div>

        <div class="invoice-content">
          <div class="invoice-brand">
            <h1>Mộc Vị <span>RESTAURANT</span></h1>
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
          <h2>🔄 Chuyển Bàn</h2>
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

    <!-- Modal Gộp Bàn -->
    <div v-if="showMergeModal" class="modal-overlay" @click.self="showMergeModal = false">
      <div class="move-modal">
        <div class="modal-header">
          <h2>🔗 Gộp Bàn</h2>
          <button @click="showMergeModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <p>Gộp toàn bộ món từ <strong>Bàn {{ movingTable?.name }}</strong> sang:</p>
          <label>Chọn bàn cần gộp vào (Có Khách):</label>
          <select v-model="targetMergeTableId" class="select-table">
            <option value="" disabled>-- Vui lòng chọn bàn --</option>
            <option v-for="t in occupiedTables.filter(x => x.id !== movingTable?.id)" :key="t.id" :value="t.id">
              Bàn {{ t.name }}
            </option>
          </select>
          <div class="move-actions">
            <button @click="confirmMergeTable" class="btn-confirm-move" :disabled="!targetMergeTableId" style="background:#3498db">Gộp Bàn Ngay</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Tách Bàn -->
    <div v-if="showSplitModal" class="modal-overlay" @click.self="showSplitModal = false">
      <div class="move-modal" style="width: 500px;">
        <div class="modal-header">
          <h2>✂️ Tách Bàn (Chuyển Món)</h2>
          <button @click="showSplitModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <p>Tách món từ <strong>Bàn {{ movingTable?.name }}</strong> sang một bàn trống mới.</p>
          
          <label>Chọn bàn mới để tách khách sang:</label>
          <select v-model="splitTargetTableId" class="select-table" style="margin-bottom: 15px;">
            <option value="" disabled>-- Vui lòng chọn bàn trống --</option>
            <option v-for="t in emptyTables" :key="t.id" :value="t.id">
              Bàn {{ t.name }}
            </option>
            <option v-for="t in occupiedTables.filter(x => x.id !== movingTable?.id)" :key="t.id" :value="t.id">
              Bàn {{ t.name }} (Có Khách)
            </option>
          </select>
          
          <label>Chọn các món cần tách sang bàn mới:</label>
          <div style="max-height: 300px; overflow-y: auto; background: rgba(0,0,0,0.2); padding: 10px; border-radius: 8px;">
            <div v-for="detail in splitSourceOrder?.orderDetails" :key="detail.id" style="display: flex; align-items: center; justify-content: space-between; padding: 10px; border-bottom: 1px solid rgba(255,255,255,0.1);">
              <div style="display: flex; align-items: center; gap: 10px;">
                <input type="checkbox" :id="'chk-'+detail.id" :checked="selectedDetailIds.includes(detail.id)" @change="toggleDetailSplit(detail.id)" style="width: 20px; height: 20px;" />
                <label :for="'chk-'+detail.id" style="margin:0; cursor:pointer;">
                  <strong>{{ detail.product?.name }}</strong> (x{{ detail.quantity }})
                </label>
              </div>
              <span>{{ detail.price.toLocaleString() }}đ</span>
            </div>
            <p v-if="splitSourceOrder?.orderDetails?.length === 0" style="text-align: center; color: #999;">Không có món nào.</p>
          </div>

          <div class="move-actions" style="margin-top: 20px;">
            <button @click="confirmSplitTable" class="btn-confirm-move" :disabled="!splitTargetTableId || selectedDetailIds.length === 0" style="background:#e67e22; width: 100%;">Xác Nhận Tách ({{ selectedDetailIds.length }} món)</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Toast Notification -->
    <div v-if="toastMsg" class="toast-notification">
      {{ toastMsg }}
    </div>

    <!-- Checkout Modal -->
    <div v-if="checkoutOrder" class="modal-overlay" @click.self="checkoutOrder = null">
      <div class="invoice-modal printable-area">
        <div class="modal-header hide-on-print">
          <h2>💰 Thanh Toán - Bàn {{ checkoutTableName }}</h2>
          <button @click="checkoutOrder = null" class="btn-close">✖</button>
        </div>

        <div class="invoice-content">
          <div class="invoice-brand">
            <h1>Mộc Vị <span>RESTAURANT</span></h1>
            <p>HÓA ĐƠN THANH TOÁN</p>
            <div class="brand-address">Bàn: {{ checkoutTableName }}</div>
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
              <tr v-for="(detail, index) in checkoutOrder.orderDetails" :key="index">
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
              <span>{{ calculateTotal(checkoutOrder).toLocaleString() }} đ</span>
            </div>
          </div>

          <div class="qr-payment">
            <img :src="`https://img.vietqr.io/image/vietcombank-1047187126-compact2.png?amount=${calculateTotal(checkoutOrder)}&addInfo=Thanh toan ban ${checkoutTableName}&accountName=NGUYEN QUANG NHAT`" alt="QR Code" />
            <p>Quét QR để thanh toán</p>
          </div>

          <div class="invoice-footer">
            <p>Cảm ơn quý khách!</p>
          </div>
        </div>

        <div class="modal-actions hide-on-print" style="display: flex; flex-direction: column; gap: 10px; padding: 15px;">
          <div style="display: flex; gap: 10px; align-items: center;">
            <input v-model="checkoutTxCode" type="text" placeholder="Nhập mã giao dịch..." style="flex:1; padding: 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 0.95rem;" />
          </div>
          <div style="display: flex; gap: 10px;">
            <button @click="printCheckoutInvoice" class="btn-export" style="flex:1; background: #3498db;">🖨️ In Hóa Đơn</button>
            <button @click="confirmCheckout" class="btn-export" style="flex:1;">✅ Xác Nhận Đã Thu Tiền</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import TimekeepingWidget from '../components/TimekeepingWidget.vue';

const router = useRouter();
const toastMsg = ref('');
const orders = ref([]);
const tables = ref([]);
const now = ref(new Date());
let timerInterval = null;
let previousReadyIds = [];
let stompClient = null;

const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

const tablesByFloor = computed(() => {
  const groups = {};
  tables.value.forEach(table => {
    let floorName = table.floor || 'Khu Vực Chung';
    if (!groups[floorName]) groups[floorName] = [];
    groups[floorName].push(table);
  });
  return Object.keys(groups).sort().reduce((acc, key) => {
    acc[key] = groups[key];
    return acc;
  }, {});
});

// FIX LỖI ÉP KIỂU: Dùng Number() để đảm bảo lọc đúng số 2
const readyOrders = computed(() => {
  return orders.value.filter(o => 
    Number(o.status) === 2 || 
    (Number(o.status) === 6 && o.orderDetails?.some(d => d.status === 1))
  );
});
const cookingOrders = computed(() => orders.value.filter(o => Number(o.status) === 1 || Number(o.status) === 6));
const occupiedTables = computed(() => tables.value.filter(t => t.isOccupied === 2 || t.isOccupied === 3));
const emptyTables = computed(() => tables.value.filter(t => t.isOccupied === 0));

// Thống kê hôm nay
const todayServed = computed(() => {
  const today = new Date().toDateString();
  return orders.value.filter(o => {
    if (!o.createDate) return false;
    return new Date(o.createDate).toDateString() === today && Number(o.status) >= 3;
  }).length;
});

// === AUDIO NOTIFICATION ===
const playNotificationSound = () => {
  try {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    [0, 0.12, 0.24].forEach((delay, i) => {
      const osc = audioCtx.createOscillator();
      const gain = audioCtx.createGain();
      osc.connect(gain);
      gain.connect(audioCtx.destination);
      osc.type = 'sine';
      osc.frequency.value = [660, 880, 1100][i];
      gain.gain.setValueAtTime(0.25, audioCtx.currentTime + delay);
      gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + delay + 0.25);
      osc.start(audioCtx.currentTime + delay);
      osc.stop(audioCtx.currentTime + delay + 0.25);
    });
  } catch (e) { /* silent */ }
};

// === TABLE NAME ===
const getTableName = (address) => {
  if (!address) return 'Ship / Mang về';
  const match = address.match(/Bàn:\s*(.*?)\s*\|/);
  return match ? match[1].trim() : address;
};

const getTableClass = (status) => {
  if (status === 0) return 'table-empty';
  if (status === 1) return 'table-booked';
  if (status === 3) return 'table-cleaning';
  return 'table-occupied';
};

// === ELAPSED TIME ===
const getElapsedTime = (createDate) => {
  if (!createDate) return '';
  const elapsed = Math.floor((now.value - new Date(createDate)) / 1000);
  const mins = Math.floor(elapsed / 60);
  const secs = elapsed % 60;
  if (mins >= 60) {
    const hrs = Math.floor(mins / 60);
    return `${hrs}h ${mins % 60}p`;
  }
  return `${mins}:${String(secs).padStart(2, '0')}`;
};

const getElapsedMinutes = (createDate) => {
  if (!createDate) return 0;
  return Math.floor((now.value - new Date(createDate)) / 60000);
};

const getServeTimerClass = (order) => {
  const mins = getElapsedMinutes(order.createDate);
  if (mins >= 15) return 'timer-critical';
  if (mins >= 8) return 'timer-warning';
  return 'timer-normal';
};

// === FETCH DATA ===
const fetchData = async () => {
  try {
    const token = localStorage.getItem('token');

    const resOrders = await axios.get('http://localhost:8080/api/admin/orders', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    orders.value = resOrders.data;

    // Kiểm tra có đơn mới cần bưng không
    const newReady = resOrders.data.filter(o => Number(o.status) === 2);
    const newReadyIds = newReady.map(o => o.id);
    const hasNewReady = newReadyIds.some(id => !previousReadyIds.includes(id));

    if (hasNewReady && previousReadyIds.length > 0) {
      playNotificationSound();
      toastMsg.value = '🔔 Bếp vừa hoàn thành món mới!';
      setTimeout(() => { toastMsg.value = ''; }, 3000);
    }
    previousReadyIds = newReadyIds;

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
    await axios.put(`http://localhost:8080/api/admin/orders/${id}/status?status=7`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    toastMsg.value = '✅ Đã bưng ra bàn thành công!';
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchData();
  } catch (error) { alert('Lỗi hệ thống!'); }
};

const markDishServed = async (order, detailId) => {
  try {
    await axios.put(`http://localhost:8080/api/orders/details/${detailId}/status?status=2`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    toastMsg.value = '✅ Đã bưng món!';
    setTimeout(() => { toastMsg.value = ''; }, 2000);
    fetchData();
  } catch (err) { alert('Lỗi cập nhật!'); }
};

// Nút KHÁCH VỀ: chuyển bàn sang trạng thái Cần dọn (3)
const markAsCleaning = async (table) => {
  const hasUnpaidOrder = orders.value.some(o => 
    !o.isPaid && 
    Number(o.status) !== 3 && // Not cancelled
    Number(o.status) !== 4 && // Not completed
    o.address && getTableName(o.address) === table.name
  );

  if (hasUnpaidOrder) {
    alert(`❌ Khách bàn ${table.name} chưa thanh toán xong!\nVui lòng chờ Thu Ngân xác nhận thanh toán trước khi cho khách về.`);
    return;
  }

  const confirmed = confirm(
    `🏠 Xác nhận Khách Về tại "${table.name}"?\n\n` +
    `✅ Bàn sẽ được chuyển sang trạng thái Cần Dọn.`
  );
  if (!confirmed) return;

  try {
    await axios.put(`http://localhost:8080/api/tables/${table.id}/status?status=3`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    toastMsg.value = `🧹 Bàn "${table.name}" đang chờ dọn!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    fetchData();
  } catch (error) {
    alert('Lỗi khi cập nhật trạng thái bàn!');
  }
};

// Nút ĐÃ DỌN XONG: chuyển bàn về Trống (0)
const checkoutTable = async (table) => {
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

// === THANH TOÁN ===
const checkoutOrder = ref(null);
const checkoutTableName = ref('');
const checkoutTxCode = ref('');

const openCheckoutModal = (table) => {
  const activeOrder = getActiveOrderForTable(table.name);
  if (activeOrder) {
    checkoutOrder.value = activeOrder;
    checkoutTableName.value = table.name;
    checkoutTxCode.value = '';
  } else {
    alert('Không tìm thấy đơn hàng nào đang mở cho bàn này!');
  }
};

const confirmCheckout = async () => {
  if (!checkoutTxCode.value) return alert('Vui lòng nhập mã giao dịch để xác nhận thanh toán!');
  if (!checkoutOrder.value) return;
  
  const token = localStorage.getItem('token');
  try {
    // 1. Cập nhật đơn hàng → status 4 (Hoàn thành)
    await axios.put(`http://localhost:8080/api/admin/orders/${checkoutOrder.value.id}/status?status=4`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    
    // 2. Cập nhật bàn → status 3 (Cần dọn)
    const table = tables.value.find(t => t.name === checkoutTableName.value);
    if (table) {
      await axios.put(`http://localhost:8080/api/tables/${table.id}/status?status=3`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
    }
    
    checkoutOrder.value = null;
    toastMsg.value = `✅ Thanh toán bàn ${checkoutTableName.value} thành công! MãGD: ${checkoutTxCode.value}`;
    setTimeout(() => { toastMsg.value = ''; }, 4000);
    fetchData();
  } catch (error) {
    alert('Lỗi khi xác nhận thanh toán!');
  }
};

const printCheckoutInvoice = () => { window.print(); };

// === XEM CHI TIẾT ĐƠN TẠI BÀN ===
const detailTable = ref(null);
const detailOrder = computed(() => {
  if (!detailTable.value) return null;
  return getActiveOrderForTable(detailTable.value.name);
});

const openTableDetail = (table) => {
  detailTable.value = table;
};

const getDetailStatusText = (status) => {
  const map = { 1: '🔥 Đang nấu', 2: '✅ Đã xong', 3: '🍽️ Đang ăn', 4: '💰 Đã thanh toán' };
  return map[status] || 'Đang xử lý';
};

const getDetailStatusClass = (status) => {
  if (status === 1) return 'badge-cooking';
  if (status === 2) return 'badge-ready';
  if (status === 3) return 'badge-serving';
  return 'badge-done';
};

// ==============================
// 10. GỌI THÊM MÓN
// ==============================
const goAddItem = (table) => {
  router.push(`/dine-in?table=${encodeURIComponent(table.name)}`);
};

// Chuyển Bàn Logic
const showMoveModal = ref(false);
const movingTable = ref(null);
const targetTableId = ref("");

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

// ==============================
// 12. GỘP BÀN & TÁCH BÀN
// ==============================
const showMergeModal = ref(false);
const targetMergeTableId = ref("");

const openMergeTable = (table) => {
  movingTable.value = table;
  targetMergeTableId.value = "";
  showMergeModal.value = true;
};

const confirmMergeTable = async () => {
  if (!targetMergeTableId.value || !movingTable.value) return;

  const targetTable = tables.value.find(t => t.id === targetMergeTableId.value);
  if (!targetTable) return;

  const token = localStorage.getItem('token');
  try {
    await axios.post('http://localhost:8080/api/orders/merge-tables', {
      fromTable: movingTable.value.name,
      toTable: targetTable.name
    }, { headers: { 'Authorization': `Bearer ${token}` } });

    toastMsg.value = `✅ Đã gộp bàn ${movingTable.value.name} vào bàn ${targetTable.name} thành công!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    showMergeModal.value = false;
    fetchData();
  } catch(error) {
    alert(error.response?.data || "Lỗi gộp bàn!");
  }
};

const showSplitModal = ref(false);
const splitTargetTableId = ref("");
const splitSourceOrder = ref(null);
const selectedDetailIds = ref([]);

const openSplitTable = (table) => {
  movingTable.value = table;
  splitTargetTableId.value = "";
  selectedDetailIds.value = [];
  splitSourceOrder.value = getActiveOrderForTable(table.name);
  if (!splitSourceOrder.value) {
    alert("Không tìm thấy đơn hàng cho bàn này!");
    return;
  }
  showSplitModal.value = true;
};

const toggleDetailSplit = (id) => {
  if (selectedDetailIds.value.includes(id)) {
    selectedDetailIds.value = selectedDetailIds.value.filter(x => x !== id);
  } else {
    selectedDetailIds.value.push(id);
  }
};

const confirmSplitTable = async () => {
  if (!splitTargetTableId.value || selectedDetailIds.value.length === 0) {
    alert("Vui lòng chọn bàn đích và ít nhất 1 món để chuyển!");
    return;
  }
  
  const targetTable = tables.value.find(t => t.id === splitTargetTableId.value);
  const token = localStorage.getItem('token');
  try {
    await axios.post('http://localhost:8080/api/orders/split-table', {
      fromTable: movingTable.value.name,
      toTable: targetTable.name,
      detailIds: selectedDetailIds.value
    }, { headers: { 'Authorization': `Bearer ${token}` } });

    toastMsg.value = `✅ Đã tách ${selectedDetailIds.value.length} món sang bàn ${targetTable.name} thành công!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    showSplitModal.value = false;
    fetchData();
  } catch(error) {
    alert(error.response?.data || "Lỗi tách bàn!");
  }
};

// --- Hành động Bàn Đặt Cọc (1) ---
const upgradeToOccupied = async (table) => {
  if (!confirm(`Khách đặt trước bàn ${table.name} đã đến?`)) return;
  try {
    await axios.put(`http://localhost:8080/api/tables/${table.id}/status?status=2`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    toastMsg.value = `✅ Đã đánh dấu Bàn ${table.name} CÓ KHÁCH!`;
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchData();
  } catch (error) {
    alert("Lỗi khi cập nhật trạng thái bàn!");
  }
};

const cancelBooking = async (table) => {
  if (!confirm(`Xác nhận HỦY CỌC bàn ${table.name}? Bàn sẽ trở về trạng thái TRỐNG.`)) return;
  try {
    await axios.put(`http://localhost:8080/api/tables/${table.id}/status?status=0`, {}, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    toastMsg.value = `❌ Đã hủy cọc Bàn ${table.name}!`;
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchData();
  } catch (error) {
    alert("Lỗi khi hủy cọc bàn!");
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

// === AI ASSISTANT ===
const getAiUpsellAdvice = async () => {
  if (!detailOrder.value || !detailOrder.value.orderDetails) return;

  showAiModal.value = true;
  aiLoading.value = true;
  aiResponse.value = '';

  const dishList = detailOrder.value.orderDetails
    .map(d => d.product?.name)
    .filter(name => name)
    .join(', ');

  if (!dishList) {
    aiResponse.value = "Chưa có món nào để xem xét!";
    aiLoading.value = false;
    return;
  }

  try {
    const res = await axios.post('http://localhost:8080/api/chatbot/chat', {
      type: 'WAITER_UPSELL',
      message: JSON.stringify(dishList)
    });
    aiResponse.value = res.data.reply;
  } catch (error) {
    aiResponse.value = "Xin lỗi, AI đang bận rùi!";
  } finally {
    aiLoading.value = false;
  }
};

// === WEBSOCKET ===
const connectWebSocket = () => {
  const socket = new SockJS('http://localhost:8080/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {}; // Tắt log debug
  stompClient.connect({}, (frame) => {
    stompClient.subscribe('/topic/waiter', (message) => {
      if (message.body === 'ORDER_READY') {
        fetchData();
      } else if (message.body === 'ORDER_PAID') {
        toastMsg.value = '💰 Thu ngân vừa xác nhận thanh toán!';
        setTimeout(() => { toastMsg.value = ''; }, 3500);
        fetchData();
      }
    });
  });
};

const disconnectWebSocket = () => {
  if (stompClient) stompClient.disconnect();
};

// === LIFECYCLE ===
onMounted(() => {
  fetchData();
  connectWebSocket();
  timerInterval = setInterval(() => { now.value = new Date(); }, 1000);
});

onUnmounted(() => {
  disconnectWebSocket();
  if (timerInterval) clearInterval(timerInterval);
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
  background: rgba(13, 27, 42, 0.4);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding: 12px 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
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

/* STATS BAR */
.stats-bar {
  display: flex; justify-content: center; gap: 8px;
  padding: 14px 24px;
  background: rgba(6,13,26,0.8);
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}
.stat-item {
  display: flex; flex-direction: column; align-items: center;
  background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: 12px; padding: 10px 18px; min-width: 100px;
}
.stat-item.stat-urgent { border-color: rgba(231,76,60,0.3); }
.stat-item.stat-active { border-color: rgba(243,156,18,0.3); }
.stat-item.stat-done { border-color: rgba(0,212,170,0.3); }
.stat-value { font-size: 1.5rem; font-weight: 900; color: var(--text-heading); }
.stat-item.stat-urgent .stat-value { color: #e74c3c; }
.stat-item.stat-active .stat-value { color: #f39c12; }
.stat-item.stat-done .stat-value { color: var(--primary); }
.stat-label {
  font-size: 0.65rem; color: var(--text-muted); text-transform: uppercase;
  letter-spacing: 0.5px; margin-top: 4px; font-weight: 600; text-align: center;
}

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
.legend-item.cleaning { background: rgba(155, 89, 182, 0.1); color: #9b59b6; }

/* Serve Grid */
.serve-grid { display: flex; flex-direction: column; gap: 14px; }
.serve-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 4px solid #e74c3c;
  border-radius: var(--radius-lg);
  padding: 0;
  display: flex; flex-direction: column;
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

.serve-main { padding: 20px 24px; }
.serve-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 14px; }
.table-name { margin: 0 0 6px 0; font-size: 1.6rem; font-weight: 900; color: var(--text-heading); }
.order-code { margin: 0; color: var(--text-muted); font-size: 0.9rem; }
.order-code span { color: var(--primary); font-weight: 700; }

/* Timer Badge */
.timer-badge {
  font-size: 0.82rem; font-weight: 800; padding: 5px 12px;
  border-radius: 20px; font-family: 'Courier New', monospace;
  white-space: nowrap;
}
.timer-normal { background: rgba(0,212,170,0.15); color: var(--primary); }
.timer-warning { background: rgba(243,156,18,0.2); color: #f39c12; }
.timer-critical { background: rgba(231,76,60,0.2); color: #e74c3c; animation: blink-timer 0.8s ease-in-out infinite; }
@keyframes blink-timer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Serve Dishes */
.serve-dishes {
  display: flex; flex-wrap: wrap; gap: 8px;
  padding-top: 12px; border-top: 1px solid var(--border-light);
}
.serve-dish-item {
  display: flex; align-items: center; gap: 8px;
  background: var(--bg-card2, rgba(0,0,0,0.15));
  border: 1px solid var(--border-light);
  padding: 6px 12px; border-radius: 10px;
}
.serve-dish-thumb {
  width: 32px; height: 32px; border-radius: 6px; object-fit: cover;
  border: 1px solid var(--border);
}
.serve-dish-icon { font-size: 1.2rem; }
.serve-dish-name { font-size: 0.85rem; font-weight: 600; color: var(--text-heading); flex: 1; }
.serve-dish-price { color: #e74c3c; font-weight: bold; margin-left: auto; }

.btn-dish-served {
  background: #27ae60;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  transition: 0.2s;
}
.btn-dish-served:hover {
  background: #2ecc71;
  transform: scale(1.05);
}

.serve-dish-qty {
  font-size: 0.78rem; font-weight: 800;
  background: rgba(0,212,170,0.15); color: var(--primary);
  padding: 2px 8px; border-radius: 10px;
}

.btn-served {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  border: none;
  padding: 14px 28px;
  font-weight: 800; font-size: 0.95rem; font-family: inherit;
  cursor: pointer;
  transition: var(--transition);
  width: 100%;
  border-top: 1px solid var(--border-light);
}
.btn-served:hover { background: linear-gradient(135deg, var(--primary-dark), var(--primary)); box-shadow: 0 6px 20px rgba(0,212,170,0.4); }

/* Empty */
.empty-state { text-align: center; padding: 40px; color: var(--text-muted); }
.empty-icon { font-size: 3rem; margin-bottom: 10px; }
.empty-state p { font-size: 0.95rem; }

/* Table Grid Redesign */
.floor-section { margin-top: 40px; }
.floor-header { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; }
.floor-title { font-size: 1.2rem; font-weight: 800; color: var(--text-primary); }
.floor-divider { flex-grow: 1; height: 1px; background: var(--border-light); }

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 35px;
  padding: 20px 0;
}
.table-box {
  background: var(--bg-card);
  border-radius: 50%;
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  border: 4px solid var(--border);
  box-shadow: 0 10px 20px rgba(0,0,0,0.2), inset 0 0 20px rgba(0,0,0,0.5);
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  cursor: pointer;
}
.table-box::before, .table-box::after, .table-chairs::before, .table-chairs::after {
  content: '';
  position: absolute;
  width: 45px; height: 12px;
  background: var(--border);
  border-radius: 10px;
  transition: 0.3s;
  box-shadow: 0 5px 10px rgba(0,0,0,0.3);
}
.table-box::before { top: -16px; left: 50%; transform: translateX(-50%); }
.table-box::after { bottom: -16px; left: 50%; transform: translateX(-50%); }
.table-chairs { position: absolute; inset: 0; pointer-events: none; }
.table-chairs::before { left: -16px; top: 50%; transform: translateY(-50%) rotate(90deg); }
.table-chairs::after { right: -16px; top: 50%; transform: translateY(-50%) rotate(90deg); }

.table-content {
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.table-box h4 { margin: 0; font-size: 1.6rem; font-weight: 900; color: var(--text-heading); text-shadow: 0 2px 4px rgba(0,0,0,0.5); }
.table-status-text { font-size: 0.85rem; font-weight: bold; padding: 4px 10px; border-radius: 20px; background: rgba(0,0,0,0.7); color: #fff; margin-top: 8px; border: 1px solid rgba(255,255,255,0.1); }

/* Table Status Colors */
.table-empty { border-color: #2ecc71; }
.table-empty::before, .table-empty::after, .table-empty .table-chairs::before, .table-empty .table-chairs::after { background: #2ecc71; }

.table-booked { border-color: #f1c40f; }
.table-booked::before, .table-booked::after, .table-booked .table-chairs::before, .table-booked .table-chairs::after { background: #f1c40f; }

.table-occupied { border-color: #e74c3c; animation: pulse-red 2s infinite; }
.table-occupied::before, .table-occupied::after, .table-occupied .table-chairs::before, .table-occupied .table-chairs::after { background: #e74c3c; }

@keyframes pulse-red {
  0% { box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.7), inset 0 0 20px rgba(0,0,0,0.5); }
  70% { box-shadow: 0 0 0 15px rgba(231, 76, 60, 0), inset 0 0 20px rgba(0,0,0,0.5); }
  100% { box-shadow: 0 0 0 0 rgba(231, 76, 60, 0), inset 0 0 20px rgba(0,0,0,0.5); }
}

.table-cleaning { border-color: #9b59b6; }
.table-cleaning::before, .table-cleaning::after, .table-cleaning .table-chairs::before, .table-cleaning .table-chairs::after { background: #9b59b6; }

/* Table Actions Menu now in Modal */
.btn-action-large {
  padding: 12px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.8);
  border: 1px solid rgba(255,255,255,0.1);
  width: max-content;
}
.table-box:hover { z-index: 5; }
.table-box:hover .table-actions {
  opacity: 1;
  visibility: visible;
  transform: translateX(-50%) translateY(10px);
}
.btn-action {
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  color: var(--text-primary);
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.btn-action:hover:not(:disabled) { background: rgba(255,255,255,0.1); transform: scale(1.05); }

.btn-add-item {
  width: 100%;
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
  border: 1px solid rgba(46, 204, 113, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-add-item:hover { background: #2ecc71; color: white; }

.btn-upgrade {
  flex: 1;
  background: rgba(46, 204, 113, 0.1);
  color: #2ecc71;
  border: 1px solid rgba(46, 204, 113, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-upgrade:hover { background: #2ecc71; color: white; }

.btn-cancel-book {
  flex: 1;
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  border: 1px solid rgba(231, 76, 60, 0.3);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-cancel-book:hover { background: #e74c3c; color: white; }

/* Toast */
.toast-notification {
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--bg-card);
  color: var(--primary);
  padding: 14px 28px;
  border-radius: 30px;
  border: 1px solid var(--primary);
  box-shadow: 0 0 30px rgba(0,212,170,0.3);
  font-weight: bold;
  z-index: 1000;
  animation: slideUp 0.3s ease;
}
@keyframes slideUp {
  from { transform: translate(-50%, 20px); opacity: 0; }
  to { transform: translate(-50%, 0); opacity: 1; }
}

/* ===== DETAIL MODAL ===== */
.detail-modal {
  background: var(--bg-card);
  border-radius: 16px;
  width: 460px;
  max-width: 95vw;
  overflow: hidden;
  box-shadow: 0 20px 50px rgba(0,0,0,0.6);
  animation: slideDown 0.3s ease;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}
.detail-meta {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.detail-meta span { color: var(--text-secondary); font-size: 0.9rem; }
.g-badge {
  padding: 4px 12px; border-radius: 20px;
  font-size: 0.78rem; font-weight: 700;
}
.badge-cooking { background: rgba(243,156,18,0.15); color: #f39c12; }
.badge-ready { background: rgba(0,212,170,0.15); color: var(--primary); }
.badge-serving { background: rgba(52,152,219,0.15); color: #3498db; }
.badge-done { background: rgba(46,204,113,0.15); color: #2ecc71; }

.detail-dishes {
  max-height: 350px; overflow-y: auto;
}
.detail-dish-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid var(--border-light);
}
.detail-dish-row:last-child { border-bottom: none; }
.detail-dish-left { display: flex; align-items: center; gap: 12px; }
.detail-dish-img {
  width: 44px; height: 44px; border-radius: 8px; object-fit: cover;
  border: 1px solid var(--border);
}
.detail-dish-placeholder { font-size: 1.5rem; width: 44px; text-align: center; }
.detail-dish-left strong { color: var(--text-heading); font-size: 0.92rem; }
.detail-dish-qty {
  margin-left: 6px; font-size: 0.78rem; font-weight: 700;
  color: var(--primary); background: rgba(0,212,170,0.1);
  padding: 2px 8px; border-radius: 10px;
}
.detail-dish-price { font-weight: 700; color: var(--text-secondary); font-size: 0.9rem; white-space: nowrap; }

.detail-total {
  display: flex; justify-content: space-between;
  padding: 16px 0 0 0; margin-top: 12px;
  border-top: 2px solid var(--border);
  font-weight: 900; font-size: 1.1rem; color: var(--primary);
}

/* Modal In Hóa Đơn Nhiệt */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.invoice-modal {
  background: white; color: #000; width: 320px; border-radius: 8px; overflow: hidden;
  max-height: 90vh; display: flex; flex-direction: column;
}
.modal-header {
  background: var(--bg-nav); color: white; display: flex; justify-content: space-between; padding: 14px 18px; align-items: center;
}
.modal-header h2 { margin: 0; font-size: 1rem; color: var(--primary); }
.btn-close { background: none; border: none; color: white; cursor: pointer; font-size: 1.2rem; transition: 0.3s; }
.btn-close:hover { color: #e74c3c; transform: scale(1.1); }

.modal-body {
  padding: 20px;
  color: var(--text-primary);
  overflow-y: auto;
  max-height: 60vh;
}

.invoice-content { padding: 20px; font-family: monospace; font-size: 0.9rem; overflow-y: auto; }
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
  * { -webkit-print-color-adjust: exact !important; print-color-adjust: exact !important; }
  body * { visibility: hidden !important; }
  .printable-area,
  .printable-area * { visibility: visible !important; }
  .printable-area {
    position: fixed !important;
    inset: 0 !important;
    width: 80mm !important;
    max-height: none !important;
    overflow: visible !important;
    box-shadow: none !important;
    border-radius: 0 !important;
    background: white !important;
    color: #000 !important;
    z-index: 99999 !important;
    margin: 0 auto !important;
  }
  .hide-on-print { display: none !important; }
  .modal-overlay { background: none !important; backdrop-filter: none !important; }
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

/* ===== RESPONSIVE ===== */
@media (max-width: 1024px) {
  .table-grid { grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
  .stats-bar { gap: 6px; padding: 12px 16px; }
  .stat-item { min-width: 85px; padding: 8px 12px; }
  .stat-value { font-size: 1.3rem; }
  .waiter-content { padding: 20px; }
}

@media (max-width: 480px) {
  .waiter-header h1 { font-size: 1.1rem; }
  .btn-print, .btn-checkout, .btn-upgrade { font-size: 0.75rem; padding: 6px; }
}

/* AI Elements */
.ai-upsell-action { margin-top: 15px; text-align: center; }
.btn-ai-analyze {
  background: linear-gradient(135deg, #1abc9c, #16a085);
  color: white; border: none; padding: 10px 20px; border-radius: 8px; width: 100%;
  font-weight: bold; cursor: pointer; transition: 0.3s;
  box-shadow: 0 4px 15px rgba(26, 188, 156, 0.4); font-family: inherit; font-size: 0.95rem;
}
.btn-ai-analyze:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(26, 188, 156, 0.6); }

.ai-modal { background: var(--bg-card); width: 450px; max-width: 90%; border-radius: 12px; padding: 20px; border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h2 { margin: 0; color: #1abc9c; font-size: 1.2rem; font-weight: 800; }
.btn-close { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted); }
.ai-loading { text-align: center; padding: 30px; color: #1abc9c; font-weight: bold; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(26, 188, 156, 0.2); border-top-color: #1abc9c; border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-result { padding: 20px; font-size: 1.05rem; line-height: 1.6; color: var(--text-primary); border-left: 4px solid #1abc9c; background: rgba(26, 188, 156, 0.05); border-radius: 0 8px 8px 0; white-space: pre-line; font-weight: 600; }

@media (max-width: 768px) {
  .waiter-header { padding: 0 14px; height: auto; flex-wrap: wrap; gap: 8px; padding-top: 10px; padding-bottom: 10px; }
  .brand h2 { font-size: 1rem; }
  .header-right { gap: 8px; flex-wrap: wrap; }
  .stats-bar { gap: 4px; padding: 10px; }
  .stat-item { min-width: 70px; padding: 6px 8px; flex: 1; }
  .stat-value { font-size: 1.1rem; }
  .stat-label { font-size: 0.55rem; }
  .waiter-content { padding: 14px; }
  .table-grid { grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 8px; }
  .table-box { padding: 12px 8px; }
  .table-box h4 { font-size: 0.85rem; }
  .serve-top { flex-direction: column; gap: 8px; }
  .table-name { font-size: 1.3rem; }
  .btn-served { padding: 16px; font-size: 1rem; }
  .legend { gap: 6px; flex-wrap: wrap; }
  .legend-item { font-size: 0.7rem; padding: 3px 8px; }
  .detail-modal { width: 95vw; }
}
</style>