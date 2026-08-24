<template>
  <AdminLayout>
  <div class="admin-wrapper">
    

    <main class="admin-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">Sơ Đồ Tình Trạng Bàn</h1>
          <p class="page-subtitle">Quản lý và theo dõi không gian nhà hàng thời gian thực</p>
        </div>
        <div class="table-search">
          <label class="sr-only" for="table-search-input">Tìm bàn</label>
          <input id="table-search-input" v-model.trim="tableSearchInput" type="search" placeholder="Tìm bàn, khu vực, trạng thái..." @keydown.esc="clearTableSearch" />
          <button v-if="tableSearchInput" type="button" aria-label="Xóa từ khóa tìm bàn" @click="clearTableSearch">×</button>
        </div>
      </div>

      <div class="content-grid">
        <!-- Form thêm bàn -->
        <div class="form-card">
          <h3> Thêm Bàn Mới</h3>
          <div class="form-group">
            <label>Tên bàn (VD: Bàn T3-04)</label>
            <input v-model="newTable.name" type="text" class="g-form-control" placeholder="Nhập tên bàn..." />
          </div>
          <div class="form-group">
            <label>Khu vực tầng</label>
            <select v-model="newTable.floor" class="g-form-control">
              <option value="Tầng 2 (Sảnh Tiệc)">Tầng 2 (Sảnh Tiệc)</option>
              <option value="Tầng 3 (Phòng VIP)">Tầng 3 (Phòng VIP)</option>
              <option value="Tầng 4 (Phòng VIP)">Tầng 4 (Phòng VIP)</option>
              <option value="Tầng 5 (Phòng VIP)">Tầng 5 (Phòng VIP)</option>
              <option value="Tầng 6 (Sân Thượng)">Tầng 6 (Sân Thượng)</option>
            </select>
          </div>
          <div class="form-group">
            <label>Khu vực phục vụ</label>
            <select v-model="newTable.areaId" class="g-form-control">
              <option :value="null">Chưa gán khu vực</option>
              <option v-for="area in areas" :key="area.id" :value="area.id">
                {{ area.nameVi }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Sức chứa (Số người)</label>
            <select v-model="newTable.capacity" class="g-form-control">
              <option value="4">4 Người</option>
              <option value="6">6 Người</option>
              <option value="8">8 Người</option>
              <option value="10">10 Người</option>
            </select>
          </div>
          <div class="form-group">
            <label>Sức chứa tối đa</label>
            <input v-model.number="newTable.maxCapacity" type="number" min="1" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Giá đặt bàn (VNĐ)</label>
            <input v-model.number="newTable.reservationPrice" type="number" min="0" step="10000" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Mô tả vị trí</label>
            <input v-model="newTable.positionDescription" type="text" class="g-form-control" placeholder="Gần cửa sổ, yên tĩnh..." />
          </div>
          <div class="form-group">
            <label>Ảnh bàn</label>
            <input v-model="newTable.imageUrl" type="url" class="g-form-control" placeholder="URL ảnh riêng của bàn" />
            <input type="file" accept="image/jpeg,image/png,image/webp" class="g-form-control" @change="uploadTableImage($event, newTable)" />
          </div>
          <div class="table-options">
            <label class="checkbox-group">
              <input v-model="newTable.windowSeat" type="checkbox" />
              Gần cửa sổ
            </label>
            <label class="checkbox-group">
              <input v-model="newTable.privateRoom" type="checkbox" />
              Phòng riêng
            </label>
            <label class="checkbox-group">
              <input v-model="newTable.childFriendly" type="checkbox" />
              Phù hợp trẻ em
            </label>
          </div>
          <div class="form-group" v-if="newTable.floor.includes('Tầng 6')">
            <label>Loại View (Dành cho Sân Thượng)</label>
            <select v-model="newTable.viewType" class="g-form-control">
              <option value="View Phố">View Phố</option>
              <option value="View Sân Vườn">View Sân Vườn</option>
              <option value="View Sông">View Sông</option>
            </select>
          </div>
          <button @click="handleAddTable" class="g-btn-primary" style="width:100%; margin-top: 16px;">
            THÊM BÀN NGAY
          </button>
        </div>

        <!-- Sơ đồ bàn -->
        <div class="floor-card">
          <div class="legend-box">
            <div class="status-legend">
              <span class="badge badge-empty"> Bàn Trống</span>
              <span class="badge badge-reserved"> Đã Đặt Trước</span>
              <span class="badge badge-occupied"> Đang Phục Vụ</span>
              <span class="badge badge-cleaning"> Chờ Dọn Bàn</span>
              <span class="badge" style="background: color-mix(in srgb, var(--secondary) 10%, transparent); color: var(--secondary);"> Đã Ghép</span>
            </div>
            
            <div class="map-actions">
              <button @click="showMergeModal = true" class="g-btn-warning" style="padding: 8px 16px; border-radius: 20px; font-weight: bold; border: none; cursor: pointer;">
                 Gộp Bàn
              </button>
              <button @click="isRealisticView = !isRealisticView" class="btn-heatmap" :class="{'active': isRealisticView}" style="border-color: var(--primary); color: var(--primary);">
                 {{ isRealisticView ? 'Tắt Sơ Đồ Thực Tế' : 'Bật Sơ Đồ Thực Tế' }}
              </button>
              <button @click="toggleHeatmap" class="btn-heatmap" :class="{'active': showHeatmap}">
                 {{ showHeatmap ? 'Tắt Bản Đồ Nhiệt' : 'Bật Bản Đồ Nhiệt' }}
              </button>
              <button @click="toggleLayoutMode" class="btn-heatmap" :class="{'active': layoutEditMode}" style="border-color: var(--secondary); color: var(--secondary);">
                 {{ layoutEditMode ? 'Tắt Chỉnh Layout' : 'Chỉnh Layout' }}
              </button>
              <button v-if="layoutEditMode" @click="saveLayouts" class="g-btn-primary" style="padding: 8px 16px; border-radius: 20px;">
                Lưu Layout
              </button>
            </div>
          </div>
          <p v-if="showHeatmap && !heatmapHasData" class="heatmap-empty">Chưa đủ dữ liệu phân tích</p>

          <div v-for="(tables, floorName) in groupedTables" :key="floorName" class="floor-section">
            <h2 class="floor-title"> {{ floorName }}</h2>
            <div v-if="layoutEditMode" class="layout-canvas">
              <div class="layout-hint">Kéo bàn để sắp xếp mặt bằng {{ floorName }}. Bấm "Lưu Layout" sau khi chỉnh.</div>
              <div
                v-for="(t, index) in tables"
                :key="t.id"
                class="table-box layout-table"
                :class="[
                  { 'empty-bg': t.isOccupied === 0, 'reserved-bg': t.isOccupied === 1, 'occupied-bg': t.isOccupied === 2, 'cleaning-bg': t.isOccupied === 3, 'linked-bg': t.isOccupied === 5 }
                ]"
                :style="getLayoutStyle(t, index, floorName)"
                @pointerdown="startDragLayout($event, t, index, floorName)"
              >
                <button @click.stop="openEditModal(t)" class="btn-edit" title="Sửa bàn"><UiIcon name="edit" /></button>
                <button @click.stop="openQrModal(t)" class="btn-qr" title="Mã QR gọi món"><UiIcon name="qr" /></button>
                <span v-if="t.capacity" class="capacity-tag"> {{ t.capacity }}</span>
                <div class="table-status-dot"></div>
                <h3 class="t-name">{{ t.name }}</h3>
                <div class="t-area-text">{{ selectedAreaName(t.areaId) }}</div>
                <div class="t-status-text">{{ t.reservedTime || 'Sẵn sàng phục vụ' }}</div>
              </div>
            </div>
            <div v-else :class="[isRealisticView ? getRealisticClass(floorName) : 'table-grid']">
              <div
                v-for="t in tables"
                :key="t.id"
                class="table-box"
                :class="[
                  { 'empty-bg': t.isOccupied === 0, 'reserved-bg': t.isOccupied === 1, 'occupied-bg': t.isOccupied === 2, 'cleaning-bg': t.isOccupied === 3, 'linked-bg': t.isOccupied === 5 },
                  isRealisticView ? 'realistic-table' : ''
                ]"
              >
                <button v-if="t.isOccupied !== 5" @click="deleteTable(t.id)" class="btn-del" title="Xóa bàn"><UiIcon name="trash" /></button>
                <button v-if="t.isOccupied === 5" @click="unlinkTable(t.id)" class="btn-unlink" title="Tách bàn"><UiIcon name="unlink" /></button>
                <button @click="openEditModal(t)" class="btn-edit" title="Sửa bàn"><UiIcon name="edit" /></button>
                <button @click="openQrModal(t)" class="btn-qr" title="Mã QR gọi món"><UiIcon name="qr" /></button>
                <span v-if="t.viewType" class="view-tag"> {{ t.viewType }}</span>
                <span v-if="t.capacity" class="capacity-tag"> {{ t.capacity }}</span>

                <div class="heatmap-overlay" v-if="showHeatmap" :class="getHeatLevel(t.id)">
                   {{ heatLabel(t.id) }}
                </div>

                <div class="table-status-dot"></div>
                <h3 class="t-name">{{ t.name }}</h3>
                <div class="t-area-text">{{ selectedAreaName(t.areaId) }}</div>
                <div class="t-status-text">{{ t.reservedTime || 'Sẵn sàng phục vụ' }}</div>

                <select :value="t.isOccupied" @change="updateStatus(t.id, $event.target.value)" class="status-dropdown">
                  <option value="0"> Dọn Bàn (Trống)</option>
                  <option value="1"> Giữ Chỗ (Cọc)</option>
                  <option value="2"> Khách Đang Ăn</option>
                  <option value="3"> Chờ Dọn Bàn</option>
                  <option value="5" disabled> Đã Ghép Bàn</option>
                </select>
              </div>
            </div>
          </div>

          <div v-if="Object.keys(groupedTables).length === 0" class="empty-floor">
            Chưa có bàn nào. Hãy thêm bàn mới.
          </div>
        </div>
      </div>
    </main>

    <!-- Merge Table Modal -->
    <div v-if="showMergeModal" class="modal-overlay" @click.self="showMergeModal = false">
      <div class="qr-box" style="max-width: 500px; text-align: left;">
        <h3 style="text-align: center; color: var(--primary); margin-bottom: 20px;"> Gộp Bàn / Chuyển Bàn</h3>
        <div class="form-group">
          <label>Chế Độ Gộp</label>
          <select v-model="mergeData.type" class="g-form-control">
            <option value="ORDER">Dồn Hóa Đơn (Bàn đang có khách)</option>
            <option value="PHYSICAL">Ghép Bàn (Bàn trống / Đặt trước)</option>
          </select>
        </div>
        <div class="form-group" style="margin-top: 15px;">
          <label>Chọn bàn cần chuyển đi / ghép (Bàn nguồn)</label>
          <select v-model="mergeData.fromTable" class="g-form-control">
            <option value="">-- Chọn bàn --</option>
            <option v-for="t in (mergeData.type === 'ORDER' ? activeTables : tablesList)" :key="t.id" :value="t.id">{{ t.name }} ({{ t.floor }})</option>
          </select>
        </div>
        <div class="form-group" style="margin-top: 15px;">
          <label>Chọn bàn đích (Bàn chính)</label>
          <select v-model="mergeData.toTable" class="g-form-control">
            <option value="">-- Chọn bàn --</option>
            <option v-for="t in (mergeData.type === 'ORDER' ? activeTables : tablesList)" :key="t.id" :value="t.id" :disabled="t.id === mergeData.fromTable">{{ t.name }} ({{ t.floor }})</option>
          </select>
        </div>
        <p v-if="mergeData.type === 'ORDER'" style="color: var(--primary); font-size: 0.85rem; font-style: italic; margin-top: 15px;">
          Lưu ý: Toàn bộ món ăn của bàn nguồn sẽ được chuyển sang bàn đích. Bàn nguồn sẽ trở thành bàn trống.
        </p>
        <p v-else style="color: var(--secondary); font-size: 0.85rem; font-style: italic; margin-top: 15px;">
          Lưu ý: Bàn nguồn sẽ được đánh dấu là "Đã Ghép" vào bàn đích. Có thể tách ra sau này.
        </p>
        <div style="display:flex; gap:10px; margin-top:20px;">
          <button @click="executeMerge" class="g-btn-primary" style="flex:1; padding: 10px;">Xác Nhận Gộp</button>
          <button @click="showMergeModal = false" class="btn-cancel" style="flex:1;">Hủy Bỏ</button>
        </div>
      </div>
    </div>

    <!-- Edit Table Modal -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="qr-box edit-table-box">
        <h3>Chỉnh Sửa Bàn</h3>
        <div class="edit-form-grid">
          <div class="form-group">
            <label>Tên bàn</label>
            <input v-model="editTable.name" type="text" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Khu vực tầng</label>
            <select v-model="editTable.floor" class="g-form-control">
              <option value="Tầng 2 (Sảnh Tiệc)">Tầng 2 (Sảnh Tiệc)</option>
              <option value="Tầng 3 (Phòng VIP)">Tầng 3 (Phòng VIP)</option>
              <option value="Tầng 4 (Phòng VIP)">Tầng 4 (Phòng VIP)</option>
              <option value="Tầng 5 (Phòng VIP)">Tầng 5 (Phòng VIP)</option>
              <option value="Tầng 6 (Sân Thượng)">Tầng 6 (Sân Thượng)</option>
            </select>
          </div>
          <div class="form-group">
            <label>Khu vực phục vụ</label>
            <select v-model="editTable.areaId" class="g-form-control">
              <option :value="null">Chưa gán khu vực</option>
              <option v-for="area in areas" :key="area.id" :value="area.id">
                {{ area.nameVi }}
              </option>
            </select>
          </div>
          <div class="form-group">
            <label>Sức chứa chuẩn</label>
            <input v-model.number="editTable.capacity" type="number" min="1" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Sức chứa tối thiểu</label>
            <input v-model.number="editTable.minCapacity" type="number" min="1" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Sức chứa tối đa</label>
            <input v-model.number="editTable.maxCapacity" type="number" min="1" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Số ghế</label>
            <input v-model.number="editTable.seatCount" type="number" min="1" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Giá đặt bàn</label>
            <input v-model.number="editTable.reservationPrice" type="number" min="0" step="10000" class="g-form-control" />
          </div>
          <div class="form-group">
            <label>Loại view</label>
            <input v-model="editTable.viewType" type="text" class="g-form-control" placeholder="View sông, view phố..." />
          </div>
          <div class="form-group">
            <label>Ảnh bàn</label>
            <input v-model="editTable.imageUrl" type="text" class="g-form-control" placeholder="URL ảnh bàn" />
            <input type="file" accept="image/jpeg,image/png,image/webp" class="g-form-control" @change="uploadTableImage($event, editTable)" />
          </div>
          <div class="form-group edit-wide">
            <label>Mô tả vị trí</label>
            <input v-model="editTable.positionDescription" type="text" class="g-form-control" />
          </div>
        </div>
        <div class="table-options edit-options">
          <label class="checkbox-group">
            <input v-model="editTable.windowSeat" type="checkbox" />
            Gần cửa sổ
          </label>
          <label class="checkbox-group">
            <input v-model="editTable.privateRoom" type="checkbox" />
            Phòng riêng
          </label>
          <label class="checkbox-group">
            <input v-model="editTable.childFriendly" type="checkbox" />
            Phù hợp trẻ em
          </label>
          <label class="checkbox-group">
            <input v-model="editTable.active" type="checkbox" />
            Đang sử dụng
          </label>
        </div>
        <div style="display:flex; gap:10px; margin-top:20px;">
          <button @click="submitEditTable" class="g-btn-primary" style="flex:1; padding: 10px;">Lưu Thay Đổi</button>
          <button @click="closeEditModal" class="btn-cancel" style="flex:1;">Đóng</button>
        </div>
      </div>
    </div>

    <!-- QR Code Modal -->
    <div v-if="showQrModal" class="modal-overlay" @click.self="showQrModal = false">
      <div class="qr-box">
        <h3>Mã QR Khách Tự Gọi Món</h3>
        <p class="qr-table-name">{{ qrTable }}</p>
        <div class="qr-wrapper" id="qr-wrapper">
          <qrcode-vue :value="qrValue" :size="250" level="H" render-as="canvas" />
        </div>
        <p class="qr-hint">In mã này đặt tại bàn. Khách dùng điện thoại quét mã để mở Menu & tự gọi món.</p>
        <div style="display:flex; gap:10px; margin-top:20px;">
          <button @click="downloadQRImage" class="g-btn-primary" style="flex:1; padding: 10px;">Tải Ảnh QR</button>
          <button @click="showQrModal = false" class="btn-cancel" style="flex:1;">Đóng</button>
        </div>
      </div>
    </div>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import api from '@/services/api';
