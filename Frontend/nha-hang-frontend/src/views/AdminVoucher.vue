<template>
  <AdminLayout>
  <div class="admin-wrapper">
    

    <main class="admin-content">
      <div class="page-header">
        <h1 class="page-title">Quản Lý Voucher Khuyến Mãi</h1>
        <button @click="openCreate" class="g-btn-primary">+ Tạo Voucher Mới</button>
      </div>

      <div class="g-table-container">
        <table class="g-table">
          <thead>
            <tr>
              <th>Mã</th>
              <th>Giảm giá</th>
              <th>Đối tượng</th>
              <th>Đã dùng / Giới hạn</th>
              <th>Thời gian áp dụng</th>
              <th>Trạng Thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in vouchers" :key="v.id">
              <td><span class="code-badge">{{ v.code }}</span></td>
              <td style="color: var(--primary); font-weight: bold;">{{ v.discountPercent }}%</td>
              <td>{{ v.accountUsername || 'Tất cả khách hàng' }}</td>
              <td>{{ v.usedCount || 0 }} / {{ v.usageLimit || 'Không giới hạn' }}</td>
              <td>{{ formatWindow(v) }}</td>
              <td>
                <span class="status-badge" :class="`status-${v.status}`">
                  {{ statusLabel(v.status) }}
                </span>
              </td>
              <td class="voucher-actions">
                <button class="g-btn-outline" @click="toggleVoucher(v)">{{ v.active ? 'Tắt' : 'Bật' }}</button>
                <button class="g-btn-primary" @click="openEdit(v)">Sửa</button>
                <button v-if="Number(v.usedCount || 0) > 0" class="g-btn-outline" @click="resetUsage(v)">Đặt lại lượt</button>
              </td>
            </tr>
            <tr v-if="!loading && vouchers.length === 0"><td colspan="7" class="empty-state">Chưa có voucher nào.</td></tr>
          </tbody>
        </table>
        <p v-if="loading" class="empty-state">Đang tải voucher...</p>
        <p v-if="errorMessage" class="error-state">{{ errorMessage }}</p>
      </div>
    </main>

    <!-- Modal Tạo Voucher -->
    <div v-if="showAddModal" class="g-modal-overlay" @click.self="closeModal">
      <div class="g-modal-box">
        <h3>{{ editingId ? 'Cập Nhật Voucher' : 'Tạo Voucher Mới' }}</h3>
        
        <div class="form-group mt-3">
          <label>Mã Code (Để trống sẽ tự tạo)</label>
          <input v-model="newVoucher.code" class="g-form-control" placeholder="VD: TET2026" :disabled="Boolean(editingId)" />
        </div>
        
        <div class="form-group mt-3">
          <label>Mức Giảm Giá (%) *</label>
          <input type="number" v-model="newVoucher.discountPercent" class="g-form-control" min="1" max="100" />
        </div>

        <label class="option-row"><input type="checkbox" v-model="newVoucher.active" /> Hoạt động</label>
        <label class="option-row"><input type="checkbox" v-model="newVoucher.hasUsageLimit" /> Giới hạn lượt sử dụng</label>
        <div v-if="newVoucher.hasUsageLimit" class="form-group mt-3">
          <label>Giới hạn lượt</label>
          <input type="number" min="1" v-model.number="newVoucher.usageLimit" class="g-form-control" />
        </div>
        <label class="option-row"><input type="checkbox" v-model="newVoucher.hasTimeLimit" /> Có giới hạn thời gian</label>
        <div v-if="newVoucher.hasTimeLimit" class="time-grid">
          <div class="form-group"><label>Bắt đầu</label><input type="datetime-local" v-model="newVoucher.startAt" class="g-form-control" /></div>
          <div class="form-group"><label>Kết thúc</label><input type="datetime-local" v-model="newVoucher.endAt" class="g-form-control" /></div>
        </div>

        <div class="form-group mt-3">
          <label>Tài Khoản Khách (Chỉ người này được dùng)</label>
          <input v-model="newVoucher.accountUsername" class="g-form-control" placeholder="Để trống nếu áp dụng cho mọi khách" />
        </div>

        <div class="modal-actions mt-4" style="display: flex; gap: 10px;">
          <button @click="saveVoucher" class="g-btn-primary" style="flex:1;">{{ editingId ? 'Lưu thay đổi' : 'Tạo ngay' }}</button>
          <button @click="closeModal" class="g-btn-outline" style="flex:1;">Hủy</button>
        </div>
      </div>
    </div>
  </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue';

import { ref, onMounted } from 'vue';
import api from '@/services/api';
import { getApiErrorMessage } from '@/services/errorMessage';
import { useToast } from '@/composables/useToast';
import { useDialog } from '@/composables/useDialog';

const vouchers = ref([]);
const showAddModal = ref(false);
const editingId = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const toast = useToast();
const { confirmDialog } = useDialog();
const emptyForm = () => ({ code: '', discountPercent: 10, accountUsername: '', active: true,
  hasUsageLimit: false, usageLimit: 10, hasTimeLimit: false, startAt: '', endAt: '' });
const newVoucher = ref(emptyForm());
const authConfig = () => ({ headers: { Authorization: `Bearer ${sessionStorage.getItem('staff_token')}` } });

const fetchVouchers = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    const res = await api.get('/api/vouchers/admin', authConfig());
    vouchers.value = Array.isArray(res.data) ? res.data : [];
  } catch (error) {
    vouchers.value = [];
    errorMessage.value = getApiErrorMessage(error, 'Không thể tải danh sách voucher.');
  } finally {
    loading.value = false;
  }
};

