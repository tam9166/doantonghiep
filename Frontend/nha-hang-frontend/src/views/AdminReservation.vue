<template>
  <AdminLayout>
    <section class="admin-reservation">
      <div class="toolbar">
        <div>
          <h1>Quản lý đặt bàn</h1>
          <p>Xác nhận yêu cầu, theo dõi tiền cọc và trạng thái khách đến.</p>
        </div>
        <button class="primary-btn" type="button" @click="refreshAdminData" :disabled="loading">
          {{ loading ? 'Đang tải...' : 'Làm mới' }}
        </button>
      </div>
      <div v-if="realtimeMessage" class="realtime-alert">{{ realtimeMessage }}</div>

      <div class="filters">
        <div class="search-control">
          <input
            v-model.trim="keywordInput"
            type="search"
            placeholder="Tìm mã, tên khách, số điện thoại, bàn hoặc khu vực..."
            aria-label="Tìm kiếm đặt bàn"
            @keydown.esc="clearSearch"
          />
          <button v-if="keywordInput" type="button" class="clear-search" aria-label="Xóa từ khóa tìm kiếm" @click="clearSearch">×</button>
        </div>
        <select v-model="statusFilter">
          <option value="">Tất cả trạng thái</option>
          <option v-for="status in statuses" :key="status" :value="status">{{ statusText(status) }}</option>
        </select>
      </div>

      <div class="status-tabs">
        <button
          v-for="group in groups"
          :key="group.key"
          type="button"
          :class="{ active: statusFilter === group.key }"
          @click="statusFilter = statusFilter === group.key ? '' : group.key"
        >
          <span>{{ countByStatus(group.key) }}</span>{{ group.label }}
        </button>
      </div>

      <div v-if="error" class="error">{{ error }}</div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã đặt bàn</th>
              <th>Khách hàng</th>
              <th>Thời gian</th>
              <th>Bàn / khu vực</th>
              <th>Tiền cọc</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredReservations" :key="item.id">
              <td>
                <strong>{{ item.reservationCode }}</strong>
                <small>{{ formatDateTime(item.createdAt) }}</small>
              </td>
              <td>
                <strong>{{ item.customerName }}</strong>
                <span>{{ item.customerPhone }}</span>
                <span v-if="item.customerEmail">{{ item.customerEmail }}</span>
              </td>
              <td>
                <strong>{{ item.reservationDate }}</strong>
                <span>{{ item.arrivalTime }} · {{ item.expectedDurationMinutes }} phút</span>
                <span>{{ item.guestCount }} khách</span>
              </td>
              <td>
                <strong>{{ tableNames(item) }}</strong>
                <span>{{ item.areaName || item.tableFloor }}</span>
              </td>
              <td>
                <strong>{{ money(item.depositAmount) }}</strong>
                <span>{{ money(item.totalAmount) }} tổng</span>
                <span>{{ item.depositStatus }}</span>
              </td>
              <td>
                <span class="status-badge" :class="item.reservationStatus">{{ statusText(item.reservationStatus) }}</span>
              </td>
              <td>
                <div class="row-actions">
                  <button v-if="item.reservationStatus === 'PENDING'" type="button" @click="confirmReservation(item)">Xác nhận</button>
                  <button v-if="item.reservationStatus === 'WAITING_TABLE_ASSIGNMENT'" type="button" @click="openAssignment(item)">Bố trí bàn</button>
                  <button v-if="['PENDING','WAITING_TABLE_ASSIGNMENT'].includes(item.reservationStatus)" type="button" class="danger" @click="rejectReservation(item)">Từ chối</button>
                  <button v-if="['DEPOSIT_REQUIRED','DEPOSIT_PENDING'].includes(item.reservationStatus)" type="button" @click="markDeposit(item)">Đã cọc</button>
                  <button v-if="['CONFIRMED','DEPOSIT_REQUIRED','DEPOSIT_PENDING','DEPOSIT_PAID','FULLY_PAID'].includes(item.reservationStatus)" type="button" @click="checkIn(item)">Check-in</button>
                  <button v-if="!['CANCELLED','REJECTED','COMPLETED'].includes(item.reservationStatus)" type="button" class="ghost" @click="cancelReservation(item)">Hủy</button>
                  <button type="button" class="ghost" @click="refreshDetail(item)">Chi tiết</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredReservations.length === 0">
              <td colspan="7" class="empty">Chưa có yêu cầu đặt bàn phù hợp.</td>
            </tr>
          </tbody>
        </table>
      </div>

      <section class="waitlist-panel">
        <div class="section-heading">
          <div>
            <h2>Danh sách chờ</h2>
            <p>Khách chưa chọn được bàn phù hợp và đang chờ nhà hàng liên hệ.</p>
          </div>
          <strong>{{ activeWaitlist.length }} đang chờ</strong>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Mã chờ</th>
                <th>Khách hàng</th>
                <th>Khung giờ</th>
                <th>Khu vực</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in waitlist" :key="item.id">
                <td>
                  <strong>{{ item.waitlistCode }}</strong>
                  <small>{{ formatDateTime(item.createdAt) }}</small>
                </td>
                <td>
                  <strong>{{ item.customerName }}</strong>
                  <span>{{ item.customerPhone }}</span>
                  <span v-if="item.customerEmail">{{ item.customerEmail }}</span>
                </td>
                <td>
                  <strong>{{ item.reservationDate }}</strong>
                  <span>{{ item.preferredStartTime }} - {{ item.preferredEndTime }}</span>
                  <span>{{ item.guestCount }} khách</span>
                </td>
                <td>
                  <strong>{{ item.areaName || '-' }}</strong>
                  <span>{{ item.seatingPreference || '-' }}</span>
                </td>
                <td>
                  <span class="status-badge" :class="item.status">{{ waitlistStatusText(item.status) }}</span>
                </td>
                <td>
                  <div class="row-actions">
                    <button v-if="['WAITING','CONTACTED'].includes(item.status)" type="button" @click="contactWaitlist(item)">Đã liên hệ</button>
                    <button v-if="['WAITING','CONTACTED'].includes(item.status)" type="button" @click="convertWaitlist(item)">Đã chuyển đặt bàn</button>
                    <button v-if="['WAITING','CONTACTED'].includes(item.status)" type="button" class="ghost" @click="cancelWaitlist(item)">Hủy</button>
                  </div>
                </td>
              </tr>
              <tr v-if="waitlist.length === 0">
                <td colspan="6" class="empty">Chưa có khách trong danh sách chờ.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <div v-if="selected" class="modal" @click.self="selected = null">
        <article class="detail-panel">
          <header>
            <h2>{{ selected.reservationCode }}</h2>
            <button type="button" @click="selected = null">Đóng</button>
          </header>
          <div class="detail-grid">
            <div><span>Khách</span><strong>{{ selected.customerName }}</strong></div>
            <div><span>SĐT</span><strong><a :href="`tel:${selected.customerPhone}`">{{ selected.customerPhone }}</a></strong></div>
            <div><span>Email</span><strong>{{ selected.customerEmail || '-' }}</strong></div>
            <div><span>Ngày giờ</span><strong>{{ selected.reservationDate }} {{ selected.arrivalTime }}</strong></div>
            <div><span>Dịp</span><strong>{{ selected.occasion || '-' }}</strong></div>
            <div><span>Sở thích vị trí</span><strong>{{ selected.seatingPreference || '-' }}</strong></div>
            <div><span>Yêu cầu</span><strong>{{ selected.specialRequest || '-' }}</strong></div>
            <div><span>Hình thức thanh toán</span><strong>{{ paymentOptionText(selected.paymentOption) }}</strong></div>
            <div><span>Tiền bàn</span><strong>{{ money(selected.tableAmount) }}</strong></div>
            <div><span>Tiền món</span><strong>{{ money(selected.foodAmount) }}</strong></div>
            <div><span>Tiền cọc yêu cầu</span><strong>{{ money(selected.depositAmount) }}</strong></div>
            <div><span>Đã thanh toán</span><strong>{{ money(selected.paidAmount) }}</strong></div>
            <div><span>Cần thanh toán ngay</span><strong>{{ money(selected.amountDueNow) }}</strong></div>
            <div><span>Còn lại</span><strong>{{ money(selected.remainingAmount) }}</strong></div>
            <div><span>Trạng thái thanh toán</span><strong>{{ paymentStatusText(selected.paymentStatus) }}</strong></div>
            <div><span>Lý do từ chối</span><strong>{{ selected.rejectedReason || '-' }}</strong></div>
            <div><span>Ghi chú nội bộ</span><strong>{{ selected.managerNote || '-' }}</strong></div>
            <div><span>Trạng thái gọi</span><strong>{{ contactStatusText(selected.contactStatus) }}</strong></div>
            <div><span>Người gọi</span><strong>{{ selected.contactCalledBy || '-' }}</strong></div>
            <div><span>Thời gian gọi</span><strong>{{ formatDateTime(selected.contactCalledAt) || '-' }}</strong></div>
            <div><span>Ghi chú cuộc gọi</span><strong>{{ selected.contactCallNote || '-' }}</strong></div>
            <div><span>Email biên nhận</span><strong>{{ selected.receiptEmailStatus || 'NOT_SENT' }}</strong></div>
            <div><span>Gửi lúc</span><strong>{{ formatDateTime(selected.receiptEmailSentAt) || '-' }}</strong></div>
          </div>
          <div class="detail-actions">
            <a class="ghost" :href="`tel:${selected.customerPhone}`">Gọi khách</a>
            <button type="button" @click="updateContact(selected, 'CONFIRMED_BY_CUSTOMER')">Khách xác nhận</button>
            <button type="button" @click="updateContact(selected, 'UNREACHABLE')">Không liên lạc được</button>
            <button type="button" @click="updateContact(selected, 'CHANGE_REQUESTED')">Yêu cầu thay đổi</button>
            <button type="button" class="ghost" @click="updateContact(selected, 'NOT_REQUIRED')">Không cần gọi</button>
            <button type="button" class="ghost" @click="resendReceipt(selected)">Gửi lại biên nhận</button>
          </div>
          <section v-if="selected.reservationStatus === 'WAITING_TABLE_ASSIGNMENT'" class="assignment-box">
            <h3>Bố trí bàn cho {{ selected.guestCount }} khách</h3>
            <p>Hệ thống chỉ đề xuất; quản lý xác nhận phương án cuối cùng.</p>
            <div v-if="assignmentLoading">Đang kiểm tra bàn trống...</div>
            <template v-else-if="assignmentOptions">
              <div class="recommendations">
                <button v-for="(option,index) in assignmentOptions.recommendedOptions || []" :key="option.join('-')" type="button" class="option-card" @click="selectOption(option)"><strong>Phương án {{ index + 1 }}</strong><span>{{ optionNames(option) }}</span><small>Tổng {{ optionCapacity(option) }} chỗ</small></button>
              </div>
              <h4>Chọn bàn thủ công</h4>
              <div class="manual-tables">
                <label v-for="table in assignmentOptions.availableTables" :key="table.tableId" :class="{ chosen: assignedTableIds.includes(table.tableId) }"><input v-model="assignedTableIds" type="checkbox" :value="table.tableId"><span><strong>{{ table.tableName }}</strong><small>{{ table.capacity }} chỗ · {{ table.floor || selected.areaName }}</small></span></label>
              </div>
              <p :class="{ 'capacity-error': selectedCapacity < selected.guestCount }">Đã chọn {{ assignedTableIds.length }} bàn · {{ selectedCapacity }}/{{ selected.guestCount }} chỗ</p>
              <textarea v-model="assignmentNote" rows="2" maxlength="500" placeholder="Ghi chú phương án bố trí"></textarea>
              <button type="button" :disabled="savingAssignment || selectedCapacity < selected.guestCount" @click="confirmAssignment">{{ savingAssignment ? 'Đang lưu...' : 'Xác nhận phương án bàn' }}</button>
            </template>
          </section>
          <h3>Món đặt trước</h3>
          <div v-if="selected.preorderItems?.length" class="preorder-list">
            <div v-for="dish in selected.preorderItems" :key="dish.id" class="preorder-row">
              <strong>{{ dish.productName }}</strong>
              <span>{{ dish.quantity }} x {{ money(dish.unitPrice) }}</span>
              <span>{{ money(dish.lineTotal) }}</span>
              <small>{{ dish.note || '-' }}</small>
            </div>
          </div>
          <p v-else class="empty-inline">Khách không đặt món trước.</p>
          <h3>Giao dịch QR</h3>
          <div v-if="selected.payments?.length" class="preorder-list">
            <div v-for="payment in selected.payments" :key="payment.paymentCode" class="preorder-row">
              <strong>{{ payment.paymentCode }}</strong>
              <span>{{ money(payment.amount) }}</span>
              <span>{{ payment.status }}</span>
              <small>{{ payment.transferContent }}</small>
            </div>
          </div>
          <p v-else class="empty-inline">Chưa tạo QR thanh toán.</p>
          <h3>Lịch sử trạng thái</h3>
          <ul>
            <li v-for="entry in selected.history || []" :key="entry">{{ entry }}</li>
          </ul>
        </article>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import AdminLayout from '@/components/AdminLayout.vue'
