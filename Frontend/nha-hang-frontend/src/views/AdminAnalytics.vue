<template>
  <AdminLayout>
  <div class="admin-wrapper luxury-theme">
    

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

      <div v-if="isLoading" class="analytics-loading" aria-label="Đang tải dữ liệu thống kê">
        <SkeletonLoader v-for="card in 4" :key="`summary-${card}`" height="112px" />
        <SkeletonLoader v-for="chart in 2" :key="`chart-${chart}`" height="320px" />
      </div>

      <div v-else-if="fetchError" class="data-error g-card" role="alert">
        <strong>Không thể tải dữ liệu thống kê.</strong>
        <span>{{ fetchError }}</span>
        <button class="g-btn-outline" type="button" @click="fetchData">Thử lại</button>
      </div>

      <template v-else>
      <!-- ====== FINANCIAL SUMMARY CARDS ====== -->
      <div class="finance-cards">
        <div class="finance-card card-revenue">
          <div class="fc-icon">💰</div>
          <div class="fc-info">
            <span class="fc-label">Doanh Thu (Đầu Ra)</span>
            <span class="fc-value">{{ totalRevenue.toLocaleString() }}đ</span>
          </div>
          <div class="fc-glow"></div>
        </div>
        <div class="finance-card card-cost">
          <div class="fc-icon">📦</div>
          <div class="fc-info">
            <span class="fc-label">Giá Vốn (Nguyên Liệu)</span>
            <span class="fc-value">{{ totalCost.toLocaleString() }}đ</span>
          </div>
          <div class="fc-glow"></div>
        </div>
        <div class="finance-card card-op">
          <div class="fc-icon">👥</div>
          <div class="fc-info">
            <span class="fc-label">Chi Phí Vận Hành</span>
            <span class="fc-value">{{ (totalStaffCost + totalOpCost).toLocaleString() }}đ</span>
            <span class="fc-ratio">Lương NV: {{ totalStaffCost.toLocaleString() }}đ</span>
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
            <Line v-if="chartDataReady && hasChartData" :data="revenueChartData" :options="chartOptions" />
            <div v-else-if="chartDataReady" class="empty-chart">Chưa có doanh thu trong kỳ này.</div>
            <SkeletonLoader v-else height="100%" />
          </div>
        </div>

        <!-- Revenue vs Cost Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">📊 Thu Chi Theo Thời Gian</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady && hasChartData" :data="revenueCostChartData" :options="chartOptions" />
            <div v-else-if="chartDataReady" class="empty-chart">Chưa có dữ liệu thu chi trong kỳ này.</div>
            <SkeletonLoader v-else height="100%" />
          </div>
        </div>

        <!-- Orders Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">🧾 Số Lượng Hóa Đơn Hoàn Thành</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady && hasChartData" :data="ordersChartData" :options="chartOptions" />
            <div v-else-if="chartDataReady" class="empty-chart">Chưa có hóa đơn hoàn thành trong kỳ này.</div>
            <SkeletonLoader v-else height="100%" />
          </div>
        </div>

        <!-- Profit Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">📈 Lợi Nhuận Theo Thời Gian</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady && hasChartData" :data="profitChartData" :options="profitChartOptions" />
            <div v-else-if="chartDataReady" class="empty-chart">Chưa có dữ liệu lợi nhuận trong kỳ này.</div>
            <SkeletonLoader v-else height="100%" />
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
                <td style="text-align: right; color: #B98229; font-weight: bold;">
                  {{ item.revenue.toLocaleString() }}đ
                </td>
                <td style="text-align: right; color: #B23B2E; font-weight: bold;">
                  {{ item.cost.toLocaleString() }}đ
                </td>
                <td :style="{ textAlign: 'right', fontWeight: 'bold', color: (item.revenue - item.cost) >= 0 ? '#2F8F5B' : '#B23B2E' }">
                  {{ (item.revenue - item.cost).toLocaleString() }}đ
                </td>
                <td style="text-align: center;">
                  <div class="hot-bar">
                    <div class="hot-fill" :style="{ width: (item.quantity / Math.max(topProducts[0]?.quantity || 1, 1) * 100) + '%' }"></div>
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
      </template>
    </main>

    <!-- AI Modal -->
    <div v-if="showAiModal" class="modal-overlay" @click.self="showAiModal = false">
      <div class="ai-modal">
        <div class="modal-header">
          <h2>🤖 Giám Đốc Kinh Doanh AI</h2>
          <button @click="showAiModal = false" class="btn-close" aria-label="Đóng cửa sổ phân tích">✖</button>
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
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';
import SkeletonLoader from '@/components/SkeletonLoader.vue';

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
const isLoading = ref(true);
const fetchError = ref('');
const topProducts = ref([]);
const showAiModal = ref(false);
const aiLoading = ref(false);
const aiResponse = ref('');

