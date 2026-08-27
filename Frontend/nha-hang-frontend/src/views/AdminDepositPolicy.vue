<template>
  <AdminLayout>
    <section class="deposit-page">
      <div class="toolbar">
        <div>
          <h1>Chính sách cọc</h1>
          <p>Cấu hình tỷ lệ cọc theo khu vực, khung giờ, số khách và mức ưu tiên.</p>
        </div>
        <button class="primary-btn" type="button" @click="startCreate">Thêm chính sách</button>
      </div>

      <div v-if="error" class="error-box">{{ error }}</div>

      <div class="content-grid">
        <form class="policy-form" @submit.prevent="savePolicy">
          <h2>{{ editingId ? 'Cập nhật chính sách' : 'Tạo chính sách mới' }}</h2>

          <div class="form-grid">
            <label>
              Mã chính sách
              <input v-model.trim="form.policyCode" type="text" placeholder="VD: WEEKEND_50" />
            </label>
            <label>
              Tên chính sách
              <input v-model.trim="form.nameVi" type="text" placeholder="Cọc cuối tuần 50%" />
            </label>
            <label>
              Tên tiếng Anh
              <input v-model.trim="form.nameEn" type="text" placeholder="Weekend deposit" />
            </label>
            <label>
              Loại tính cọc
              <select v-model="form.policyType">
                <option value="PERCENTAGE">Theo phần trăm</option>
                <option value="FIXED">Số tiền cố định</option>
                <option value="PER_GUEST">Theo mỗi khách</option>
              </select>
            </label>
            <label v-if="form.policyType === 'PERCENTAGE'">
              Tỷ lệ cọc
              <input v-model.number="form.percentageRate" type="number" min="0" max="1" step="0.01" />
            </label>
            <label v-if="form.policyType === 'FIXED'">
              Số tiền cố định
              <input v-model.number="form.fixedAmount" type="number" min="0" step="1000" />
            </label>
            <label v-if="form.policyType === 'PER_GUEST'">
              Tiền cọc mỗi khách
              <input v-model.number="form.amountPerGuest" type="number" min="0" step="1000" />
            </label>
            <label>
              Cọc tối thiểu
              <input v-model.number="form.minimumAmount" type="number" min="0" step="1000" />
            </label>
            <label>
              Cọc tối đa
              <input v-model.number="form.maximumAmount" type="number" min="0" step="1000" />
            </label>
            <label>
              Khu vực
              <select v-model="form.areaId">
                <option :value="null">Tất cả khu vực</option>
                <option v-for="area in areas" :key="area.id" :value="area.id">{{ area.nameVi || area.nameEn }}</option>
              </select>
            </label>
            <label>
              Loại bàn
              <select v-model="form.tableType">
                <option value="">Tất cả</option>
                <option value="PRIVATE_ROOM">Phòng riêng</option>
                <option value="WINDOW">Gần cửa sổ</option>
              </select>
            </label>
            <label>
              Thứ trong tuần
              <select v-model="form.dayOfWeek">
                <option :value="null">Tất cả các ngày</option>
                <option v-for="day in days" :key="day.value" :value="day.value">{{ day.label }}</option>
              </select>
            </label>
            <label>
              Từ giờ
              <input v-model="form.startTime" type="time" />
            </label>
            <label>
              Đến giờ
              <input v-model="form.endTime" type="time" />
            </label>
            <label>
              Số khách tối thiểu
              <input v-model.number="form.minimumGuests" type="number" min="1" />
            </label>
            <label>
              Giá trị đơn tối thiểu
              <input v-model.number="form.minimumOrderAmount" type="number" min="0" step="1000" />
            </label>
            <label>
              Ưu tiên
              <input v-model.number="form.priority" type="number" min="0" />
            </label>
            <label>
              Hiệu lực từ
              <input v-model="form.effectiveFrom" type="date" />
            </label>
            <label>
              Hiệu lực đến
              <input v-model="form.effectiveTo" type="date" />
            </label>
            <label class="check-row">
              <input v-model="form.active" type="checkbox" />
              Đang áp dụng
            </label>
          </div>

          <div class="form-actions">
            <button class="primary-btn" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu chính sách' }}</button>
            <button class="ghost-btn" type="button" @click="resetForm">Hủy</button>
          </div>
        </form>

        <div class="policy-list">
          <div class="list-header">
            <h2>Danh sách chính sách</h2>
            <button class="ghost-btn" type="button" @click="fetchData">Làm mới</button>
          </div>

          <article v-for="policy in policies" :key="policy.id" class="policy-card" :class="{ inactive: !policy.active }">
            <div class="policy-main">
              <div>
                <strong>{{ policy.nameVi }}</strong>
                <span>{{ policy.policyCode }} - {{ policy.policyType }}</span>
              </div>
              <span class="priority">Priority {{ policy.priority || 0 }}</span>
            </div>
            <div class="policy-meta">
              <span>{{ formulaText(policy) }}</span>
              <span>{{ areaName(policy.areaId) }}</span>
              <span>{{ timeText(policy) }}</span>
              <span>{{ policy.active ? 'Đang áp dụng' : 'Đã tắt' }}</span>
            </div>
            <div class="row-actions">
              <button type="button" @click="editPolicy(policy)">Sửa</button>
              <button type="button" class="danger" @click="deactivatePolicy(policy)" :disabled="!policy.active">Tắt</button>
            </div>
          </article>

          <div v-if="policies.length === 0" class="empty">Chưa có chính sách cọc nào.</div>
        </div>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import api from '@/services/api'
