<template>
  <AdminLayout>
    <div class="area-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Khu Vực Phục Vụ</h1>
          <p class="page-subtitle">Quản lý tầng, phòng VIP, sân thượng và giá nền theo khu vực.</p>
        </div>
        <button class="g-btn-primary" @click="resetForm">Tạo khu vực mới</button>
      </div>

      <div class="area-grid">
        <section class="form-panel">
          <h3>{{ form.id ? 'Cập nhật khu vực' : 'Thêm khu vực' }}</h3>
          <div class="form-row">
            <label>Tên tiếng Việt</label>
            <input v-model.trim="form.nameVi" class="g-form-control" placeholder="Tầng 2 - Sảnh tiệc" />
          </div>
          <div class="form-row">
            <label>Tên tiếng Anh</label>
            <input v-model.trim="form.nameEn" class="g-form-control" placeholder="Second floor hall" />
          </div>
          <div class="form-row">
            <label>Mô tả tiếng Việt</label>
            <textarea v-model.trim="form.descriptionVi" class="g-form-control" rows="3" />
          </div>
          <div class="form-row">
            <label>Mô tả tiếng Anh</label>
            <textarea v-model.trim="form.descriptionEn" class="g-form-control" rows="3" />
          </div>
          <div class="form-columns">
            <div class="form-row">
              <label>Sức chứa</label>
              <input v-model.number="form.capacity" type="number" min="0" class="g-form-control" />
            </div>
            <div class="form-row">
              <label>Giá nền</label>
              <input v-model.number="form.basePrice" type="number" min="0" step="10000" class="g-form-control" />
            </div>
          </div>
          <div class="form-row">
            <label>Ảnh khu vực</label>
            <input v-model.trim="form.imageUrl" class="g-form-control" placeholder="URL ảnh minh họa" />
          </div>
          <div class="form-row">
            <label>Trạng thái</label>
            <select v-model="form.status" class="g-form-control">
              <option value="ACTIVE">Đang sử dụng</option>
              <option value="INACTIVE">Tạm ẩn</option>
            </select>
          </div>
          <div class="form-actions">
            <button class="g-btn-primary" @click="submitArea" :disabled="saving">
              {{ saving ? 'Đang lưu...' : 'Lưu khu vực' }}
            </button>
            <button class="btn-secondary" @click="resetForm">Làm mới</button>
          </div>
        </section>

        <section class="list-panel">
          <div class="list-header">
            <h3>Danh sách khu vực</h3>
            <span>{{ areas.length }} khu vực</span>
          </div>
          <div v-if="loading" class="empty-state">Đang tải dữ liệu...</div>
          <div v-else-if="areas.length === 0" class="empty-state">Chưa có khu vực nào.</div>
          <div v-else class="area-list">
            <article v-for="area in sortedAreas" :key="area.id" class="area-card" :class="{ inactive: area.status !== 'ACTIVE' }">
              <div class="area-media" :style="area.imageUrl ? { backgroundImage: `url(${area.imageUrl})` } : {}">
                <span v-if="!area.imageUrl">Khu vực</span>
              </div>
              <div class="area-body">
                <div class="area-title-row">
                  <div>
                    <h4>{{ area.nameVi }}</h4>
                    <p>{{ area.nameEn || 'Chưa có tên tiếng Anh' }}</p>
                  </div>
                  <span class="status-pill" :class="area.status === 'ACTIVE' ? 'active' : 'inactive'">
                    {{ area.status === 'ACTIVE' ? 'Đang dùng' : 'Tạm ẩn' }}
                  </span>
                </div>
                <p class="area-desc">{{ area.descriptionVi || 'Chưa có mô tả.' }}</p>
                <div class="area-meta">
                  <span>Sức chứa: {{ area.capacity || 0 }}</span>
                  <span>Giá nền: {{ formatCurrency(area.basePrice) }}</span>
                </div>
                <div class="area-actions">
                  <button class="btn-secondary" @click="editArea(area)">Sửa</button>
                  <button
                    v-if="area.status === 'ACTIVE'"
                    class="btn-danger"
                    @click="deactivateArea(area)"
                  >
                    Tạm ẩn
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import api from '@/services/api';
import AdminLayout from '@/components/AdminLayout.vue';

const defaultForm = () => ({
  id: null,
  nameVi: '',
  nameEn: '',
  descriptionVi: '',
  descriptionEn: '',
  imageUrl: '',
  basePrice: 0,
  capacity: 0,
  status: 'ACTIVE'
});

const areas = ref([]);
const form = ref(defaultForm());
const loading = ref(false);
const saving = ref(false);

const authHeader = () => ({
  headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
});

const sortedAreas = computed(() => {
  return [...areas.value].sort((a, b) => {
    if (a.status !== b.status) return a.status === 'ACTIVE' ? -1 : 1;
    return (a.nameVi || '').localeCompare(b.nameVi || '', 'vi');
  });
});

const formatCurrency = (value) => {
  return Number(value || 0).toLocaleString('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0
  });
};

