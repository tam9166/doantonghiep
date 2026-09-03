<template>
  <AdminLayout>
    <div class="area-page">
      <div class="page-header">
        <div>
          <h1 class="page-title">Khu Vực Phục Vụ</h1>
          <p class="page-subtitle">Khu phục vụ thường miễn phí; phí phòng và mức chi tối thiểu chỉ áp dụng cho phòng riêng.</p>
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
          <div class="form-row">
            <label>Sức chứa tối đa (người)</label>
            <input v-model.number="form.capacity" type="number" min="0" class="g-form-control" />
          </div>
          <div class="form-row">
            <label>Loại khu vực</label>
            <select v-model="form.areaType" class="g-form-control"><option value="DINING">Khu phục vụ thường (miễn phí)</option><option value="PRIVATE_ROOM">Phòng riêng / VIP</option><option value="EVENT_HALL">Sảnh sự kiện</option></select>
          </div>
          <div v-if="form.areaType === 'PRIVATE_ROOM'" class="form-columns">
            <div class="form-row"><label>Phí phòng</label><input v-model.number="form.roomFee" type="number" min="0" step="10000" class="g-form-control" /></div>
            <div class="form-row"><label>Chi tiêu tối thiểu</label><input v-model.number="form.minimumSpend" type="number" min="0" step="10000" class="g-form-control" /></div>
          </div>
          <div class="form-row">
            <label>Ảnh khu vực</label>
            <input v-model.trim="form.imageUrl" class="g-form-control" placeholder="URL ảnh minh họa" />
          </div>
          <template v-if="form.areaType === 'EVENT_HALL'">
            <div class="form-columns"><div class="form-row"><label>Khách tối thiểu</label><input v-model.number="form.minGuestCount" type="number" min="1" class="g-form-control"></div><div class="form-row"><label>Khách tối đa</label><input v-model.number="form.maxGuestCount" type="number" min="1" class="g-form-control"></div></div>
            <div class="form-columns"><div class="form-row"><label>Số bàn tối đa</label><input v-model.number="form.maxTables" type="number" min="1" class="g-form-control"></div><div class="form-row"><label>Khách/bàn mặc định</label><input v-model.number="form.defaultGuestsPerTable" type="number" min="1" class="g-form-control"></div></div>
            <div class="form-columns"><div class="form-row"><label>Giờ thuê tối thiểu</label><input v-model.number="form.minBookingHours" type="number" min="1" class="g-form-control"></div><div class="form-row"><label>Giá theo giờ</label><input v-model.number="form.hourlyRate" type="number" min="0" class="g-form-control"></div></div>
            <div class="form-row"><label>Giá gói sự kiện</label><input v-model.number="form.packagePrice" type="number" min="0" class="g-form-control"></div>
            <div class="form-row"><label>Loại sự kiện phù hợp</label><div class="event-checks"><label v-for="type in eventTypes" :key="type.value"><input v-model="form.suitableEventTypes" type="checkbox" :value="type.value"> {{ type.label }}</label></div></div>
          </template>
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
                  <span
                    class="status-pill booking"
                    :class="area.bookingReady ? 'active' : 'inactive'"
                    :title="area.bookingReadyReason || ''"
                  >
                    {{ area.bookingReady ? 'Sẵn sàng đặt bàn' : 'Chưa sẵn sàng đặt bàn' }}
                  </span>
                </div>
                <p class="area-desc">{{ area.descriptionVi || 'Chưa có mô tả.' }}</p>
                <div class="area-meta">
                  <span>Sức chứa: {{ area.capacity || 0 }} người</span>
                  <span v-if="area.usableTableCount != null">Bàn hoạt động: {{ area.usableTableCount }}</span>
                  <span v-if="area.totalTableCapacity != null">Tổng chỗ bàn: {{ area.totalTableCapacity }}</span>
                  <span v-if="area.bookingReadyReason">Lý do: {{ area.bookingReadyReason }}</span>
                  <span v-if="area.areaType === 'DINING'">Miễn phí đặt khu vực</span>
                  <span v-if="area.areaType === 'PRIVATE_ROOM'">Phí phòng: {{ formatCurrency(area.roomFee) }} · Chi tối thiểu: {{ formatCurrency(area.minimumSpend) }}</span>
                  <span v-if="area.areaType === 'EVENT_HALL'">Sảnh: {{ area.minGuestCount }}–{{ area.maxGuestCount }} khách · tối đa {{ area.maxTables || '-' }} bàn</span>
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
import { useToast } from '@/composables/useToast';
import { useDialog } from '@/composables/useDialog';
import { getApiErrorMessage } from '@/services/errorMessage';

