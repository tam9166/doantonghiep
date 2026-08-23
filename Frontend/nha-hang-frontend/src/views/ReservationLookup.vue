<template>
  <CustomerLayout>
    <main class="lookup-page">
      <section class="lookup-shell">
        <header class="lookup-header">
          <p class="eyebrow">Mộc Vị Reservation</p>
          <h1>Tra cứu đặt bàn</h1>
          <p>Nhập mã đặt bàn và số điện thoại để xem trạng thái xác nhận, tiền cọc và mã QR thanh toán.</p>
        </header>

        <form class="lookup-card" @submit.prevent="lookupReservation">
          <label>
            Mã đặt bàn
            <input v-model.trim="form.code" type="text" autocomplete="off" placeholder="VD: RV20260705-001" />
          </label>
          <label>
            Số điện thoại
            <input v-model.trim="form.phone" type="tel" autocomplete="tel" placeholder="VD: 0912345678" />
          </label>
          <button class="primary-btn" type="submit" :disabled="loading">
            {{ loading ? 'Đang tra cứu...' : 'Tra cứu' }}
          </button>
        </form>

        <div v-if="error" class="error-box">{{ error }}</div>

        <section class="cancel-card">
          <div>
            <p class="eyebrow">Yêu cầu hủy đặt bàn / hoàn cọc</p>
            <h2>Không nhớ mã đặt bàn vẫn có thể gửi yêu cầu</h2>
            <p>Nhập ít nhất hai thông tin chính xác của cùng một đặt bàn. Nhà hàng sẽ xem xét trước khi hủy.</p>
          </div>
          <form class="cancel-form" @submit.prevent="submitCancellationRequest">
            <label>Mã đặt bàn (không bắt buộc)<input v-model="cancelForm.reservationCode" maxlength="30" /></label>
            <label>Họ tên<input v-model="cancelForm.customerName" maxlength="150" autocomplete="name" /></label>
            <label>Số điện thoại<input v-model="cancelForm.customerPhone" maxlength="30" autocomplete="tel" /></label>
            <label>Email<input v-model="cancelForm.customerEmail" maxlength="150" type="email" autocomplete="email" /></label>
            <label class="cancel-reason">Lý do hủy<textarea v-model="cancelForm.reason" maxlength="1000" rows="3" /></label>
            <button class="danger-btn" type="submit" :disabled="cancelLoading">
              {{ cancelLoading ? 'Đang gửi...' : 'Gửi yêu cầu hủy' }}
            </button>
          </form>
          <div v-if="cancelMessage" class="success-box">{{ cancelMessage }}</div>
          <div v-if="cancelError" class="error-box">{{ cancelError }}</div>
        </section>

        <section v-if="reservation" class="result-card">
          <div class="realtime-box" :class="{ connected: realtimeConnected }">
            <span>{{ realtimeConnected ? 'Đang theo dõi realtime' : 'Đang kết nối realtime...' }}</span>
            <strong v-if="realtimeMessage">{{ realtimeMessage }}</strong>
          </div>

          <div class="result-head">
            <div>
              <p class="eyebrow">Kết quả</p>
              <h2>{{ reservation.reservationCode }}</h2>
            </div>
            <span class="status-badge" :class="reservation.reservationStatus">{{ statusText(reservation.reservationStatus) }}</span>
          </div>

          <div class="summary-grid">
            <div><span>Khách hàng</span><strong>{{ reservation.customerName }}</strong></div>
            <div><span>Số điện thoại</span><strong>{{ reservation.customerPhone }}</strong></div>
            <div><span>Ngày giờ</span><strong>{{ reservation.reservationDate }} {{ reservation.arrivalTime }}</strong></div>
            <div><span>Số khách</span><strong>{{ reservation.guestCount }}</strong></div>
            <div><span>Bàn</span><strong>{{ reservation.tableName || '-' }}</strong></div>
            <div><span>Khu vực</span><strong>{{ reservation.areaName || reservation.tableFloor || '-' }}</strong></div>
            <div><span>Tổng tiền</span><strong>{{ money(reservation.totalAmount) }}</strong></div>
            <div><span>Tiền cọc yêu cầu</span><strong>{{ money(reservation.depositAmount) }}</strong></div>
            <div><span>Đã thanh toán</span><strong>{{ money(reservation.paidAmount) }}</strong></div>
            <div><span>Cần thanh toán ngay</span><strong>{{ money(reservation.amountDueNow) }}</strong></div>
            <div><span>Còn lại</span><strong>{{ money(reservation.remainingAmount) }}</strong></div>
            <div><span>Thanh toán</span><strong>{{ paymentStatusText(reservation.paymentStatus) }}</strong></div>
          </div>

          <article v-if="latestPayment" class="qr-card">
            <div>
              <h3>QR thanh toán</h3>
              <dl>
                <div><dt>Mã giao dịch</dt><dd>{{ latestPayment.paymentCode }}</dd></div>
                <div><dt>Ngân hàng</dt><dd>{{ latestPayment.bankCode }}</dd></div>
                <div><dt>Số tài khoản</dt><dd>{{ latestPayment.accountNumber }}</dd></div>
                <div><dt>Chủ tài khoản</dt><dd>{{ latestPayment.accountHolder }}</dd></div>
                <div><dt>Nội dung</dt><dd>{{ latestPayment.transferContent }}</dd></div>
                <div><dt>Hết hạn</dt><dd>{{ formatDateTime(latestPayment.expiresAt) }}</dd></div>
              </dl>
              <button v-if="canRegenerateQr" class="ghost-btn" type="button" @click="regenerateQr" :disabled="qrLoading">
                {{ qrLoading ? 'Đang tạo QR...' : 'Tạo lại QR' }}
              </button>
            </div>
            <img :src="latestPayment.qrUrl" alt="QR thanh toán" />
          </article>

          <div v-else-if="canCreateQr" class="payment-action">
            <div>
              <strong>Chưa có QR thanh toán</strong>
              <p>Tạo QR để thanh toán tiền cọc/số tiền cần thanh toán ngay.</p>
            </div>
            <button class="primary-btn" type="button" @click="createQr" :disabled="qrLoading">
              {{ qrLoading ? 'Đang tạo QR...' : 'Tạo QR thanh toán' }}
            </button>
          </div>

          <section v-if="reservation.preorderItems?.length" class="preorder-box">
            <h3>Món đặt trước</h3>
            <div v-for="item in reservation.preorderItems" :key="item.id" class="preorder-row">
              <strong>{{ item.productName }}</strong>
              <span>{{ item.quantity }} x {{ money(item.unitPrice) }}</span>
              <span>{{ money(item.lineTotal) }}</span>
            </div>
          </section>

          <section v-if="canReview" class="review-box">
            <div class="review-head">
              <div>
                <h3>Đánh giá trải nghiệm</h3>
                <p>Cảm ơn bạn đã dùng bữa tại Mộc Vị. Đánh giá giúp nhà hàng phục vụ tốt hơn.</p>
              </div>
              <span v-if="myReview" class="review-done">Đã gửi đánh giá</span>
            </div>
            <div v-if="myReview" class="submitted-review">
              <strong>{{ myReview.overallRating }}/5 sao</strong>
              <p>{{ myReview.content || 'Khách hàng không để lại bình luận.' }}</p>
              <small v-if="myReview.adminReply">Phản hồi nhà hàng: {{ myReview.adminReply }}</small>
            </div>
            <form v-else class="review-form" @submit.prevent="submitReview">
              <div class="rating-grid">
                <label> Tổng quan <select v-model.number="reviewForm.overallRating"><option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option></select></label>
                <label> Món ăn <select v-model.number="reviewForm.foodRating"><option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option></select></label>
                <label> Phục vụ <select v-model.number="reviewForm.serviceRating"><option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option></select></label>
                <label> Không gian <select v-model.number="reviewForm.ambienceRating"><option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option></select></label>
                <label> Vệ sinh <select v-model.number="reviewForm.cleanlinessRating"><option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option></select></label>
              </div>
              <label>
                Nội dung đánh giá
                <textarea v-model.trim="reviewForm.content" rows="4" placeholder="Chia sẻ cảm nhận của bạn..." />
              </label>
              <label class="inline-check">
                <input v-model="reviewForm.anonymous" type="checkbox" />
                Gửi đánh giá ẩn danh
              </label>
              <button class="primary-btn" type="submit" :disabled="reviewLoading">
                {{ reviewLoading ? 'Đang gửi...' : 'Gửi đánh giá' }}
              </button>
            </form>
          </section>
        </section>
      </section>
    </main>
  </CustomerLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useRoute, useRouter } from 'vue-router'
