<template>
  <AdminLayout>
    <section class="page">
      <header class="toolbar">
        <div><h1>Yêu cầu hủy đặt bàn</h1><p>Duyệt hủy và theo dõi hoàn cọc dựa trên giao dịch thực tế.</p></div>
        <button type="button" @click="load" :disabled="loading">{{ loading ? 'Đang tải...' : 'Làm mới' }}</button>
      </header>
      <div v-if="error" class="message error">{{ error }}</div>
      <div v-if="message" class="message success">{{ message }}</div>
      <div class="filters">
        <input v-model.trim="search" placeholder="Tìm mã yêu cầu, booking, khách hàng" aria-label="Tìm yêu cầu hủy" />
        <select v-model="statusFilter" aria-label="Lọc trạng thái"><option value="">Tất cả trạng thái</option><option value="PENDING">Chờ duyệt</option><option value="REFUND_PENDING">Chờ hoàn tiền</option><option value="REFUNDED">Đã hoàn tiền</option><option value="APPROVED">Đã duyệt</option><option value="REJECTED">Từ chối</option></select>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Mã yêu cầu / booking</th><th>Khách hàng</th><th>Thời gian đặt</th><th>Tiền cọc</th><th>Chính sách</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>
            <tr v-for="item in pagedRequests" :key="item.id">
              <td><strong>{{ item.requestCode }}</strong><span>{{ item.reservationCode }}</span><small>{{ dateTime(item.requestedAt) }}</small></td>
              <td><strong>{{ item.customerName }}</strong><span>{{ item.customerPhone }}</span><span>{{ item.customerEmail || '-' }}</span></td>
              <td><strong>{{ item.reservationDate }} {{ item.arrivalTime }}</strong><span>{{ item.guestCount }} khách</span><span>Còn {{ item.hoursBeforeReservation }} giờ lúc gửi</span></td>
              <td><strong>Đã cọc: {{ money(item.paidDepositAmount) }}</strong><span>Dự kiến hoàn: {{ money(item.expectedRefundAmount) }}</span><span>Cọc booking: {{ money(item.depositAmount) }}</span></td>
              <td><strong>{{ Number(item.refundRate || 0) * 100 }}%</strong><span>{{ item.reason || 'Không nêu lý do' }}</span></td>
              <td><span class="badge" :class="item.status">{{ statusText(item.status) }}</span></td>
              <td><div class="actions">
                <button v-if="item.status === 'PENDING'" type="button" @click="openDecision(item, 'approve')">Duyệt</button>
                <button v-if="item.status === 'PENDING'" class="danger" type="button" @click="openDecision(item, 'reject')">Từ chối</button>
                <button v-if="item.status === 'REFUND_PENDING'" type="button" @click="openDecision(item, 'complete')">Xác nhận đã hoàn</button>
                <button class="ghost" type="button" @click="selected = item">Chi tiết</button>
              </div></td>
            </tr>
            <tr v-if="!pagedRequests.length"><td colspan="7" class="empty">Chưa có yêu cầu hủy.</td></tr>
          </tbody>
        </table>
      </div>
      <nav v-if="totalPages > 1" class="pagination" aria-label="Phân trang yêu cầu hủy">
        <button type="button" class="ghost" :disabled="page === 1" @click="page--">Trang trước</button>
        <button v-for="number in totalPages" :key="number" type="button" :class="{ active: number === page }" @click="page = number">{{ number }}</button>
        <button type="button" class="ghost" :disabled="page === totalPages" @click="page++">Trang sau</button>
        <span>{{ filteredRequests.length }} yêu cầu · Trang {{ page }}/{{ totalPages }}</span>
      </nav>
    </section>

    <div v-if="dialog" class="modal" @click.self="closeDialog">
      <form class="dialog" @submit.prevent="submitDecision">
        <h2>{{ dialogTitle }}</h2>
        <p>{{ dialog.requestCode }} · {{ dialog.reservationCode }}</p>
        <p v-if="decisionType === 'complete'" class="generated-reference">Mã giao dịch sẽ được hệ thống tự tạo sau khi xác nhận.</p>
        <label>Ghi chú xử lý<textarea v-model.trim="decisionNote" maxlength="1000" rows="4" required /></label>
        <div class="dialog-actions"><button class="ghost" type="button" @click="closeDialog">Đóng</button><button type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Xác nhận' }}</button></div>
      </form>
    </div>

    <div v-if="selected" class="modal" @click.self="selected = null">
      <article class="dialog"><h2>Chi tiết {{ selected.requestCode }}</h2><dl>
        <div><dt>Booking</dt><dd>{{ selected.reservationCode }}</dd></div>
        <div><dt>Khách</dt><dd>{{ selected.customerName }} · {{ selected.customerPhone }} · {{ selected.customerEmail || '-' }}</dd></div>
        <div><dt>Ngày giờ</dt><dd>{{ selected.reservationDate }} {{ selected.arrivalTime }}</dd></div>
        <div><dt>Tiền đã cọc</dt><dd>{{ money(selected.paidDepositAmount) }}</dd></div>
        <div><dt>Hoàn dự kiến</dt><dd>{{ money(selected.expectedRefundAmount) }} ({{ Number(selected.refundRate || 0) * 100 }}%)</dd></div>
        <div><dt>Lý do</dt><dd>{{ selected.reason || '-' }}</dd></div>
        <div><dt>Phương thức liên lạc</dt><dd>{{ selected.contactMethod === 'EMAIL' ? 'Email' : 'Điện thoại' }} · {{ selected.contactMethod === 'EMAIL' ? selected.customerEmail : selected.customerPhone }}</dd></div>
        <div v-if="selected.expectedRefundAmount > 0"><dt>Ngân hàng hoàn tiền</dt><dd>{{ selected.refundBankName || '-' }}</dd></div>
        <div v-if="selected.expectedRefundAmount > 0"><dt>Số tài khoản</dt><dd>{{ selected.refundAccountNumber || '-' }}</dd></div>
        <div v-if="selected.expectedRefundAmount > 0"><dt>Chủ tài khoản</dt><dd>{{ selected.refundAccountHolder || '-' }}</dd></div>
        <div v-if="selected.refundTransactionId"><dt>Mã giao dịch hoàn</dt><dd>REFUND-{{ selected.refundTransactionId }}</dd></div>
        <div><dt>Người xử lý</dt><dd>{{ selected.processedBy || '-' }} · {{ dateTime(selected.processedAt) }}</dd></div>
        <div><dt>Ghi chú</dt><dd>{{ selected.processingNote || '-' }}</dd></div>
      </dl><button type="button" @click="selected = null">Đóng</button></article>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import api from '@/services/api'