const saveVoucher = async () => {
  if (!newVoucher.value.discountPercent) return toast.warning('Vui lòng nhập phần trăm giảm giá.');
  if (newVoucher.value.hasUsageLimit && Number(newVoucher.value.usageLimit) < 1) return toast.warning('Giới hạn lượt phải lớn hơn 0.');
  if (newVoucher.value.hasTimeLimit && (!newVoucher.value.startAt || !newVoucher.value.endAt)) return toast.warning('Vui lòng nhập đủ thời gian bắt đầu và kết thúc.');
  try {
    const payload = {
      code: newVoucher.value.code,
      discountPercent: newVoucher.value.discountPercent,
      account: newVoucher.value.accountUsername ? { username: newVoucher.value.accountUsername } : null,
      active: newVoucher.value.active,
      usageLimit: newVoucher.value.hasUsageLimit ? newVoucher.value.usageLimit : null,
      startAt: newVoucher.value.hasTimeLimit ? new Date(newVoucher.value.startAt).toISOString() : null,
      endAt: newVoucher.value.hasTimeLimit ? new Date(newVoucher.value.endAt).toISOString() : null
    };
    if (editingId.value) await api.put(`/api/vouchers/admin/${editingId.value}`, payload, authConfig());
    else await api.post('/api/vouchers/admin/create', payload, authConfig());
    closeModal();
    fetchVouchers();
  } catch (error) {
    toast.error(getApiErrorMessage(error, 'Không thể lưu voucher.'));
  }
};

const openCreate = () => { editingId.value = null; newVoucher.value = emptyForm(); showAddModal.value = true; };
const closeModal = () => { showAddModal.value = false; editingId.value = null; newVoucher.value = emptyForm(); };
const toLocalInput = value => value ? new Date(value).toISOString().slice(0, 16) : '';
const openEdit = voucher => {
  editingId.value = voucher.id;
  newVoucher.value = {
    code: voucher.code, discountPercent: voucher.discountPercent,
    accountUsername: voucher.accountUsername || '', active: Boolean(voucher.active),
    hasUsageLimit: voucher.usageLimit != null, usageLimit: voucher.usageLimit || 10,
    hasTimeLimit: voucher.startAt != null || voucher.endAt != null,
    startAt: toLocalInput(voucher.startAt), endAt: toLocalInput(voucher.endAt)
  };
  showAddModal.value = true;
};
const toggleVoucher = async voucher => {
  try { await api.put(`/api/vouchers/admin/${voucher.id}/active?active=${!voucher.active}`, null, authConfig()); await fetchVouchers(); }
  catch (error) { toast.error(getApiErrorMessage(error, 'Không thể đổi trạng thái voucher.')); }
};
const resetUsage = async voucher => {
  if (!await confirmDialog({ title: 'Đặt lại lượt dùng', message: `Đặt bộ đếm lượt dùng của ${voucher.code} về 0? Lịch sử giao dịch vẫn được giữ.`, confirmLabel: 'Đặt lại', danger: true })) return;
  try { await api.post(`/api/vouchers/admin/${voucher.id}/reset-usage`, null, authConfig()); await fetchVouchers(); }
  catch (error) { toast.error(getApiErrorMessage(error, 'Không thể đặt lại lượt dùng.')); }
};
const statusLabel = status => ({ ACTIVE: 'Đang hoạt động', PAUSED: 'Tạm dừng', NOT_STARTED: 'Chưa có hiệu lực', EXHAUSTED: 'Hết lượt', EXPIRED: 'Hết hạn' }[status] || status);
const formatWindow = voucher => {
  if (!voucher.startAt && !voucher.endAt) return 'Không giới hạn';
  const format = value => value ? new Date(value).toLocaleString('vi-VN') : 'Không giới hạn';
  return `${format(voucher.startAt)} → ${format(voucher.endAt)}`;
};

onMounted(fetchVouchers);
</script>

<style scoped>
.admin-wrapper { background: var(--bg-root); min-height: 100vh; }
.admin-content { max-width: 1400px; margin: 0 auto; padding: 36px 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.code-badge { background: var(--color-tertiary); color: var(--text-primary); padding: 4px 10px; border-radius: 4px; font-weight: bold; font-family: var(--font-primary); letter-spacing: 1px;}
.status-badge { background: color-mix(in srgb, var(--success) 20%, transparent); color: var(--success); padding: 4px 10px; border-radius: 20px; font-weight: bold; font-size: 0.85rem;}
.status-badge.used { background: rgba(111, 122, 115, 0.2); color: var(--text-muted); }
.status-PAUSED, .status-EXHAUSTED, .status-EXPIRED { background: color-mix(in srgb, var(--danger) 14%, transparent); color: var(--danger); }
.status-NOT_STARTED { background: color-mix(in srgb, var(--warning) 18%, transparent); color: var(--text-primary); }
.voucher-actions { display: flex; flex-wrap: wrap; gap: 6px; }
.voucher-actions button { min-height: 34px; }
.option-row { display: flex; align-items: center; gap: 8px; margin-top: 15px; font-weight: 700; }
.time-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 15px; }
.empty-state { padding: 24px; text-align: center; color: var(--text-muted); }
.error-state { margin: 12px; padding: 12px; color: var(--danger); border: 1px solid var(--danger); border-radius: 8px; }
.mt-3 { margin-top: 15px; }
.mt-4 { margin-top: 20px; }
</style>