import { useDialog } from '@/composables/useDialog'
import api from '@/services/api'

const { promptDialog } = useDialog()

const reservations = ref([])
const waitlist = ref([])
const loading = ref(false)
const error = ref('')
const keywordInput = ref('')
const keyword = ref('')
const statusFilter = ref('')
const selected = ref(null)
const realtimeMessage = ref('')
const assignmentOptions = ref(null)
const assignedTableIds = ref([])
const assignmentNote = ref('')
const assignmentLoading = ref(false)
const savingAssignment = ref(false)
let stompClient = null
let realtimeTimer = null
let keywordTimer = null

const statuses = ['PENDING', 'WAITING_TABLE_ASSIGNMENT', 'CONFIRMED', 'DEPOSIT_REQUIRED', 'DEPOSIT_PENDING', 'DEPOSIT_PAID', 'FULLY_PAID', 'CHECKED_IN', 'IN_SERVICE', 'COMPLETED', 'CANCELLED', 'REJECTED', 'NO_SHOW', 'EXPIRED']
const groups = [
  { key: 'PENDING', label: 'Yêu cầu mới' },
  { key: 'WAITING_TABLE_ASSIGNMENT', label: 'Chờ bố trí bàn' },
  { key: 'DEPOSIT_REQUIRED', label: 'Cần cọc' },
  { key: 'DEPOSIT_PAID', label: 'Đã cọc' },
  { key: 'CHECKED_IN', label: 'Đã đến' },
  { key: 'CANCELLED', label: 'Đã hủy' },
  { key: 'REJECTED', label: 'Từ chối' }
]