const requests = ref([])
const search = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const message = ref('')
const dialog = ref(null)
const selected = ref(null)
const decisionType = ref('')
const decisionNote = ref('')
const filteredRequests = computed(() => requests.value.filter(item => {
  const query = search.value.toLowerCase()
  const matchesSearch = !query || [item.requestCode, item.reservationCode, item.customerName, item.customerPhone, item.customerEmail].some(value => String(value || '').toLowerCase().includes(query))
  return matchesSearch && (!statusFilter.value || item.status === statusFilter.value)
}))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredRequests.value.length / pageSize)))
const pagedRequests = computed(() => filteredRequests.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const dialogTitle = computed(() => ({ approve: 'Duyệt yêu cầu hủy', reject: 'Từ chối yêu cầu', complete: 'Xác nhận hoàn tiền thực tế' }[decisionType.value]))

const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value || 0))
const dateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '-'
const statusText = status => ({ PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', REFUND_PENDING: 'Chờ hoàn tiền', REFUNDED: 'Đã hoàn tiền', REFUND_FAILED: 'Hoàn tiền lỗi' }[status] || status)

async function load() {
  loading.value = true; error.value = ''
  try { requests.value = (await api.get('/api/admin/reservation-cancellations')).data; page.value = 1 }
  catch (err) { error.value = err.response?.data?.message || 'Không tải được yêu cầu hủy.' }
  finally { loading.value = false }
}
function openDecision(item, type) { dialog.value = item; decisionType.value = type; decisionNote.value = '' }
function closeDialog() { dialog.value = null }
async function submitDecision() {
  if (!dialog.value || saving.value) return
  saving.value = true; error.value = ''; message.value = ''
  try {
    const base = `/api/admin/reservation-cancellations/${dialog.value.id}`
    const body = { note: decisionNote.value }
    const response = await api.patch(`${base}/${decisionType.value === 'complete' ? 'refund-complete' : decisionType.value}`, body)
    message.value = decisionType.value === 'complete'
      ? `Đã xác nhận hoàn tiền. Mã giao dịch: REFUND-${response.data.refundTransactionId}.`
      : 'Đã cập nhật yêu cầu hủy an toàn.'
    closeDialog(); await load()
  } catch (err) { error.value = err.response?.data?.message || 'Không thể xử lý yêu cầu.' }
  finally { saving.value = false }
}
onMounted(load)
watch([search, statusFilter], () => { page.value = 1 })
</script>

<style scoped>
.page { padding: 24px; color: var(--text-primary); }
.filters { display: flex; gap: 10px; margin: 0 0 14px; }.filters input,.filters select { min-height: 38px; padding: 8px 10px; border: 1px solid var(--border); border-radius: 9px; background: var(--bg-card); color: var(--text-primary); }.filters input { flex: 1; }
.toolbar { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-bottom: 20px; }
.toolbar h1 { margin: 0; }.toolbar p { color: var(--text-secondary); }
button { border: 0; border-radius: 9px; padding: 9px 13px; background: var(--primary); color: var(--color-on-primary); font-weight: 800; cursor: pointer; }
button:disabled { opacity: .6; }.danger { background: var(--danger); }.ghost { background: var(--bg-card2); color: var(--text-primary); }
.table-wrap { overflow: auto; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-card); }
table { width: 100%; border-collapse: collapse; min-width: 1180px; }th, td { padding: 13px; text-align: left; border-bottom: 1px solid var(--border-light); vertical-align: top; }th { background: var(--bg-card2); }
td strong, td span, td small { display: block; margin-bottom: 4px; }.actions { display: flex; flex-wrap: wrap; gap: 7px; }.empty { text-align: center; padding: 32px; }
.badge { display: inline-block; border-radius: 999px; padding: 6px 10px; background: var(--color-secondary-fixed); }.badge.REJECTED,.badge.REFUND_FAILED { background: var(--color-error-container); color: var(--color-on-error-container); }.badge.REFUNDED,.badge.APPROVED { color: var(--success); }
.message { margin: 12px 0; padding: 12px; border-radius: 9px; }.error { background: var(--color-error-container); color: var(--color-on-error-container); }.success { background: var(--color-secondary-fixed); color: var(--success); }
.pagination { display: flex; align-items: center; justify-content: flex-end; gap: 7px; padding: 14px 0; flex-wrap: wrap; }.pagination button.active { background: var(--danger); }.pagination span { color: var(--text-secondary); margin-left: 6px; }.generated-reference { padding: 10px 12px; border-radius: 9px; background: var(--color-secondary-fixed); color: var(--text-primary); }
.modal { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 20px; background: var(--overlay-dark); }.dialog { width: min(600px, 100%); max-height: 90vh; overflow: auto; padding: 22px; border-radius: 14px; background: var(--bg-card); }.dialog label { display: grid; gap: 7px; margin-top: 14px; font-weight: 800; }.dialog input,.dialog textarea { padding: 11px; border: 1px solid var(--border); border-radius: 9px; font: inherit; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }.dialog dl div { margin: 10px 0; }.dialog dt { color: var(--text-secondary); }.dialog dd { margin: 3px 0 0; font-weight: 700; }
@media (max-width: 700px) { .toolbar { align-items: flex-start; flex-direction: column; }.page { padding: 14px; }.filters { flex-direction: column; }.filters input { width: 100%; } }
</style>
