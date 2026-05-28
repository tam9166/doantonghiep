<template>
  <AdminLayout>
  <div class="admin-wrapper">
    

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Sơ Đồ Tình Trạng Bàn</h1>
        <p class="page-subtitle">Quản lý và theo dõi không gian nhà hàng thời gian thực</p>
      </div>

      <div class="content-grid">
        <!-- Form thêm bàn -->
        <div class="form-card">
          <h3>✨ Thêm Bàn Mới</h3>
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
            <label>Sức chứa (Số người)</label>
            <select v-model="newTable.capacity" class="g-form-control">
              <option value="4">4 Người</option>
              <option value="6">6 Người</option>
              <option value="8">8 Người</option>
              <option value="10">10 Người</option>
            </select>
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
          <div class="legend-box" style="justify-content: space-between; align-items: center;">
            <div>
              <span class="badge badge-empty">🟢 Bàn Trống</span>
              <span class="badge badge-reserved">🟡 Đã Đặt Trước</span>
              <span class="badge badge-occupied">🔴 Đang Phục Vụ</span>
              <span class="badge badge-cleaning">🟣 Chờ Dọn Bàn</span>
              <span class="badge" style="background: rgba(52,152,219,0.1); color: #2980b9;">🔗 Đã Ghép</span>
            </div>
            
            <div style="display: flex; gap: 10px;">
              <button @click="showMergeModal = true" class="g-btn-warning" style="padding: 8px 16px; border-radius: 20px; font-weight: bold; border: none; cursor: pointer;">
                🔗 Gộp Bàn
              </button>
              <button @click="isRealisticView = !isRealisticView" class="btn-heatmap" :class="{'active': isRealisticView}" style="border-color: var(--primary); color: var(--primary);">
                🗺️ {{ isRealisticView ? 'Tắt Sơ Đồ Thực Tế' : 'Bật Sơ Đồ Thực Tế' }}
              </button>
              <button @click="toggleHeatmap" class="btn-heatmap" :class="{'active': showHeatmap}">
                🔥 {{ showHeatmap ? 'Tắt Bản Đồ Nhiệt' : 'Bật Bản Đồ Nhiệt' }}
              </button>
            </div>
          </div>

          <div v-for="(tables, floorName) in groupedTables" :key="floorName" class="floor-section">
            <h2 class="floor-title">📍 {{ floorName }}</h2>
            <div :class="[isRealisticView ? getRealisticClass(floorName) : 'table-grid']">
              <div
                v-for="t in tables"
                :key="t.id"
                class="table-box"
                :class="[
                  { 'empty-bg': t.isOccupied === 0, 'reserved-bg': t.isOccupied === 1, 'occupied-bg': t.isOccupied === 2, 'cleaning-bg': t.isOccupied === 3, 'linked-bg': t.isOccupied === 5 },
                  isRealisticView ? 'realistic-table' : ''
                ]"
              >
                <button v-if="t.isOccupied !== 5" @click="deleteTable(t.id)" class="btn-del" title="Xóa bàn">✖</button>
                <button v-if="t.isOccupied === 5" @click="unlinkTable(t.id)" class="btn-unlink" title="Tách bàn">✂️</button>
                <button @click="openQrModal(t.name)" class="btn-qr" title="Mã QR gọi món">📱</button>
                <span v-if="t.viewType" class="view-tag">★ {{ t.viewType }}</span>
                <span v-if="t.capacity" class="capacity-tag">👥 {{ t.capacity }}</span>

                <div class="heatmap-overlay" v-if="showHeatmap" :class="getHeatLevel(t.name)">
                   {{ tableHeat[t.name] || 0 }} đơn
                </div>

                <div class="table-status-dot"></div>
                <h3 class="t-name">{{ t.name }}</h3>
                <div class="t-status-text">{{ t.reservedTime || 'Sẵn sàng phục vụ' }}</div>

                <select :value="t.isOccupied" @change="updateStatus(t.id, $event.target.value)" class="status-dropdown">
                  <option value="0">🟢 Dọn Bàn (Trống)</option>
                  <option value="1">🟡 Giữ Chỗ (Cọc)</option>
                  <option value="2">🔴 Khách Đang Ăn</option>
                  <option value="3">🟣 Chờ Dọn Bàn</option>
                  <option value="5" disabled>🔗 Đã Ghép Bàn</option>
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
        <h3 style="text-align: center; color: var(--primary); margin-bottom: 20px;">🔗 Gộp Bàn / Chuyển Bàn</h3>
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
        <p v-if="mergeData.type === 'ORDER'" style="color: #e74c3c; font-size: 0.85rem; font-style: italic; margin-top: 15px;">
          Lưu ý: Toàn bộ món ăn của bàn nguồn sẽ được chuyển sang bàn đích. Bàn nguồn sẽ trở thành bàn trống.
        </p>
        <p v-else style="color: #3498db; font-size: 0.85rem; font-style: italic; margin-top: 15px;">
          Lưu ý: Bàn nguồn sẽ được đánh dấu là "Đã Ghép" vào bàn đích. Có thể tách ra sau này.
        </p>
        <div style="display:flex; gap:10px; margin-top:20px;">
          <button @click="executeMerge" class="g-btn-primary" style="flex:1; padding: 10px;">Xác Nhận Gộp</button>
          <button @click="showMergeModal = false" class="btn-cancel" style="flex:1;">Hủy Bỏ</button>
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

