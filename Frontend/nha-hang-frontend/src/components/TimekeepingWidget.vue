<template>
  <div class="timekeeping-widget">
    <div class="widget-header" @click="toggleExpanded">
      <span class="icon">🕒</span> 
      <span class="title">Chấm Công</span>
      <span class="arrow">{{ isExpanded ? '▼' : '▲' }}</span>
    </div>
    
    <div class="widget-body" v-if="isExpanded">
      <p class="date">{{ new Date().toLocaleDateString('vi-VN') }}</p>
      
      <div v-if="loading" class="status loading">Đang tải...</div>
      
      <div v-else class="status-container">
        <p class="current-status" :class="statusClass">
          Trạng thái: <strong>{{ currentStatus || 'Chưa Check-in' }}</strong>
        </p>
        
        <div class="time-info" v-if="checkInTime">
          <p>Giờ vào: {{ formatTime(checkInTime) }}</p>
        </div>
        
        <div class="time-info" v-if="checkOutTime">
          <p>Giờ ra: {{ formatTime(checkOutTime) }}</p>
        </div>

        <div class="actions">
          <button 
            v-if="!checkInTime" 
            class="btn-checkin" 
            @click="performCheck('IN')">
            📍 Bắt Đầu Ca
          </button>
          
          <button 
            v-else-if="checkInTime && !checkOutTime" 
            class="btn-checkout" 
            @click="performCheck('OUT')">
            🚪 Kết Thúc Ca
          </button>
          
          <div v-else class="done-msg">🎉 Đã hoàn thành ca làm!</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import api from '@/services/api';

const isExpanded = ref(false);
const loading = ref(true);
const currentStatus = ref('');
const checkInTime = ref(null);
const checkOutTime = ref(null);

const configHeader = () => {
  const token = localStorage.getItem('staff_token') || localStorage.getItem('token');
  return { headers: { Authorization: 'Bearer ' + token } };
};

const statusClass = computed(() => {
  if (currentStatus.value === 'Đã Check-in') return 'text-warning';
  if (currentStatus.value === 'Hoàn thành') return 'text-success';
  return 'text-muted';
});

const formatTime = (dateStr) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleTimeString('vi-VN');
};

const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value;
  if (isExpanded.value) {
    fetchStatus();
  }
};

const fetchStatus = async () => {
  loading.value = true;
  try {
    const res = await api.get('/api/timekeeping/status', configHeader());
    if (res.data && res.data.id) {
      currentStatus.value = res.data.status;
      checkInTime.value = res.data.checkInTime;
      checkOutTime.value = res.data.checkOutTime;
    } else {
      currentStatus.value = '';
      checkInTime.value = null;
      checkOutTime.value = null;
    }
  } catch (err) {
    console.error('Lỗi lấy trạng thái chấm công', err);
  } finally {
    loading.value = false;
  }
};

const performCheck = async (type) => {
  try {
    const res = await api.post('/api/timekeeping/check', { type }, configHeader());
    
    alert(type === 'IN' ? 'Đã check-in bắt đầu ca!' : 'Đã check-out kết thúc ca!');
    
    currentStatus.value = res.data.status;
    checkInTime.value = res.data.checkInTime;
    checkOutTime.value = res.data.checkOutTime;
  } catch (err) {
    alert(err.response?.data || 'Lỗi hệ thống');
  }
};

onMounted(() => {
  // Lấy trạng thái nhưng không mở popup
  fetchStatus();
});
</script>

<style scoped>
.timekeeping-widget {
  position: fixed;
  bottom: 20px;
  left: 20px;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.15);
  border: 1px solid #E2DCC2;
  width: 250px;
  z-index: 1000;
  overflow: hidden;
  transition: all 0.3s ease;
}

.widget-header {
  background: #55503E;
  color: #FFFFFF;
  padding: 12px 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  font-weight: bold;
}
.widget-header:hover {
  background: #201D14;
}
.title { flex: 1; margin-left: 10px; }

.widget-body {
  padding: 15px;
  background: #FFFFFF;
}
.date { text-align: center; color: #7A7460; font-size: 0.9rem; margin-bottom: 10px; margin-top: 0; border-bottom: 1px dashed #A6B0AA; padding-bottom: 5px; }

.status-container { font-size: 0.95rem; }
.current-status { margin-bottom: 10px; }
.text-warning { color: #B98229; }
.text-success { color: #2F8F5B; }
.text-muted { color: #7A7460; }

.time-info { margin: 5px 0; color: #55503E; }
.time-info p { margin: 0; }

.actions { margin-top: 15px; text-align: center; }
.btn-checkin, .btn-checkout {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 6px;
  font-weight: bold;
  cursor: pointer;
  transition: 0.2s;
  color: #FFFFFF;
}
.btn-checkin { background: #5A6E45; }
.btn-checkin:hover { background: #33422A; }
.btn-checkout { background: #C08A2E; }
.btn-checkout:hover { background: #8A641F; }

.done-msg {
  color: #2F8F5B;
  font-weight: bold;
  padding: 10px;
  background: #EEF5EF;
  border-radius: 6px;
}
</style>