const filteredReservations = computed(() => {
  const q = keyword.value.toLowerCase()
  return reservations.value.filter(item => {
    const matchStatus = !statusFilter.value || item.reservationStatus === statusFilter.value
    const haystack = `${item.reservationCode} ${item.customerName} ${item.customerPhone} ${item.customerEmail || ''} ${tableNames(item)} ${item.areaName || ''} ${item.tableFloor || ''}`.toLowerCase()
    return matchStatus && (!q || haystack.includes(q))
  })
})

function tableNames(reservation) {
  const names = (reservation.tables || []).map(table => table.tableName).filter(Boolean)
  return names.length ? names.join(' + ') : (reservation.tableName || 'Chưa xếp bàn')
}
const selectedCapacity = computed(() => (assignmentOptions.value?.availableTables || []).filter(t => assignedTableIds.value.includes(t.tableId)).reduce((sum, t) => sum + (t.capacity || 0), 0))
function optionNames(ids) { return (assignmentOptions.value?.availableTables || []).filter(t => ids.includes(t.tableId)).map(t => t.tableName).join(' + ') }
function optionCapacity(ids) { return (assignmentOptions.value?.availableTables || []).filter(t => ids.includes(t.tableId)).reduce((sum, t) => sum + (t.capacity || 0), 0) }
function selectOption(ids) { assignedTableIds.value = [...ids] }