import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';

const tablesList = ref([]);
const newTable = ref({ name: '', floor: 'Tầng 2 (Sảnh Tiệc)', capacity: 4, viewType: '' });

const isRealisticView = ref(false);
const showMergeModal = ref(false);
const mergeData = ref({ type: 'PHYSICAL', fromTable: '', toTable: '' });

const activeTables = computed(() => {
  return tablesList.value.filter(t => t.isOccupied === 1 || t.isOccupied === 2 || t.isOccupied === 3);
});

const executeMerge = async () => {
  if (!mergeData.value.fromTable || !mergeData.value.toTable) {
    alert('Vui lòng chọn đầy đủ bàn nguồn và bàn đích!');
    return;
  }
  
  const fromT = tablesList.value.find(t => t.id === mergeData.value.fromTable);
  const toT = tablesList.value.find(t => t.id === mergeData.value.toTable);
  
  if (!confirm(`Bạn chắc chắn muốn ghép/gộp ${fromT.name} vào ${toT.name}?`)) return;
  
  try {
    const token = localStorage.getItem('token');
    
    if (mergeData.value.type === 'ORDER') {
      const res = await axios.post('http://localhost:8080/api/orders/merge-tables', {
        fromTable: fromT.name,
        toTable: toT.name
      }, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      alert(res.data.message || 'Gộp bàn thành công!');
    } else {
      const res = await axios.put(`http://localhost:8080/api/tables/${fromT.id}/link/${toT.id}`, {}, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      alert(res.data || 'Ghép bàn vật lý thành công!');
    }
    
    showMergeModal.value = false;
    mergeData.value = { type: 'PHYSICAL', fromTable: '', toTable: '' };
    fetchTables();
  } catch (err) {
    alert(err.response?.data || 'Lỗi khi thao tác. Vui lòng kiểm tra lại!');
  }
};

const unlinkTable = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn tách bàn này ra không?')) return;
  try {
    const token = localStorage.getItem('token');
    const res = await axios.put(`http://localhost:8080/api/tables/${id}/unlink`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    alert(res.data || 'Tách bàn thành công!');
    fetchTables();
  } catch (err) {
    alert('Lỗi khi tách bàn!');
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
const qrValue = computed(() => {
  return `http://localhost:3000/dine-in?table=${encodeURIComponent(qrTable.value)}`;
});

const openQrModal = (tableName) => {
  qrTable.value = tableName;
  showQrModal.value = true;
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
    alert('Lỗi tạo ảnh QR! Vui lòng thử lại.');
  }
};

const fetchTables = async () => {
  const token = localStorage.getItem('token');
  try {
    const res = await axios.get('http://localhost:8080/api/tables', {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
    tablesList.value = res.data;
  } catch (error) { console.error('Lỗi lấy danh sách bàn', error); }
};

const handleAddTable = async () => {
  if (!newTable.value.name) return;
  const token = localStorage.getItem('token');
  try {
    await axios.post('http://localhost:8080/api/tables', newTable.value, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    newTable.value.name = '';
    fetchTables();
  } catch (error) { alert('Lỗi thêm bàn!'); }
};

const updateStatus = async (tableId, newStatus) => {
  if (newStatus == 0 && !confirm('Dọn bàn sẽ ĐÓNG GÓI tất cả đơn hàng tại bàn này. Xác nhận?')) {
    fetchTables(); return;
  }
  const token = localStorage.getItem('token');
  try {
    await axios.put(`http://localhost:8080/api/tables/${tableId}/status?status=${newStatus}`, {}, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    fetchTables();
  } catch (error) { fetchTables(); }
};

// Heatmap Logic
const showHeatmap = ref(false);
const tableHeat = ref({});

const toggleHeatmap = async () => {
  showHeatmap.value = !showHeatmap.value;
  if (showHeatmap.value) {
    try {
      const res = await axios.get('http://localhost:8080/api/orders/history', {
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      const orders = res.data;
      
      let heatCount = {};
      tablesList.value.forEach(t => heatCount[t.name] = 0);
      
      orders.forEach(o => {
        if (!o.address) return;
        tablesList.value.forEach(t => {
          if (o.address.includes(t.name)) {
            heatCount[t.name]++;
          }
        });
      });
      tableHeat.value = heatCount;
    } catch (e) {
      console.error(e);
      alert('Không thể tải dữ liệu để phân tích bản đồ nhiệt.');
      showHeatmap.value = false;
    }
  }
};

const getHeatLevel = (tName) => {
  if (!showHeatmap.value) return '';
  const count = tableHeat.value[tName] || 0;
  if (count >= 10) return 'heat-high';
  if (count >= 5) return 'heat-medium';
  if (count > 0) return 'heat-low';
  return 'heat-none';
};

const deleteTable = async (id) => {
  if (!confirm('Xóa bàn này khỏi hệ thống?')) return;
  try {
    await axios.delete(`http://localhost:8080/api/tables/${id}`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    });
    fetchTables();
  } catch (error) { alert('Không thể xóa bàn đang có dữ liệu hóa đơn!'); }
};

const groupedTables = computed(() => {
  return tablesList.value.reduce((groups, table) => {
    if (!groups[table.floor]) groups[table.floor] = [];
    groups[table.floor].push(table);
    return groups;
  }, {});
});

onMounted(fetchTables);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }

.page-header { margin-bottom: 32px; }
.page-title { font-size: 2rem; font-weight: 900; color: var(--text-heading); margin: 0 0 6px 0; }
.page-subtitle { color: var(--text-muted); font-size: 0.95rem; margin: 0; }

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

.floor-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  padding: 28px;
  box-shadow: var(--shadow-md);
}

/* Legend */
.legend-box {
  display: flex; gap: 12px;
  margin-bottom: 28px; padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}
.badge { padding: 7px 16px; border-radius: 20px; font-size: 0.82rem; font-weight: 700; }
.badge-empty { background: rgba(0,212,170,0.1); color: var(--primary); }
.badge-reserved { background: rgba(241,196,15,0.1); color: #f1c40f; }
.badge-occupied { background: rgba(231,76,60,0.1); color: #e74c3c; }
.badge-cleaning { background: rgba(155,89,182,0.1); color: #9b59b6; }

.floor-title {
  font-size: 1rem; font-weight: 700; color: var(--primary);
  margin: 24px 0 16px 0; letter-spacing: 1px; text-transform: uppercase;
}
.table-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 16px; }

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
  border-color: rgba(0,212,170,0.3);
  background: linear-gradient(135deg, var(--bg-card2), rgba(0,212,170,0.04));
}
.empty-bg .table-status-dot { background: var(--primary); box-shadow: 0 0 8px rgba(0,212,170,0.6); }

.reserved-bg {
  border-color: rgba(241,196,15,0.3);
  background: linear-gradient(135deg, var(--bg-card2), rgba(241,196,15,0.04));
}
.reserved-bg .table-status-dot { background: #f1c40f; box-shadow: 0 0 8px rgba(241,196,15,0.6); }

.occupied-bg {
  border-color: rgba(231,76,60,0.3);
  background: linear-gradient(135deg, var(--bg-card2), rgba(231,76,60,0.04));
}
.occupied-bg .table-status-dot { background: #e74c3c; box-shadow: 0 0 8px rgba(231,76,60,0.6); }

.cleaning-bg {
  border-color: rgba(155,89,182,0.3);
  background: linear-gradient(135deg, var(--bg-card2), rgba(155,89,182,0.04));
}
.cleaning-bg .table-status-dot { background: #9b59b6; box-shadow: 0 0 8px rgba(155,89,182,0.6); }

.linked-bg {
  border-color: rgba(52,152,219,0.3);
  background: linear-gradient(135deg, var(--bg-card2), rgba(52,152,219,0.04));
}
.linked-bg .table-status-dot { background: #2980b9; box-shadow: 0 0 8px rgba(52,152,219,0.6); }

.btn-del {
  position: absolute; top: 8px; left: 8px;
  background: rgba(0,0,0,0.3); border: none;
  color: var(--text-muted); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.7rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-del:hover { background: rgba(231,76,60,0.4); color: #e74c3c; }

.btn-unlink {
  position: absolute; top: 8px; left: 8px;
  background: rgba(41, 128, 185, 0.2); border: none;
  color: #2980b9; border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.9rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-unlink:hover { background: rgba(41, 128, 185, 0.4); transform: scale(1.1); }

.btn-qr {
  position: absolute; top: 8px; right: 8px;
  background: rgba(46, 204, 113, 0.2); border: none;
  color: #2ecc71; border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.9rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-qr:hover { background: rgba(46, 204, 113, 0.4); transform: scale(1.1); }

.view-tag {
  position: absolute; top: 8px; right: 38px;
  background: rgba(142,68,173,0.25); color: #9b59b6;
  font-size: 0.68rem; padding: 2px 7px;
  border-radius: 10px; font-weight: 700;
  border: 1px solid rgba(142,68,173,0.3);
}

.capacity-tag {
  position: absolute; top: 8px; left: 36px;
  background: rgba(52,152,219,0.25); color: #2980b9;
  font-size: 0.68rem; padding: 2px 7px;
  border-radius: 10px; font-weight: 700;
  border: 1px solid rgba(52,152,219,0.3);
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
.qr-wrapper { background: #fff; padding: 20px; border-radius: 12px; display: inline-block; margin-bottom: 15px; border: 4px solid var(--primary); }
.qr-hint { color: var(--text-muted); font-size: 0.85rem; font-style: italic; line-height: 1.5;}
.btn-cancel { background: transparent; border: 1px solid var(--border-light); color: var(--text-muted); padding: 10px; border-radius: var(--radius-md); font-weight: 600; cursor: pointer; transition: 0.3s;}
.btn-cancel:hover { background: rgba(255,255,255,0.05); }

/* Heatmap Styles */
.btn-heatmap {
  background: transparent; border: 1px solid #e74c3c; color: #e74c3c; padding: 8px 20px; border-radius: 20px; font-weight: bold; cursor: pointer; transition: 0.3s; font-size: 0.9rem;
}
.btn-heatmap:hover { background: rgba(231,76,60,0.1); }
.btn-heatmap.active {
  background: #e74c3c; color: #fff; box-shadow: 0 0 15px rgba(231,76,60,0.6);
}

.table-box { overflow: hidden; }

.heatmap-overlay {
  position: absolute; top: 0; left: 0; right: 0; bottom: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 1.8rem; font-weight: 900; color: #fff; text-shadow: 0 2px 10px rgba(0,0,0,0.8);
  z-index: 10; opacity: 0.85; pointer-events: none; transition: 0.5s;
}

.heat-high { background: linear-gradient(135deg, rgba(231,76,60,0.9), rgba(192,57,43,0.9)); }
.heat-medium { background: linear-gradient(135deg, rgba(243,156,18,0.9), rgba(211,84,0,0.9)); }
.heat-low { background: linear-gradient(135deg, rgba(241,196,15,0.8), rgba(243,156,18,0.8)); }
.heat-none { background: rgba(149,165,166,0.8); color: #ecf0f1; font-size: 1.2rem; }

/* Realistic View Styles */
.realistic-hall {
  display: flex; flex-wrap: wrap; justify-content: center; gap: 30px;
  background: url('https://www.transparenttextures.com/patterns/wood-pattern.png'), #f8f9fa;
  padding: 40px; border-radius: 12px; border: 8px solid #bdc3c7;
  box-shadow: inset 0 0 20px rgba(0,0,0,0.1);
}
.realistic-vip {
  display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
  background: #34495e; padding: 20px; border-radius: 8px;
}
.realistic-vip .table-box {
  background: #ecf0f1; border: 4px solid #f1c40f; border-radius: 0;
  position: relative; padding: 30px 10px;
}
.realistic-vip .table-box::before {
  content: "Cửa vào"; position: absolute; bottom: -4px; left: 50%; transform: translateX(-50%);
  background: #34495e; color: #fff; padding: 2px 10px; font-size: 0.6rem;
}
.realistic-rooftop {
  display: flex; flex-wrap: wrap; justify-content: space-around; gap: 40px;
  background: #a9dfbf; padding: 50px 20px; border-radius: 50px;
  border: 4px dashed #27ae60; position: relative;
}
.realistic-rooftop::after {
  content: "🌴 Cây xanh & View Sông 🌊"; position: absolute; top: 10px; left: 50%; transform: translateX(-50%);
  color: #2c3e50; font-weight: bold; font-size: 1.2rem; opacity: 0.4;
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