import { useToast } from '@/composables/useToast'
import { useDialog } from '@/composables/useDialog'

const policies = ref([])
const areas = ref([])
const saving = ref(false)
const error = ref('')
const editingId = ref(null)
const toast = useToast()
const { confirmDialog } = useDialog()

const days = [
  { value: 1, label: 'Thứ 2' },
  { value: 2, label: 'Thứ 3' },
  { value: 3, label: 'Thứ 4' },
  { value: 4, label: 'Thứ 5' },
  { value: 5, label: 'Thứ 6' },
  { value: 6, label: 'Thứ 7' },
  { value: 7, label: 'Chủ nhật' }
]

const blankForm = () => ({
  policyCode: '',
  nameVi: '',
  nameEn: '',
  policyType: 'PERCENTAGE',
  percentageRate: 0.5,
  fixedAmount: null,
  amountPerGuest: null,
  minimumAmount: 0,
  maximumAmount: null,
  areaId: null,
  tableType: '',
  dayOfWeek: null,
  startTime: '',
  endTime: '',
  minimumGuests: null,
  minimumOrderAmount: null,
  priority: 100,
  active: true,
  effectiveFrom: '',
  effectiveTo: ''
})

const form = ref(blankForm())

function normalizePayload() {
  const payload = { ...form.value }
  for (const key of ['fixedAmount', 'amountPerGuest', 'maximumAmount', 'areaId', 'dayOfWeek', 'minimumGuests', 'minimumOrderAmount']) {
    if (payload[key] === '' || payload[key] === undefined) payload[key] = null
  }
  for (const key of ['nameEn', 'tableType', 'startTime', 'endTime', 'effectiveFrom', 'effectiveTo']) {
    if (!payload[key]) payload[key] = null
  }
  if (payload.policyType !== 'PERCENTAGE') payload.percentageRate = null
  if (payload.policyType !== 'FIXED') payload.fixedAmount = null
  if (payload.policyType !== 'PER_GUEST') payload.amountPerGuest = null
  return payload
}

async function fetchData() {
  error.value = ''
  try {
    const [policyRes, areaRes] = await Promise.all([
      api.get('/api/admin/deposit-policies'),
      api.get('/api/areas')
    ])
    policies.value = Array.isArray(policyRes.data) ? policyRes.data : []
    areas.value = Array.isArray(areaRes.data) ? areaRes.data : []
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không tải được chính sách cọc'
  }
}

async function savePolicy() {
  error.value = ''
  if (!form.value.policyCode || !form.value.nameVi) {
    error.value = 'Vui lòng nhập mã và tên chính sách.'
    return
  }
  saving.value = true
  try {
    const payload = normalizePayload()
    if (editingId.value) {
      await api.put(`/api/admin/deposit-policies/${editingId.value}`, payload)
    } else {
      await api.post('/api/admin/deposit-policies', payload)
    }
    resetForm()
    await fetchData()
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không lưu được chính sách cọc'
  } finally {
    saving.value = false
  }
}

