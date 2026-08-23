<template>
  <AdminLayout>
    <section class="page">
      <header class="toolbar">
        <div><h1>Yêu cầu hủy đặt bàn</h1><p>Duyệt hủy và theo dõi hoàn cọc dựa trên giao dịch thực tế.</p></div>
        <button type="button" @click="load" :disabled="loading">{{ loading ? 'Đang tải...' : 'Làm mới' }}</button>
      </header>
      <div v-if="error" class="message error">{{ error }}</div>
      <div v-if="message" class="message success">{{ message }}</div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Mã yêu cầu / booking</th><th>Khách hàng</th><th>Thời gian đặt</th><th>Tiền cọc</th><th>Chính sách</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
          <tbody>
            <tr v-for="item in requests" :key="item.id">
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
            <tr v-if="!requests.length"><td colspan="7" class="empty">Chưa có yêu cầu hủy.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <div v-if="dialog" class="modal" @click.self="closeDialog">
      <form class="dialog" @submit.prevent="submitDecision">
        <h2>{{ dialogTitle }}</h2>
        <p>{{ dialog.requestCode }} · {{ dialog.reservationCode }}</p>
        <label v-if="decisionType === 'complete'">Mã giao dịch hoàn tiền thực tế<input v-model.trim="providerReference" maxlength="120" required /></label>
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
        <div><dt>Người xử lý</dt><dd>{{ selected.processedBy || '-' }} · {{ dateTime(selected.processedAt) }}</dd></div>
        <div><dt>Ghi chú</dt><dd>{{ selected.processingNote || '-' }}</dd></div>
      </dl><button type="button" @click="selected = null">Đóng</button></article>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import api from '@/services/api'

const requests = ref([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const message = ref('')
const dialog = ref(null)
const selected = ref(null)
const decisionType = ref('')
const decisionNote = ref('')
const providerReference = ref('')
const dialogTitle = computed(() => ({ approve: 'Duyệt yêu cầu hủy', reject: 'Từ chối yêu cầu', complete: 'Xác nhận hoàn tiền thực tế' }[decisionType.value]))

const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value || 0))
const dateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '-'
const statusText = status => ({ PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', REFUND_PENDING: 'Chờ hoàn tiền', REFUNDED: 'Đã hoàn tiền', REFUND_FAILED: 'Hoàn tiền lỗi' }[status] || status)

async function load() {
  loading.value = true; error.value = ''
  try { requests.value = (await api.get('/api/admin/reservation-cancellations')).data }
  catch (err) { error.value = err.response?.data?.message || 'Không tải được yêu cầu hủy.' }
  finally { loading.value = false }
}
function openDecision(item, type) { dialog.value = item; decisionType.value = type; decisionNote.value = ''; providerReference.value = '' }
function closeDialog() { dialog.value = null }
async function submitDecision() {
  if (!dialog.value || saving.value) return
  saving.value = true; error.value = ''; message.value = ''
  try {
    const base = `/api/admin/reservation-cancellations/${dialog.value.id}`
    const body = decisionType.value === 'complete'
      ? { providerReference: providerReference.value, note: decisionNote.value }
      : { note: decisionNote.value }
    await api.patch(`${base}/${decisionType.value === 'complete' ? 'refund-complete' : decisionType.value}`, body)
    message.value = 'Đã cập nhật yêu cầu hủy an toàn.'
    closeDialog(); await load()
  } catch (err) { error.value = err.response?.data?.message || 'Không thể xử lý yêu cầu.' }
  finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.page { padding: 24px; color: var(--text-primary); }
.toolbar { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-bottom: 20px; }
.toolbar h1 { margin: 0; }.toolbar p { color: var(--text-secondary); }
button { border: 0; border-radius: 9px; padding: 9px 13px; background: var(--primary); color: var(--color-on-primary); font-weight: 800; cursor: pointer; }
button:disabled { opacity: .6; }.danger { background: var(--danger); }.ghost { background: var(--bg-card2); color: var(--text-primary); }
.table-wrap { overflow: auto; border: 1px solid var(--border); border-radius: 14px; background: var(--bg-card); }
table { width: 100%; border-collapse: collapse; min-width: 1180px; }th, td { padding: 13px; text-align: left; border-bottom: 1px solid var(--border-light); vertical-align: top; }th { background: var(--bg-card2); }
td strong, td span, td small { display: block; margin-bottom: 4px; }.actions { display: flex; flex-wrap: wrap; gap: 7px; }.empty { text-align: center; padding: 32px; }
.badge { display: inline-block; border-radius: 999px; padding: 6px 10px; background: var(--color-secondary-fixed); }.badge.REJECTED,.badge.REFUND_FAILED { background: var(--color-error-container); color: var(--color-on-error-container); }.badge.REFUNDED,.badge.APPROVED { color: var(--success); }
.message { margin: 12px 0; padding: 12px; border-radius: 9px; }.error { background: var(--color-error-container); color: var(--color-on-error-container); }.success { background: var(--color-secondary-fixed); color: var(--success); }
.modal { position: fixed; inset: 0; z-index: 1000; display: grid; place-items: center; padding: 20px; background: var(--overlay-dark); }.dialog { width: min(600px, 100%); max-height: 90vh; overflow: auto; padding: 22px; border-radius: 14px; background: var(--bg-card); }.dialog label { display: grid; gap: 7px; margin-top: 14px; font-weight: 800; }.dialog input,.dialog textarea { padding: 11px; border: 1px solid var(--border); border-radius: 9px; font: inherit; }.dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 18px; }.dialog dl div { margin: 10px 0; }.dialog dt { color: var(--text-secondary); }.dialog dd { margin: 3px 0 0; font-weight: 700; }
@media (max-width: 700px) { .toolbar { align-items: flex-start; flex-direction: column; }.page { padding: 14px; } }
</style>
