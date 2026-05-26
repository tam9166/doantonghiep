<template>
  <div class="admin-wrapper luxury-theme">
    <header class="g-navbar">
      <div class="g-logo">
        <h2>FPOLY <span>RESTAURANT</span></h2>
        <p>Premium Analytics Dashboard</p>
      </div>
      <nav class="g-nav-links">
        <router-link to="/admin">Thực Đơn</router-link>
        <router-link to="/admin/categories">Danh Mục</router-link>
        <router-link to="/admin/ingredients">Nguyên Liệu</router-link>
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/vouchers">Khuyến Mãi</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
        <router-link to="/admin/analytics" class="active">Thống Kê</router-link>
      </nav>
      <button @click="$router.push('/')" class="g-btn-nav">🏠 Trang Khách</button>
    </header>

    <main class="admin-content">
      <div class="header-actions">
        <h1 class="page-title">Biểu Đồ & Thống Kê</h1>
        <div class="filter-group">
          <span class="filter-label">⏱️ Phạm vi thống kê:</span>
          <select v-model="timeFilter" class="g-form-control filter-select">
            <option value="week">7 Ngày Qua (Mặc định)</option>
            <option value="month">Tháng Này</option>
            <option value="year">Năm Nay</option>
          </select>
          <button @click="analyzeWithAI" class="btn-ai-analyze">🧠 Phân Tích Bằng AI</button>
        </div>
      </div>

      <!-- ====== FINANCIAL SUMMARY CARDS ====== -->
      <div class="finance-cards">
        <div class="finance-card card-revenue">
          <div class="fc-icon">💰</div>
          <div class="fc-info">
            <span class="fc-label">Đầu Ra (Doanh Thu)</span>
            <span class="fc-value">{{ totalRevenue.toLocaleString() }}đ</span>
          </div>
          <div class="fc-glow"></div>
        </div>
        <div class="finance-card card-cost">
          <div class="fc-icon">📦</div>
          <div class="fc-info">
            <span class="fc-label">Đầu Vào (Giá Vốn)</span>
            <span class="fc-value">{{ totalCost.toLocaleString() }}đ</span>
          </div>
          <div class="fc-glow"></div>
        </div>
        <div class="finance-card" :class="profit >= 0 ? 'card-profit' : 'card-loss'">
          <div class="fc-icon">{{ profit >= 0 ? '📈' : '📉' }}</div>
          <div class="fc-info">
            <span class="fc-label">Lợi Nhuận Ròng</span>
            <span class="fc-value">{{ profit.toLocaleString() }}đ</span>
            <span class="fc-ratio" v-if="totalRevenue > 0">
              Biên lợi nhuận: {{ ((profit / totalRevenue) * 100).toFixed(1) }}%
            </span>
          </div>
          <div class="fc-glow"></div>
        </div>
      </div>

      <!-- Charts Section -->
      <div class="charts-grid">
        <!-- Revenue Line Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">💰 Biểu Đồ Doanh Thu (VNĐ)</h3>
          <div class="chart-container">
            <Line v-if="chartDataReady" :data="revenueChartData" :options="chartOptions" />
            <div v-else class="loading-state">Đang tải dữ liệu...</div>
          </div>
        </div>

        <!-- Revenue vs Cost Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">📊 Thu Chi Theo Thời Gian</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady" :data="revenueCostChartData" :options="chartOptions" />
            <div v-else class="loading-state">Đang tải dữ liệu...</div>
          </div>
        </div>

        <!-- Orders Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">🧾 Số Lượng Hóa Đơn Hoàn Thành</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady" :data="ordersChartData" :options="chartOptions" />
            <div v-else class="loading-state">Đang tải dữ liệu...</div>
          </div>
        </div>

        <!-- Profit Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">📈 Lợi Nhuận Theo Thời Gian</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady" :data="profitChartData" :options="profitChartOptions" />
            <div v-else class="loading-state">Đang tải dữ liệu...</div>
          </div>
        </div>
      </div>

      <!-- Leaderboard Section for AI -->
      <div class="leaderboard-section g-card">
        <div class="leaderboard-header">
          <h3 class="chart-title">⭐ Bảng Xếp Hạng Món Ăn Ưa Chuộng (Top 10)</h3>
          <p class="subtitle">Dữ liệu nguồn để huấn luyện & tích hợp AI gợi ý món ăn sau này</p>
        </div>
        
        <div class="table-responsive">
          <table class="g-table">
            <thead>
              <tr>
                <th style="width: 80px; text-align: center;">TOP</th>
                <th>TÊN MÓN ĂN</th>
                <th style="text-align: center;">ĐÃ BÁN (Đĩa/Phần)</th>
                <th style="text-align: right;">DOANH THU</th>
                <th style="text-align: right;">GIÁ VỐN</th>
                <th style="text-align: right;">LỢI NHUẬN</th>
                <th style="width: 150px; text-align: center;">MỨC ĐỘ HOT</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in topProducts" :key="item.name" class="leader-row">
                <td style="text-align: center;">
                  <span :class="['rank-badge', `rank-${index + 1}`]">{{ index + 1 }}</span>
                </td>
                <td class="product-name">
                  <span class="p-icon" v-if="index === 0">👑</span>
                  <span class="p-icon" v-else-if="index === 1">🥈</span>
                  <span class="p-icon" v-else-if="index === 2">🥉</span>
                  <span class="p-icon" v-else>🍽️</span>
                  {{ item.name }}
                </td>
                <td style="text-align: center; font-weight: bold; color: var(--primary);">
                  {{ item.quantity }}
                </td>
                <td style="text-align: right; color: #f1c40f; font-weight: bold;">
                  {{ item.revenue.toLocaleString() }}đ
                </td>
                <td style="text-align: right; color: #e74c3c; font-weight: bold;">
                  {{ item.cost.toLocaleString() }}đ
                </td>
                <td :style="{ textAlign: 'right', fontWeight: 'bold', color: (item.revenue - item.cost) >= 0 ? '#2ecc71' : '#e74c3c' }">
                  {{ (item.revenue - item.cost).toLocaleString() }}đ
                </td>
                <td style="text-align: center;">
                  <div class="hot-bar">
                    <div class="hot-fill" :style="{ width: (item.quantity / topProducts[0].quantity * 100) + '%' }"></div>
                  </div>
                </td>
              </tr>
              <tr v-if="topProducts.length === 0">
                <td colspan="7" class="empty-row">Chưa có dữ liệu bán hàng.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>

    <!-- AI Modal -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
      <div class="ai-modal">
        <div class="modal-header">
          <h2>🤖 Giám Đốc Kinh Doanh AI</h2>
          <button @click="showAiModal = false" class="btn-close">✖</button>
        </div>
        <div class="modal-body">
          <div v-if="aiLoading" class="ai-loading">
            <div class="spinner"></div>
            <p>AI đang phân tích dữ liệu tài chính...</p>
          </div>
          <div v-else class="ai-result">
            <p>{{ aiResponse }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import axios from 'axios';
import {
  Chart as ChartJS,
  Title,
  Tooltip,
  Legend,
  BarElement,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Filler
} from 'chart.js';
import { Bar, Line } from 'vue-chartjs';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, PointElement, LineElement, Filler);