function startCreate() {
  resetForm()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function editPolicy(policy) {
  editingId.value = policy.id
  form.value = {
    ...blankForm(),
    ...policy,
    tableType: policy.tableType || '',
    startTime: policy.startTime || '',
    endTime: policy.endTime || '',
    effectiveFrom: policy.effectiveFrom || '',
    effectiveTo: policy.effectiveTo || ''
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function deactivatePolicy(policy) {
  if (!await confirmDialog({ title: 'Tắt chính sách cọc', message: `Tắt chính sách ${policy.nameVi}?`, confirmLabel: 'Tắt chính sách', danger: true })) return
  try {
    await api.delete(`/api/admin/deposit-policies/${policy.id}`)
    toast.success('Đã tắt chính sách cọc.')
    await fetchData()
  } catch (err) {
    toast.error(err.response?.data?.message || 'Không thể tắt chính sách cọc.')
  }
}

function resetForm() {
  editingId.value = null
  form.value = blankForm()
}

function areaName(areaId) {
  if (!areaId) return 'Tất cả khu vực'
  const area = areas.value.find(item => item.id === areaId)
  return area ? (area.nameVi || area.nameEn) : `Khu vực #${areaId}`
}

function formulaText(policy) {
  if (policy.policyType === 'FIXED') return `Cố định ${money(policy.fixedAmount)}`
  if (policy.policyType === 'PER_GUEST') return `${money(policy.amountPerGuest)} / khách`
  return `${Number(policy.percentageRate || 0) * 100}% tổng tiền`
}

function timeText(policy) {
  const parts = []
  if (policy.dayOfWeek) parts.push(days.find(day => day.value === policy.dayOfWeek)?.label)
  if (policy.startTime || policy.endTime) parts.push(`${policy.startTime || '--:--'} - ${policy.endTime || '--:--'}`)
  if (policy.minimumGuests) parts.push(`từ ${policy.minimumGuests} khách`)
  return parts.filter(Boolean).join(' | ') || 'Mọi thời điểm'
}

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0
}).format(Number(value || 0))

onMounted(fetchData)
</script>

<style scoped>
.deposit-page {
  color: var(--text-primary);
}

.toolbar,
.list-header,
.policy-main,
.form-actions,
.row-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.toolbar {
  margin-bottom: 18px;
}

h1,
h2 {
  margin: 0 0 6px;
  color: var(--color-on-secondary-container);
}

p {
  margin: 0;
  color: var(--text-muted);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(420px, 1.05fr);
  gap: 18px;
  align-items: start;
}

.policy-form,
.policy-list,
.policy-card {
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: 8px;
}

.policy-form,
.policy-list {
  padding: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  font-weight: 800;
  color: var(--text-secondary);
}

input,
select {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  min-height: 40px;
  padding: 0 10px;
  font: inherit;
}

.check-row {
  grid-template-columns: auto 1fr;
  align-items: center;
}

.check-row input {
  width: 18px;
  min-height: 18px;
}

.form-actions {
  justify-content: flex-start;
  margin-top: 16px;
}

.primary-btn,
.ghost-btn,
.row-actions button {
  min-height: 38px;
  border-radius: 8px;
  padding: 0 12px;
  font-weight: 900;
  cursor: pointer;
}

.primary-btn,
.row-actions button {
  border: 1px solid var(--secondary);
  background: var(--secondary);
  color: #FFFFFF;
}

.ghost-btn {
  border: 1px solid var(--border);
  background: #FFFFFF;
  color: var(--text-secondary);
}

.row-actions .danger {
  border-color: var(--primary);
  background: var(--primary);
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.policy-list {
  display: grid;
  gap: 12px;
}

.policy-card {
  padding: 14px;
  display: grid;
  gap: 12px;
}

.policy-card.inactive {
  opacity: 0.65;
}

.policy-main strong,
.policy-main span {
  display: block;
}

.policy-main span {
  color: var(--text-muted);
  margin-top: 4px;
}

.priority {
  white-space: nowrap;
  background: var(--bg-hover);
  color: var(--secondary);
  border-radius: 999px;
  padding: 5px 10px;
  font-weight: 900;
}

.policy-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.policy-meta span {
  background: var(--bg-card2);
  border-radius: 8px;
  padding: 8px;
  color: var(--text-secondary);
}

.error-box,
.empty {
  border-radius: 8px;
  padding: 12px;
}

.error-box {
  background: #F4E8E5;
  color: #8F2F25;
  border: 1px solid #E8C9C4;
  margin-bottom: 14px;
}

.empty {
  background: var(--bg-card2);
  color: var(--text-muted);
  text-align: center;
}

@media (max-width: 980px) {
  .content-grid,
  .form-grid,
  .policy-meta {
    grid-template-columns: 1fr;
  }

  .toolbar,
  .list-header,
  .policy-main {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