import CustomerLayout from '@/components/CustomerLayout.vue'
import api from '@/services/api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const qrLoading = ref(false)
const error = ref('')
const cancelError = ref('')
const cancelMessage = ref('')
const cancelLoading = ref(false)
const reservation = ref(null)
const form = ref({ code: '', phone: '' })
const cancelForm = ref({ reservationCode: '', customerName: '', customerPhone: '', customerEmail: '', reason: '' })
const realtimeConnected = ref(false)
const realtimeMessage = ref('')
const reviewLoading = ref(false)
const myReview = ref(null)
const reviewForm = ref({
  overallRating: 5,
  foodRating: 5,
  serviceRating: 5,
  ambienceRating: 5,
  cleanlinessRating: 5,
  content: '',
  anonymous: false
})
let stompClient = null
let realtimeTimer = null

const latestPayment = computed(() => reservation.value?.payments?.[0] || null)
const paymentCapability = computed(() => reservation.value?.reservationCode
  ? sessionStorage.getItem(`reservation-capability:${reservation.value.reservationCode}`)
  : '')
const hasPaymentAccess = computed(() => Boolean(paymentCapability.value || sessionStorage.getItem('token')))
const activeStatuses = ['PENDING', 'CONFIRMED', 'DEPOSIT_REQUIRED', 'DEPOSIT_PENDING', 'DEPOSIT_PAID']
const canCreateQr = computed(() => reservation.value
  && Number(reservation.value.amountDueNow || 0) > 0
  && reservation.value.paymentOption !== 'PAY_AT_RESTAURANT'
  && hasPaymentAccess.value
  && activeStatuses.includes(reservation.value.reservationStatus))