const orders = ref([]);
const recipes = ref([]);
const ingredients = ref([]);
const timeFilter = ref('week');
const chartDataReady = ref(false);
const topProducts = ref([]);
const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

// Financial totals
const totalRevenue = ref(0);
const totalCost = ref(0);
const profit = computed(() => totalRevenue.value - totalCost.value);

const getToken = () => localStorage.getItem('token');
const configHeader = () => ({ headers: { 'Authorization': `Bearer ${getToken()}` } });

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: '#e0f7f4', font: { family: 'Inter', size: 13 } } }
  },
  scales: {
    x: { 
      ticks: { color: '#a0aabf' }, 
      grid: { color: 'rgba(255,255,255,0.05)' } 
    },
    y: { 
      ticks: { color: '#a0aabf' }, 
      grid: { color: 'rgba(255,255,255,0.05)' },
      beginAtZero: true
    }
  }
};

const profitChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: '#e0f7f4', font: { family: 'Inter', size: 13 } } }
  },
  scales: {
    x: {
      ticks: { color: '#a0aabf' },
      grid: { color: 'rgba(255,255,255,0.05)' }
    },
    y: {
      ticks: { color: '#a0aabf' },
      grid: { color: 'rgba(255,255,255,0.05)' }
    }
  }
};

const revenueChartData = ref({ labels: [], datasets: [] });
const ordersChartData = ref({ labels: [], datasets: [] });
const revenueCostChartData = ref({ labels: [], datasets: [] });
const profitChartData = ref({ labels: [], datasets: [] });