watch(keywordInput, (value) => {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordTimer = setTimeout(() => { keyword.value = value }, 220)
})

function clearSearch() {
  if (keywordTimer) clearTimeout(keywordTimer)
  keywordInput.value = ''
  keyword.value = ''
}

const activeWaitlist = computed(() => waitlist.value.filter(item => ['WAITING', 'CONTACTED'].includes(item.status)))

const money = (value) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value || 0))
const formatDateTime = (value) => value ? new Date(value).toLocaleString('vi-VN') : ''
const countByStatus = (status) => reservations.value.filter(item => item.reservationStatus === status).length

function statusText(status) {
  const map = {
    PENDING: 'Chờ xác nhận',
    WAITING_TABLE_ASSIGNMENT: 'Chờ bố trí bàn',
    CONFIRMED: 'Đã xác nhận',
    DEPOSIT_REQUIRED: 'Cần thanh toán cọc',
    REJECTED: 'Từ chối',
    DEPOSIT_PENDING: 'Đang chờ xác nhận cọc',
    DEPOSIT_PAID: 'Đã cọc',
    FULLY_PAID: 'Đã thanh toán đủ',
    CHECKED_IN: 'Đã đến',
    IN_SERVICE: 'Đang phục vụ',
    COMPLETED: 'Hoàn thành',
    CANCELLED: 'Đã hủy',
    EXPIRED: 'Quá hạn',
    NO_SHOW: 'Không đến'
  }
  return map[status] || status
}

