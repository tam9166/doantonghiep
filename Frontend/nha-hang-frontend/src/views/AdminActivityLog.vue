<template>
  <AdminLayout>
  <div class="admin-wrapper page-3d-enter">
    <main class="admin-content">
      <div class="page-header parallax-header">
        <h1 class="page-title text-3d">📋 Nhật Ký Thao Tác</h1>
        <p class="page-subtitle">Theo dõi mọi hành động của nhân viên trong hệ thống</p>
      </div>

      <!-- Stats Cards -->
      <div class="stats-row">
        <div class="stat-card depth-card float-card">
          <div class="stat-icon">📝</div>
          <div class="stat-info">
            <span class="stat-value neon-badge">{{ stats.totalLogs }}</span>
            <span class="stat-label">Tổng Thao Tác</span>
          </div>
        </div>
        <div class="stat-card depth-card float-card" style="border-left: 3px solid #2F8F5B;">
          <div class="stat-icon">➕</div>
          <div class="stat-info">
            <span class="stat-value" style="color: #2F8F5B;">{{ stats.creates }}</span>
            <span class="stat-label">Tạo Mới</span>
          </div>
        </div>
        <div class="stat-card depth-card float-card" style="border-left: 3px solid #5A6E45;">
          <div class="stat-icon">✏️</div>
          <div class="stat-info">
            <span class="stat-value" style="color: #5A6E45;">{{ stats.updates }}</span>
            <span class="stat-label">Cập Nhật</span>
          </div>
        </div>
        <div class="stat-card depth-card float-card" style="border-left: 3px solid #B23B2E;">
          <div class="stat-icon">🗑️</div>
          <div class="stat-info">
            <span class="stat-value" style="color: #B23B2E;">{{ stats.deletes }}</span>
            <span class="stat-label">Xóa</span>
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="filter-bar depth-card" style="padding: 16px 20px; margin-bottom: 24px;">
        <div class="filter-group-row">
          <div class="filter-item">
            <label>👤 Người thực hiện</label>
            <select v-model="filterUser" class="g-form-control" @change="fetchLogs">
              <option value="">Tất cả</option>
              <option v-for="u in userList" :key="u" :value="u">{{ u }}</option>
            </select>
          </div>
          <div class="filter-item">
            <label>⚡ Loại hành động</label>
            <select v-model="filterAction" class="g-form-control" @change="fetchLogs">
              <option value="">Tất cả</option>
              <option value="CREATE">Tạo mới</option>
              <option value="UPDATE">Cập nhật</option>
              <option value="DELETE">Xóa</option>
            </select>
          </div>
          <div class="filter-item">
            <label>📂 Đối tượng</label>
            <select v-model="filterEntity" class="g-form-control" @change="fetchLogs">
              <option value="">Tất cả</option>
              <option value="Product">Sản phẩm</option>
              <option value="Ingredient">Nguyên liệu</option>
              <option value="Order">Đơn hàng</option>
              <option value="ImportInvoice">Nhập kho</option>
              <option value="PurchaseSuggestion">Đề xuất mua</option>
            </select>
          </div>
          <button v-if="filterUser || filterAction || filterEntity" @click="resetFilters" class="g-btn-secondary" style="align-self: flex-end; padding: 10px 16px;">
            ✕ Xóa lọc
          </button>
        </div>
      </div>

      <!-- Timeline -->
      <div class="timeline-container">
        <div class="timeline-3d">
          <div v-for="log in logs" :key="log.id" class="timeline-entry depth-card neon-glow" style="margin-bottom: 12px; padding: 16px 20px 16px 60px;">
            <div class="timeline-dot" :style="{ background: getActionColor(log.action) }" style="position: absolute; left: 18px; top: 22px;"></div>
            <div class="log-header">
              <div class="log-user-info">
                <span class="log-avatar" :style="{ background: getAvatarColor(log.username) }">
                  {{ log.username?.charAt(0)?.toUpperCase() }}
                </span>
                <div>
                  <strong class="log-username">{{ log.username }}</strong>
                  <span class="log-time">{{ formatTime(log.timestamp) }}</span>
                </div>
              </div>
              <span class="log-action-badge" :class="'action-' + log.action?.toLowerCase()">
                {{ getActionLabel(log.action) }}
              </span>
            </div>
            <p class="log-description">{{ log.description }}</p>
            <div class="log-meta">
              <span class="log-entity-badge">{{ getEntityLabel(log.entityType) }} #{{ log.entityId }}</span>
              <div v-if="log.oldValue || log.newValue" class="log-diff">
                <span v-if="log.oldValue" class="diff-old">{{ log.oldValue }}</span>
                <span v-if="log.oldValue && log.newValue" class="diff-arrow">→</span>
                <span v-if="log.newValue" class="diff-new">{{ log.newValue }}</span>
              </div>
            </div>
          </div>
          <div v-if="logs.length === 0" class="empty-state depth-card" style="text-align: center; padding: 60px; margin-left: 40px;">
            <div style="font-size: 3rem; margin-bottom: 12px;">📋</div>
            <h3>Chưa có nhật ký thao tác nào</h3>
            <p style="color: var(--text-muted);">Các thao tác CRUD sẽ được ghi lại tự động ở đây.</p>
          </div>
        </div>
      </div>
    </main>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';
import { ref, onMounted } from 'vue';
import api from '@/services/api';

const logs = ref([]);
const stats = ref({ totalLogs: 0, creates: 0, updates: 0, deletes: 0 });
const userList = ref([]);
const filterUser = ref('');
const filterAction = ref('');
const filterEntity = ref('');

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

