<template>
  <AdminLayout>
  <div class="admin-wrapper page-3d-enter">
    <main class="admin-content">
      <div class="page-header parallax-header">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h1 class="page-title text-3d">🛒 Đề Xuất Mua Hàng Tự Động</h1>
            <p class="page-subtitle">Hệ thống phân tích tốc độ tiêu thụ và đề xuất nhập kho thông minh</p>
          </div>
          <div style="display: flex; gap: 10px;">
            <button @click="fetchSuggestions" class="g-btn-primary btn-3d">🔄 Làm Mới</button>
            <button @click="analyzeWithAI" class="btn-ai btn-3d">🤖 AI Phân Tích Sâu</button>
          </div>
        </div>
      </div>

      <!-- Summary Cards -->
      <div class="summary-cards">
        <div class="summary-card depth-card float-card card-total">
          <div class="sc-icon">📦</div>
          <div class="sc-info">
            <span class="sc-value">{{ summary.totalItems }}</span>
            <span class="sc-label">Cần Nhập Kho</span>
          </div>
        </div>
        <div class="summary-card depth-card float-card card-critical">
          <div class="sc-icon">🔴</div>
          <div class="sc-info">
            <span class="sc-value">{{ summary.criticalCount }}</span>
            <span class="sc-label">Hết Hàng</span>
          </div>
        </div>
        <div class="summary-card depth-card float-card card-warning">
          <div class="sc-icon">🟡</div>
          <div class="sc-info">
            <span class="sc-value">{{ summary.warningCount }}</span>
            <span class="sc-label">Sắp Hết</span>
          </div>
        </div>
        <div class="summary-card depth-card float-card card-cost">
          <div class="sc-icon">💰</div>
          <div class="sc-info">
            <span class="sc-value">{{ formatMoney(summary.totalEstimatedCost) }}</span>
            <span class="sc-label">Chi Phí Ước Tính</span>
          </div>
        </div>
      </div>

      <!-- Suggestions Table -->
      <div class="suggestions-wrap depth-card" style="padding: 20px; border-radius: 16px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
          <h3 style="margin: 0; font-size: 1.1rem; color: var(--text-heading);">📋 Danh Sách Đề Xuất Nhập Kho</h3>
          <button v-if="suggestions.length > 0" @click="approveAll" class="g-btn-primary" style="font-size: 0.85rem;">
            ✅ Duyệt Tất Cả ({{ suggestions.length }})
          </button>
        </div>

        <table class="g-table">
          <thead>
            <tr>
              <th>Nguyên Liệu</th>
              <th>Tồn Hiện Tại</th>
              <th>Định Mức</th>
              <th>Tiêu Thụ/Ngày</th>
              <th>Còn Dùng Được</th>
              <th>Đề Xuất Mua</th>
              <th>Chi Phí Ước Tính</th>
              <th>Mức Độ</th>
              <th>Thao Tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in suggestions" :key="item.ingredientId" class="suggestion-row">
              <td>
                <div style="display: flex; align-items: center; gap: 10px;">
                  <img v-if="item.image" :src="item.image" style="width: 36px; height: 36px; border-radius: 8px; object-fit: cover;" />
                  <span v-else style="font-size: 1.3rem;">🧅</span>
                  <strong>{{ item.name }}</strong>
                </div>
              </td>
              <td :style="{ color: item.currentStock <= 0 ? '#B23B2E' : item.currentStock <= item.minStock ? '#B98229' : '#2F8F5B', fontWeight: 'bold' }">
                {{ item.currentStock }} {{ item.unit }}
              </td>
              <td style="color: var(--text-muted);">{{ item.minStock }} {{ item.unit }}</td>
              <td style="color: var(--primary); font-weight: 700;">{{ item.dailyConsumption }} {{ item.unit }}</td>
              <td>
                <span :style="{ color: item.daysLeft <= 1 ? '#B23B2E' : item.daysLeft <= 3 ? '#B98229' : '#2F8F5B', fontWeight: 'bold' }">
                  {{ item.daysLeft >= 999 ? '∞' : item.daysLeft + ' ngày' }}
                </span>
              </td>
              <td style="font-weight: 900; color: var(--primary); font-size: 1.05rem;">
                {{ item.suggestedAmount }} {{ item.unit }}
              </td>
              <td style="color: #B23B2E; font-weight: bold;">{{ formatMoney(item.estimatedCost) }}</td>
              <td>
                <span class="urgency-badge" :class="'urgency-' + item.urgency">
                  {{ item.urgencyLabel }}
                </span>
              </td>
              <td>
                <button @click="approveSuggestion(item)" class="g-btn-primary btn-3d" style="font-size: 0.8rem; padding: 6px 12px;" :disabled="item.approved">
                  {{ item.approved ? '✅ Đã duyệt' : '🛒 Duyệt' }}
                </button>
              </td>
            </tr>
            <tr v-if="suggestions.length === 0">
              <td colspan="9" class="empty-td">
                <div style="text-align: center; padding: 50px;">
                  <div style="font-size: 3rem; margin-bottom: 10px;">✅</div>
                  <h3 style="color: #2F8F5B;">Kho hàng đầy đủ!</h3>
                  <p style="color: var(--text-muted);">Tất cả nguyên liệu đang ở mức an toàn. Không cần nhập thêm.</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- AI Modal -->
      <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
        <div class="ai-modal depth-card glass-pro" style="max-width: 600px; width: 90%; padding: 24px; border-radius: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2 style="margin: 0;">🤖 AI Phân Tích Kho Hàng</h2>
            <button @click="showAiModal = false" style="background: none; border: none; font-size: 1.5rem; color: var(--text-muted); cursor: pointer;">✖</button>
          </div>
          <div v-if="aiLoading" style="text-align: center; padding: 40px; color: var(--primary);">
            <div class="spinner"></div>
            <p style="font-weight: bold; margin-top: 16px;">AI đang phân tích dữ liệu tồn kho...</p>
          </div>
          <div v-else style="padding: 16px; border-left: 4px solid var(--primary); background: rgba(90, 110, 69, 0.05); border-radius: 0 8px 8px 0; white-space: pre-line; line-height: 1.7; font-size: 0.95rem;">
            {{ aiResponse }}
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

