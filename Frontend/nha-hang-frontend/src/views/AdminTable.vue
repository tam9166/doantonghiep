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
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
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
              <option value="Tầng 2">Tầng 2 (VIP)</option>
              <option value="Tầng 3">Tầng 3 (Gia đình)</option>
              <option value="Sân thượng">Sân Thượng (Rooftop)</option>
            </select>
          </div>
          <div class="checkbox-group">
            <input type="checkbox" v-model="newTable.hasView" id="hasView" />
            <label for="hasView">Bàn có View thành phố</label>
          </div>
          <button @click="handleAddTable" class="g-btn-primary" style="width:100%; margin-top: 16px;">
            THÊM BÀN NGAY
          </button>
        </div>

        <!-- Sơ đồ bàn -->
        <div class="floor-card">
          <div class="legend-box">
            <span class="badge badge-empty">🟢 Bàn Trống</span>
            <span class="badge badge-reserved">🟡 Đã Đặt Trước</span>
            <span class="badge badge-occupied">🔴 Đang Phục Vụ</span>
          </div>

          <div v-for="(tables, floorName) in groupedTables" :key="floorName" class="floor-section">
            <h2 class="floor-title">📍 {{ floorName }}</h2>
            <div class="table-grid">
              <div
                v-for="t in tables"
                :key="t.id"
                class="table-box"
                :class="{ 'empty-bg': t.isOccupied === 0, 'reserved-bg': t.isOccupied === 1, 'occupied-bg': t.isOccupied === 2 }"
              >
                <button @click="deleteTable(t.id)" class="btn-del">✖</button>
                <span v-if="t.hasView" class="view-tag">★ View</span>

                <div class="table-status-dot"></div>
                <h3 class="t-name">{{ t.name }}</h3>
                <div class="t-status-text">{{ t.reservedTime || 'Sẵn sàng phục vụ' }}</div>

                <select :value="t.isOccupied" @change="updateStatus(t.id, $event.target.value)" class="status-dropdown">
                  <option value="0">🟢 Dọn Bàn (Trống)</option>
                  <option value="1">🟡 Giữ Chỗ (Cọc)</option>
                  <option value="2">🔴 Khách Đang Ăn</option>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';

const tablesList = ref([]);
const newTable = ref({ name: '', floor: 'Tầng 2', hasView: false });

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

.btn-del {
  position: absolute; top: 8px; left: 8px;
  background: rgba(0,0,0,0.3); border: none;
  color: var(--text-muted); border-radius: 50%;
  width: 24px; height: 24px; font-size: 0.7rem;
  cursor: pointer; transition: var(--transition);
  display: flex; align-items: center; justify-content: center;
}
.btn-del:hover { background: rgba(231,76,60,0.4); color: #e74c3c; }

.view-tag {
  position: absolute; top: 8px; right: 8px;
  background: rgba(142,68,173,0.25); color: #9b59b6;
  font-size: 0.68rem; padding: 2px 7px;
  border-radius: 10px; font-weight: 700;
  border: 1px solid rgba(142,68,173,0.3);
}

.empty-floor { text-align: center; color: var(--text-muted); padding: 60px; font-style: italic; }
</style>