const defaultForm = () => ({
  id: null,
  nameVi: '',
  nameEn: '',
  descriptionVi: '',
  descriptionEn: '',
  imageUrl: '',
  gallery: [],
  roomFee: 0,
  minimumSpend: 0,
  capacity: 0,
  status: 'ACTIVE', areaType: 'DINING', minGuestCount: 1, maxGuestCount: 1000, minBookingHours: 2,
  hourlyRate: 0, packagePrice: 0, maxTables: null, defaultGuestsPerTable: 10, suitableEventTypes: []
});
const eventTypes = [{value:'WEDDING',label:'Tiệc cưới'},{value:'ENGAGEMENT',label:'Ăn hỏi'},{value:'BIRTHDAY',label:'Sinh nhật'},{value:'REUNION',label:'Họp lớp/Liên hoan'},{value:'CORPORATE',label:'Tiệc công ty'},{value:'CONFERENCE',label:'Hội nghị'},{value:'OTHER',label:'Khác'}]

const areas = ref([]);
const form = ref(defaultForm());
const loading = ref(false);
const saving = ref(false);
const toast = useToast();
const { confirmDialog } = useDialog();

const authHeader = () => ({
  headers: { Authorization: `Bearer ${sessionStorage.getItem('staff_token')}` }
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
  } catch (error) {
    toast.error(getApiErrorMessage(error, 'Không thể tải danh sách khu vực.'));
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
    roomFee: Number(area.roomFee || 0),
    minimumSpend: Number(area.minimumSpend || 0),
    capacity: Number(area.capacity || 0),
    gallery: Array.isArray(area.gallery) ? [...area.gallery] : [],
    status: area.status || 'ACTIVE'
  };
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const normalizePayload = () => {
  const base = { ...form.value }
  delete base.id
  return { ...base, basePrice: 0, roomFee: form.value.areaType === 'PRIVATE_ROOM' ? Number(form.value.roomFee || 0) : 0,
    minimumSpend: form.value.areaType === 'PRIVATE_ROOM' ? Number(form.value.minimumSpend || 0) : 0, capacity: Number(form.value.capacity || 0),
    status: form.value.status || 'ACTIVE', gallery: Array.isArray(form.value.gallery) ? form.value.gallery : [],
    suitableEventTypes: form.value.suitableEventTypes || [] }
};

const submitArea = async () => {
  if (!form.value.nameVi) {
    toast.warning('Vui lòng nhập tên khu vực.');
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
    toast.error(getApiErrorMessage(error, 'Không thể lưu khu vực.'));
  } finally {
    saving.value = false;
  }
};

const deactivateArea = async (area) => {
  if (!await confirmDialog({ title: 'Tạm ẩn khu vực', message: `Tạm ẩn khu vực "${area.nameVi}"? Bàn đã gán khu vực này vẫn giữ dữ liệu cũ.`, confirmLabel: 'Tạm ẩn', danger: true })) return;
  try {
    await api.put(`/api/areas/${area.id}/deactivate`, {}, authHeader());
    await fetchAreas();
  } catch (error) {
    toast.error(getApiErrorMessage(error, 'Không thể tạm ẩn khu vực.'));
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
.event-checks{display:grid;grid-template-columns:1fr 1fr;gap:8px}.event-checks label{display:flex;align-items:center;gap:6px;text-transform:none}.event-checks input{width:auto}
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
.btn-danger { border-color: color-mix(in srgb, var(--primary) 45%, transparent); color: var(--primary); }
.btn-danger:hover { background: color-mix(in srgb, var(--primary) 12%, transparent); }
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
  background: linear-gradient(135deg, color-mix(in srgb, var(--secondary) 16%, transparent), color-mix(in srgb, var(--secondary) 12%, transparent));
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
.status-pill.active { background: color-mix(in srgb, var(--secondary) 12%, transparent); color: var(--primary); }
.status-pill.inactive { background: rgba(111, 122, 115, 0.16); color: var(--text-muted); }
.status-pill.booking.active { background: color-mix(in srgb, #1f8a4c 14%, transparent); color: #17633a; }
.status-pill.booking.inactive { background: color-mix(in srgb, var(--primary) 12%, transparent); color: var(--primary); }
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