const fetchAreas = async () => {
  loading.value = true;
  try {
    const res = await api.get('/api/areas/admin', authHeader());
    areas.value = Array.isArray(res.data) ? res.data : [];
  } catch {
    alert('Không thể tải danh sách khu vực.');
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = defaultForm();
};

const editArea = (area) => {
  form.value = {
    ...defaultForm(),
    ...area,
    basePrice: Number(area.basePrice || 0),
    capacity: Number(area.capacity || 0),
    status: area.status || 'ACTIVE'
  };
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const normalizePayload = () => ({
  ...form.value,
  basePrice: Number(form.value.basePrice || 0),
  capacity: Number(form.value.capacity || 0),
  status: form.value.status || 'ACTIVE'
});

const submitArea = async () => {
  if (!form.value.nameVi) {
    alert('Vui lòng nhập tên khu vực.');
    return;
  }
  saving.value = true;
  try {
    if (form.value.id) {
      await api.put(`/api/areas/${form.value.id}`, normalizePayload(), authHeader());
    } else {
      await api.post('/api/areas', normalizePayload(), authHeader());
    }
    resetForm();
    await fetchAreas();
  } catch (error) {
    alert(error.response?.data?.message || 'Không thể lưu khu vực.');
  } finally {
    saving.value = false;
  }
};

const deactivateArea = async (area) => {
  if (!confirm(`Tạm ẩn khu vực "${area.nameVi}"? Bàn đã gán khu vực này vẫn giữ dữ liệu cũ.`)) return;
  try {
    await api.put(`/api/areas/${area.id}/deactivate`, {}, authHeader());
    await fetchAreas();
  } catch {
    alert('Không thể tạm ẩn khu vực.');
  }
};

onMounted(fetchAreas);
</script>

<style scoped>
.area-page { max-width: 1320px; margin: 0 auto; }
.page-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 24px; }
.page-title { margin: 0 0 6px; color: var(--text-heading); font-size: 1.9rem; font-weight: 900; }
.page-subtitle { margin: 0; color: var(--text-muted); }
.area-grid { display: grid; grid-template-columns: 380px minmax(0, 1fr); gap: 24px; align-items: start; }
.form-panel, .list-panel {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  padding: 22px;
}
.form-panel h3, .list-header h3 { margin: 0; color: var(--text-heading); font-size: 1.1rem; }
.form-panel h3 { margin-bottom: 18px; }
.form-row { display: grid; gap: 7px; margin-bottom: 14px; }
.form-row label { color: var(--text-muted); font-size: 0.8rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.4px; }
.form-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-actions { display: flex; gap: 10px; margin-top: 18px; }
.btn-secondary, .btn-danger {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  color: var(--text-secondary);
  background: transparent;
  cursor: pointer;
  font-weight: 700;
}
.btn-secondary:hover { border-color: var(--primary); color: var(--primary); }
.btn-danger { border-color: rgba(178, 59, 46, 0.45); color: #B23B2E; }
.btn-danger:hover { background: rgba(178, 59, 46, 0.12); }
.list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.list-header span { color: var(--text-muted); font-size: 0.86rem; font-weight: 700; }
.area-list { display: grid; gap: 14px; }
.area-card {
  display: grid;
  grid-template-columns: 160px minmax(0, 1fr);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--bg-card2);
}
.area-card.inactive { opacity: 0.65; }
.area-media {
  min-height: 150px;
  background: linear-gradient(135deg, rgba(90, 110, 69, 0.16), rgba(90, 110, 69, 0.12));
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  font-weight: 900;
}
.area-body { padding: 16px; }
.area-title-row { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.area-title-row h4 { margin: 0 0 4px; color: var(--text-heading); font-size: 1.05rem; }
.area-title-row p { margin: 0; color: var(--text-muted); font-size: 0.82rem; }
.status-pill { border-radius: 999px; padding: 5px 10px; font-size: 0.75rem; font-weight: 800; white-space: nowrap; }
.status-pill.active { background: rgba(90, 110, 69, 0.12); color: var(--primary); }
.status-pill.inactive { background: rgba(111, 122, 115, 0.16); color: #7A7460; }
.area-desc { color: var(--text-secondary); line-height: 1.45; margin: 12px 0; }
.area-meta { display: flex; flex-wrap: wrap; gap: 10px; color: var(--text-muted); font-size: 0.84rem; font-weight: 700; }
.area-actions { display: flex; gap: 10px; margin-top: 14px; }
.empty-state { color: var(--text-muted); text-align: center; padding: 48px 16px; border: 1px dashed var(--border-light); border-radius: var(--radius-md); }
@media (max-width: 980px) {
  .area-grid { grid-template-columns: 1fr; }
  .page-header { flex-direction: column; }
}
@media (max-width: 640px) {
  .area-card { grid-template-columns: 1fr; }
  .area-media { min-height: 120px; }
  .form-columns { grid-template-columns: 1fr; }
}
</style>