const fetchLogs = async () => {
  try {
    let url = '/api/admin/activity-logs?';
    if (filterUser.value) url += `username=${filterUser.value}&`;
    if (filterAction.value) url += `action=${filterAction.value}&`;
    if (filterEntity.value) url += `entityType=${filterEntity.value}&`;
    const res = await api.get(url, configHeader());
    logs.value = res.data;
  } catch (err) { console.error('Lỗi lấy nhật ký', err); }
};

const fetchStats = async () => {
  try {
    const res = await api.get('/api/admin/activity-logs/stats', configHeader());
    stats.value = res.data;
    userList.value = res.data.users || [];
  } catch (err) { console.error('Lỗi lấy thống kê', err); }
};

const resetFilters = () => {
  filterUser.value = '';
  filterAction.value = '';
  filterEntity.value = '';
  fetchLogs();
};

const formatTime = (ts) => {
  if (!ts) return '';
  const d = new Date(ts);
  const now = new Date();
  const diff = (now - d) / 1000;
  if (diff < 60) return 'Vừa xong';
  if (diff < 3600) return Math.floor(diff / 60) + ' phút trước';
  if (diff < 86400) return Math.floor(diff / 3600) + ' giờ trước';
  return d.toLocaleString('vi-VN');
};

const getActionColor = (action) => {
  if (action === 'CREATE') return '#2F8F5B';
  if (action === 'UPDATE') return '#5A6E45';
  if (action === 'DELETE') return '#B23B2E';
  return '#33422A';
};

const getAvatarColor = (username) => {
  const colors = ['#33422A', '#5A6E45', '#B23B2E', '#B98229', '#8A641F', '#5A6E45'];
  let hash = 0;
  for (let i = 0; i < (username?.length || 0); i++) hash = username.charCodeAt(i) + ((hash << 5) - hash);
  return colors[Math.abs(hash) % colors.length];
};

const getActionLabel = (action) => {
  const map = { CREATE: '➕ Tạo mới', UPDATE: '✏️ Cập nhật', DELETE: '🗑️ Xóa' };
  return map[action] || action;
};

const getEntityLabel = (entity) => {
  const map = { Product: '🍔 Sản phẩm', Ingredient: '🧅 Nguyên liệu', Order: '📋 Đơn hàng', ImportInvoice: '📦 Nhập kho', PurchaseSuggestion: '🛒 Đề xuất mua' };
  return map[entity] || entity;
};

onMounted(() => {
  fetchLogs();
  fetchStats();
});
</script>

<style scoped>
@import '@/assets/admin-3d.css';

.admin-content { max-width: 1200px; margin: 0 auto; padding: 28px 24px; }
.page-header { margin-bottom: 28px; padding: 20px 0; }
.page-title { margin: 0; font-size: 1.8rem; font-weight: 900; }
.page-subtitle { margin: 6px 0 0; color: var(--text-muted); font-size: 0.95rem; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 14px; padding: 20px; border-radius: 14px; }
.stat-icon { font-size: 1.8rem; }
.stat-info { display: flex; flex-direction: column; }
.stat-value { font-size: 1.6rem; font-weight: 900; color: var(--primary); }
.stat-label { font-size: 0.78rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }

.filter-bar { border-radius: 14px; }
.filter-group-row { display: flex; gap: 16px; align-items: flex-end; flex-wrap: wrap; }
.filter-item { display: flex; flex-direction: column; gap: 6px; min-width: 180px; }
.filter-item label { font-size: 0.78rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }

.timeline-container { position: relative; }
.timeline-entry { position: relative; border-radius: 12px; transition: all 0.3s ease; }
.timeline-entry:hover { border-color: rgba(90, 110, 69, 0.2); }

.log-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.log-user-info { display: flex; align-items: center; gap: 10px; }
.log-avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: #FFFFFF; font-weight: 900; font-size: 0.9rem; flex-shrink: 0; }
.log-username { display: block; font-size: 0.95rem; color: var(--text-heading); }
.log-time { font-size: 0.75rem; color: var(--text-muted); }
.log-action-badge { padding: 4px 12px; border-radius: 100px; font-size: 0.75rem; font-weight: 700; }
.action-create { background: rgba(47, 143, 91, 0.15); color: #2F8F5B; border: 1px solid rgba(47, 143, 91, 0.3); }
.action-update { background: rgba(90, 110, 69, 0.15); color: #5A6E45; border: 1px solid rgba(90, 110, 69, 0.3); }
.action-delete { background: rgba(178, 59, 46, 0.15); color: #B23B2E; border: 1px solid rgba(178, 59, 46, 0.3); }
.log-description { margin: 0 0 8px; color: var(--text-primary); font-size: 0.92rem; line-height: 1.5; }
.log-meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.log-entity-badge { background: rgba(90, 110, 69, 0.1); color: var(--primary); padding: 3px 10px; border-radius: 6px; font-size: 0.75rem; font-weight: 600; }
.log-diff { display: flex; align-items: center; gap: 8px; font-size: 0.8rem; }
.diff-old { background: rgba(178, 59, 46, 0.1); color: #B23B2E; padding: 2px 8px; border-radius: 4px; text-decoration: line-through; }
.diff-arrow { color: var(--text-muted); font-weight: bold; }
.diff-new { background: rgba(47, 143, 91, 0.1); color: #2F8F5B; padding: 2px 8px; border-radius: 4px; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .filter-group-row { flex-direction: column; }
  .filter-item { min-width: 100%; }
}
</style>