const allSchedules = ref([]);
const staffList = ref([]);

// Financial totals
const totalRevenue = ref(0);
const totalCost = ref(0);
const totalStaffCost = ref(0);
const totalOpCost = ref(0);
const profit = computed(() => totalRevenue.value - totalCost.value - totalStaffCost.value - totalOpCost.value);

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: '#201D14', font: { family: 'Times New Roman', size: 13 } } }
  },
  scales: {
    x: { 
      ticks: { color: '#55503E' }, 
      grid: { color: 'rgba(90, 110, 69, 0.08)' } 
    },
    y: { 
      ticks: { color: '#55503E' }, 
      grid: { color: 'rgba(90, 110, 69, 0.08)' },
      beginAtZero: true
    }
  }
};

const profitChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { labels: { color: '#201D14', font: { family: 'Times New Roman', size: 13 } } }
  },
  scales: {
    x: {
      ticks: { color: '#55503E' },
      grid: { color: 'rgba(90, 110, 69, 0.08)' }
    },
    y: {
      ticks: { color: '#55503E' },
      grid: { color: 'rgba(90, 110, 69, 0.08)' }
    }
  }
};

const revenueChartData = ref({ labels: [], datasets: [] });
const ordersChartData = ref({ labels: [], datasets: [] });
const revenueCostChartData = ref({ labels: [], datasets: [] });
const profitChartData = ref({ labels: [], datasets: [] });
const hasChartData = computed(() => revenueChartData.value.labels.length > 0);

// Build a map: productId -> cost per dish (sum of amountRequired * unitPrice for each ingredient)
const buildCostMap = () => {
  const costMap = {}; // productId -> cost per 1 serving
  recipes.value.forEach(recipe => {
    const productId = recipe.product?.id;
    if (!productId) return;
    const ingUnitPrice = recipe.ingredient?.unitPrice || 0;
    const amountReq = recipe.amountRequired || 0;
    const unit = (recipe.ingredient?.unit || '').toLowerCase();
    
    // Nếu đơn giá là cho 1Kg hoặc 1L, nhưng định lượng ghi là g hoặc ml thì cần chia 1000
    let finalAmount = amountReq;
    if (unit === 'g' || unit === 'ml') {
      finalAmount = amountReq / 1000;
    }
    
    const costPerServing = finalAmount * ingUnitPrice;
    if (!costMap[productId]) {
      costMap[productId] = 0;
    }
    costMap[productId] += costPerServing;
  });
  return costMap;
};