// Build a map: productId -> cost per dish (sum of amountRequired * unitPrice for each ingredient)
const buildCostMap = () => {
  const costMap = {}; // productId -> cost per 1 serving
  recipes.value.forEach(recipe => {
    const productId = recipe.product?.id;
    if (!productId) return;
    const ingUnitPrice = recipe.ingredient?.unitPrice || 0;
    const amountReq = recipe.amountRequired || 0;
    const costPerServing = amountReq * ingUnitPrice;
    if (!costMap[productId]) {
      costMap[productId] = 0;
    }
    costMap[productId] += costPerServing;
  });
  return costMap;
};

const fetchData = async () => {
  try {
    const token = getToken();
    const headers = { 'Authorization': `Bearer ${token}` };

    const [resOrders, resRecipes, resIngredients] = await Promise.all([
      axios.get('http://localhost:8080/api/admin/orders', { headers }),
      axios.get('http://localhost:8080/api/admin/recipes', { headers }),
      axios.get('http://localhost:8080/api/admin/ingredients', { headers })
    ]);

    orders.value = resOrders.data.filter(o => o.status === 4);
    recipes.value = resRecipes.data;
    ingredients.value = resIngredients.data;

    processData();
  } catch (error) {
    console.error('Lỗi lấy dữ liệu thống kê:', error);
  }
};

const processData = () => {
  chartDataReady.value = false;
  const now = new Date();
  const costMap = buildCostMap();
  
  const groupedData = {};
  
  orders.value.forEach(order => {
    if (!order.createDate) return;
    const date = new Date(order.createDate);
    let key = '';

    if (timeFilter.value === 'week') {
      const diff = Math.ceil(Math.abs(now - date) / (1000 * 60 * 60 * 24));
      if (diff > 7) return;
      key = date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
    } else if (timeFilter.value === 'month') {
      if (date.getMonth() !== now.getMonth() || date.getFullYear() !== now.getFullYear()) return;
      key = date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
    } else if (timeFilter.value === 'year') {
      if (date.getFullYear() !== now.getFullYear()) return;
      key = `Tháng ${date.getMonth() + 1}`;
    }

    if (!groupedData[key]) {
      groupedData[key] = { revenue: 0, cost: 0, count: 0 };
    }
    
    groupedData[key].count += 1;
    if (order.orderDetails) {
      order.orderDetails.forEach(detail => {
        const rev = detail.price || 0;
        const qty = detail.quantity || 0;
        const productId = detail.product?.id;
        const costPerServing = productId ? (costMap[productId] || 0) : 0;

        groupedData[key].revenue += rev;
        groupedData[key].cost += costPerServing * qty;
      });
    }
  });

  const labels = Object.keys(groupedData).sort();
  const revData = labels.map(k => groupedData[k].revenue);
  const costData = labels.map(k => groupedData[k].cost);
  const countData = labels.map(k => groupedData[k].count);
  const profitData = labels.map(k => groupedData[k].revenue - groupedData[k].cost);

  // Total financial figures
  totalRevenue.value = revData.reduce((a, b) => a + b, 0);
  totalCost.value = costData.reduce((a, b) => a + b, 0);

  revenueChartData.value = {
    labels,
    datasets: [{
      label: 'Doanh Thu (VNĐ)',
      data: revData,
      borderColor: '#00d4aa',
      backgroundColor: 'rgba(0, 212, 170, 0.2)',
      borderWidth: 3,
      fill: true,
      tension: 0.3,
      pointBackgroundColor: '#00d4aa'
    }]
  };

  ordersChartData.value = {
    labels,
    datasets: [{
      label: 'Số lượng đơn hàng',
      data: countData,
      backgroundColor: '#3498db',
      borderRadius: 6,
      barThickness: 30
    }]
  };

  revenueCostChartData.value = {
    labels,
    datasets: [
      {
        label: 'Doanh Thu (Đầu ra)',
        data: revData,
        backgroundColor: 'rgba(46, 204, 113, 0.7)',
        borderRadius: 6,
        barThickness: 24
      },
      {
        label: 'Giá Vốn (Đầu vào)',
        data: costData,
        backgroundColor: 'rgba(231, 76, 60, 0.7)',
        borderRadius: 6,
        barThickness: 24
      }
    ]
  };

  profitChartData.value = {
    labels,
    datasets: [{
      label: 'Lợi Nhuận (VNĐ)',
      data: profitData,
      backgroundColor: profitData.map(v => v >= 0 ? 'rgba(46, 204, 113, 0.7)' : 'rgba(231, 76, 60, 0.7)'),
      borderRadius: 6,
      barThickness: 30
    }]
  };

  // Top products with cost
  const productMap = {};
  orders.value.forEach(order => {
    if (timeFilter.value === 'week') {
      const d = new Date(order.createDate);
      if (Math.ceil(Math.abs(now - d) / (1000 * 60 * 60 * 24)) > 7) return;
    }

    if (order.orderDetails) {
      order.orderDetails.forEach(detail => {
        const pName = detail.product?.name || 'Món không tên';
        const productId = detail.product?.id;
        const qty = detail.quantity || 0;
        const costPerServing = productId ? (costMap[productId] || 0) : 0;
        
        if (!productMap[pName]) {
          productMap[pName] = { name: pName, quantity: 0, revenue: 0, cost: 0 };
        }
        productMap[pName].quantity += qty;
        productMap[pName].revenue += detail.price || 0;
        productMap[pName].cost += costPerServing * qty;
      });
    }
  });

  topProducts.value = Object.values(productMap)
    .sort((a, b) => b.quantity - a.quantity)
    .slice(0, 10);

  chartDataReady.value = true;
};