function paymentOptionText(option) {
  const map = {
    DEPOSIT_50: 'Đặt cọc 50%',
    FULL: 'Thanh toán toàn bộ',
    PAY_AT_RESTAURANT: 'Thanh toán tại nhà hàng'
  }
  return map[option] || option || '-'
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

function contactStatusText(status) {
  return {
    NOT_CALLED: 'Chưa gọi',
    NEEDS_CONFIRMATION_CALL: 'Cần gọi xác nhận',
    CONFIRMED_BY_CUSTOMER: 'Đã gọi – khách xác nhận',
    UNREACHABLE: 'Không liên lạc được',
    CHANGE_REQUESTED: 'Khách yêu cầu thay đổi',
    NOT_REQUIRED: 'Không cần gọi'
  }[status] || status || 'Chưa gọi'
}

function waitlistStatusText(status) {
  const map = {
    WAITING: 'Đang chờ',
    CONTACTED: 'Đã liên hệ',
    CONVERTED: 'Đã chuyển đặt bàn',
    CANCELLED: 'Đã hủy',
    EXPIRED: 'Hết hạn'
  }
  return map[status] || status || '-'
}

async function refreshAdminData() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([fetchReservations(), fetchWaitlist()])
  } finally {
    loading.value = false
  }
}

async function fetchReservations() {
  try {
    const res = await api.get('/api/admin/reservations')
    reservations.value = res.data
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không tải được danh sách đặt bàn'
  }
}

async function fetchWaitlist() {
  try {
    const res = await api.get('/api/admin/reservation-waitlist')
    waitlist.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    error.value = err.response?.data?.message || err.response?.data || 'Không tải được danh sách chờ'
  }
}

function upsertReservation(item) {
  if (!item?.id) return
  const idx = reservations.value.findIndex(r => r.id === item.id)
  if (idx >= 0) {
    reservations.value[idx] = { ...reservations.value[idx], ...item }
  } else {
    reservations.value.unshift(item)
    playSoftBeep()
  }
}

function handleRealtimeEvent(event) {
  if (!event) return
  if (event.reservation) upsertReservation(event.reservation)
  realtimeMessage.value = event.message || `Cập nhật ${event.reservationCode || ''}`
  window.clearTimeout(realtimeTimer)
  realtimeTimer = window.setTimeout(() => {
    realtimeMessage.value = ''
  }, 5000)
}

function connectRealtime() {
  if (stompClient?.active) return
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: sessionStorage.getItem('staff_token')
      ? { Authorization: `Bearer ${sessionStorage.getItem('staff_token')}` }
      : {},
    reconnectDelay: 5000,
    onConnect: () => {
      stompClient.subscribe('/topic/admin/reservations', message => {
        try {
          handleRealtimeEvent(JSON.parse(message.body))
        } catch (err) {
          console.warn('Không đọc được sự kiện đặt bàn realtime', err)
        }
      })
    }
  })
  stompClient.activate()
}