const fetchData = async () => {
  isLoading.value = true;
  fetchError.value = '';
  try {
    const token = localStorage.getItem('token');
    const headers = { 'Authorization': `Bearer ${token}` };

    const today = new Date();
    const lastYear = new Date(); lastYear.setFullYear(today.getFullYear() - 1);
    const endStr = today.toISOString().split('T')[0];
    const startStr = lastYear.toISOString().split('T')[0];

    const [resOrders, resRecipes, resIngredients, resSchedules, resStaff] = await Promise.all([
      axios.get('/api/admin/orders', { headers }),
      axios.get('/api/admin/recipes', { headers }),
      axios.get('/api/admin/ingredients', { headers }),
      axios.get(`/api/schedules?startDate=${startStr}&endDate=${endStr}`, { headers }),
      axios.get('/api/admin/staff', { headers })
    ]);

    orders.value = resOrders.data.filter(o => o.status === 4);
    recipes.value = resRecipes.data;
    ingredients.value = resIngredients.data;
    allSchedules.value = resSchedules.data;
    staffList.value = resStaff.data;

    processData();
  } catch (error) {
    console.error('Lỗi lấy dữ liệu thống kê:', error);
    fetchError.value = error.response?.data?.message || 'Vui lòng kiểm tra kết nối và quyền truy cập rồi thử lại.';
  } finally {
    isLoading.value = false;
  }
};

