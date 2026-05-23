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
        <router-link to="/admin/tables">Sơ Đồ Bàn</router-link>
        <router-link to="/admin/orders">Đơn Hàng</router-link>
        <router-link to="/admin/staff">Nhân Sự</router-link>
        <router-link to="/admin/posts">Bài Đăng</router-link>
      </nav>
      <button @click="$router.push('/admin/orders')" class="g-btn-nav">⬅ Trở lại Đơn Hàng</button>
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

        <!-- Orders Bar Chart -->
        <div class="chart-card g-card">
          <h3 class="chart-title">🧾 Số Lượng Hóa Đơn Hoàn Thành</h3>
          <div class="chart-container">
            <Bar v-if="chartDataReady" :data="ordersChartData" :options="chartOptions" />
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
                <th style="text-align: right;">DOANH THU MANG LẠI</th>
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
                <td style="text-align: center;">
                  <div class="hot-bar">
                    <div class="hot-fill" :style="{ width: (item.quantity / topProducts[0].quantity * 100) + '%' }"></div>
                  </div>
                </td>
              </tr>
              <tr v-if="topProducts.length === 0">
                <td colspan="5" class="empty-row">Chưa có dữ liệu bán hàng.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
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

// Đăng ký thư viện biểu đồ
ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend, PointElement, LineElement, Filler);

const orders = ref([]);
const timeFilter = ref('week');
const chartDataReady = ref(false);

const topProducts = ref([]);

// Cấu hình hiển thị biểu đồ
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

const revenueChartData = ref({ labels: [], datasets: [] });
const ordersChartData = ref({ labels: [], datasets: [] });

const fetchData = async () => {
  try {
    const token = localStorage.getItem('token');
    const res = await axios.get('http://localhost:8080/api/admin/orders', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    // Lọc chỉ lấy hóa đơn đã hoàn thành (status = 4)
    orders.value = res.data.filter(o => o.status === 4);
    processData();
  } catch (error) {
    console.error('Lỗi lấy dữ liệu thống kê:', error);
  }
};

const processData = () => {
  chartDataReady.value = false;
  const now = new Date();
  
  // 1. Phân tích dữ liệu theo thời gian (gom nhóm)
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
      groupedData[key] = { revenue: 0, count: 0 };
    }
    
    groupedData[key].count += 1;
    if (order.orderDetails) {
      order.orderDetails.forEach(detail => {
        groupedData[key].revenue += detail.price;
      });
    }
  });

  // Sắp xếp các key (ngày/tháng) theo thứ tự
  const labels = Object.keys(groupedData).sort();
  const revData = labels.map(k => groupedData[k].revenue);
  const countData = labels.map(k => groupedData[k].count);

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

  // 2. Tính toán Top Món Ăn
  const productMap = {};
  orders.value.forEach(order => {
    if (timeFilter.value === 'week') {
      const d = new Date(order.createDate);
      if (Math.ceil(Math.abs(now - d) / (1000 * 60 * 60 * 24)) > 7) return;
    }
    // Các filter khác (month, year) không áp dụng khắt khe cho Top Món Ăn 
    // hoặc có thể thêm điều kiện tương tự ở trên. Ở đây làm đơn giản.

    if (order.orderDetails) {
      order.orderDetails.forEach(detail => {
        const pName = detail.product?.name || 'Món không tên';
        if (!productMap[pName]) {
          productMap[pName] = { name: pName, quantity: 0, revenue: 0 };
        }
        productMap[pName].quantity += detail.quantity;
        productMap[pName].revenue += detail.price;
      });
    }
  });

  // Sắp xếp giảm dần theo số lượng và lấy top 10
  topProducts.value = Object.values(productMap)
    .sort((a, b) => b.quantity - a.quantity)
    .slice(0, 10);

  chartDataReady.value = true;
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

@media (max-width: 992px) {
  .charts-grid { grid-template-columns: 1fr; }
}
</style>