import QrcodeVue from 'qrcode.vue';
import { useDialog } from '@/composables/useDialog';
import { useToast } from '@/composables/useToast';

const { confirmDialog } = useDialog();
const toast = useToast();

const tablesList = ref([]);
const areas = ref([]);
const tableSearchInput = ref('');
const tableSearch = ref('');
let tableSearchTimer = null;
const tableStatusLabels = {
  0: 'ban trong san sang empty',
  1: 'da dat truoc reserved',
  2: 'dang phuc vu occupied',
  3: 'cho don cleaning',
  5: 'da ghep linked'
};
const defaultNewTable = () => ({
  name: '',
  floor: 'Tầng 2 (Sảnh Tiệc)',
  capacity: 4,
  minCapacity: 1,
  maxCapacity: 4,
  seatCount: 4,
  reservationPrice: 400000,
  imageUrl: '',
  areaId: null,
  viewType: '',
  positionDescription: '',
  windowSeat: false,
  privateRoom: false,
  active: true,
  childFriendly: true
});
const newTable = ref(defaultNewTable());
const showEditModal = ref(false);
const editTable = ref(defaultNewTable());

const isRealisticView = ref(false);
const showMergeModal = ref(false);
const mergeData = ref({ type: 'PHYSICAL', fromTable: '', toTable: '' });
const layoutEditMode = ref(false);
const tableLayouts = ref({});
const draggingLayout = ref(null);