const canRegenerateQr = computed(() => latestPayment.value && ['PENDING', 'EXPIRED'].includes(latestPayment.value.status))
const canReview = computed(() => reservation.value?.reservationStatus === 'COMPLETED')

const money = value => new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0
}).format(Number(value || 0))

const formatDateTime = value => value ? new Date(value).toLocaleString('vi-VN') : '-'

function statusText(status) {
  const map = {
    PENDING: 'Chờ xác nhận',
    CONFIRMED: 'Đã xác nhận',
    DEPOSIT_REQUIRED: 'Cần thanh toán cọc',
    DEPOSIT_PENDING: 'Đang chờ cọc',
    DEPOSIT_PAID: 'Đã cọc',
    FULLY_PAID: 'Đã thanh toán đủ',
    CHECKED_IN: 'Đã đến',
    IN_SERVICE: 'Đang phục vụ',
    COMPLETED: 'Hoàn thành',
    CANCELLED: 'Đã hủy',
    REJECTED: 'Từ chối',
    EXPIRED: 'Quá hạn',
    NO_SHOW: 'Không đến'
  }
  return map[status] || status || '-'
}

function paymentStatusText(status) {
  const map = {
    PENDING: 'Chờ thanh toán',
    UNPAID: 'Chưa thanh toán',
    PARTIALLY_PAID: 'Đã thanh toán một phần',
    PAID: 'Đã thanh toán',
    OVERPAID: 'Thanh toán dư',
    EXPIRED: 'Hết hạn',
    CANCELLED: 'Đã hủy'
  }
  return map[status] || status || '-'
}

