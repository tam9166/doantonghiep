<template>
  <div class="admin-wrapper">
    <header class="g-navbar">
      <div class="g-logo">
        <h2>FPOLY <span>RESTAURANT</span></h2>
        <p>Admin Dashboard</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/admin">Thực Đơn</router-link>
        <router-link to="/admin/categories">Danh Mục</router-link>
        <router-link to="/admin/ingredients">Nguyên Liệu</router-link>
        <router-link to="/admin/tables" class="active">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/vouchers">Khuyến Mãi</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
        <router-link to="/admin/analytics">Thống Kê</router-link>
      </nav>
      <button @click="$router.push('/')" class="g-btn-nav">🏠 Trang Khách</button>
    </header>

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
            </div>
            
            <button @click="toggleHeatmap" class="btn-heatmap" :class="{'active': showHeatmap}">
              🔥 {{ showHeatmap ? 'Tắt Bản Đồ Nhiệt' : 'Bật Bản Đồ Nhiệt' }}
            </button>
          </div>

          <div v-for="(tables, floorName) in groupedTables" :key="floorName" class="floor-section">
            <h2 class="floor-title">📍 {{ floorName }}</h2>
            <div class="table-grid">
              <div
                v-for="t in tables"
                :key="t.id"
                class="table-box"
                :class="{ 'empty-bg': t.isOccupied === 0, 'reserved-bg': t.isOccupied === 1, 'occupied-bg': t.isOccupied === 2, 'cleaning-bg': t.isOccupied === 3 }"
              >
                <button @click="deleteTable(t.id)" class="btn-del" title="Xóa bàn">✖</button>
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
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import QrcodeVue from 'qrcode.vue';

const tablesList = ref([]);
const newTable = ref({ name: '', floor: 'Tầng 2 (Sảnh Tiệc)', capacity: 4, viewType: '' });

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

.btn-del {
  position: absolute; top: 8px; left: 8px;
  background: rgba(0,0,0,0.3); border: none;
  color: var(--text-muted); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.7rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-del:hover { background: rgba(231,76,60,0.4); color: #e74c3c; }

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
</style>