const analyzeWithAI = async () => {
  showAiModal.value = true;
  aiLoading.value = true;
  aiResponse.value = '';
  
  const top5 = topProducts.value.slice(0, 5).map(p => 
    `${p.name}: bán ${p.quantity} phần, doanh thu ${p.revenue.toLocaleString()} VND, giá vốn ${p.cost.toLocaleString()} VND, lợi nhuận ${(p.revenue - p.cost).toLocaleString()} VND`
  );

  const dataForAI = {
    doanh_thu_tong: totalRevenue.value,
    gia_von_tong: totalCost.value,
    loi_nhuan_tong: profit.value,
    bien_loi_nhuan: totalRevenue.value > 0 ? ((profit.value / totalRevenue.value) * 100).toFixed(1) + '%' : '0%',
    top_5_mon: top5
  };

  try {
    const res = await axios.post('http://localhost:8080/api/chatbot/chat', {
      type: 'ADMIN_ANALYTICS',
      message: JSON.stringify(dataForAI)
    });
    aiResponse.value = res.data.reply;
  } catch (error) {
    aiResponse.value = "Xin lỗi, chức năng AI đang tạm thời gián đoạn!";
  } finally {
    aiLoading.value = false;
  }
};

watch(timeFilter, processData);

onMounted(fetchData);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }

.header-actions {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 28px;
}
.page-title {
  margin: 0; font-size: 1.8rem; font-weight: 900;
  background: linear-gradient(135deg, #00d4aa, #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.filter-group { display: flex; align-items: center; gap: 12px; }
.filter-label { font-weight: 600; color: var(--text-muted); }
.filter-select { width: 220px; border-color: var(--primary); color: var(--primary); font-weight: 700; background: rgba(0,212,170,0.05); }

/* ====== FINANCIAL SUMMARY CARDS ====== */
.finance-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}
.finance-card {
  position: relative;
  overflow: hidden;
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 24px 28px;
  display: flex;
  align-items: center;
  gap: 18px;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.finance-card:hover {
  transform: translateY(-4px);
}
.fc-icon {
  font-size: 2.5rem;
  width: 64px;
  height: 64px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.fc-info {
  display: flex;
  flex-direction: column;
  z-index: 1;
}
.fc-label {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.fc-value {
  font-size: 1.6rem;
  font-weight: 900;
  line-height: 1.2;
}
.fc-ratio {
  font-size: 0.8rem;
  margin-top: 4px;
  font-weight: 600;
}
.fc-glow {
  position: absolute;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  top: -30px;
  right: -30px;
  filter: blur(40px);
  opacity: 0.4;
  z-index: 0;
}

/* Card Revenue */
.card-revenue { border-color: rgba(46, 204, 113, 0.3); }
.card-revenue .fc-icon { background: rgba(46, 204, 113, 0.15); color: #2ecc71; }
.card-revenue .fc-value { color: #2ecc71; }
.card-revenue .fc-glow { background: #2ecc71; }
.card-revenue:hover { box-shadow: 0 8px 30px rgba(46, 204, 113, 0.2); }

/* Card Cost */
.card-cost { border-color: rgba(231, 76, 60, 0.3); }
.card-cost .fc-icon { background: rgba(231, 76, 60, 0.15); color: #e74c3c; }
.card-cost .fc-value { color: #e74c3c; }
.card-cost .fc-glow { background: #e74c3c; }
.card-cost:hover { box-shadow: 0 8px 30px rgba(231, 76, 60, 0.2); }

/* Card Profit */
.card-profit { border-color: rgba(241, 196, 15, 0.3); }
.card-profit .fc-icon { background: rgba(241, 196, 15, 0.15); color: #f1c40f; }
.card-profit .fc-value { color: #f1c40f; }
.card-profit .fc-ratio { color: #2ecc71; }
.card-profit .fc-glow { background: #f1c40f; }
.card-profit:hover { box-shadow: 0 8px 30px rgba(241, 196, 15, 0.2); }

/* Card Loss */
.card-loss { border-color: rgba(231, 76, 60, 0.5); }
.card-loss .fc-icon { background: rgba(231, 76, 60, 0.2); color: #e74c3c; }
.card-loss .fc-value { color: #e74c3c; }
.card-loss .fc-ratio { color: #e74c3c; }
.card-loss .fc-glow { background: #e74c3c; }
.card-loss:hover { box-shadow: 0 8px 30px rgba(231, 76, 60, 0.3); }

/* Charts */
.charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 30px;
}
.chart-card { padding: 24px; border-top: 4px solid var(--primary); }
.chart-title { margin: 0 0 20px 0; font-size: 1.1rem; color: var(--text-heading); }
.chart-container { height: 320px; position: relative; }
.loading-state {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  color: var(--primary); font-weight: bold; font-style: italic;
}

/* Leaderboard */
.leaderboard-section { padding: 24px; border-left: 4px solid #f1c40f; }
.leaderboard-header { margin-bottom: 20px; }
.subtitle { margin: 4px 0 0 0; font-size: 0.9rem; color: var(--text-muted); font-style: italic; }

.rank-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--bg-nav); font-weight: 900; color: #fff;
}
.rank-1 { background: #f1c40f; color: #111; box-shadow: 0 0 10px rgba(241,196,15,0.6); transform: scale(1.2); }
.rank-2 { background: #bdc3c7; color: #111; }
.rank-3 { background: #cd7f32; color: #fff; }

.leader-row:hover { background: rgba(255,255,255,0.03); }
.product-name { font-size: 1.05rem; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.p-icon { font-size: 1.2rem; }

.hot-bar {
  width: 100%; height: 8px;
  background: var(--bg-nav); border-radius: 4px; overflow: hidden;
}
.hot-fill {
  height: 100%; background: linear-gradient(90deg, #e74c3c, #f1c40f);
  border-radius: 4px; transition: width 1s ease-out;
}
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }

.btn-ai-analyze {
  background: linear-gradient(135deg, #9b59b6, #8e44ad);
  color: white; border: none; padding: 10px 20px; border-radius: 8px;
  font-weight: bold; cursor: pointer; transition: 0.3s;
  box-shadow: 0 4px 15px rgba(155, 89, 182, 0.4);
}
.btn-ai-analyze:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(155, 89, 182, 0.6); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.ai-modal { background: var(--bg-card); width: 550px; max-width: 90%; border-radius: 12px; padding: 20px; border: 1px solid var(--border-light); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.btn-close { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted); }
.ai-loading { text-align: center; padding: 30px; color: var(--primary); font-weight: bold; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(0, 212, 170, 0.2); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-result { padding: 20px; font-size: 1.05rem; line-height: 1.6; color: var(--text-primary); border-left: 4px solid var(--primary); background: rgba(0,212,170,0.05); border-radius: 0 8px 8px 0; white-space: pre-line; }

@media (max-width: 992px) {
  .charts-grid { grid-template-columns: 1fr; }
  .finance-cards { grid-template-columns: 1fr; }
}
@media (max-width: 768px) {
  .header-actions { flex-direction: column; align-items: flex-start; }
}
</style>