async function lookupReservation() {
  error.value = ''
  reservation.value = null
  if (!form.value.code || !form.value.phone) {
    error.value = 'Vui lòng nhập cả mã đặt bàn và số điện thoại.'
    return
  }
  loading.value = true
  try {
    const res = await api.post('/api/reservations/lookup', {
      reservationCode: form.value.code.trim(),
      customerPhone: form.value.phone.replace(/\s/g, '')
    })
    reservation.value = res.data
    router.replace({ path: '/reservation-lookup', query: {
      code: form.value.code || undefined
    } })
    cancelForm.value = {
      ...cancelForm.value,
      reservationCode: reservation.value.reservationCode || '',
      customerName: reservation.value.customerName || '',
      customerPhone: form.value.phone || '',
      customerEmail: reservation.value.customerEmail || ''
    }
    connectRealtime(reservation.value.reservationCode)
    await loadMyReview()
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không tìm thấy đặt bàn phù hợp.'
  } finally {
    loading.value = false
  }
}

async function submitCancellationRequest() {
  cancelError.value = ''
  cancelMessage.value = ''
  const verificationValues = [
    cancelForm.value.reservationCode,
    cancelForm.value.customerName,
    cancelForm.value.customerPhone,
    cancelForm.value.customerEmail
  ].filter(value => String(value || '').trim())
  if (verificationValues.length < 2) {
    cancelError.value = 'Vui lòng nhập ít nhất 2 trong 4 thông tin xác minh.'
    return
  }
  cancelLoading.value = true
  try {
    const response = await api.post('/api/reservation-cancellations', cancelForm.value)
    cancelMessage.value = `Đã ghi nhận yêu cầu ${response.data.requestCode}. Nhà hàng sẽ liên hệ sau khi xem xét.`
  } catch (err) {
    cancelError.value = err.response?.data?.message
      || 'Không thể xác minh thông tin đặt bàn. Vui lòng kiểm tra lại thông tin đã nhập.'
  } finally {
    cancelLoading.value = false
  }
}

async function refreshReservationSilently() {
  if (!form.value.code || !form.value.phone) return
  try {
    const res = await api.post('/api/reservations/lookup', {
      reservationCode: form.value.code.trim(),
      customerPhone: form.value.phone.replace(/\s/g, '')
    })
    reservation.value = res.data
    if (canReview.value) loadMyReview()
  } catch {
    // Keep the last visible state; the next manual lookup can surface errors.
  }
}

async function loadMyReview() {
  myReview.value = null
  if (!reservation.value?.reservationCode || !form.value.phone || !canReview.value) return
  try {
    // P0-02: Use POST body instead of GET path params to avoid PII in URL
    const res = await api.post('/api/reservation-reviews/mine', {
      reservationCode: reservation.value.reservationCode,
      customerPhone: form.value.phone.replace(/\s/g, '')
    })
    myReview.value = res.data
  } catch {
    myReview.value = null
  }
}

async function submitReview() {
  if (!reservation.value) return
  reviewLoading.value = true
  error.value = ''
  try {
    const res = await api.post('/api/reservation-reviews', {
      reservationCode: reservation.value.reservationCode,
      customerPhone: form.value.phone.replace(/\s/g, ''),
      ...reviewForm.value
    })
    myReview.value = res.data
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không gửi được đánh giá.'
  } finally {
    reviewLoading.value = false
  }
}

function handleRealtimeEvent(event) {
  if (!event) return
  if (event.reservation) {
    reservation.value = event.reservation
  } else {
    refreshReservationSilently()
  }
  realtimeMessage.value = event.message || 'Trạng thái đặt bàn vừa được cập nhật'
  window.clearTimeout(realtimeTimer)
  realtimeTimer = window.setTimeout(() => {
    realtimeMessage.value = ''
  }, 7000)
}

function disconnectRealtime() {
  realtimeConnected.value = false
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

function connectRealtime(code) {
  disconnectRealtime()
  if (!code) return
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: sessionStorage.getItem('token')
      ? { Authorization: `Bearer ${sessionStorage.getItem('token')}` }
      : {},
    reconnectDelay: 5000,
    onConnect: () => {
      realtimeConnected.value = true
      const capability = sessionStorage.getItem(`reservation-capability:${code}`)
      const headers = capability ? { 'X-Reservation-Capability': capability } : {}
      stompClient.subscribe(`/topic/reservations/${code}`, message => {
        try {
          handleRealtimeEvent(JSON.parse(message.body))
        } catch {
          realtimeMessage.value = 'Có cập nhật mới, vui lòng bấm tra cứu lại nếu cần.'
        }
      }, headers)
    },
    onWebSocketClose: () => {
      realtimeConnected.value = false
    },
    onStompError: () => {
      realtimeConnected.value = false
    }
  })
  stompClient.activate()
}

