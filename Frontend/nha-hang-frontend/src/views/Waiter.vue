<template>
  <div class="waiter-wrapper">
    <header class="waiter-header">
      <div class="header-left">
        <div class="brand">
        <span class="brand-icon"><UiIcon name="waiter" /></span>
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
        <button @click="$router.push('/staff/profile')" class="btn-profile" style="background:var(--warning); color:#FFFFFF; padding:8px 15px; border:none; border-radius:6px; font-weight:bold; cursor:pointer;"> Cá Nhân</button>
        <button @click="handleLogout" class="btn-logout"> Tan Ca</button>
      </div>
    </header>

    <TimekeepingWidget />

    <!-- Zone Info Banner -->
    <div v-if="myAssignedFloors.length > 0" class="zone-info-banner">
      <div class="zone-info-left">
          <span class="zone-info-icon"><UiIcon name="location" /></span>
        <div>
          <div class="zone-info-title">Khu vực phục vụ của bạn hôm nay:</div>
          <div class="zone-info-floors">
            <span v-for="f in myAssignedFloors" :key="f" class="zone-floor-tag">{{ f }}</span>
          </div>
        </div>
      </div>
      <button @click="showAllFloors = !showAllFloors" class="btn-toggle-floors" :class="{ active: showAllFloors }">
        {{ showAllFloors ? ' Chỉ khu vực tôi' : ' Xem tất cả tầng' }}
      </button>
    </div>

    <!-- Stats Bar -->
    <div class="stats-bar">
      <div class="stat-item stat-urgent">
        <span class="stat-value">{{ filteredReadyOrders.length }}</span>
        <span class="stat-label">Cần Bưng</span>
      </div>
      <div class="stat-item">
        <span class="stat-value">{{ filteredCookingOrders.length }}</span>
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
          <h3 class="section-title"> Món Đã Xong — Cần Bưng Ngay</h3>
          <span class="count-badge" :class="{ 'count-pulse': filteredReadyOrders.length > 0 }">
            {{ filteredReadyOrders.length }}
          </span>
        </div>

        <div v-if="filteredReadyOrders.length > 0" class="serve-grid">
          <div v-for="order in filteredReadyOrders" :key="order.id" class="serve-card">
            <div class="serve-card-glow"></div>
            <div class="serve-main">
              <div class="serve-top">
                <div class="serve-info">
                  <h2 class="table-name">{{ getTableName(order) }}</h2>
                  <p class="order-code">Mã đơn: <span>#{{ String(order.id).padStart(4, '0') }}</span></p>
                </div>
                <div class="serve-timer">
                  <span :class="['timer-badge', getServeTimerClass(order)]">
                     {{ getElapsedTime(order.createDate) }}
                  </span>
                </div>
              </div>
              <!-- Chi tiết món ăn -->
              <div class="serve-dishes">
                <template v-for="(detail, idx) in order.orderDetails" :key="idx">
                  <div class="serve-dish-item" v-if="Number(order.status) === 2 || detail.status === 1">
                    <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="serve-dish-thumb" @error="replaceFoodImage" />
                  <span v-else class="serve-dish-icon"><UiIcon name="dish" /></span>
                    <span class="serve-dish-name">{{ detail.product?.name || 'Món ăn' }}</span>
                    <span class="serve-dish-qty">x{{ detail.quantity }}</span>
                    <button v-if="Number(order.status) === 6" @click="markDishServed(order, detail.id)" class="btn-dish-served" style="margin-left: auto;"> Đã Bưng</button>
                    <span v-else class="serve-dish-price">{{ detail.price?.toLocaleString() }}đ</span>
                  </div>
                </template>
              </div>
            </div>
            <button v-if="Number(order.status) === 2" @click="markAsServed(order.id)" class="btn-served">
               BƯNG TOÀN BỘ BÀN NÀY
            </button>
          </div>
        </div>

        <div v-else class="empty-state">
        <div class="empty-icon"><UiIcon name="check" /></div>
          <p>Chưa có món nào từ bếp truyền ra</p>
        </div>
      </section>

      <!-- Món đang nấu -->
      <section class="section">
        <div class="section-header">
          <h3 class="section-title"> Món Đang Làm</h3>
          <span class="count-badge" style="background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); border-color: color-mix(in srgb, var(--color-tertiary) 40%, transparent);">
            {{ filteredCookingOrders.length }}
          </span>
        </div>

        <div v-if="filteredCookingOrders.length > 0" class="serve-grid">
          <div v-for="order in filteredCookingOrders" :key="'cook-'+order.id" class="serve-card" style="border-left-color: var(--color-tertiary);">
            <div class="serve-main">
              <div class="serve-top">
                <div class="serve-info">
                  <h2 class="table-name">{{ getTableName(order) }}</h2>
                  <p class="order-code">Mã đơn: <span>#{{ String(order.id).padStart(4, '0') }}</span></p>
                </div>
                <div class="serve-timer">
                  <span :class="['timer-badge', getServeTimerClass(order)]">
                     {{ getElapsedTime(order.createDate) }}
                  </span>
                </div>
              </div>
              <div class="serve-dishes">
                <template v-for="(detail, idx) in order.orderDetails" :key="'cd-'+idx">
                  <div class="serve-dish-item" v-if="!detail.status || detail.status === 0">
                    <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="serve-dish-thumb" @error="replaceFoodImage" />
                  <span v-else class="serve-dish-icon"><UiIcon name="dish" /></span>
                    <span class="serve-dish-name">{{ detail.product?.name || 'Món ăn' }}</span>
                    <span class="serve-dish-qty">x{{ detail.quantity }}</span>
                    <span class="serve-dish-price" style="color: var(--color-tertiary); font-size: 0.8rem; font-weight: bold; background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); padding: 4px 8px; border-radius: 10px; margin-left: auto;"> Đang nấu</span>
                  </div>
                </template>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
        <div class="empty-icon"><UiIcon name="check" /></div>
          <p>Không có món nào đang được nấu</p>
        </div>
      </section>

      <!-- Tình trạng bàn -->
      <section class="section">
        <div class="section-header">
          <h3 class="section-title"> Tình Trạng Bàn</h3>
          <div class="legend">
            <span class="legend-item empty"> Trống</span>
            <span class="legend-item booked"> Đặt cọc</span>
            <span class="legend-item occupied"> Có khách</span>
            <span class="legend-item cleaning"> Cần dọn</span>
            <span class="legend-item linked"> Đã ghép</span>
          </div>
        </div>

        <div v-for="(tablesInFloor, floorName) in tablesByFloor" :key="floorName" class="floor-section" :class="{ 'floor-collapsed': !showAllFloors && myAssignedFloors.length > 0 && !isMyFloor(floorName) }">
          <div class="floor-header" @click="toggleCollapsedFloor(floorName)">
            <h3 class="floor-title">
              <span><UiIcon :name="floorName.toLowerCase().includes('vip') ? 'private' : 'indoor'" /></span>
              {{ floorName }}
              <span v-if="isMyFloor(floorName)" class="my-zone-badge"> Khu vực của bạn</span>
              <span v-else-if="myAssignedFloors.length > 0 && !showAllFloors" class="other-zone-hint">▸ Nhấn để mở</span>
            </h3>
            <div class="floor-divider"></div>
          </div>

          <div class="table-grid" v-show="showAllFloors || myAssignedFloors.length === 0 || isMyFloor(floorName) || expandedFloors.includes(floorName)">
            <div
              v-for="table in tablesInFloor"
              :key="table.id"
              :class="['table-box', getTableClass(table.isOccupied), { 'vip-table': table.floor && table.floor.toLowerCase().includes('vip') }]"
              @click="openTableDetail(table)"
            >
              <div class="tc-top">
                <span class="tc-capacity"> {{ table.capacity || 4 }}</span>
              <span class="tc-icon"><UiIcon name="table" /></span>
              </div>
              <div class="tc-center">
                <div class="tc-dot"></div>
                <h4>{{ table.name }}</h4>
                <p class="tc-subtitle">
                  {{ table.isOccupied === 0 ? 'Sẵn sàng phục vụ' : table.isOccupied === 1 ? 'Đã được đặt cọc' : table.isOccupied === 3 ? 'Đang dọn dẹp' : 'Khách đang ăn' }}
                </p>
              </div>
              <div class="tc-bottom">
                <span class="tc-status">
                  {{ table.isOccupied === 0 ? ' Trống ⌄' : table.isOccupied === 1 ? ' Đã cọc ⌄' : table.isOccupied === 3 ? ' Cần dọn ⌄' : table.isOccupied === 5 ? ' Đã ghép ⌄' : ' Có khách ⌄' }}
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
          <h2> Chi Tiết — {{ detailTable.name }}</h2>
          <button @click="detailTable = null" class="btn-close"><UiIcon name="x" /></button>
        </div>
        <div class="modal-body">
          <div v-if="detailTable.isOccupied === 3" style="padding: 30px; text-align: center;">
            <p style="font-size: 1.2rem; margin-bottom: 20px;">Bàn này đang chờ dọn dẹp</p>
            <button @click="checkoutTable(detailTable)" class="btn-action-large" style="width: 100%; background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); border: 1px solid var(--success);"> Đã Dọn Xong</button>
          </div>
          
          <div v-else-if="detailTable.isOccupied === 0" class="empty-state" style="padding: 30px;">
            <p>Bàn này đang trống</p>
            <div style="display: flex; gap: 10px; margin-top: 15px;">
              <button @click="goAddItem(detailTable)" class="btn-action-large" style="flex: 1; background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); border: 1px solid var(--success);"> Đón Khách Mới</button>
              <button @click="openMergeTable(detailTable, 'PHYSICAL')" class="btn-action-large" style="flex: 1; background: color-mix(in srgb, var(--secondary) 20%, transparent); color: var(--secondary); border: 1px solid var(--secondary);"> Ghép Bàn Này</button>
            </div>
          </div>
          
          <div v-else-if="detailOrder">
            <!-- ĐÃ CÓ ĐƠN HÀNG (Có thể là đang ăn hoặc đặt trước) -->
            <div class="detail-meta">
              <span>Mã đơn: <strong>#{{ String(detailOrder.id).padStart(4, '0') }}</strong></span>
              <span :class="['g-badge', getDetailStatusClass(detailOrder.status)]">
                {{ getDetailStatusText(detailOrder.status) }}
              </span>
            </div>
            <div class="detail-dishes">
              <div v-for="(detail, idx) in detailOrder.orderDetails" :key="idx" class="detail-dish-row">
                <div class="detail-dish-left">
                  <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="detail-dish-img" @error="replaceFoodImage" />
              <span v-else class="detail-dish-placeholder"><UiIcon name="dish" /></span>
                  <div>
                    <strong>{{ detail.product?.name }}</strong>
                    <span class="detail-dish-qty">x{{ detail.quantity }}</span>
                  </div>
                </div>
                <span class="detail-dish-price">{{ detail.price?.toLocaleString() }}đ</span>
              </div>
            </div>
            <div class="detail-total" style="flex-direction: column; align-items: flex-start;">
              <div style="width: 100%; display: flex; justify-content: space-between; font-size: 0.9rem; color: var(--color-outline); margin-bottom: 5px;">
                <span>Tạm tính:</span>
                <span>{{ calculateSubTotal(detailOrder).toLocaleString() }} đ</span>
              </div>
              <div style="width: 100%; display: flex; justify-content: space-between; font-size: 0.9rem; color: var(--color-outline); border-bottom: 1px dashed rgba(255,255,255,0.2); padding-bottom: 10px; margin-bottom: 10px;">
                <span>Thuế GTGT:</span>
                <span>{{ calculateTax(detailOrder).toLocaleString() }} đ</span>
              </div>
              <div style="width: 100%; display: flex; justify-content: space-between;">
                <span>TỔNG THANH TOÁN:</span>
                <span>{{ calculateTotal(detailOrder).toLocaleString() }} đ</span>
              </div>
            </div>
            
            <div class="ai-upsell-action">
              <button @click="getAiUpsellAdvice" class="btn-ai-analyze"> AI Gợi Ý Mời Món</button>
            </div>

            <!-- Hành động tùy theo trạng thái bàn -->
            <div v-if="detailTable.isOccupied === 1" style="margin-top: 20px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,0.1); display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
              <button @click="upgradeToOccupied(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); border: 1px solid var(--success);"> Khách Đã Đến</button>
              <button @click="cancelBooking(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--primary) 20%, transparent); color: var(--primary); border: 1px solid var(--primary);"> Khách Hủy</button>
              <button @click="goAddItem(detailTable)" class="btn-action-large" style="grid-column: span 2; background: color-mix(in srgb, var(--secondary) 20%, transparent); color: var(--secondary); border: 1px solid var(--secondary);"> Gọi Thêm</button>
            </div>
            
            <div v-else class="modal-table-actions" style="margin-top: 20px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,0.1); display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
               <button @click="openMoveTable(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); border: 1px solid var(--color-tertiary);"> Chuyển Bàn</button>
               <button @click="goAddItem(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); border: 1px solid var(--success);"> Gọi Thêm</button>
               <button @click="openMergeTable(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--secondary) 20%, transparent); color: var(--secondary); border: 1px solid var(--secondary);"> Gộp Bàn</button>
               <button @click="openSplitTable(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); border: 1px solid var(--color-tertiary);"> Tách Bàn</button>
               <button @click="openInvoice(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); border: 1px solid var(--color-tertiary);"> In Tạm Tính</button>
               <button @click="openCheckoutModal(detailTable)" class="btn-action-large" style="grid-column: span 2; background: color-mix(in srgb, var(--success) 80%, transparent); color: #FFFFFF; border: none; font-weight: bold; font-size: 1.1rem; padding: 12px;"> THANH TOÁN</button>
            </div>
          </div>
          
          <div v-else-if="detailTable.isOccupied === 5" style="padding: 30px; text-align: center;">
            <p style="font-size: 1.2rem; margin-bottom: 20px;">Bàn này đã được ghép với bàn khác.</p>
            <p style="color: var(--color-tertiary); font-weight: bold;">{{ detailTable.reservedTime }}</p>
            <button @click="unlinkTable(detailTable)" class="btn-action-large" style="width: 100%; margin-top: 15px; background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); border: 1px solid var(--color-tertiary);"> Tách Bàn Này Ra</button>
          </div>

          <div v-else-if="detailTable.isOccupied === 1" style="padding: 30px; text-align: center;">
            <!-- CÓ CỌC NHƯNG CHƯA ĐẶT MÓN -->
            <p style="font-size: 1.2rem; margin-bottom: 20px;">Bàn đang được khách đặt cọc trước (Chưa gọi món)</p>
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px;">
              <button @click="upgradeToOccupied(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); border: 1px solid var(--success);"> Khách Đã Đến</button>
              <button @click="cancelBooking(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--primary) 20%, transparent); color: var(--primary); border: 1px solid var(--primary);"> Khách Hủy</button>
            </div>
          </div>
          
          <div v-else-if="detailTable.isOccupied === 2" style="padding: 30px; text-align: center;">
            <!-- CÓ KHÁCH NHƯNG CHƯA GỌI MÓN (Hoặc đã thanh toán đơn trước đó nhưng chưa dọn bàn) -->
            <p style="font-size: 1.2rem; margin-bottom: 20px;">Bàn có khách nhưng hiện tại chưa có món nào</p>
            <div style="display: grid; grid-template-columns: 1fr; gap: 10px;">
              <button @click="goAddItem(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--secondary) 20%, transparent); color: var(--secondary); border: 1px solid var(--secondary);"> Khách Gọi Món</button>
              <button @click="markAsCleaning(detailTable)" class="btn-action-large" style="background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); border: 1px solid var(--color-tertiary);"> Khách Đã Về (Cần Dọn)</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI Modal -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
      <div class="ai-modal">
        <div class="modal-header">
          <h2> Chuyên Gia Bán Chéo AI</h2>
          <button @click="showAiModal = false" class="btn-close"><UiIcon name="x" /></button>
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
          <button @click="closeModal" class="btn-close"><UiIcon name="x" /></button>
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
                  <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="bill-thumb" @error="replaceFoodImage" />
                  <span v-else class="no-img-icon"><UiIcon name="dish" /></span>
                </td>
                <td><strong>{{ detail.product?.name }}</strong></td>
                <td style="text-align:right">{{ (detail.price / detail.quantity).toLocaleString() }}đ</td>
                <td style="text-align:center">{{ detail.quantity }}</td>
                <td style="text-align:right; font-weight: bold;">{{ detail.price.toLocaleString() }}đ</td>
              </tr>
            </tbody>
          </table>

          <div class="invoice-total">
            <div class="total-row" style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 5px;">
              <span>Tạm tính:</span>
              <span>{{ calculateSubTotal(selectedOrder).toLocaleString() }} đ</span>
            </div>
            <div class="total-row" style="font-size: 0.9rem; color: var(--text-secondary); border-bottom: 1px dashed var(--color-outline); padding-bottom: 10px; margin-bottom: 10px;">
              <span>Thuế GTGT:</span>
              <span>{{ calculateTax(selectedOrder).toLocaleString() }} đ</span>
            </div>
            <div class="total-row">
              <span>TỔNG CỘNG:</span>
              <span>{{ calculateTotal(selectedOrder).toLocaleString() }} đ</span>
            </div>
          </div>

          <div class="qr-payment">
            <p>QR chuyển khoản được phát hành và đối soát tại màn hình Thu ngân.</p>
          </div>

          <div class="invoice-footer">
            <p>Cảm ơn quý khách!</p>
          </div>
        </div>

        <div class="modal-actions hide-on-print">
          <button @click="printInvoice" class="btn-export"> IN HÓA ĐƠN NÀY</button>
        </div>
      </div>
    </div>

    <!-- Modal Chuyển Bàn -->
    <div v-if="showMoveModal" class="modal-overlay" @click.self="showMoveModal = false">
      <div class="move-modal">
        <div class="modal-header">
          <h2> Chuyển Bàn</h2>
          <button @click="showMoveModal = false" class="btn-close"><UiIcon name="x" /></button>
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
          <h2> {{ mergeMode === 'PHYSICAL' ? 'Ghép Bàn Vật Lý' : 'Gộp Bàn (Dồn Đơn)' }}</h2>
          <button @click="showMergeModal = false" class="btn-close"><UiIcon name="x" /></button>
        </div>
        <div class="modal-body">
          <p v-if="mergeMode === 'PHYSICAL'">Ghép <strong>Bàn {{ movingTable?.name }}</strong> vào:</p>
          <p v-else>Gộp toàn bộ món từ <strong>Bàn {{ movingTable?.name }}</strong> sang:</p>
          
          <label>Chọn bàn đích:</label>
          <select v-model="targetMergeTableId" class="select-table">
            <option value="" disabled>-- Vui lòng chọn bàn --</option>
            <template v-if="mergeMode === 'PHYSICAL'">
              <option v-for="t in tables.filter(x => x.id !== movingTable?.id)" :key="'p'+t.id" :value="t.id">
                Bàn {{ t.name }} ({{ t.isOccupied === 0 ? 'Trống' : t.isOccupied === 1 ? 'Cọc' : t.isOccupied === 3 ? 'Cần dọn' : 'Có Khách' }})
              </option>
            </template>
            <template v-else>
              <option v-for="t in occupiedTables.filter(x => x.id !== movingTable?.id)" :key="'o'+t.id" :value="t.id">
                Bàn {{ t.name }}
              </option>
            </template>
          </select>
          <div class="move-actions">
            <button @click="confirmMergeTable" class="btn-confirm-move" :disabled="!targetMergeTableId" style="background:var(--secondary)">
              {{ mergeMode === 'PHYSICAL' ? 'Ghép Bàn Ngay' : 'Gộp Bàn Ngay' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Modal Tách Bàn -->
    <div v-if="showSplitModal" class="modal-overlay" @click.self="showSplitModal = false">
      <div class="move-modal" style="width: 500px;">
        <div class="modal-header">
          <h2> Tách Bàn (Chuyển Món)</h2>
          <button @click="showSplitModal = false" class="btn-close"><UiIcon name="x" /></button>
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
            <p v-if="splitSourceOrder?.orderDetails?.length === 0" style="text-align: center; color: var(--text-muted);">Không có món nào.</p>
          </div>

          <div class="move-actions" style="margin-top: 20px;">
            <button @click="confirmSplitTable" class="btn-confirm-move" :disabled="!splitTargetTableId || selectedDetailIds.length === 0" style="background:var(--color-tertiary); width: 100%;">Xác Nhận Tách ({{ selectedDetailIds.length }} món)</button>
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
          <h2> Thanh Toán - Bàn {{ checkoutTableName }}</h2>
          <button @click="checkoutOrder = null" class="btn-close"><UiIcon name="x" /></button>
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
                  <img v-if="detail.product?.image" :src="foodImage(detail.product.image)" class="bill-thumb" @error="replaceFoodImage" />
              <span v-else class="no-img-icon"><UiIcon name="dish" /></span>
                </td>
                <td><strong>{{ detail.product?.name }}</strong></td>
                <td style="text-align:right">{{ (detail.price / detail.quantity).toLocaleString() }}đ</td>
                <td style="text-align:center">{{ detail.quantity }}</td>
                <td style="text-align:right; font-weight: bold;">{{ detail.price.toLocaleString() }}đ</td>
              </tr>
            </tbody>
          </table>

          <div class="invoice-total">
            <div class="total-row" style="font-size: 0.9rem; color: var(--text-secondary); margin-bottom: 5px;">
              <span>Tạm tính:</span>
              <span>{{ calculateSubTotal(checkoutOrder).toLocaleString() }} đ</span>
            </div>
            <div class="total-row" style="font-size: 0.9rem; color: var(--text-secondary); border-bottom: 1px dashed var(--color-outline); padding-bottom: 10px; margin-bottom: 10px;">
              <span>Thuế GTGT:</span>
              <span>{{ calculateTax(checkoutOrder).toLocaleString() }} đ</span>
            </div>
            <div class="total-row">
              <span>TỔNG CỘNG:</span>
              <span>{{ calculateTotal(checkoutOrder).toLocaleString() }} đ</span>
            </div>
          </div>

          <div class="qr-payment">
            <p>Vui lòng chuyển hóa đơn tới Thu ngân để tạo QR hoặc thu tiền.</p>
          </div>

          <div class="invoice-footer">
            <p>Cảm ơn quý khách!</p>
          </div>
        </div>

        <div class="modal-actions hide-on-print" style="display: flex; flex-direction: column; gap: 10px; padding: 15px;">
          <div style="display: flex; gap: 10px;">
            <button @click="printCheckoutInvoice" class="btn-export" style="flex:1; background: var(--secondary);"> In Hóa Đơn</button>
          </div>
        </div>
      </div>
    </div>
    <StaffOperationsAssistant />
  </div>
</template>

<script setup>
import StaffOperationsAssistant from '@/components/StaffOperationsAssistant.vue'
import { ref, onMounted, onUnmounted, computed } from 'vue';
import api from '@/services/api';
import { toBusinessDate } from '@/utils/businessDate';
import { useRouter } from 'vue-router';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import TimekeepingWidget from '../components/TimekeepingWidget.vue';
import { foodImage, replaceFoodImage } from '@/utils/imageFallback';
import { clearStaffSession, getStaffToken, getStaffUser } from '@/services/session';
import { useDialog } from '@/composables/useDialog';

const { confirmDialog } = useDialog();

const router = useRouter();
const toastMsg = ref('');
const showToast = (message, duration = 5000) => {
  toastMsg.value = String(message || 'Có lỗi xảy ra.');
  setTimeout(() => { toastMsg.value = ''; }, duration);
};
const orders = ref([]);
const tables = ref([]);
const now = ref(new Date());
let timerInterval = null;
let previousReadyIds = [];
let stompClient = null;

const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

// === PHÂN KHU VỰC PHỤC VỤ ===
const myAssignedFloors = ref([]);
const showAllFloors = ref(false);
const expandedFloors = ref([]);

// Lấy khu vực phân công của nhân viên hiện tại
const fetchMyZones = async () => {
  try {
    const u = getStaffUser();
    if (!u || !u.username) return;
    const token = getStaffToken();
    const today = toBusinessDate();
    const res = await api.get(`/api/service-zones/my?date=${today}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    // Xác định ca hiện tại
    const hour = new Date().getHours();
    let currentShift = 'Sáng';
    if (hour >= 14 && hour < 22) currentShift = 'Chiều';
    else if (hour >= 22 || hour < 6) currentShift = 'Tối';
    
    // Lọc khu vực theo ca hiện tại
    const zones = res.data.filter(z => z.shift === currentShift);
    myAssignedFloors.value = [...new Set(zones.map(z => z.floor))];
    
    // Nếu không có phân công cho ca hiện tại, thử lấy tất cả ca trong ngày
    if (myAssignedFloors.value.length === 0) {
      myAssignedFloors.value = [...new Set(res.data.map(z => z.floor))];
    }
  } catch (e) {
    console.warn('Chưa có phân công khu vực hoặc lỗi API:', e);
  }
};

// Kiểm tra tầng có phải khu vực mình không
const isMyFloor = (floorName) => {
  if (myAssignedFloors.value.length === 0) return false;
  return myAssignedFloors.value.some(f => floorName.includes(f) || f.includes(floorName));
};

// Toggle collapse/expand tầng không phải khu vực mình
const toggleCollapsedFloor = (floorName) => {
  if (showAllFloors.value || myAssignedFloors.value.length === 0 || isMyFloor(floorName)) return;
  if (expandedFloors.value.includes(floorName)) {
    expandedFloors.value = expandedFloors.value.filter(f => f !== floorName);
  } else {
    expandedFloors.value.push(floorName);
  }
};

// Kiểm tra đơn hàng thuộc tầng mình phụ trách
const isOrderInMyZone = (order) => {
  if (order.orderType !== 'DINE_IN' || !order.tableId) return false;
  if (myAssignedFloors.value.length === 0 || showAllFloors.value) return true;
  const matchedTable = tables.value.find(t => Number(t.id) === Number(order.tableId));
  if (!matchedTable) return false;
  return myAssignedFloors.value.some(f => matchedTable.floor && (matchedTable.floor.includes(f) || f.includes(matchedTable.floor)));
};

const tablesByFloor = computed(() => {
  const groups = {};
  tables.value.forEach(table => {
    let floorName = table.floor || 'Khu Vực Chung';
    if (!groups[floorName]) groups[floorName] = [];
    groups[floorName].push(table);
  });
  // Sắp xếp: tầng mình phụ trách lên trước
  return Object.keys(groups).sort((a, b) => {
    const aIsMine = isMyFloor(a);
    const bIsMine = isMyFloor(b);
    if (aIsMine && !bIsMine) return -1;
    if (!aIsMine && bIsMine) return 1;
    return a.localeCompare(b);
  }).reduce((acc, key) => {
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

// Lọc đơn hàng theo khu vực phân công
const filteredReadyOrders = computed(() => readyOrders.value.filter(isOrderInMyZone));
const filteredCookingOrders = computed(() => cookingOrders.value.filter(isOrderInMyZone));

const occupiedTables = computed(() => tables.value.filter(t => t.isOccupied === 2 || t.isOccupied === 3));
const emptyTables = computed(() => tables.value.filter(t => t.isOccupied === 0));

// Thống kê hôm nay
const todayServed = computed(() => {
  const today = new Date().toDateString();
  return orders.value.filter(o => {
    if (!o.createDate) return false;
    return new Date(o.createDate).toDateString() === today && [4, 7].includes(Number(o.status));
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
const getTableName = (order) => {
  if (order?.orderType === 'DINE_IN') return order.tableName || `Bàn #${order.tableId}`;
  return order?.orderType === 'DELIVERY' ? 'Giao hàng' : 'Mang đi';
};

const getTableClass = (status) => {
  if (status === 0) return 'table-empty';
  if (status === 1) return 'table-booked';
  if (status === 3) return 'table-cleaning';
  if (status === 5) return 'table-linked';
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
    const token = getStaffToken();

    const resOrders = await api.get('/api/admin/orders', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    orders.value = resOrders.data;

    // Kiểm tra có đơn mới cần bưng không
    const newReady = resOrders.data.filter(o => Number(o.status) === 2);
    const newReadyIds = newReady.map(o => o.id);
    const hasNewReady = newReadyIds.some(id => !previousReadyIds.includes(id));

    if (hasNewReady && previousReadyIds.length > 0) {
      playNotificationSound();
      toastMsg.value = ' Bếp vừa hoàn thành món mới!';
      setTimeout(() => { toastMsg.value = ''; }, 3000);
    }
    previousReadyIds = newReadyIds;

    const resTables = await api.get('/api/tables');
    tables.value = resTables.data;

  } catch (error) {
    if (error.response && error.response.status === 403) {
      showToast('Tài khoản phục vụ không có quyền lấy danh sách đơn.');
    }
    console.error('Lỗi lấy dữ liệu phục vụ:', error);
  }
};

const markAsServed = async (id) => {
  try {
    await api.put(`/api/admin/orders/${id}/status?status=7`, {}, {
      headers: { 'Authorization': `Bearer ${getStaffToken()}` }
    });
    toastMsg.value = ' Đã bưng ra bàn thành công!';
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchData();
  } catch (error) { showToast('Không thể hoàn thành đơn lúc này.'); }
};

const markDishServed = async (order, detailId) => {
  try {
    await api.put(`/api/orders/details/${detailId}/status?status=2`, {}, {
      headers: { 'Authorization': `Bearer ${getStaffToken()}` }
    });
    toastMsg.value = ' Đã bưng món!';
    setTimeout(() => { toastMsg.value = ''; }, 2000);
    fetchData();
  } catch (err) { showToast('Không thể cập nhật món lúc này.'); }
};

// Nút KHÁCH VỀ: chuyển bàn sang trạng thái Cần dọn (3)
const markAsCleaning = async (table) => {
  const hasUnpaidOrder = orders.value.some(o => 
    !o.isPaid && 
    Number(o.status) !== 3 && // Not cancelled
    Number(o.status) !== 4 && // Not completed
    Number(o.tableId) === Number(table.id)
  );

  if (hasUnpaidOrder) {
      showToast(`Khách bàn ${table.name} chưa thanh toán xong. Vui lòng chờ thu ngân xác nhận.`);
    return;
  }

  const confirmed = await confirmDialog({
    title: 'Xác nhận khách về',
    message: `Khách tại "${table.name}" đã về? Bàn sẽ chuyển sang trạng thái Cần Dọn.`,
    confirmLabel: 'Chuyển sang cần dọn',
  });
  if (!confirmed) return;

  try {
    await api.put(`/api/tables/${table.id}/status?status=3`, {}, {
      headers: { 'Authorization': `Bearer ${getStaffToken()}` }
    });
    toastMsg.value = ` Bàn "${table.name}" đang chờ dọn!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    fetchData();
  } catch (error) {
    showToast('Không thể cập nhật trạng thái bàn.');
  }
};

// Nút ĐÃ DỌN XONG: chuyển bàn về Trống (0)
const checkoutTable = async (table) => {
  try {
    await api.put(`/api/tables/${table.id}/status?status=0`, {}, {
      headers: { 'Authorization': `Bearer ${getStaffToken()}` }
    });
    showCheckoutToast(table.name);
    detailTable.value = null;
    fetchData();
  } catch (error) {
    if (error.response?.status === 403) {
      showToast('Tài khoản phục vụ chưa được cấp quyền dọn bàn.');
    } else {
      showToast('Không thể dọn bàn: ' + (error.response?.data?.message || error.response?.data || error.message));
    }
  }
};

// In Hóa Đơn Tạm Tính
const selectedOrder = ref(null);
const selectedTableName = ref('');

const getActiveOrderForTable = (table) => {
  return orders.value.find(o => 
    o.status !== 4 && 
    o.orderType === 'DINE_IN' &&
    Number(o.tableId) === Number(table.id)
  );
};

const openInvoice = (table) => {
  const activeOrder = getActiveOrderForTable(table);
  if (activeOrder) {
    selectedOrder.value = activeOrder;
    selectedTableName.value = table.name;
  } else {
    showToast('Không tìm thấy đơn hàng nào đang mở cho bàn này.');
  }
};

const closeModal = () => { selectedOrder.value = null; };

const calculateSubTotal = (order) => {
  if (!order?.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + (item.price || 0), 0);
};

const calculateTax = (order) => {
  if (!order?.orderDetails) return 0;
  return order.orderDetails.reduce((sum, item) => sum + (item.taxAmount || 0), 0);
};

const calculateTotal = (order) => {
  return calculateSubTotal(order) + calculateTax(order);
};

const printInvoice = () => { window.print(); };

// === THANH TOÁN ===
const checkoutOrder = ref(null);
const checkoutTableName = ref('');

const openCheckoutModal = (table) => {
  const activeOrder = getActiveOrderForTable(table);
  if (activeOrder) {
    checkoutOrder.value = activeOrder;
    checkoutTableName.value = table.name;
  } else {
    showToast('Không tìm thấy đơn hàng nào đang mở cho bàn này.');
  }
};

const printCheckoutInvoice = () => { window.print(); };

// === XEM CHI TIẾT ĐƠN TẠI BÀN ===
const detailTable = ref(null);
const detailOrder = computed(() => {
  if (!detailTable.value) return null;
  return getActiveOrderForTable(detailTable.value);
});

const openTableDetail = (table) => {
  detailTable.value = table;
};

const getDetailStatusText = (status) => {
  const map = { 1: ' Đang nấu', 2: ' Đã xong', 3: ' Đang ăn', 4: ' Đã thanh toán' };
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

  const activeOrder = getActiveOrderForTable(movingTable.value);
  if (!activeOrder) {
    showToast('Không tìm thấy đơn hàng của bàn này.');
    return;
  }

  const newTable = tables.value.find(t => t.id === targetTableId.value);
  const token = getStaffToken();
  
  try {
    // Backend moves the order, updates both table states and revokes both QR
    // capabilities atomically. Do not repeat those state changes here.
    await api.put(`/api/admin/orders/${activeOrder.id}/table?newTableId=${newTable.id}`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });

    showMoveModal.value = false;
    toastMsg.value = ` Đã chuyển khách sang Bàn ${newTable.name} thành công!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    fetchData();
  } catch (error) {
    console.error("Lỗi chuyển bàn", error);
    showToast('Không thể chuyển bàn lúc này.');
  }
};

// ==============================
// 12. GỘP BÀN & TÁCH BÀN
// ==============================
const showMergeModal = ref(false);
const targetMergeTableId = ref("");
const mergeMode = ref('ORDER');

const openMergeTable = (table, mode = 'ORDER') => {
  movingTable.value = table;
  targetMergeTableId.value = "";
  mergeMode.value = mode;
  showMergeModal.value = true;
};

const confirmMergeTable = async () => {
  if (!targetMergeTableId.value || !movingTable.value) return;

  const targetTable = tables.value.find(t => t.id === targetMergeTableId.value);
  if (!targetTable) return;

  const token = getStaffToken();
  try {
    if (mergeMode.value === 'PHYSICAL') {
      await api.put(`/api/tables/${movingTable.value.id}/link/${targetTable.id}`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      toastMsg.value = ` Đã ghép bàn vật lý ${movingTable.value.name} vào ${targetTable.name}!`;
    } else {
      await api.post('/api/orders/merge-tables', {
        fromTableId: movingTable.value.id,
        toTableId: targetTable.id
      }, { headers: { 'Authorization': `Bearer ${token}` } });
      toastMsg.value = ` Đã gộp đơn từ bàn ${movingTable.value.name} sang ${targetTable.name} thành công!`;
    }
    
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    showMergeModal.value = false;
    fetchData();
  } catch(error) {
    showToast(error.response?.data?.message || error.response?.data || 'Không thể gộp/ghép bàn.');
  }
};

const unlinkTable = async (table) => {
  if (!await confirmDialog({
    title: 'Tách bàn đã ghép',
    message: `Tách ${table.name} và đưa bàn về trạng thái trống?`,
    confirmLabel: 'Tách bàn',
    danger: true,
  })) return;
  try {
    const token = getStaffToken();
    await api.put(`/api/tables/${table.id}/unlink`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    toastMsg.value = ` Đã tách ${table.name} thành công!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    detailTable.value = null;
    fetchData();
  } catch (err) {
    showToast('Không thể tách bàn lúc này.');
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
  splitSourceOrder.value = getActiveOrderForTable(table);
  if (!splitSourceOrder.value) {
    showToast('Không tìm thấy đơn hàng cho bàn này.');
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
    showToast('Vui lòng chọn bàn đích và ít nhất một món để chuyển.');
    return;
  }
  
  const targetTable = tables.value.find(t => t.id === splitTargetTableId.value);
  const token = getStaffToken();
  try {
    await api.post('/api/orders/split-table', {
      fromTableId: movingTable.value.id,
      toTableId: targetTable.id,
      detailIds: selectedDetailIds.value
    }, { headers: { 'Authorization': `Bearer ${token}` } });

    toastMsg.value = ` Đã tách ${selectedDetailIds.value.length} món sang bàn ${targetTable.name} thành công!`;
    setTimeout(() => { toastMsg.value = ''; }, 3500);
    showSplitModal.value = false;
    fetchData();
  } catch(error) {
    showToast(error.response?.data?.message || error.response?.data || 'Không thể tách bàn.');
  }
};

// --- Hành động Bàn Đặt Cọc (1) ---
const upgradeToOccupied = async (table) => {
  if (!await confirmDialog({
    title: 'Xác nhận khách đến',
    message: `Khách đặt trước bàn ${table.name} đã đến?`,
    confirmLabel: 'Khách đã đến',
  })) return;
  try {
    await api.put(`/api/tables/${table.id}/status?status=2`, {}, {
      headers: { 'Authorization': `Bearer ${getStaffToken()}` }
    });
    toastMsg.value = ` Đã đánh dấu Bàn ${table.name} CÓ KHÁCH!`;
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchData();
  } catch (error) {
    showToast('Không thể cập nhật trạng thái bàn.');
  }
};

const cancelBooking = async (table) => {
  if (!await confirmDialog({
    title: 'Hủy giữ bàn',
    message: `Hủy giữ bàn ${table.name} và đưa bàn về trạng thái trống?`,
    confirmLabel: 'Hủy giữ bàn',
    danger: true,
  })) return;
  try {
    await api.put(`/api/tables/${table.id}/status?status=0`, {}, {
      headers: { 'Authorization': `Bearer ${getStaffToken()}` }
    });
    toastMsg.value = ` Đã hủy cọc Bàn ${table.name}!`;
    setTimeout(() => { toastMsg.value = ''; }, 3000);
    fetchData();
  } catch (error) {
    showToast('Không thể hủy cọc bàn.');
  }
};

// Hiệu ứng toast thông báo dọn bàn thành công
const showCheckoutToast = (tableName) => {
  toastMsg.value = ` Bàn "${tableName}" đã dọn xong!`;
  setTimeout(() => { toastMsg.value = ''; }, 3500);
};

const handleLogout = async () => {
  if (await confirmDialog({
    title: 'Kết thúc ca làm',
    message: 'Bạn có chắc chắn muốn đăng xuất tan ca không?',
    confirmLabel: 'Đăng xuất',
  })) {
    clearStaffSession();
    window.location.href = '/staff-login';
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
    const res = await api.post('/api/staff/ai/waiter', {
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
  const socket = new SockJS('/ws');
  stompClient = Stomp.over(socket);
  stompClient.debug = () => {}; // Tắt log debug
  const token = getStaffToken();
  stompClient.connect(token ? { Authorization: `Bearer ${token}` } : {}, () => {
    stompClient.subscribe('/topic/waiter', (message) => {
      if (message.body === 'ORDER_READY' || message.body === 'DISH_STATUS_CHANGED') {
        fetchData();
      } else if (message.body === 'ORDER_PAID') {
        toastMsg.value = ' Thu ngân vừa xác nhận thanh toán!';
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
  fetchMyZones();
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
  background: rgba(255, 255, 255, 0.72);
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
.brand-icon { font-size: 2rem; filter: drop-shadow(0 0 10px color-mix(in srgb, var(--secondary) 50%, transparent)); }
.brand h2 { margin: 0; font-size: 1.3rem; font-weight: 900; color: var(--primary); letter-spacing: 1px; }
.brand p { margin: 0; font-size: 0.7rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 2px; }

.alert-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  background: color-mix(in srgb, var(--primary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  color: var(--primary);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 0.83rem;
  font-weight: 700;
  animation: fadeInAlert 0.4s ease;
}
.alert-dot {
  width: 7px; height: 7px;
  background: var(--primary);
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
  background: color-mix(in srgb, var(--primary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  color: var(--primary);
  padding: 8px 18px;
  border-radius: var(--radius-md);
  cursor: pointer; font-weight: 700; font-size: 0.88rem; font-family: inherit;
  transition: var(--transition);
}
.btn-logout:hover { background: color-mix(in srgb, var(--primary) 30%, transparent); }

/* STATS BAR */
.stats-bar {
  display: flex; justify-content: center; gap: 8px;
  padding: 14px 24px;
  background: rgba(43, 36, 32, 0.18);
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}
.stat-item {
  display: flex; flex-direction: column; align-items: center;
  background: var(--bg-card); border: 1px solid var(--border-light);
  border-radius: 12px; padding: 10px 18px; min-width: 100px;
}
.stat-item.stat-urgent { border-color: color-mix(in srgb, var(--primary) 30%, transparent); }
.stat-item.stat-active { border-color: color-mix(in srgb, var(--color-tertiary) 30%, transparent); }
.stat-item.stat-done { border-color: color-mix(in srgb, var(--secondary) 30%, transparent); }
.stat-value { font-size: 1.5rem; font-weight: 900; color: var(--text-heading); }
.stat-item.stat-urgent .stat-value { color: var(--primary); }
.stat-item.stat-active .stat-value { color: var(--color-tertiary); }
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
.legend-item.empty { background: color-mix(in srgb, var(--secondary) 10%, transparent); color: var(--primary); }
.legend-item.booked { background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); color: var(--color-tertiary); }
.legend-item.occupied { background: color-mix(in srgb, var(--primary) 10%, transparent); color: var(--primary); }
.legend-item.cleaning { background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); color: var(--color-tertiary); }

/* Serve Grid */
.serve-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; }
.serve-card {
  position: relative;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-left: 4px solid var(--primary);
  border-radius: var(--radius-lg);
  padding: 0;
  display: flex; flex-direction: column;
  box-shadow: var(--shadow-md);
  overflow: hidden;
  animation: slideIn 0.4s ease;
  transition: var(--transition);
}
.serve-card:hover { box-shadow: 0 0 30px color-mix(in srgb, var(--primary) 15%, transparent), var(--shadow-md); }
.serve-card-glow {
  position: absolute;
  left: 0; top: 0; bottom: 0; width: 200px;
  background: linear-gradient(90deg, color-mix(in srgb, var(--primary) 5%, transparent), transparent);
  pointer-events: none;
}
@keyframes slideIn {
  from { transform: translateX(-20px); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}

.serve-main { padding: 12px 16px; }
.serve-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.table-name { margin: 0 0 4px 0; font-size: 1.2rem; font-weight: 900; color: var(--text-heading); }
.order-code { margin: 0; color: var(--text-muted); font-size: 0.8rem; }
.order-code span { color: var(--primary); font-weight: 700; }

/* Timer Badge */
.timer-badge {
  font-size: 0.75rem; font-weight: 800; padding: 4px 10px;
  border-radius: 20px; font-family: var(--font-primary);
  white-space: nowrap;
}
.timer-normal { background: color-mix(in srgb, var(--secondary) 15%, transparent); color: var(--primary); }
.timer-warning { background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); }
.timer-critical { background: color-mix(in srgb, var(--primary) 20%, transparent); color: var(--primary); animation: blink-timer 0.8s ease-in-out infinite; }
@keyframes blink-timer {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* Serve Dishes */
.serve-dishes {
  display: flex; flex-wrap: wrap; gap: 6px;
  padding-top: 10px; border-top: 1px solid var(--border-light);
}
.serve-dish-item {
  display: flex; align-items: center; gap: 6px;
  background: var(--bg-card2, rgba(0,0,0,0.15));
  border: 1px solid var(--border-light);
  padding: 4px 8px; border-radius: 8px;
}
.serve-dish-thumb {
  width: 24px; height: 24px; border-radius: 4px; object-fit: cover;
  border: 1px solid var(--border);
}
.serve-dish-icon { font-size: 1rem; }
.serve-dish-name { font-size: 0.8rem; font-weight: 600; color: var(--text-heading); flex: 1; }
.serve-dish-price { color: var(--primary); font-weight: bold; margin-left: auto; font-size: 0.8rem; }

.btn-dish-served {
  background: var(--success);
  color: #FFFFFF;
  border: none;
  padding: 3px 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.75rem;
  transition: 0.2s;
}
.btn-dish-served:hover {
  background: var(--success);
  transform: scale(1.05);
}

.serve-dish-qty {
  font-size: 0.78rem; font-weight: 800;
  background: color-mix(in srgb, var(--secondary) 15%, transparent); color: var(--primary);
  padding: 2px 8px; border-radius: 10px;
}

.btn-served {
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: var(--bg-dark);
  border: none;
  padding: 10px 20px;
  font-weight: 800; font-size: 0.85rem; font-family: inherit;
  cursor: pointer;
  transition: var(--transition);
  width: 100%;
  border-top: 1px solid var(--border-light);
}
.btn-served:hover { background: linear-gradient(135deg, var(--primary-dark), var(--primary)); box-shadow: 0 6px 20px color-mix(in srgb, var(--secondary) 40%, transparent); }

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
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
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
.table-box.table-empty { border-color: var(--success); box-shadow: 0 0 10px color-mix(in srgb, var(--success) 10%, transparent); }
.table-box.table-booked { border-color: var(--color-tertiary); box-shadow: 0 0 10px color-mix(in srgb, var(--color-tertiary) 10%, transparent); }
.table-box.table-occupied { border-color: var(--primary); box-shadow: 0 0 10px color-mix(in srgb, var(--primary) 10%, transparent); }
.table-box.table-cleaning { border-color: var(--color-tertiary); box-shadow: 0 0 10px color-mix(in srgb, var(--color-tertiary) 10%, transparent); }
.table-box.table-linked { border-color: var(--secondary); box-shadow: 0 0 10px color-mix(in srgb, var(--secondary) 10%, transparent); background: color-mix(in srgb, var(--secondary) 5%, transparent); }

.tc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.tc-capacity {
  background: rgba(255,255,255,0.05);
  color: var(--secondary);
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
.table-empty .tc-dot { background: var(--success); box-shadow: 0 0 8px var(--success); }
.table-booked .tc-dot { background: var(--color-tertiary); box-shadow: 0 0 8px var(--color-tertiary); }
.table-occupied .tc-dot { background: var(--primary); box-shadow: 0 0 8px var(--primary); }
.table-cleaning .tc-dot { background: var(--color-tertiary); box-shadow: 0 0 8px var(--color-tertiary); }
.table-linked .tc-dot { background: var(--secondary); box-shadow: 0 0 8px var(--secondary); }

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
  background: color-mix(in srgb, var(--success) 10%, transparent);
  color: var(--success);
  border: 1px solid color-mix(in srgb, var(--success) 30%, transparent);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-add-item:hover { background: var(--success); color: #FFFFFF; }

.btn-upgrade {
  flex: 1;
  background: color-mix(in srgb, var(--success) 10%, transparent);
  color: var(--success);
  border: 1px solid color-mix(in srgb, var(--success) 30%, transparent);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-upgrade:hover { background: var(--success); color: #FFFFFF; }

.btn-cancel-book {
  flex: 1;
  background: color-mix(in srgb, var(--primary) 10%, transparent);
  color: var(--primary);
  border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  padding: 6px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-cancel-book:hover { background: var(--primary); color: #FFFFFF; }

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
  box-shadow: 0 0 30px color-mix(in srgb, var(--secondary) 30%, transparent);
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
.badge-cooking { background: color-mix(in srgb, var(--color-tertiary) 15%, transparent); color: var(--color-tertiary); }
.badge-ready { background: color-mix(in srgb, var(--secondary) 15%, transparent); color: var(--primary); }
.badge-serving { background: color-mix(in srgb, var(--secondary) 15%, transparent); color: var(--secondary); }
.badge-done { background: color-mix(in srgb, var(--success) 15%, transparent); color: var(--success); }

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
  color: var(--primary); background: color-mix(in srgb, var(--secondary) 10%, transparent);
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
  background: #FFFFFF; color: var(--text-primary); width: 320px; border-radius: 8px; overflow: hidden;
  max-height: 90vh; display: flex; flex-direction: column;
}
.modal-header {
  background: var(--bg-nav); color: #FFFFFF; display: flex; justify-content: space-between; padding: 14px 18px; align-items: center;
}
.modal-header h2 { margin: 0; font-size: 1rem; color: var(--primary); }
.btn-close { background: none; border: none; color: #FFFFFF; cursor: pointer; font-size: 1.2rem; transition: 0.3s; }
.btn-close:hover { color: var(--primary); transform: scale(1.1); }

.modal-body {
  padding: 20px;
  color: var(--text-primary);
  overflow-y: auto;
  max-height: 60vh;
}

.invoice-content { padding: 20px; font-family: var(--font-primary); font-size: 0.9rem; overflow-y: auto; }
.invoice-brand { text-align: center; border-bottom: 1px dashed var(--text-primary); padding-bottom: 10px; margin-bottom: 10px; }
.invoice-brand h1 { margin: 0; font-size: 1.2rem; }
.print-table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }
.print-table th, .print-table td { padding: 4px 2px; border-bottom: 1px dashed var(--color-outline); vertical-align: middle; font-size: 0.8rem; }
.bill-thumb {
  width: 32px; height: 32px; border-radius: 4px; object-fit: cover;
  border: 1px solid var(--border);
}
.no-img-icon { font-size: 1.2rem; }
.total-row { display: flex; justify-content: space-between; font-weight: bold; font-size: 1.1rem; border-top: 1px dashed var(--text-primary); padding-top: 10px; }
.qr-payment { text-align: center; margin-top: 20px; border-top: 1px dashed var(--text-primary); padding-top: 10px; }
.qr-payment img { width: 150px; }
.invoice-footer { text-align: center; margin-top: 10px; font-size: 0.8rem; }
.modal-actions { padding: 10px; text-align: center; background: var(--border); }
.btn-export { background: var(--secondary); color: #FFFFFF; border: none; padding: 10px 20px; border-radius: 4px; font-weight: bold; cursor: pointer; width: 100%; }

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
    background: #FFFFFF !important;
    color: var(--text-primary) !important;
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
  background: var(--color-tertiary);
  color: #FFFFFF;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}
.btn-confirm-move:disabled {
  background: var(--text-muted);
  cursor: not-allowed;
}
.btn-confirm-move:not(:disabled):hover {
  background: var(--color-tertiary);
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
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #FFFFFF; border: none; padding: 10px 20px; border-radius: 8px; width: 100%;
  font-weight: bold; cursor: pointer; transition: 0.3s;
  box-shadow: 0 4px 15px var(--primary-glow); font-family: inherit; font-size: 0.95rem;
}
.btn-ai-analyze:hover { transform: translateY(-2px); box-shadow: 0 6px 20px var(--primary-glow); }

.ai-modal { background: var(--bg-card); width: 450px; max-width: 90%; border-radius: 12px; padding: 20px; border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.modal-header h2 { margin: 0; color: var(--primary); font-size: 1.2rem; font-weight: 800; }
.btn-close { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted); }
.ai-loading { text-align: center; padding: 30px; color: var(--primary); font-weight: bold; }
.spinner { width: 40px; height: 40px; border: 4px solid var(--primary-glow); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-result { padding: 20px; font-size: 1.05rem; line-height: 1.6; color: var(--text-primary); border-left: 4px solid var(--primary); background: var(--primary-glow2); border-radius: 0 8px 8px 0; white-space: pre-line; font-weight: 600; }

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
  .zone-info-banner { flex-direction: column; gap: 10px; padding: 10px 14px; }
}

/* === ZONE INFO BANNER === */
.zone-info-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 40px;
  background: linear-gradient(135deg, color-mix(in srgb, var(--secondary) 8%, transparent), color-mix(in srgb, var(--secondary) 6%, transparent));
  border-bottom: 1px solid color-mix(in srgb, var(--secondary) 15%, transparent);
  animation: slideDownBanner 0.5s ease;
}

@keyframes slideDownBanner {
  from { transform: translateY(-20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.zone-info-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.zone-info-icon {
  font-size: 1.5rem;
  filter: drop-shadow(0 0 6px color-mix(in srgb, var(--secondary) 50%, transparent));
}

.zone-info-title {
  font-size: 0.78rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  font-weight: 600;
  margin-bottom: 4px;
}

.zone-info-floors {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.zone-floor-tag {
  padding: 4px 12px;
  background: color-mix(in srgb, var(--secondary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--secondary) 30%, transparent);
  color: var(--primary);
  border-radius: 16px;
  font-size: 0.82rem;
  font-weight: 700;
}

.btn-toggle-floors {
  padding: 8px 18px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: var(--text-secondary);
  border-radius: 20px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: 0.3s;
  white-space: nowrap;
}
.btn-toggle-floors:hover {
  background: rgba(255, 255, 255, 0.1);
}
.btn-toggle-floors.active {
  background: color-mix(in srgb, var(--secondary) 15%, transparent);
  border-color: color-mix(in srgb, var(--secondary) 40%, transparent);
  color: var(--primary);
}

/* === FLOOR COLLAPSE === */
.floor-collapsed .floor-header {
  cursor: pointer;
  opacity: 0.6;
  transition: 0.3s;
}
.floor-collapsed .floor-header:hover {
  opacity: 1;
}

.my-zone-badge {
  display: inline-block;
  margin-left: 10px;
  padding: 3px 10px;
  background: color-mix(in srgb, var(--secondary) 15%, transparent);
  border: 1px solid color-mix(in srgb, var(--secondary) 30%, transparent);
  color: var(--primary);
  border-radius: 12px;
  font-size: 0.72rem;
  font-weight: 700;
  vertical-align: middle;
  animation: pulseZone 2s ease-in-out infinite;
}

@keyframes pulseZone {
  0%, 100% { box-shadow: 0 0 0 0 color-mix(in srgb, var(--secondary) 30%, transparent); }
  50% { box-shadow: 0 0 8px 2px color-mix(in srgb, var(--secondary) 20%, transparent); }
}

.other-zone-hint {
  display: inline-block;
  margin-left: 10px;
  font-size: 0.72rem;
  color: var(--text-muted);
  font-weight: 600;
  font-style: italic;
}
</style>
