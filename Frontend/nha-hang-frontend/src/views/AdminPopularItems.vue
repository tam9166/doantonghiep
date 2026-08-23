<template>
  <AdminLayout>
  <div class="admin-wrapper page-3d-enter">
    <main class="admin-content">
      <div class="page-header parallax-header">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h1 class="page-title text-3d">🔥 Món Ăn & Nguyên Liệu Phổ Biến</h1>
            <p class="page-subtitle">Danh sách món ăn bán chạy và nguyên liệu tiêu thụ nhiều nhất</p>
          </div>
          <div class="filter-group">
            <select v-model="period" class="g-form-control" @change="fetchData" style="width: 200px; border-color: var(--primary); color: var(--primary); font-weight: 700;">
              <option value="week">7 Ngày Qua</option>
              <option value="month">Tháng Này</option>
              <option value="year">Năm Nay</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs-header" style="margin-bottom: 24px;">
        <button @click="activeTab = 'products'" :class="['tab-btn', { active: activeTab === 'products' }]">
          🍔 Top Món Ăn Bán Chạy
        </button>
        <button @click="activeTab = 'ingredients'" :class="['tab-btn', { active: activeTab === 'ingredients' }]">
          🧅 Nguyên Liệu Tiêu Thụ Nhiều
        </button>
      </div>

      <!-- Tab 1: Top Products -->
      <div v-if="activeTab === 'products'" class="tab-content">
        <div class="leaderboard-grid">
          <div v-for="(item, index) in topProducts" :key="item.name" 
               class="leader-card depth-card neon-glow tilt-card"
               :class="{ 'top-1': index === 0, 'top-2': index === 1, 'top-3': index === 2 }">
            <div class="leader-rank">
              <span v-if="index === 0" class="rank-crown">👑</span>
              <span v-else-if="index === 1" class="rank-medal">🥈</span>
              <span v-else-if="index === 2" class="rank-medal">🥉</span>
              <span v-else class="rank-number">#{{ index + 1 }}</span>
            </div>
            <div class="leader-img-wrap">
              <img v-if="item.image" :src="item.image" :alt="item.name" class="leader-img" />
              <div v-else class="leader-img-placeholder">🍽️</div>
            </div>
            <div class="leader-info tilt-content">
              <h3 class="leader-name">{{ item.name }}</h3>
              <div class="leader-stats-row">
                <div class="ls-item">
                  <span class="ls-value" style="color: var(--primary);">{{ item.totalQuantity }}</span>
                  <span class="ls-label">Đã bán</span>
                </div>
                <div class="ls-item">
                  <span class="ls-value" style="color: var(--color-tertiary);">{{ formatMoney(item.totalRevenue) }}</span>
                  <span class="ls-label">Doanh thu</span>
                </div>
                <div class="ls-item">
                  <span class="ls-value" style="color: var(--secondary);">{{ item.orderCount }}</span>
                  <span class="ls-label">Lần gọi</span>
                </div>
              </div>
              <div class="leader-bar">
                <div class="leader-bar-fill" :style="{ width: getBarWidth(item.totalQuantity, topProducts) }"></div>
              </div>
            </div>
          </div>
        </div>
        <div v-if="topProducts.length === 0" class="empty-state depth-card" style="text-align: center; padding: 60px;">
          <div style="font-size: 3rem;">🍽️</div>
          <h3>Chưa có dữ liệu bán hàng</h3>
          <p style="color: var(--text-muted);">Dữ liệu sẽ được tổng hợp từ các đơn hàng hoàn thành.</p>
        </div>
      </div>

      <!-- Tab 2: Top Ingredients -->
      <div v-if="activeTab === 'ingredients'" class="tab-content">
        <div class="ingredient-table-wrap depth-card" style="padding: 20px; border-radius: 14px;">
          <table class="g-table">
            <thead>
              <tr>
                <th style="width: 60px; text-align: center;">TOP</th>
                <th>Nguyên Liệu</th>
                <th>Đã Tiêu Thụ</th>
                <th>Tồn Kho Hiện Tại</th>
                <th>Định Mức Tối Thiểu</th>
                <th style="width: 160px;">Mức Tiêu Thụ</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in topIngredients" :key="item.name" class="hover-row">
                <td style="text-align: center;">
                  <span :class="['rank-badge-sm', `rank-${index + 1}`]">{{ index + 1 }}</span>
                </td>
                <td>
                  <div style="display: flex; align-items: center; gap: 10px;">
                    <img v-if="item.image" :src="item.image" style="width: 32px; height: 32px; border-radius: 8px; object-fit: cover;" />
                    <span v-else style="font-size: 1.2rem;">🧅</span>
                    <strong>{{ item.name }}</strong>
                  </div>
                </td>
                <td style="color: var(--primary); font-weight: 800;">{{ item.totalConsumed?.toFixed(2) }} {{ item.unit }}</td>
                <td>
                  <span :style="{ color: item.currentStock <= item.minStock ? 'var(--primary)' : 'var(--success)', fontWeight: 'bold' }">
                    {{ item.currentStock?.toFixed(2) }} {{ item.unit }}
                  </span>
                </td>
                <td style="color: var(--text-muted);">{{ item.minStock }} {{ item.unit }}</td>
                <td>
                  <div class="consume-bar">
                    <div class="consume-fill" :style="{ width: getBarWidth(item.totalConsumed, topIngredients, 'totalConsumed') }"></div>
                  </div>
                </td>
              </tr>
              <tr v-if="topIngredients.length === 0">
                <td colspan="6" style="text-align: center; padding: 40px; color: var(--text-muted);">Chưa có dữ liệu tiêu thụ nguyên liệu.</td>
              </tr>
            </tbody>
          </table>
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