async function createQr() {
  if (!reservation.value) return
  qrLoading.value = true
  error.value = ''
  try {
    const res = await api.post('/api/payments/qr', {
      reservationCode: reservation.value.reservationCode,
      paymentOption: reservation.value.paymentOption
    }, { headers: paymentRequestHeaders() })
    reservation.value.payments = [res.data, ...(reservation.value.payments || [])]
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không tạo được QR thanh toán.'
  } finally {
    qrLoading.value = false
  }
}

async function regenerateQr() {
  if (!latestPayment.value?.paymentCode) return
  qrLoading.value = true
  error.value = ''
  try {
    const res = await api.post(
      `/api/payments/${latestPayment.value.paymentCode}/regenerate`,
      null,
      { headers: paymentRequestHeaders() }
    )
    reservation.value.payments = [res.data, ...(reservation.value.payments || []).filter(item => item.paymentCode !== res.data.paymentCode)]
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không tạo lại được QR thanh toán.'
  } finally {
    qrLoading.value = false
  }
}

function paymentRequestHeaders() {
  const headers = { 'X-Idempotency-Key': crypto.randomUUID() }
  if (paymentCapability.value) headers['X-Payment-Capability'] = paymentCapability.value
  return headers
}

onMounted(() => {
  form.value.code = String(route.query.code || '')
  cancelForm.value.reservationCode = form.value.code
})

onBeforeUnmount(() => {
  window.clearTimeout(realtimeTimer)
  disconnectRealtime()
})
</script>

<style scoped>
.lookup-page {
  min-height: 100vh;
  background: var(--color-background);
  color: var(--color-on-surface);
  padding: 32px 16px 56px;
}

.lookup-shell {
  max-width: 1060px;
  margin: 0 auto;
}

.lookup-header {
  margin-bottom: 20px;
}

.eyebrow {
  margin: 0 0 6px;
  color: var(--color-primary);
  font-weight: 900;
  text-transform: uppercase;
  font-size: 0.78rem;
}

.lookup-header h1,
.result-head h2 {
  margin: 0;
  color: var(--color-on-background);
}

.lookup-header p {
  max-width: 720px;
  color: var(--color-on-surface-variant);
}

.lookup-card,
.result-card {
  background: var(--color-surface-container-lowest);
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
  box-shadow: var(--shadow-lg);
}

.lookup-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: end;
  padding: 18px;
  margin-bottom: 16px;
}

label {
  display: grid;
  gap: 7px;
  color: var(--color-on-surface);
  font-weight: 800;
}

input {
  width: 100%;
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
  padding: 11px 12px;
  font: inherit;
}

select,
textarea {
  width: 100%;
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
  padding: 11px 12px;
  font: inherit;
  background: var(--color-surface-container-lowest);
}

.primary-btn,
.ghost-btn {
  min-height: 42px;
  border-radius: 8px;
  padding: 0 16px;
  font-weight: 900;
  cursor: pointer;
}

.primary-btn {
  border: 1px solid var(--color-primary);
  background: var(--color-primary);
  color: var(--color-on-primary);
}

.ghost-btn {
  border: 1px solid var(--color-outline-variant);
  background: var(--color-surface-container-lowest);
  color: var(--color-on-surface);
  margin-top: 12px;
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.error-box {
  background: var(--color-error-container);
  color: var(--color-on-error-container);
  border: 1px solid #ffb4ab;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.result-card {
  padding: 22px;
}

.realtime-box {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--color-secondary-fixed-dim);
  background: var(--color-secondary-fixed);
  color: var(--color-on-secondary-fixed);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 14px;
  font-weight: 800;
}

.realtime-box.connected {
  border-color: #a9e3bd;
  background: #e0f5e5;
  color: #005326;
}