const activeTables = computed(() => {
  return tablesList.value.filter(t => t.isOccupied === 1 || t.isOccupied === 2 || t.isOccupied === 3);
});

const executeMerge = async () => {
  if (!mergeData.value.fromTable || !mergeData.value.toTable) {
    toast.warning('Vui lòng chọn đầy đủ bàn nguồn và bàn đích!');
    return;
  }
  
  const fromT = tablesList.value.find(t => t.id === mergeData.value.fromTable);
  const toT = tablesList.value.find(t => t.id === mergeData.value.toTable);
  
  const confirmed = await confirmDialog({
    title: 'Xác nhận gộp bàn',
    message: `Bạn chắc chắn muốn ghép/gộp ${fromT.name} vào ${toT.name}?`,
    confirmLabel: 'Gộp bàn',
    danger: true,
  });
  if (!confirmed) return;
  
  try {
    const token = sessionStorage.getItem('staff_token');
    
    if (mergeData.value.type === 'ORDER') {
      const res = await api.post('/api/orders/merge-tables', {
        fromTableId: fromT.id,
        toTableId: toT.id
      }, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      toast.success(res.data.message || 'Gộp bàn thành công!');
    } else {
      const res = await api.put(`/api/tables/${fromT.id}/link/${toT.id}`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      toast.success(res.data || 'Ghép bàn vật lý thành công!');
    }
    
    showMergeModal.value = false;
    mergeData.value = { type: 'PHYSICAL', fromTable: '', toTable: '' };
    fetchTables();
  } catch (err) {
    toast.error(err.response?.data?.message || err.response?.data || 'Lỗi khi thao tác. Vui lòng kiểm tra lại!');
  }
};

const unlinkTable = async (id) => {
  const confirmed = await confirmDialog({
    title: 'Xác nhận tách bàn',
    message: 'Bạn có chắc chắn muốn tách bàn này ra không?',
    confirmLabel: 'Tách bàn',
    danger: true,
  });
  if (!confirmed) return;
  try {
    const token = sessionStorage.getItem('staff_token');
    const res = await api.put(`/api/tables/${id}/unlink`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    toast.success(res.data || 'Tách bàn thành công!');
    fetchTables();
  } catch {
    toast.error('Lỗi khi tách bàn!');
  }
};

const getRealisticClass = (floor) => {
  if (floor.includes('Tầng 2')) return 'realistic-hall';
  if (floor.includes('VIP')) return 'realistic-vip';
  if (floor.includes('Tầng 6')) return 'realistic-rooftop';
  return 'table-grid';
};

const showQrModal = ref(false);
const qrTable = ref('');
const qrCapability = ref('');
const qrValue = computed(() => {
  return qrCapability.value
    ? `${window.location.origin}/dine-in?cap=${encodeURIComponent(qrCapability.value)}`
    : '';
});

const openQrModal = async (table) => {
  try {
    const token = sessionStorage.getItem('staff_token');
    const response = await api.post(`/api/table-sessions/admin/${table.id}`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    qrTable.value = response.data.tableName || table.name;
    qrCapability.value = response.data.token;
    showQrModal.value = true;
  } catch (error) {
    toast.error(error.response?.data?.message || 'Không thể cấp mã QR an toàn cho bàn này.');
  }
};

const downloadQRImage = () => {
  const canvas = document.querySelector('#qr-wrapper canvas');
  if (canvas) {
    const url = canvas.toDataURL("image/png");
    const link = document.createElement('a');
    link.href = url;
    link.download = `QR_Code_${qrTable.value}.png`;
    link.click();
  } else {
    toast.error('Lỗi tạo ảnh QR! Vui lòng thử lại.');
  }
};

const fetchTables = async () => {
  const token = sessionStorage.getItem('staff_token');
  try {
    const res = await api.get('/api/tables', {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    tablesList.value = res.data;
  } catch (error) { console.error('Lỗi lấy danh sách bàn', error); }
};

const fetchAreas = async () => {
  try {
    const res = await api.get('/api/areas');
    areas.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    console.error('Lỗi lấy danh sách khu vực', error);
  }
};

const fetchLayouts = async () => {
  try {
    const res = await api.get('/api/admin/table-layouts', {
      headers: { 'Authorization': `Bearer ${sessionStorage.getItem('staff_token')}` }
    });
    const next = {};
    (Array.isArray(res.data) ? res.data : []).forEach(layout => {
      next[layout.tableId] = {
        tableId: layout.tableId,
        areaId: layout.areaId ?? null,
        floorName: layout.floorName || '',
        xPosition: Number(layout.xPosition || 0),
        yPosition: Number(layout.yPosition || 0),
        width: Number(layout.width || 170),
        height: Number(layout.height || 130),
        shape: layout.shape || 'RECTANGLE',
        rotation: Number(layout.rotation || 0)
      };
    });
    tableLayouts.value = next;
  } catch {
    toast.error('Không thể tải layout bàn. Vui lòng đăng nhập bằng Admin/Manager.');
  }
};

const defaultLayoutFor = (table, index, floorName) => ({
  tableId: table.id,
  areaId: table.areaId ?? null,
  floorName,
  xPosition: 24 + (index % 4) * 190,
  yPosition: 52 + Math.floor(index / 4) * 150,
  width: 170,
  height: 130,
  shape: 'RECTANGLE',
  rotation: 0
});

const getLayout = (table, index, floorName) => {
  if (!tableLayouts.value[table.id]) {
    tableLayouts.value[table.id] = defaultLayoutFor(table, index, floorName);
  }
  return tableLayouts.value[table.id];
};

const getLayoutStyle = (table, index, floorName) => {
  const layout = getLayout(table, index, floorName);
  return {
    left: `${layout.xPosition}px`,
    top: `${layout.yPosition}px`,
    width: `${layout.width}px`,
    minHeight: `${layout.height}px`,
    transform: `rotate(${layout.rotation || 0}deg)`
  };
};

const toggleLayoutMode = async () => {
  layoutEditMode.value = !layoutEditMode.value;
  if (layoutEditMode.value) {
    isRealisticView.value = false;
    showHeatmap.value = false;
    await fetchLayouts();
  }
};

const startDragLayout = (event, table, index, floorName) => {
  if (!layoutEditMode.value || event.button !== 0) return;
  const canvas = event.currentTarget.closest('.layout-canvas');
  const canvasRect = canvas.getBoundingClientRect();
  const layout = getLayout(table, index, floorName);
  draggingLayout.value = {
    tableId: table.id,
    floorName,
    areaId: table.areaId ?? null,
    offsetX: event.clientX - canvasRect.left - layout.xPosition,
    offsetY: event.clientY - canvasRect.top - layout.yPosition,
    canvas
  };
  event.currentTarget.setPointerCapture(event.pointerId);
  event.preventDefault();
};

const moveDragLayout = (event) => {
  const drag = draggingLayout.value;
  if (!drag) return;
  const canvasRect = drag.canvas.getBoundingClientRect();
  const layout = tableLayouts.value[drag.tableId];
  if (!layout) return;
  const maxX = Math.max(0, canvasRect.width - layout.width - 12);
  const maxY = Math.max(0, canvasRect.height - layout.height - 12);
  layout.xPosition = Math.min(Math.max(12, event.clientX - canvasRect.left - drag.offsetX), maxX);
  layout.yPosition = Math.min(Math.max(36, event.clientY - canvasRect.top - drag.offsetY), maxY);
  layout.floorName = drag.floorName;
  layout.areaId = drag.areaId;
};

const stopDragLayout = () => {
  draggingLayout.value = null;
};

const saveLayouts = async () => {
  const payload = tablesList.value.map((table, index) => {
    const layout = getLayout(table, index, table.floor);
    return {
      tableId: table.id,
      areaId: table.areaId ?? null,
      floorName: table.floor,
      xPosition: Number(layout.xPosition || 0),
      yPosition: Number(layout.yPosition || 0),
      width: Number(layout.width || 170),
      height: Number(layout.height || 130),
      shape: layout.shape || 'RECTANGLE',
      rotation: Number(layout.rotation || 0)
    };
  });
  try {
    await api.put('/api/admin/table-layouts/bulk', payload, {
      headers: { 'Authorization': `Bearer ${sessionStorage.getItem('staff_token')}` }
    });
    toast.success('Đã lưu layout bàn.');
    await fetchLayouts();
  } catch (error) {
    toast.error(error.response?.data?.message || 'Không thể lưu layout bàn.');
  }
};

const selectedAreaName = (areaId) => {
  if (!areaId) return 'Chưa gán khu vực';
  return areas.value.find(area => area.id === areaId)?.nameVi || `Khu vực #${areaId}`;
};

const normalizeTablePayload = (table) => ({
  ...table,
  areaId: table.areaId === '' || table.areaId === undefined ? null : table.areaId,
  capacity: Number(table.capacity || 4),
  minCapacity: Number(table.minCapacity || 1),
  maxCapacity: Number(table.maxCapacity || table.capacity || 4),
  seatCount: Number(table.seatCount || table.maxCapacity || table.capacity || 4),
  reservationPrice: Number(table.reservationPrice || 0),
  hasView: Boolean(table.windowSeat || table.viewType),
  windowSeat: Boolean(table.windowSeat),
  privateRoom: Boolean(table.privateRoom),
  childFriendly: table.childFriendly !== false,
  active: table.active !== false
});

const uploadTableImage = async (event, table) => {
  const file = event.target.files?.[0];
  if (!file) return;
  const formData = new FormData();
  formData.append('file', file);
  try {
    const response = await api.post('/api/admin/tables/images', formData, {
      headers: {
        'Authorization': `Bearer ${sessionStorage.getItem('staff_token')}`,
        'Content-Type': 'multipart/form-data'
      }
    });
    table.imageUrl = response.data.imageUrl;
  } catch (error) {
    toast.error(error.response?.data?.message || error.response?.data || 'Không thể tải ảnh bàn lên.');
  } finally {
    event.target.value = '';
  }
};

const handleAddTable = async () => {
  if (!newTable.value.name) return;
  const token = sessionStorage.getItem('staff_token');
  try {
    await api.post('/api/tables', normalizeTablePayload(newTable.value), {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    newTable.value = defaultNewTable();
    fetchTables();
  } catch { toast.error('Lỗi thêm bàn!'); }
};

const openEditModal = (table) => {
  editTable.value = {
    ...defaultNewTable(),
    ...table,
    areaId: table.areaId ?? null,
    minCapacity: table.minCapacity ?? 1,
    maxCapacity: table.maxCapacity ?? table.capacity ?? 4,
    seatCount: table.seatCount ?? table.capacity ?? 4,
    reservationPrice: table.reservationPrice ?? 0,
    windowSeat: Boolean(table.windowSeat),
    privateRoom: Boolean(table.privateRoom),
    childFriendly: table.childFriendly !== false,
    active: table.active !== false
  };
  showEditModal.value = true;
};

const closeEditModal = () => {
  showEditModal.value = false;
  editTable.value = defaultNewTable();
};

const submitEditTable = async () => {
  if (!editTable.value.id || !editTable.value.name) {
    toast.warning('Vui lòng nhập đầy đủ tên bàn.');
    return;
  }
  try {
    await api.put(`/api/admin/tables/${editTable.value.id}`, normalizeTablePayload(editTable.value), {
      headers: { 'Authorization': `Bearer ${sessionStorage.getItem('staff_token')}` }
    });
    closeEditModal();
    fetchTables();
  } catch (error) {
    toast.error(error.response?.data?.message || error.response?.data || 'Lỗi cập nhật bàn!');
  }
};

const updateStatus = async (tableId, newStatus) => {
  if (newStatus == 0) {
    const confirmed = await confirmDialog({
      title: 'Xác nhận dọn và giải phóng bàn',
      message: 'Dọn bàn sẽ đóng các đơn đủ điều kiện tại bàn này. Xác nhận?',
      confirmLabel: 'Dọn bàn',
      danger: true,
    });
    if (!confirmed) {
      fetchTables(); return;
    }
  }
  const token = sessionStorage.getItem('staff_token');
  try {
    await api.put(`/api/tables/${tableId}/status?status=${newStatus}`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    fetchTables();
  } catch { fetchTables(); }
};

// Heatmap Logic
const showHeatmap = ref(false);
const tableHeat = ref({});
const heatmapHasData = computed(() => Object.values(tableHeat.value).some(item => item.uses > 0));

const toggleHeatmap = async () => {
  showHeatmap.value = !showHeatmap.value;
  if (showHeatmap.value) {
    try {
      const res = await api.get('/api/admin/orders?limit=500', {
        headers: { 'Authorization': `Bearer ${sessionStorage.getItem('staff_token')}` }
      });
      const orders = Array.isArray(res.data) ? res.data : [];
      const heat = Object.fromEntries(tablesList.value.map(table => [table.id, { uses: 0, revenue: 0 }]));
      orders.forEach(order => {
        const metric = heat[order.tableId];
        if (!metric) return;
        metric.uses += 1;
        metric.revenue += Number(order.totalAmount || 0);
      });
      tableHeat.value = heat;
    } catch (e) {
      console.error(e);
      toast.error('Không thể tải dữ liệu để phân tích bản đồ nhiệt.');
      showHeatmap.value = false;
    }
  }
};

const getHeatScore = tableId => {
  const metric = tableHeat.value[tableId] || { uses: 0, revenue: 0 };
  return metric.uses + Math.min(metric.revenue / 1000000, 10);
};

const heatLabel = tableId => {
  const metric = tableHeat.value[tableId] || { uses: 0, revenue: 0 };
  return `${metric.uses} lượt · ${Math.round(metric.revenue / 1000).toLocaleString('vi-VN')}k`;
};

const getHeatLevel = (tableId) => {
  if (!showHeatmap.value) return '';
  const score = getHeatScore(tableId);
  if (score >= 12) return 'heat-high';
  if (score >= 6) return 'heat-medium';
  if (score > 0) return 'heat-low';
  return 'heat-none';
};

const deleteTable = async (id) => {
  const confirmed = await confirmDialog({
    title: 'Xóa bàn',
    message: 'Xóa bàn này khỏi hệ thống? Thao tác không thể hoàn tác.',
    confirmLabel: 'Xóa bàn',
    danger: true,
  });
  if (!confirmed) return;
  try {
    await api.delete(`/api/admin/tables/${id}`, {
      headers: { 'Authorization': `Bearer ${sessionStorage.getItem('staff_token')}` }
    });
    fetchTables();
  } catch { toast.error('Không thể xóa bàn đang có dữ liệu hóa đơn!'); }
};

const groupedTables = computed(() => {
  const query = tableSearch.value.toLocaleLowerCase('vi-VN');
  return tablesList.value.filter((table) => {
    if (!query) return true;
    const status = tableStatusLabels[table.isOccupied] || '';
    return `${table.name || ''} ${table.floor || ''} ${table.areaName || ''} ${table.capacity || ''} ${status}`.toLocaleLowerCase('vi-VN').includes(query);
  }).reduce((groups, table) => {
    if (!groups[table.floor]) groups[table.floor] = [];
    groups[table.floor].push(table);
    return groups;
  }, {});
});

watch(tableSearchInput, (value) => {
  if (tableSearchTimer) clearTimeout(tableSearchTimer);
  tableSearchTimer = setTimeout(() => { tableSearch.value = value; }, 220);
});

const clearTableSearch = () => {
  if (tableSearchTimer) clearTimeout(tableSearchTimer);
  tableSearchInput.value = '';
  tableSearch.value = '';
};

onMounted(() => {
  fetchTables();
  fetchAreas();
  window.addEventListener('pointermove', moveDragLayout);
  window.addEventListener('pointerup', stopDragLayout);
});

onUnmounted(() => {
  if (tableSearchTimer) clearTimeout(tableSearchTimer);
  window.removeEventListener('pointermove', moveDragLayout);
  window.removeEventListener('pointerup', stopDragLayout);
});
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }

.page-header { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }
.table-search { position: relative; width: min(100%, 340px); }
.table-search input { width: 100%; min-height: 42px; padding: 0 42px 0 16px; border: 1px solid var(--border); border-radius: 999px; background: var(--bg-card2); color: var(--text-primary); font: inherit; }
.table-search input:focus { outline: 2px solid var(--primary-glow); border-color: var(--primary); }
.table-search button { position: absolute; top: 50%; right: 8px; width: 28px; height: 28px; padding: 0; transform: translateY(-50%); border: 0; border-radius: 50%; background: transparent; color: var(--text-muted); font-size: 1.35rem; cursor: pointer; }
.table-search button:hover { color: var(--primary); background: var(--bg-hover); }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }

.content-grid { display: grid; grid-template-columns: 300px 1fr; gap: 28px; }

.form-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
  height: fit-content;
}
.form-card h3 {
  margin: 0 0 24px 0; font-size: 1.1rem; font-weight: 700; color: var(--text-heading);
  padding-bottom: 16px; border-bottom: 1px solid var(--border-light);
}
.form-group { margin-bottom: 16px; }
.form-group label {
  display: block; font-size: 0.83rem; font-weight: 600; color: var(--text-muted);
  margin-bottom: 7px; text-transform: uppercase; letter-spacing: 0.5px;
}
.checkbox-group {
  display: flex; align-items: center; gap: 10px;
  color: var(--text-secondary); font-size: 0.9rem; font-weight: 500;
}
.checkbox-group input { accent-color: var(--primary); width: 16px; height: 16px; }
.table-options {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  background: var(--bg-card2);
  margin-bottom: 16px;
}

.floor-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
}

/* Legend */
.legend-box {
  display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px;
  margin-bottom: 28px; padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}
.status-legend, .map-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.badge { padding: 7px 16px; border-radius: 20px; font-size: 0.82rem; font-weight: 700; white-space: nowrap; }
.badge-empty { background: color-mix(in srgb, var(--secondary) 10%, transparent); color: var(--primary); }
.badge-reserved { background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); color: var(--color-tertiary); }
.badge-occupied { background: color-mix(in srgb, var(--primary) 10%, transparent); color: var(--primary); }
.badge-cleaning { background: color-mix(in srgb, var(--color-tertiary) 10%, transparent); color: var(--color-tertiary); }

.floor-title {
  font-size: 1rem; font-weight: 700; color: var(--primary);
  margin: 24px 0 16px 0; letter-spacing: 1px; text-transform: uppercase;
}
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }

.layout-canvas {
  position: relative;
  min-height: 680px;
  border: 1px dashed color-mix(in srgb, var(--secondary) 45%, transparent);
  border-radius: var(--radius-lg);
  background:
    linear-gradient(rgba(255,255,255,0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.035) 1px, transparent 1px),
    var(--bg-card2);
  background-size: 32px 32px;
  overflow: hidden;
  touch-action: none;
}
.layout-hint {
  position: absolute;
  top: 10px;
  left: 14px;
  right: 14px;
  color: var(--text-muted);
  font-size: 0.82rem;
  font-weight: 700;
  pointer-events: none;
}
.layout-table {
  position: absolute;
  cursor: grab;
  user-select: none;
  z-index: 1;
}
.layout-table:active {
  cursor: grabbing;
  z-index: 5;
  box-shadow: 0 14px 34px rgba(0,0,0,0.35);
}

/* Table Box */
.table-box {
  position: relative;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 24px 14px 16px 14px;
  text-align: center;
  background: var(--bg-card2);
  transition: var(--transition);
}
.table-box:hover { transform: translateY(-4px); box-shadow: var(--shadow-md); }

.table-status-dot {
  width: 12px; height: 12px; border-radius: 50%;
  margin: 0 auto 10px auto;
}

.t-name { margin: 0 0 6px 0; font-size: 1.1rem; font-weight: 800; color: var(--text-heading); }
.t-area-text { font-size: 0.72rem; color: var(--primary); font-weight: 700; min-height: 16px; margin-bottom: 4px; }
.t-status-text { font-size: 0.78rem; color: var(--text-muted); font-style: italic; margin-bottom: 14px; min-height: 18px; }

.status-dropdown {
  width: 100%; padding: 8px;
  background: var(--bg-input); border: 1px solid var(--border-light);
  border-radius: var(--radius-sm); color: var(--text-secondary);
  font-size: 0.82rem; font-weight: 600; cursor: pointer; outline: none;
  transition: var(--transition);
}
.status-dropdown:focus { border-color: var(--primary); }

/* Status Variants */
.empty-bg {
  border-color: color-mix(in srgb, var(--secondary) 30%, transparent);
  background: linear-gradient(135deg, var(--bg-card2), color-mix(in srgb, var(--secondary) 4%, transparent));
}
.empty-bg .table-status-dot { background: var(--primary); box-shadow: 0 0 8px color-mix(in srgb, var(--secondary) 60%, transparent); }

.reserved-bg {
  border-color: color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  background: linear-gradient(135deg, var(--bg-card2), color-mix(in srgb, var(--color-tertiary) 4%, transparent));
}
.reserved-bg .table-status-dot { background: var(--color-tertiary); box-shadow: 0 0 8px color-mix(in srgb, var(--color-tertiary) 60%, transparent); }

.occupied-bg {
  border-color: color-mix(in srgb, var(--primary) 30%, transparent);
  background: linear-gradient(135deg, var(--bg-card2), color-mix(in srgb, var(--primary) 4%, transparent));
}
.occupied-bg .table-status-dot { background: var(--primary); box-shadow: 0 0 8px color-mix(in srgb, var(--primary) 60%, transparent); }

.cleaning-bg {
  border-color: color-mix(in srgb, var(--color-tertiary) 30%, transparent);
  background: linear-gradient(135deg, var(--bg-card2), color-mix(in srgb, var(--color-tertiary) 4%, transparent));
}
.cleaning-bg .table-status-dot { background: var(--color-tertiary); box-shadow: 0 0 8px color-mix(in srgb, var(--color-tertiary) 60%, transparent); }

.linked-bg {
  border-color: color-mix(in srgb, var(--secondary) 30%, transparent);
  background: linear-gradient(135deg, var(--bg-card2), color-mix(in srgb, var(--secondary) 4%, transparent));
}
.linked-bg .table-status-dot { background: var(--secondary); box-shadow: 0 0 8px color-mix(in srgb, var(--secondary) 60%, transparent); }

.btn-del {
  position: absolute; top: 8px; left: 8px;
  background: rgba(0,0,0,0.3); border: none;
  color: var(--text-muted); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.7rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-del:hover { background: color-mix(in srgb, var(--primary) 40%, transparent); color: var(--primary); }

.btn-unlink {
  position: absolute; top: 8px; left: 8px;
  background: color-mix(in srgb, var(--secondary) 20%, transparent); border: none;
  color: var(--secondary); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.9rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-unlink:hover { background: color-mix(in srgb, var(--secondary) 40%, transparent); transform: scale(1.1); }

.btn-qr {
  position: absolute; top: 8px; right: 8px;
  background: color-mix(in srgb, var(--success) 20%, transparent); border: none;
  color: var(--success); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.9rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-qr:hover { background: color-mix(in srgb, var(--success) 40%, transparent); transform: scale(1.1); }

.btn-edit {
  position: absolute; top: 38px; right: 8px;
  background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); border: none;
  color: var(--color-tertiary); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.9rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-edit:hover { background: color-mix(in srgb, var(--color-tertiary) 40%, transparent); transform: scale(1.1); }

.view-tag {
  position: absolute; top: 8px; right: 38px;
  background: rgba(123,96,47,0.25); color: var(--color-tertiary);
  font-size: 0.68rem; padding: 2px 7px;
  border-radius: 10px; font-weight: 700;
  border: 1px solid rgba(123,96,47,0.3);
}

.capacity-tag {
  position: absolute; top: 8px; left: 36px;
  background: color-mix(in srgb, var(--secondary) 25%, transparent); color: var(--secondary);
  font-size: 0.68rem; padding: 2px 7px;
  border-radius: 10px; font-weight: 700;
  border: 1px solid color-mix(in srgb, var(--secondary) 30%, transparent);
}

.empty-floor { text-align: center; color: var(--text-muted); padding: 60px; font-style: italic; }

/* QR Modal */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); z-index: 999; display: flex; align-items: center; justify-content: center; }
.qr-box {
  background: var(--bg-card); padding: 30px; border-radius: 16px;
  text-align: center; width: 100%; max-width: 400px;
  border: 1px solid var(--primary); box-shadow: 0 10px 30px rgba(0,0,0,0.8);
}
.qr-box h3 { margin-top: 0; color: var(--text-heading); }
.qr-table-name { font-size: 1.5rem; font-weight: 900; color: var(--primary); margin: 10px 0 20px 0; letter-spacing: 1px;}
.qr-wrapper { background: #FFFFFF; padding: 20px; border-radius: 12px; display: inline-block; margin-bottom: 15px; border: 4px solid var(--primary); }
.qr-hint { color: var(--text-muted); font-size: 0.85rem; font-style: italic; line-height: 1.5;}
.btn-cancel { background: transparent; border: 1px solid var(--border-light); color: var(--text-muted); padding: 10px; border-radius: var(--radius-md); font-weight: 600; cursor: pointer; transition: 0.3s;}
.btn-cancel:hover { background: rgba(255,255,255,0.05); }

.edit-table-box { max-width: 760px; text-align: left; max-height: 90vh; overflow-y: auto; }
.edit-table-box h3 { text-align: center; color: var(--primary); margin-bottom: 20px; }
.edit-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px 16px; }
.edit-wide { grid-column: 1 / -1; }
.edit-options { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 4px; }

/* Heatmap Styles */
.btn-heatmap {
  display: flex; align-items: center; justify-content: center; gap: 8px; white-space: nowrap; background: transparent; border: 1px solid var(--primary); color: var(--primary); padding: 8px 20px; border-radius: 20px; font-weight: bold; cursor: pointer; transition: 0.3s; font-size: 0.9rem;
}
.map-actions .g-btn-warning, .map-actions .g-btn-primary { display: flex; align-items: center; justify-content: center; gap: 8px; white-space: nowrap; }
.heatmap-empty { margin: -14px 0 20px; padding: 12px 16px; border: 1px dashed var(--border); border-radius: 10px; background: var(--color-primary-fixed); color: var(--text-muted); text-align: center; font-weight: 700; }
.btn-heatmap:hover { background: color-mix(in srgb, var(--primary) 10%, transparent); }
.btn-heatmap.active {
  background: var(--primary); color: #FFFFFF; box-shadow: 0 0 15px color-mix(in srgb, var(--primary) 60%, transparent);
}

.table-box { overflow: hidden; }

.heatmap-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.8rem; font-weight: 900; color: #FFFFFF; text-shadow: 0 2px 10px rgba(0,0,0,0.8);
  z-index: 10; opacity: 0.85; pointer-events: none; transition: 0.5s;
}

.heat-high { background: linear-gradient(135deg, color-mix(in srgb, var(--primary) 90%, transparent), rgba(192,57,43,0.9)); }
.heat-medium { background: linear-gradient(135deg, color-mix(in srgb, var(--color-tertiary) 90%, transparent), rgba(211,84,0,0.9)); }
.heat-low { background: linear-gradient(135deg, color-mix(in srgb, var(--color-tertiary) 80%, transparent), color-mix(in srgb, var(--color-tertiary) 80%, transparent)); }
.heat-none { background: rgba(111,122,115,0.8); color: var(--border); font-size: 1.2rem; }

/* Realistic View Styles */
.realistic-hall {
  display: flex; flex-wrap: wrap; justify-content: center; gap: 30px;
  background: url('https://www.transparenttextures.com/patterns/wood-pattern.png'), var(--bg-card2);
  padding: 40px; border-radius: 12px; border: 8px solid var(--color-outline);
  box-shadow: inset 0 0 20px rgba(0,0,0,0.1);
}
.realistic-vip {
  display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
  background: var(--text-secondary); padding: 20px; border-radius: 8px;
}
.realistic-vip .table-box {
  background: var(--border); border: 4px solid var(--color-tertiary); border-radius: 0;
  position: relative; padding: 30px 10px;
}
.realistic-vip .table-box::before {
  content: "Cửa vào"; position: absolute; bottom: -4px; left: 50%; transform: translateX(-50%);
  background: var(--text-secondary); color: #FFFFFF; padding: 2px 10px; font-size: 0.6rem;
}
.realistic-rooftop {
  display: flex; flex-wrap: wrap; justify-content: space-around; gap: 40px;
  background: #B9D8C2; padding: 50px 20px; border-radius: 50px;
  border: 4px dashed var(--success); position: relative;
}
.realistic-rooftop::after {
  content: " Cây xanh & View Sông "; position: absolute; top: 10px; left: 50%; transform: translateX(-50%);
  color: var(--text-primary); font-weight: bold; font-size: 1.2rem; opacity: 0.4;
}

.realistic-table {
  width: 160px; height: 160px; border-radius: 50%;
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
  margin: 10px;
}
.realistic-hall .realistic-table {
  width: 200px; height: 100px; border-radius: 8px; /* Bàn chữ nhật */
}
</style>