function playSoftBeep() {
  try {
    const AudioContext = window.AudioContext || window.webkitAudioContext
    if (!AudioContext) return
    const ctx = new AudioContext()
    const oscillator = ctx.createOscillator()
    const gain = ctx.createGain()
    oscillator.frequency.value = 720
    gain.gain.value = 0.04
    oscillator.connect(gain)
    gain.connect(ctx.destination)
    oscillator.start()
    oscillator.stop(ctx.currentTime + 0.08)
  } catch {
    // Browser may block sound until the user interacts with the page.
  }
}

async function refreshDetail(item) {
  const res = await api.get(`/api/admin/reservations/${item.id}`)
  const idx = reservations.value.findIndex(r => r.id === item.id)
  if (idx >= 0) reservations.value[idx] = res.data
  selected.value = res.data
}
async function openAssignment(item) {
  assignmentLoading.value = true
  assignmentOptions.value = null
  assignedTableIds.value = []
  assignmentNote.value = ''
  try {
    await refreshDetail(item)
    const { data } = await api.get(`/api/admin/reservations/${item.id}/assignment-options`)
    assignmentOptions.value = data
    if (data.recommendedOptions?.length) selectOption(data.recommendedOptions[0])
  } finally { assignmentLoading.value = false }
}
async function confirmAssignment() {
  if (!assignedTableIds.value.length || selectedCapacity.value < selected.value.guestCount) return
  savingAssignment.value = true
  try {
    await api.patch(`/api/admin/reservations/${selected.value.id}/confirm`, { tableId: assignedTableIds.value[0], tableIds: assignedTableIds.value, areaId: selected.value.areaId, note: assignmentNote.value })
    selected.value = null
    await fetchReservations()
  } finally { savingAssignment.value = false }
}

async function updateContact(item, status) {
  const note = await promptDialog({
    title: 'Cập nhật liên hệ',
    message: 'Ghi lại nội dung cuộc gọi với khách.',
    inputLabel: 'Ghi chú cuộc gọi',
    defaultValue: item.contactCallNote || '',
  })
  if (note === null) return
  const res = await api.patch(`/api/admin/reservations/${item.id}/contact-status`, { status, note })
  selected.value = res.data
  const idx = reservations.value.findIndex(reservation => reservation.id === item.id)
  if (idx >= 0) reservations.value[idx] = res.data
}

async function resendReceipt(item) {
  await api.post(`/api/admin/reservations/${item.id}/resend-receipt`)
  window.setTimeout(() => refreshDetail(item), 800)
}

async function confirmReservation(item) {
  const note = await promptDialog({
    title: 'Xác nhận đặt bàn',
    message: `Xác nhận yêu cầu ${item.reservationCode || ''}.`,
    inputLabel: 'Ghi chú (không bắt buộc)',
    confirmLabel: 'Xác nhận',
  })
  if (note === null) return
  await api.patch(`/api/admin/reservations/${item.id}/confirm`, { note })
  await fetchReservations()
}

async function rejectReservation(item) {
  const reason = await promptDialog({
    title: 'Từ chối đặt bàn',
    message: `Nhập lý do từ chối yêu cầu ${item.reservationCode || ''}.`,
    inputLabel: 'Lý do từ chối',
    required: true,
    danger: true,
    confirmLabel: 'Từ chối',
  })
  if (reason === null) return
  await api.patch(`/api/admin/reservations/${item.id}/reject`, { reason })
  await fetchReservations()
}

async function markDeposit(item) {
  await api.patch(`/api/admin/reservations/${item.id}/deposit`, { note: 'Đã nhận tiền đặt cọc' })
  await fetchReservations()
}

async function checkIn(item) {
  await api.patch(`/api/admin/reservations/${item.id}/check-in`, { note: 'Khách đã đến' })
  await fetchReservations()
}