.realtime-box strong {
  color: inherit;
  text-align: right;
}

.result-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.status-badge {
  border-radius: 999px;
  padding: 6px 12px;
  background: var(--color-primary-fixed);
  color: var(--color-on-primary-fixed);
  font-weight: 900;
  white-space: nowrap;
}

.status-badge.CANCELLED,
.status-badge.REJECTED,
.status-badge.EXPIRED,
.status-badge.NO_SHOW {
  background: var(--color-error-container);
  color: var(--color-on-error-container);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.summary-grid > div {
  display: grid;
  gap: 4px;
  background: var(--color-surface-container-low);
  border-radius: 8px;
  padding: 12px;
}

.summary-grid span,
.qr-card dt,
.payment-action p {
  color: var(--color-on-surface-variant);
}

.qr-card {
  display: grid;
  grid-template-columns: 1fr 240px;
  gap: 18px;
  margin-top: 18px;
  padding: 18px;
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
}

.qr-card img {
  width: 240px;
  height: 240px;
  object-fit: contain;
}

.qr-card dl {
  display: grid;
  gap: 8px;
  margin: 0;
}

.qr-card dl div {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 10px;
}

.payment-action {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  margin-top: 18px;
  border: 1px dashed var(--color-outline-variant);
  border-radius: 8px;
  padding: 16px;
}

.preorder-box {
  margin-top: 18px;
}

.preorder-row {
  display: grid;
  grid-template-columns: 1fr 140px 140px;
  gap: 10px;
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
  padding: 10px;
  margin-top: 8px;
}

.review-box {
  margin-top: 18px;
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
  padding: 18px;
  background: var(--color-surface-container-low);
}

.review-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.review-head h3,
.review-head p {
  margin: 0;
}

.review-head p {
  color: var(--color-on-surface-variant);
  margin-top: 4px;
}

.review-done {
  border-radius: 999px;
  background: var(--color-primary-fixed);
  color: var(--color-on-primary-fixed);
  padding: 6px 10px;
  font-weight: 900;
  white-space: nowrap;
}

.rating-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.inline-check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 12px 0;
}

.inline-check input {
  width: auto;
}

.submitted-review {
  background: var(--color-surface-container-lowest);
  border: 1px solid var(--color-outline-variant);
  border-radius: 8px;
  padding: 14px;
}

.submitted-review p {
  color: var(--color-on-surface);
}

.submitted-review small {
  display: block;
  color: var(--color-on-surface);
  font-weight: 800;
}

.cancel-card {
  margin-top: 22px;
  padding: 24px;
  border: 1px solid var(--color-outline-variant);
  border-radius: 18px;
  background: var(--color-surface);
}
.cancel-card h2 { margin: 4px 0 8px; }
.cancel-card p { color: var(--color-on-surface-variant); }
.cancel-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}
.cancel-form label { display: grid; gap: 7px; font-weight: 700; }
.cancel-form input, .cancel-form textarea {
  border: 1px solid var(--color-outline-variant);
  border-radius: 10px;
  padding: 11px 12px;
  font: inherit;
}
.cancel-reason { grid-column: 1 / -1; }
.danger-btn {
  justify-self: start;
  border: 0;
  border-radius: 10px;
  padding: 12px 18px;
  background: var(--color-error);
  color: var(--color-on-error);
  font-weight: 800;
  cursor: pointer;
}
.danger-btn:disabled { opacity: .6; cursor: wait; }
.success-box {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--color-secondary-fixed);
  color: var(--success);
}

@media (max-width: 780px) {
  .lookup-card,
  .summary-grid,
  .qr-card,
  .preorder-row,
  .rating-grid {
    grid-template-columns: 1fr;
  }

  .result-head,
  .payment-action,
  .realtime-box,
  .review-head {
    display: grid;
  }

  .realtime-box strong {
    text-align: left;
  }

  .qr-card img {
    width: 100%;
    height: auto;
  }

  .qr-card dl div {
    grid-template-columns: 1fr;
  }

  .cancel-form { grid-template-columns: 1fr; }
  .cancel-reason { grid-column: auto; }
}
</style>