const activeTab = ref('products');
const period = ref('week');
const topProducts = ref([]);
const topIngredients = ref([]);

const getToken = () => sessionStorage.getItem('staff_token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

const fetchData = async () => {
  try {
    const [prodRes, ingRes] = await Promise.all([
      api.get(`/api/admin/popular-items/products?period=${period.value}`, configHeader()),
      api.get(`/api/admin/popular-items/ingredients?period=${period.value}`, configHeader())
    ]);
    topProducts.value = prodRes.data;
    topIngredients.value = ingRes.data;
  } catch (err) { console.error('Lỗi lấy dữ liệu', err); }
};

const formatMoney = (val) => {
  if (!val) return '0đ';
  if (val >= 1000000) return (val / 1000000).toFixed(1) + 'Tr';
  if (val >= 1000) return (val / 1000).toFixed(0) + 'K';
  return val.toLocaleString() + 'đ';
};

const getBarWidth = (value, list, key = 'totalQuantity') => {
  if (!list.length) return '0%';
  const max = Math.max(...list.map(i => i[key] || 0));
  return max > 0 ? ((value / max) * 100) + '%' : '0%';
};

onMounted(fetchData);
</script>

<style scoped>
@import '@/assets/admin-3d.css';

.admin-content { max-width: 1400px; margin: 0 auto; padding: 28px 24px; }
.page-header { margin-bottom: 28px; padding: 20px 0; }
.page-title { margin: 0; font-size: 1.8rem; font-weight: 900; }
.page-subtitle { margin: 6px 0 0; color: var(--text-muted); font-size: 0.95rem; }

.tabs-header { display: flex; gap: 8px; border-bottom: 2px solid rgba(255,255,255,0.05); padding-bottom: 0; }
.tab-btn { background: transparent; border: none; color: var(--text-muted); padding: 12px 20px; font-size: 0.92rem; font-weight: 700; cursor: pointer; border-bottom: 3px solid transparent; transition: all 0.2s; font-family: inherit; }
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { color: var(--primary); border-bottom-color: var(--primary); }

/* Leaderboard Grid */
.leaderboard-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.leader-card { padding: 20px; border-radius: 16px; position: relative; display: flex; gap: 16px; align-items: center; }
.leader-card.top-1 { border-left: 4px solid var(--color-tertiary); box-shadow: 0 4px 20px color-mix(in srgb, var(--color-tertiary) 15%, transparent); }
.leader-card.top-2 { border-left: 4px solid var(--color-outline); }
.leader-card.top-3 { border-left: 4px solid var(--color-tertiary); }

.leader-rank { position: absolute; top: 10px; right: 14px; }
.rank-crown { font-size: 1.5rem; filter: drop-shadow(0 0 8px color-mix(in srgb, var(--color-tertiary) 60%, transparent)); }
.rank-medal { font-size: 1.3rem; }
.rank-number { font-size: 0.85rem; font-weight: 900; color: var(--text-muted); background: rgba(255,255,255,0.05); padding: 3px 8px; border-radius: 6px; }

.leader-img-wrap { flex-shrink: 0; }
.leader-img { width: 72px; height: 72px; border-radius: 14px; object-fit: cover; border: 2px solid rgba(255,255,255,0.08); }
.leader-img-placeholder { width: 72px; height: 72px; border-radius: 14px; background: color-mix(in srgb, var(--secondary) 10%, transparent); display: flex; align-items: center; justify-content: center; font-size: 2rem; }

.leader-info { flex: 1; }
.leader-name { margin: 0 0 8px; font-size: 1.05rem; font-weight: 800; color: var(--text-heading); }
.leader-stats-row { display: flex; gap: 16px; margin-bottom: 10px; }
.ls-item { display: flex; flex-direction: column; }
.ls-value { font-size: 1.1rem; font-weight: 900; }
.ls-label { font-size: 0.68rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.3px; }

.leader-bar { height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden; }
.leader-bar-fill { height: 100%; background: linear-gradient(90deg, var(--secondary), var(--secondary)); border-radius: 3px; transition: width 1s ease-out; }

/* Ingredient Table */
.hover-row { transition: background 0.2s; }
.hover-row:hover { background: color-mix(in srgb, var(--secondary) 4%, transparent); }

.rank-badge-sm { display: inline-flex; align-items: center; justify-content: center; width: 26px; height: 26px; border-radius: 50%; font-weight: 900; font-size: 0.75rem; background: rgba(255,255,255,0.05); color: var(--text-muted); }
.rank-1 { background: var(--color-tertiary); color: var(--text-primary); box-shadow: 0 0 8px color-mix(in srgb, var(--color-tertiary) 50%, transparent); }
.rank-2 { background: var(--color-outline); color: var(--text-primary); }
.rank-3 { background: var(--color-tertiary); color: #FFFFFF; }

.consume-bar { height: 6px; background: rgba(255,255,255,0.05); border-radius: 3px; overflow: hidden; }
.consume-fill { height: 100%; background: linear-gradient(90deg, var(--primary), var(--color-tertiary)); border-radius: 3px; transition: width 1s ease-out; }

@media (max-width: 768px) {
  .leaderboard-grid { grid-template-columns: 1fr; }
}
</style>