async function cancelReservation(item) {
  const note = await promptDialog({
    title: 'Hủy đặt bàn',
    message: `Nhập lý do hủy yêu cầu ${item.reservationCode || ''}.`,
    inputLabel: 'Lý do hoặc ghi chú',
    required: true,
    danger: true,
    confirmLabel: 'Hủy đặt bàn',
  })
  if (note === null) return
  await api.patch(`/api/admin/reservations/${item.id}/cancel`, { note })
  await fetchReservations()
}

async function contactWaitlist(item) {
  const note = await promptDialog({
    title: 'Liên hệ khách chờ',
    inputLabel: 'Ghi chú liên hệ',
    defaultValue: item.managerNote || '',
  })
  if (note === null) return
  await api.patch(`/api/admin/reservation-waitlist/${item.id}/contact`, { note })
  await fetchWaitlist()
}

async function convertWaitlist(item) {
  const linkedReservationCode = await promptDialog({
    title: 'Chuyển thành đặt bàn',
    message: 'Nhập mã đặt bàn đã tạo đúng cho khách trong danh sách chờ.',
    inputLabel: 'Mã đặt bàn',
    defaultValue: item.linkedReservationCode || '',
    required: true,
    confirmLabel: 'Tiếp tục',
  })
  if (linkedReservationCode === null) return
  const note = await promptDialog({
    title: 'Ghi chú chuyển đổi',
    inputLabel: 'Ghi chú (không bắt buộc)',
    defaultValue: item.managerNote || '',
    confirmLabel: 'Hoàn tất',
  })
  if (note === null) return
  await api.patch(`/api/admin/reservation-waitlist/${item.id}/convert`, { linkedReservationCode, note })
  await fetchWaitlist()
}

async function cancelWaitlist(item) {
  const note = await promptDialog({
    title: 'Hủy yêu cầu chờ',
    inputLabel: 'Lý do hủy',
    defaultValue: item.managerNote || '',
    required: true,
    danger: true,
    confirmLabel: 'Hủy yêu cầu',
  })
  if (note === null) return
  await api.patch(`/api/admin/reservation-waitlist/${item.id}/cancel`, { note })
  await fetchWaitlist()
}

onMounted(() => {
  refreshAdminData()
  connectRealtime()
})

onBeforeUnmount(() => {
  if (keywordTimer) clearTimeout(keywordTimer)
  window.clearTimeout(realtimeTimer)
  if (stompClient) stompClient.deactivate()
})
</script>

<style scoped>
.admin-reservation {
  color: var(--text-primary);
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.realtime-alert {
  border: 1px solid #D7E3ED;
  background: #EEF3F6;
  color: var(--secondary);
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 14px;
  font-weight: 800;
}

h1 {
  margin: 0 0 6px;
  color: var(--color-on-secondary-container);
}

p {
  margin: 0;
  color: var(--text-muted);
}

.primary-btn,
.row-actions button,
.detail-panel header button {
  border: 1px solid var(--secondary);
  background: var(--secondary);
  color: #FFFFFF;
  border-radius: 8px;
  min-height: 36px;
  padding: 0 12px;
  font-weight: 800;
  cursor: pointer;
}

.filters {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 220px;
  gap: 12px;
  margin-bottom: 14px;
}

.search-control {
  position: relative;
}

.search-control::before {
  content: '⌕';
  position: absolute;
  top: 50%;
  left: 13px;
  color: var(--text-muted);
  font-size: 1.25rem;
  line-height: 1;
  transform: translateY(-54%);
  pointer-events: none;
}

.filters input,
.filters select {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  min-height: 40px;
  padding: 0 12px;
  font: inherit;
  background: var(--bg-card);
  color: var(--text-primary);
}

.filters input { padding-left: 40px; padding-right: 42px; }
.filters input:focus,
.filters select:focus { outline: 2px solid var(--primary-glow); border-color: var(--primary); }
.clear-search {
  position: absolute;
  top: 50%;
  right: 8px;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--text-muted);
  font-size: 1.35rem;
  line-height: 1;
  cursor: pointer;
  transform: translateY(-50%);
}
.clear-search:hover { background: var(--bg-hover); color: var(--primary); }

.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.status-tabs button {
  border: 1px solid var(--border);
  background: #FFFFFF;
  border-radius: 8px;
  min-height: 40px;
  padding: 0 12px;
  color: var(--text-secondary);
  font-weight: 800;
  cursor: pointer;
}