const suggestions = ref([]);
const summary = ref({ totalItems: 0, criticalCount: 0, warningCount: 0, totalEstimatedCost: 0 });
const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

const fetchSuggestions = async () => {
  try {
    const res = await api.get('/api/admin/purchase-suggestions', configHeader());
    suggestions.value = (res.data.suggestions || []).map(s => ({ ...s, approved: false }));
    summary.value = {
      totalItems: res.data.totalItems || 0,
      criticalCount: res.data.criticalCount || 0,
      warningCount: res.data.warningCount || 0,
      totalEstimatedCost: res.data.totalEstimatedCost || 0
    };
  } catch (err) { console.error('Lỗi lấy đề xuất', err); }
};

const approveSuggestion = async (item) => {
  if (item.approved) return;
  try {
    await api.post(
      `/api/admin/purchase-suggestions/approve/${item.ingredientId}?quantity=${item.suggestedAmount}`,
      {}, configHeader()
    );
    item.approved = true;
    alert(`✅ Đã duyệt nhập kho: ${item.suggestedAmount} ${item.unit} ${item.name}`);
  } catch (err) { alert('Lỗi duyệt đề xuất: ' + (err.response?.data || err.message)); }
};

const approveAll = async () => {
  if (!confirm(`Xác nhận duyệt tất cả ${suggestions.value.length} đề xuất?`)) return;
  for (const item of suggestions.value) {
    if (!item.approved) {
      await approveSuggestion(item);
    }
  }
};

const formatMoney = (val) => {
  if (!val) return '0đ';
  if (val >= 1000000) return (val / 1000000).toFixed(1) + ' Triệu';
  if (val >= 1000) return Math.round(val / 1000) + 'K';
  return Math.round(val).toLocaleString() + 'đ';
};

const analyzeWithAI = async () => {
  showAiModal.value = true;
  aiLoading.value = true;
  aiResponse.value = '';

  const dataForAI = suggestions.value.map(s => ({
    name: s.name,
    currentStock: s.currentStock,
    minStock: s.minStock,
    dailyConsumption: s.dailyConsumption,
    daysLeft: s.daysLeft,
    suggestedAmount: s.suggestedAmount,
    urgency: s.urgency
  }));

  try {
    const res = await api.post('/api/admin/ai/inventory', {
      message: JSON.stringify({
        type: 'PURCHASE_SUGGESTION',
        total_items_need_restock: summary.value.totalItems,
        critical_count: summary.value.criticalCount,
        estimated_cost: summary.value.totalEstimatedCost,
        items: dataForAI
      })
    });
    aiResponse.value = res.data.reply;
  } catch (err) {
    aiResponse.value = 'Xin lỗi, chức năng AI đang tạm thời gián đoạn!';
  } finally {
    aiLoading.value = false;
  }
};

onMounted(fetchSuggestions);
</script>

<style scoped>
@import '@/assets/admin-3d.css';

.admin-content { max-width: 1400px; margin: 0 auto; padding: 28px 24px; }
.page-header { margin-bottom: 28px; padding: 20px 0; }
.page-title { margin: 0; font-size: 1.8rem; font-weight: 900; }
.page-subtitle { margin: 6px 0 0; color: var(--text-muted); font-size: 0.95rem; }

.summary-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.summary-card { display: flex; align-items: center; gap: 14px; padding: 20px; border-radius: 14px; }
.sc-icon { font-size: 1.8rem; }
.sc-info { display: flex; flex-direction: column; }
.sc-value { font-size: 1.5rem; font-weight: 900; }
.sc-label { font-size: 0.75rem; color: var(--text-muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }

.card-total { border-left: 3px solid var(--primary); }
.card-total .sc-value { color: var(--primary); }
.card-critical { border-left: 3px solid #B23B2E; }
.card-critical .sc-value { color: #B23B2E; }
.card-warning { border-left: 3px solid #B98229; }
.card-warning .sc-value { color: #B98229; }
.card-cost { border-left: 3px solid #5A6E45; }
.card-cost .sc-value { color: #5A6E45; font-size: 1.2rem; }

.suggestion-row { transition: background 0.2s; }
.suggestion-row:hover { background: rgba(90, 110, 69, 0.04); }

.urgency-badge { padding: 4px 12px; border-radius: 100px; font-size: 0.75rem; font-weight: 700; display: inline-block; }

.btn-ai { background: linear-gradient(135deg, #C08A2E, #8A641F); color: #FFFFFF; border: none; padding: 10px 20px; border-radius: 10px; font-weight: bold; cursor: pointer; font-family: inherit; }
.btn-ai:hover { filter: brightness(1.1); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(90, 110, 69, 0.2); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto; }
@keyframes spin { to { transform: rotate(360deg); } }

@media (max-width: 992px) {
  .summary-cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 640px) {
  .summary-cards { grid-template-columns: 1fr; }
}
</style>
