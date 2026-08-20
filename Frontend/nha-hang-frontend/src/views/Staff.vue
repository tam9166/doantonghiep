<template>
  <div class="staff-dashboard">
    <header class="staff-header">
      <div class="header-left">
        <div class="brand">
          <span class="brand-icon">👤</span>
          <div>
            <h2>THÔNG TIN CÁ NHÂN</h2>
            <p>Xin chào, {{ user?.fullname || user?.username }}</p>
          </div>
        </div>
      </div>
      <div class="header-right">
        <button @click="$router.push(getRoleHomeRoute())" class="btn-back">⬅ Quay lại khu vực làm việc</button>
      </div>
    </header>

    <main class="staff-content">
      <div class="tabs">
        <button :class="{ active: currentTab === 'schedule' }" @click="currentTab = 'schedule'">📅 Lịch Làm Việc</button>
        <button :class="{ active: currentTab === 'timekeeping' }" @click="currentTab = 'timekeeping'">⏱ Lịch Sử Chấm Công</button>
        <button :class="{ active: currentTab === 'salary' }" @click="currentTab = 'salary'">💰 Lương Tạm Tính</button>
      </div>

      <!-- TAB: LỊCH LÀM VIỆC -->
      <div v-if="currentTab === 'schedule'" class="tab-panel fade-in">
        <div class="panel-header">
          <h3>Lịch Phân Công Ca Làm</h3>
          <div class="date-picker">
            <label>Tháng: </label>
            <input type="month" v-model="selectedMonth" @change="fetchData" />
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Ngày Làm Việc</th>
              <th>Ca Làm</th>
              <th>Thời Gian</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="sched in scheduleList" :key="sched.id">
              <td>{{ new Date(sched.workDate).toLocaleDateString('vi-VN') }}</td>
              <td><span class="shift-badge">{{ sched.shift }}</span></td>
              <td style="font-weight: 500; color: var(--text-secondary);">{{ getShiftTime(sched.shift) }}</td>
            </tr>
            <tr v-if="scheduleList.length === 0">
              <td colspan="3" class="text-center">Bạn chưa có lịch làm việc nào trong tháng này.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: CHẤM CÔNG -->
      <div v-if="currentTab === 'timekeeping'" class="tab-panel fade-in">
        <div class="panel-header">
          <h3>Lịch Sử Chấm Công Cá Nhân</h3>
          <div class="date-picker">
            <label>Tháng: </label>
            <input type="month" v-model="selectedMonth" @change="fetchData" />
          </div>
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>Ngày</th>
              <th>Giờ Check-in</th>
              <th>Giờ Check-out</th>
              <th>Trạng Thái</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tk in timekeepingList" :key="tk.id">
              <td>{{ new Date(tk.workDate).toLocaleDateString('vi-VN') }}</td>
              <td>{{ formatTime(tk.checkInTime) }}</td>
              <td>{{ formatTime(tk.checkOutTime) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(tk.status)">{{ tk.status }}</span>
              </td>
            </tr>
            <tr v-if="timekeepingList.length === 0">
              <td colspan="4" class="text-center">Chưa có dữ liệu chấm công.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- TAB: BẢNG LƯƠNG -->
      <div v-if="currentTab === 'salary'" class="tab-panel fade-in">
        <div class="panel-header">
          <h3>Chi Tiết Lương Tháng {{ selectedMonth }}</h3>
          <div class="date-picker">
            <label>Tháng: </label>
            <input type="month" v-model="selectedMonth" @change="fetchData" />
          </div>
        </div>
        
        <div class="salary-card">
          <div class="salary-row">
            <span>Chức vụ hiện tại:</span>
            <strong>{{ translateRole(userRole) }}</strong>
          </div>
          <div class="salary-row">
            <span>Số ca được xếp trong tháng:</span>
            <strong>{{ scheduleList.length }} ca</strong>
          </div>
          <div class="salary-row">
            <span>Số ca đã đi làm (có Check-in):</span>
            <strong class="text-primary">{{ workedShifts }} ca</strong>
          </div>
          <div class="salary-row">
            <span>Đơn giá trên 1 ca:</span>
            <strong>{{ rate.toLocaleString('vi-VN') }} đ</strong>
          </div>
          <hr />
          <div class="salary-row total">
            <span>TỔNG LƯƠNG TẠM TÍNH:</span>
            <strong class="text-success">{{ (workedShifts * rate).toLocaleString('vi-VN') }} đ</strong>
          </div>
          <p class="salary-note">* Lương tạm tính dựa trên số lượng ca đã chấm công hợp lệ. Có thể thay đổi theo phụ cấp, thưởng phạt cuối tháng.</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/services/api';

const router = useRouter();
const currentTab = ref('schedule');
const selectedMonth = ref(new Date().toISOString().substring(0, 7));

const user = ref(JSON.parse(localStorage.getItem('staff_user')) || {});
const userRole = computed(() => {
  if (!user.value || !user.value.roles) return '';
  return user.value.roles.find(r => r.startsWith('ROLE_')) || '';
});

const rate = computed(() => {
  if (userRole.value === 'ROLE_KITCHEN') return 250000;
  if (userRole.value === 'ROLE_MANAGER') return 357143;
  return 214286;
});

const scheduleList = ref([]);
const timekeepingList = ref([]);

const workedShifts = computed(() => {
  return timekeepingList.value.filter(tk => tk.checkInTime).length;
});

const configHeader = () => {
  const token = localStorage.getItem('staff_token');
  return { headers: { Authorization: 'Bearer ' + token } };
};

const getRoleHomeRoute = () => {
  if (userRole.value === 'ROLE_KITCHEN') return '/kitchen';
  if (userRole.value === 'ROLE_WAITER') return '/waiter';
  if (userRole.value === 'ROLE_CASHIER') return '/cashier';
  if (userRole.value === 'ROLE_MANAGER' || userRole.value === 'ROLE_ADMIN') return '/admin';
  return '/';
};

const translateRole = (r) => {
  if (r === 'ROLE_ADMIN') return 'Quản Trị Viên';
  if (r === 'ROLE_MANAGER') return 'Quản Lý';
  if (r === 'ROLE_KITCHEN') return 'Bếp';
  if (r === 'ROLE_WAITER') return 'Phục Vụ';
  if (r === 'ROLE_CASHIER') return 'Thu Ngân';
  return r;
};

const getShiftTime = (shift) => {
  if (shift === 'Sáng') return '09:00 - 14:00';
  if (shift === 'Chiều') return '14:00 - Hết khách';
  if (shift === 'Tối' || shift === 'Full Time' || shift === 'Full ca') return '09:00 - 23:00';
  return 'Chưa xác định';
};

const formatTime = (timeStr) => {
  if (!timeStr) return '-';
  return new Date(timeStr).toLocaleTimeString('vi-VN');
};

const getStatusClass = (status) => {
  if (status === 'Đúng giờ') return 'status-success';
  if (status === 'Đi trễ') return 'status-danger';
  if (status === 'Về sớm') return 'status-warning';
  if (status === 'Hoàn thành' || status === 'Đã Chấm Công') return 'status-success';
  return 'status-secondary';
};

const fetchData = async () => {
  try {
    const year = selectedMonth.value.split('-')[0];
    const month = selectedMonth.value.split('-')[1];
    const startDate = `${year}-${month}-01`;
    const lastDay = new Date(year, month, 0).getDate();
    const endDate = `${year}-${month}-${lastDay}`;
    
    const [resSched, resTk] = await Promise.all([
      api.get(`/api/schedules/my-schedules?startDate=${startDate}&endDate=${endDate}`, configHeader()),
      api.get(`/api/timekeeping/me?startDate=${startDate}&endDate=${endDate}`, configHeader())
    ]);
    
    scheduleList.value = resSched.data;
    timekeepingList.value = resTk.data;
  } catch (err) {
    console.error('Lỗi lấy dữ liệu cá nhân', err);
  }
};

onMounted(() => {
  if (!user.value.username) {
    router.push('/login');
    return;
  }
  fetchData();
});
</script>

<style scoped>
.staff-dashboard {
  background-color: var(--bg-root);
  min-height: 100vh;
  color: var(--text-main);
  font-family: var(--font-primary);
}

.staff-header {
  background: var(--bg-card);
  padding: 15px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
}

.brand { display: flex; align-items: center; gap: 15px; }
.brand-icon { font-size: 2.2rem; }
.brand h2 { margin: 0; font-size: 1.3rem; color: var(--primary); }
.brand p { margin: 5px 0 0 0; font-size: 0.9rem; color: var(--text-muted); }

.btn-back {
  background: var(--border-light);
  color: var(--text-main);
  border: 1px solid var(--border);
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: 0.2s;
}
.btn-back:hover {
  background: var(--primary);
  color: var(--bg-dark);
  border-color: var(--primary);
}

.staff-content {
  padding: 30px;
  max-width: 1000px;
  margin: 0 auto;
}

.tabs { display: flex; gap: 10px; margin-bottom: 20px; }
.tabs button {
  padding: 12px 24px;
  border: none;
  background: var(--bg-card);
  border-radius: 8px 8px 0 0;
  font-weight: bold;
  cursor: pointer;
  transition: 0.3s;
  color: var(--text-muted);
  font-size: 1rem;
}
.tabs button.active {
  background: var(--primary);
  color: var(--bg-dark);
}

.tab-panel {
  background: var(--bg-card);
  padding: 25px;
  border-radius: 0 8px 8px 8px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.panel-header h3 { margin: 0; color: var(--text-heading); font-size: 1.4rem; }

.date-picker { display: flex; align-items: center; gap: 10px; }
.date-picker input {
  background: var(--bg-root); color: var(--text-main);
  border: 1px solid var(--border); padding: 8px 12px;
  border-radius: 6px; outline: none;
}

/* TABLE STYLES */
.data-table {
  width: 100%; border-collapse: collapse; margin-top: 10px;
}
.data-table th, .data-table td {
  padding: 12px 15px; border-bottom: 1px solid var(--border-light); text-align: left;
}
.data-table th {
  background: var(--bg-root); color: var(--text-secondary); font-weight: 600;
}
.data-table tr:hover { background: rgba(255,255,255,0.03); }
.text-center { text-align: center !important; color: var(--text-muted); }

/* BADGES */
.shift-badge { background: var(--secondary); color: #FFFFFF; padding: 4px 10px; border-radius: 12px; font-size: 0.85rem; font-weight: 600; }
.status-badge { padding: 5px 10px; border-radius: 15px; font-size: 0.85rem; font-weight: bold; }
.status-success { background: rgba(39, 174, 96, 0.2); color: var(--success); }
.status-warning { background: color-mix(in srgb, var(--color-tertiary) 20%, transparent); color: var(--color-tertiary); }
.status-danger { background: color-mix(in srgb, var(--primary) 20%, transparent); color: var(--primary); }
.status-secondary { background: rgba(111, 122, 115, 0.2); color: var(--color-outline); }

/* SALARY CARD */
.salary-card {
  background: var(--bg-root);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 30px;
  max-width: 600px;
  margin: 0 auto;
}
.salary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 1.1rem;
}
.salary-row.total {
  font-size: 1.4rem;
  margin-top: 15px;
}
.text-primary { color: var(--primary); }
.text-success { color: var(--success); }
.salary-note {
  margin-top: 25px;
  font-size: 0.9rem;
  color: var(--text-muted);
  text-align: center;
  font-style: italic;
}
hr { border-color: var(--border-light); margin: 20px 0; }

.fade-in { animation: fadeIn 0.3s ease-in-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
</style>