.status-tabs button.active {
  border-color: var(--secondary);
  color: var(--color-on-secondary-container);
}

.status-tabs span {
  display: inline-flex;
  margin-right: 8px;
  background: var(--bg-hover);
  color: var(--secondary);
  border-radius: 999px;
  padding: 2px 8px;
}

.table-wrap {
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: auto;
}

.waitlist-panel {
  margin-top: 24px;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 12px;
}

.section-heading h2 {
  margin: 0 0 4px;
  color: var(--color-on-secondary-container);
}

.section-heading strong {
  white-space: nowrap;
  color: var(--secondary);
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 980px;
}

th,
td {
  padding: 13px 14px;
  border-bottom: 1px solid var(--border);
  text-align: left;
  vertical-align: top;
}

th {
  position: sticky;
  top: 0;
  background: var(--bg-card2);
  color: var(--text-secondary);
  font-size: 0.78rem;
  text-transform: uppercase;
}

td strong,
td span,
td small {
  display: block;
}

td span,
td small {
  color: var(--text-muted);
  margin-top: 4px;
}

.status-badge {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--bg-hover);
  color: var(--secondary);
  font-weight: 900;
  font-size: 0.8rem;
}

.status-badge.REJECTED,
.status-badge.CANCELLED {
  background: #F4E8E5;
  color: var(--primary);
}

.status-badge.DEPOSIT_PENDING,
.status-badge.DEPOSIT_REQUIRED {
  background: #F5F0E4;
  color: var(--color-tertiary);
}

.status-badge.FULLY_PAID,
.status-badge.DEPOSIT_PAID {
  background: #EEF5EF;
  color: var(--secondary);
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.row-actions .danger {
  background: var(--primary);
  border-color: var(--primary);
}

.row-actions .ghost {
  background: #FFFFFF;
  color: var(--text-secondary);
  border-color: var(--border);
}

.empty,
.error {
  text-align: center;
  color: var(--text-muted);
  padding: 28px;
}

.error {
  background: #F4E8E5;
  color: #8F2F25;
  border: 1px solid #E8C9C4;
  border-radius: 8px;
  margin-bottom: 14px;
}

.modal {
  position: fixed;
  inset: 0;
  background: var(--overlay-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 500;
}

.detail-panel {
  background: #FFFFFF;
  border-radius: 8px;
  width: min(760px, 100%);
  max-height: 86vh;
  overflow: auto;
  padding: 22px;
}

.detail-panel header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid div {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 18px 0;
}

.detail-actions a {
  text-decoration: none;
}
.assignment-box{margin:18px 0;padding:16px;border:1px solid #d7b56d;border-radius:10px;background:#fffaf0}.recommendations{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:10px;margin:10px 0 16px}.option-card{display:grid;text-align:left;gap:4px;background:var(--warning)}.option-card small{color:#fff}.manual-tables{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:8px}.manual-tables label{display:flex;gap:8px;border:1px solid var(--border);border-radius:8px;padding:10px;cursor:pointer}.manual-tables label.chosen{border-color:var(--warning);background:#f7eee6}.manual-tables label span{display:grid}.manual-tables small{color:var(--text-muted)}.assignment-box textarea{width:100%;padding:10px;border:1px solid var(--border);border-radius:8px;margin:8px 0}.capacity-error{color:var(--danger);font-weight:700}

.detail-grid span {
  display: block;
  color: var(--text-muted);
  margin-bottom: 4px;
}

.preorder-list {
  display: grid;
  gap: 10px;
  margin: 10px 0 18px;
}

.preorder-row {
  display: grid;
  grid-template-columns: 1.4fr 120px 120px;
  gap: 10px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
}

.preorder-row small {
  grid-column: 1 / -1;
  color: var(--text-muted);
}

.empty-inline {
  border: 1px dashed var(--border);
  border-radius: 8px;
  padding: 12px;
  margin: 8px 0 18px;
}

@media (max-width: 780px) {
  .toolbar,
  .filters {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .preorder-row {
    grid-template-columns: 1fr;
  }
}
</style>