const processData = () => {
  chartDataReady.value = false;
  const now = new Date();
  const costMap = buildCostMap();
  
  let filteredSchedules = [];
  let daysCount = 0;
  
  if (timeFilter.value === 'week') {
    filteredSchedules = allSchedules.value.filter(s => Math.ceil(Math.abs(now - new Date(s.workDate)) / (1000 * 60 * 60 * 24)) <= 7);
    daysCount = 7;
  } else if (timeFilter.value === 'month') {
    filteredSchedules = allSchedules.value.filter(s => new Date(s.workDate).getMonth() === now.getMonth() && new Date(s.workDate).getFullYear() === now.getFullYear());
    daysCount = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  } else if (timeFilter.value === 'year') {
    filteredSchedules = allSchedules.value.filter(s => new Date(s.workDate).getFullYear() === now.getFullYear());
    daysCount = 365;
  }

  // Map username -> role
  const staffMap = {};
  staffList.value.forEach(st => {
    staffMap[st.username] = st.role;
  });

  // Tính chi phí lương theo chức vụ (chia cho 28 công/tháng)
  // Bếp: 7.000.000 / 28 ≈ 250.000đ/ca
  // Phục vụ / Thu ngân: 6.000.000 / 28 ≈ 214.286đ/ca
  let calculatedStaffCost = 0;
  filteredSchedules.forEach(s => {
    const role = staffMap[s.account?.username] || 'ROLE_WAITER';
    if (role === 'ROLE_KITCHEN') {
      calculatedStaffCost += 250000; // 7tr / 28
    } else if (role === 'ROLE_MANAGER') {
      calculatedStaffCost += 357143; // 10tr / 28
    } else {
      calculatedStaffCost += 214286; // 6tr / 28
    }
  });

  totalStaffCost.value = calculatedStaffCost;
  // Chi phí vận hành cố định (mặt bằng, điện nước...): 500,000 VND / ngày
  totalOpCost.value = daysCount * 500000;
  
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
  const profitData = labels.map(k => groupedData[k].revenue - groupedData[k].cost - (totalStaffCost.value / daysCount) - (totalOpCost.value / daysCount));

  // Total financial figures
  totalRevenue.value = revData.reduce((a, b) => a + b, 0);
  totalCost.value = costData.reduce((a, b) => a + b, 0);

  revenueChartData.value = {
    labels,
    datasets: [{
      label: 'Doanh Thu (VNĐ)',
      data: revData,
      borderColor: '#33422A',
      backgroundColor: 'rgba(90, 110, 69, 0.2)',
      borderWidth: 3,
      fill: true,
      tension: 0.3,
      pointBackgroundColor: '#33422A'
    }]
  };

  ordersChartData.value = {
    labels,
    datasets: [{
      label: 'Số lượng đơn hàng',
      data: countData,
      backgroundColor: '#5A6E45',
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
        backgroundColor: 'rgba(47, 143, 91, 0.7)',
        borderRadius: 6,
        barThickness: 24
      },
      {
        label: 'Giá Vốn (Đầu vào)',
        data: costData,
        backgroundColor: 'rgba(178, 59, 46, 0.7)',
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
      backgroundColor: profitData.map(v => v >= 0 ? 'rgba(47, 143, 91, 0.7)' : 'rgba(178, 59, 46, 0.7)'),
      borderRadius: 6,
      barThickness: 30
    }]
  };

  // Top products with cost
  const productMap = {};
  orders.value.forEach(order => {
    if (!order.createDate) return;
    const d = new Date(order.createDate);
    if (timeFilter.value === 'week') {
      if (Math.ceil(Math.abs(now - d) / (1000 * 60 * 60 * 24)) > 7) return;
    } else if (timeFilter.value === 'month') {
      if (d.getMonth() !== now.getMonth() || d.getFullYear() !== now.getFullYear()) return;
    } else if (timeFilter.value === 'year') {
      if (d.getFullYear() !== now.getFullYear()) return;
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
    chi_phi_nguyen_lieu: totalCost.value,
    chi_phi_nhan_su_tong: totalStaffCost.value,
    chi_phi_mat_bang_dien_nuoc: totalOpCost.value,
    loi_nhuan_rong: profit.value,
    bien_loi_nhuan: totalRevenue.value > 0 ? ((profit.value / totalRevenue.value) * 100).toFixed(1) + '%' : '0%',
    top_5_mon: top5
  };

  try {
    const res = await axios.post('/api/chatbot/chat', {
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
  background: linear-gradient(135deg, #33422A, #5A6E45);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.filter-group { display: flex; align-items: center; gap: 12px; }
.filter-label { font-weight: 600; color: var(--text-muted); }
.filter-select { width: 220px; border-color: var(--primary); color: var(--primary); font-weight: 700; background: rgba(90, 110, 69, 0.05); }

.analytics-loading {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}
.analytics-loading > :nth-child(n + 5) { grid-column: span 2; }
.data-error {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border-left: 4px solid var(--danger);
  color: var(--text-primary);
}
.data-error span { flex: 1; color: var(--text-muted); }

/* ====== FINANCIAL SUMMARY CARDS ====== */
.finance-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
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
.card-revenue { border-color: rgba(47, 143, 91, 0.3); }
.card-revenue .fc-icon { background: rgba(47, 143, 91, 0.15); color: #2F8F5B; }
.card-revenue .fc-value { color: #2F8F5B; }
.card-revenue .fc-glow { background: #2F8F5B; }
.card-revenue:hover { box-shadow: 0 8px 30px rgba(47, 143, 91, 0.2); }

/* Card Cost */
.card-cost { border-color: rgba(178, 59, 46, 0.3); }
.card-cost .fc-icon { background: rgba(178, 59, 46, 0.15); color: #B23B2E; }
.card-cost .fc-value { color: #B23B2E; }
.card-cost .fc-glow { background: #B23B2E; }
.card-cost:hover { box-shadow: 0 8px 30px rgba(178, 59, 46, 0.2); }

/* Card Profit */
.card-profit { border-color: rgba(185, 130, 41, 0.3); }
.card-profit .fc-icon { background: rgba(185, 130, 41, 0.15); color: #B98229; }
.card-profit .fc-value { color: #B98229; }
.card-profit .fc-ratio { color: #2F8F5B; }
.card-profit .fc-glow { background: #B98229; }
.card-profit:hover { box-shadow: 0 8px 30px rgba(185, 130, 41, 0.2); }

/* Card OP */
.card-op { border-color: rgba(90, 110, 69, 0.3); }
.card-op .fc-icon { background: rgba(90, 110, 69, 0.15); color: #5A6E45; }
.card-op .fc-value { color: #5A6E45; }
.card-op .fc-ratio { color: #33422A; }
.card-op .fc-glow { background: #5A6E45; }
.card-op:hover { box-shadow: 0 8px 30px rgba(90, 110, 69, 0.2); }

/* Card Loss */
.card-loss { border-color: rgba(178, 59, 46, 0.5); }
.card-loss .fc-icon { background: rgba(178, 59, 46, 0.2); color: #B23B2E; }
.card-loss .fc-value { color: #B23B2E; }
.card-loss .fc-ratio { color: #B23B2E; }
.card-loss .fc-glow { background: #B23B2E; }
.card-loss:hover { box-shadow: 0 8px 30px rgba(178, 59, 46, 0.3); }

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
.empty-chart {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
  color: var(--text-muted); font-weight: bold; text-align: center;
}

/* Leaderboard */
.leaderboard-section { padding: 24px; border-left: 4px solid #B98229; }
.leaderboard-header { margin-bottom: 20px; }
.subtitle { margin: 4px 0 0 0; font-size: 0.9rem; color: var(--text-muted); font-style: italic; }

.rank-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--bg-nav); font-weight: 900; color: #FFFFFF;
}
.rank-1 { background: #B98229; color: #1A170F; box-shadow: 0 0 10px rgba(185,130,41,0.6); transform: scale(1.2); }
.rank-2 { background: #A6B0AA; color: #1A170F; }
.rank-3 { background: #C08A2E; color: #FFFFFF; }

.leader-row:hover { background: rgba(255,255,255,0.03); }
.product-name { font-size: 1.05rem; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.p-icon { font-size: 1.2rem; }

.hot-bar {
  width: 100%; height: 8px;
  background: var(--bg-nav); border-radius: 4px; overflow: hidden;
}
.hot-fill {
  height: 100%; background: linear-gradient(90deg, #B23B2E, #B98229);
  border-radius: 4px; transition: width 1s ease-out;
}
.empty-row { text-align: center; color: var(--text-muted); padding: 40px; font-style: italic; }

.btn-ai-analyze {
  background: linear-gradient(135deg, #C08A2E, #8A641F);
  color: #FFFFFF; border: none; padding: 10px 20px; border-radius: 8px;
  font-weight: bold; cursor: pointer; transition: 0.3s;
  box-shadow: 0 4px 15px rgba(192, 138, 46, 0.4);
}
.btn-ai-analyze:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(192, 138, 46, 0.6); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.ai-modal { background: var(--bg-card); width: 550px; max-width: 90%; border-radius: 12px; padding: 20px; border: 1px solid var(--border-light); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.btn-close {
  min-width: 44px; min-height: 44px; padding: 0;
  background: none; border: none; font-size: 1.5rem; cursor: pointer; color: var(--text-muted);
}
.ai-loading { text-align: center; padding: 30px; color: var(--primary); font-weight: bold; }
.spinner { width: 40px; height: 40px; border: 4px solid rgba(90, 110, 69, 0.2); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 15px auto; }
@keyframes spin { to { transform: rotate(360deg); } }
.ai-result { padding: 20px; font-size: 1.05rem; line-height: 1.6; color: var(--text-primary); border-left: 4px solid var(--primary); background: rgba(90, 110, 69, 0.05); border-radius: 0 8px 8px 0; white-space: pre-line; }

@media (max-width: 1024px) {
  .charts-grid { grid-template-columns: 1fr; }
  .finance-cards { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .analytics-loading { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .analytics-loading > :nth-child(n + 5) { grid-column: span 1; }
  .header-actions { align-items: flex-start; flex-direction: column; gap: 18px; }
}

@media (max-width: 640px) {
  .admin-content { padding: 24px 14px; }
  .page-title { font-size: 1.5rem; }
  .filter-group { width: 100%; align-items: stretch; flex-direction: column; }
  .filter-select { width: 100%; min-height: 44px; }
  .btn-ai-analyze { width: 100%; min-height: 44px; }
  .finance-cards,
  .analytics-loading { grid-template-columns: 1fr; }
  .finance-card,
  .chart-card,
  .leaderboard-section { padding: 18px 16px; }
  .chart-container { height: 280px; }
  .chart-title { font-size: 1rem; }
  .data-error { align-items: stretch; flex-direction: column; }
  .data-error .g-btn-outline { min-height: 44px; }
  .table-responsive { overflow-x: auto; }
  .ai-modal { width: calc(100% - 24px); max-width: none; }
}
</style>

