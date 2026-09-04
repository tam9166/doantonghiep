<template>
  <CustomerLayout>
    <main class="lookup-page">
      <section class="lookup-shell">
        <header class="lookup-header">
          <p class="eyebrow">{{ t('reservationLookup.eyebrow') }}</p>
          <h1>{{ t('reservationLookup.title') }}</h1>
          <p>{{ t('reservationLookup.subtitle') }}</p>
        </header>

        <form class="lookup-card" @submit.prevent="lookupReservation">
          <label>
            {{ t('reservationLookup.code') }}
            <input v-model.trim="form.code" type="text" autocomplete="off" :placeholder="t('reservationLookup.codePlaceholder')" />
          </label>
          <label>
            {{ t('reservationLookup.phone') }}
            <input v-model.trim="form.phone" type="tel" autocomplete="tel" :placeholder="t('reservationLookup.phonePlaceholder')" />
          </label>
          <button class="primary-btn" type="submit" :disabled="loading">
            {{ loading ? t('reservationLookup.loading') : t('reservationLookup.lookup') }}
          </button>
        </form>

        <div v-if="error" class="error-box">{{ error }}</div>

        <section class="cancel-card">
          <div>
            <p class="eyebrow">{{ t('reservationLookup.cancellationEyebrow') }}</p>
            <h2>{{ t('reservationLookup.cancellationTitle') }}</h2>
            <p>{{ t('reservationLookup.cancellationHint') }}</p>
          </div>
          <form class="cancel-form" @submit.prevent="submitCancellationRequest">
            <div v-if="reservation" class="verified-booking">
              <strong>{{ t('reservationLookup.verified', { code: reservation.reservationCode }) }}</strong>
              <span>{{ reservation.customerName }} · {{ reservation.customerPhone }}</span>
            </div>
            <template v-else>
              <label>{{ t('reservationLookup.codeOptional') }}<input v-model="cancelForm.reservationCode" maxlength="30" @input="onCancelFieldInput" /></label>
              <label>{{ t('reservationLookup.fullName') }}<input v-model="cancelForm.customerName" maxlength="150" autocomplete="name" @input="onCancelFieldInput" /></label>
              <label>{{ t('reservationLookup.phone') }}<input v-model="cancelForm.customerPhone" maxlength="30" autocomplete="tel" @input="onCancelFieldInput" /></label>
              <label>{{ t('reservationLookup.email') }}<input v-model="cancelForm.customerEmail" maxlength="150" type="email" autocomplete="email" @input="onCancelFieldInput" /></label>
            </template>
            <label class="cancel-reason">{{ t('reservationLookup.reason') }}<textarea v-model="cancelForm.reason" maxlength="1000" rows="3" /></label>
            <label>{{ t('reservationLookup.contactMethod') }}
              <select v-model="cancelForm.contactMethod">
                <option value="PHONE">{{ t('reservationLookup.phone') }} ({{ reservation?.customerPhone || cancelForm.customerPhone || t('reservationLookup.verifiedPhone') }})</option>
                <option value="EMAIL" :disabled="!verifiedEmail">{{ t('reservationLookup.email') }} ({{ reservation?.customerEmail || cancelForm.customerEmail || t('reservationLookup.noVerifiedEmail') }})</option>
              </select>
            </label>
            <button class="ghost-btn refund-preview-trigger" type="button" :disabled="previewLoading" @click="loadRefundPreview">
              {{ previewLoading ? t('reservationLookup.calculating') : t('reservationLookup.refundPreview') }}
            </button>
            <section v-if="refundPreview" class="refund-preview">
              <strong>{{ t('reservationLookup.expectedRefund') }}: {{ money(refundPreview.expectedRefundAmount) }}</strong>
              <dl>
                <div><dt>{{ t('reservationLookup.orderTotal') }}</dt><dd>{{ money(refundPreview.orderTotalAmount || reservation?.totalAmount) }}</dd></div>
                <div><dt>{{ t('reservationLookup.paidDeposit') }}</dt><dd>{{ money(refundPreview.paidDepositAmount) }}</dd></div>
                <div><dt>{{ t('reservationLookup.timeRemaining') }}</dt><dd>{{ t('reservationLookup.hours', { count: refundPreview.hoursBeforeReservation }) }}</dd></div>
                <div><dt>{{ t('reservationLookup.appliedPolicy') }}</dt><dd>{{ refundPolicyText(refundPreview) }}</dd></div>
                <div><dt>{{ t('reservationLookup.penaltyAmount') }}</dt><dd>{{ money(refundPreview.penaltyAmount) }}</dd></div>
                <div><dt>{{ t('reservationLookup.expectedRefund') }}</dt><dd>{{ money(refundPreview.expectedRefundAmount) }}</dd></div>
              </dl>
              <p>{{ refundPreviewMessage(refundPreview) }}</p>
            </section>
            <section v-if="refundRequired" class="refund-destination">
              <h3>{{ t('reservationLookup.refundDestinationTitle') }}</h3>
              <p>{{ t('reservationLookup.refundDestinationHint') }}</p>
              <label>{{ t('reservationLookup.bank') }}<input v-model.trim="cancelForm.refundBankName" maxlength="120" autocomplete="organization" /></label>
              <label>{{ t('reservationLookup.accountNumber') }}<input v-model.trim="cancelForm.refundAccountNumber" maxlength="40" inputmode="numeric" autocomplete="off" /></label>
              <label>{{ t('reservationLookup.accountHolder') }}<input v-model.trim="cancelForm.refundAccountHolder" maxlength="150" autocomplete="name" /></label>
            </section>
            <button v-if="canSendCancellationRequest" class="danger-btn" type="submit" :disabled="cancelLoading">
              {{ cancelLoading ? t('reservationLookup.sending') : t('reservationLookup.sendCancellation') }}
            </button>
            <p v-else-if="cancellationAvailabilityMessage" class="cancellation-message">{{ cancellationAvailabilityMessage }}</p>
          </form>
          <div v-if="cancelMessage" class="success-box">{{ cancelMessage }}</div>
          <div v-if="cancellationReceipt" class="cancellation-status" role="status">
            <strong>{{ t('reservationLookup.requestStatus') }}: {{ cancellationStatusText(cancellationReceipt.status) }}</strong>
            <span>{{ t('reservationLookup.requestCode') }}: {{ cancellationReceipt.requestCode }}</span>
          </div>
          <div v-if="cancelError" class="error-box">{{ cancelError }}</div>
        </section>

        <section v-if="reservation" class="result-card">
          <div class="realtime-box" :class="realtimeState.toLowerCase()">
            <span>{{ realtimeStatusText }}</span>
            <strong v-if="realtimeMessage">{{ realtimeMessage }}</strong>
          </div>

          <div class="result-head">
            <div>
              <p class="eyebrow">{{ t('reservationLookup.result') }}</p>
              <h2>{{ reservation.reservationCode }}</h2>
            </div>
            <span class="status-badge" :class="reservation.reservationStatus">{{ statusText(reservation.reservationStatus) }}</span>
          </div>

          <div class="summary-grid">
            <div><span>{{ t('reservationLookup.customer') }}</span><strong>{{ reservation.customerName }}</strong></div>
            <div><span>{{ t('reservationLookup.phone') }}</span><strong>{{ reservation.customerPhone }}</strong></div>
            <div><span>{{ t('reservationLookup.dateTime') }}</span><strong>{{ reservation.reservationDate }} {{ reservation.arrivalTime }}</strong></div>
            <div><span>{{ t('reservationLookup.guests') }}</span><strong>{{ reservation.guestCount }}</strong></div>
            <div><span>{{ t('reservationLookup.table') }}</span><strong>{{ reservation.tableName || '-' }}</strong></div>
            <div><span>{{ t('reservationLookup.area') }}</span><strong>{{ reservation.areaName || reservation.tableFloor || '-' }}</strong></div>
            <div><span>{{ t('reservationLookup.orderTotal') }}</span><strong>{{ money(reservation.totalAmount) }}</strong></div>
            <div><span>{{ t('reservationLookup.requiredDeposit') }}</span><strong>{{ money(reservation.depositAmount) }}</strong></div>
            <div><span>{{ t('reservationLookup.paid') }}</span><strong>{{ money(reservation.paidAmount) }}</strong></div>
            <div><span>{{ t('reservationLookup.amountDueNow') }}</span><strong>{{ money(reservation.amountDueNow) }}</strong></div>
            <div><span>{{ t('reservationLookup.remaining') }}</span><strong>{{ money(reservation.remainingAmount) }}</strong></div>
            <div><span>{{ t('reservationLookup.payment') }}</span><strong>{{ paymentStatusText(reservation.paymentStatus) }}</strong></div>
          </div>

          <article v-if="latestPayment" class="qr-card">
            <div>
              <h3>{{ t('reservationLookup.paymentQr') }}</h3>
              <dl>
                <div><dt>{{ t('reservationLookup.paymentCode') }}</dt><dd>{{ latestPayment.paymentCode }}</dd></div>
                <div><dt>{{ t('reservationLookup.bank') }}</dt><dd>{{ latestPayment.bankCode }}</dd></div>
                <div><dt>{{ t('reservationLookup.accountNumber') }}</dt><dd>{{ latestPayment.accountNumber }}</dd></div>
                <div><dt>{{ t('reservationLookup.accountHolder') }}</dt><dd>{{ latestPayment.accountHolder }}</dd></div>
                <div><dt>{{ t('reservationLookup.transferContent') }}</dt><dd>{{ latestPayment.transferContent }}</dd></div>
                <div><dt>{{ t('reservationLookup.expiresAt') }}</dt><dd>{{ formatDateTime(latestPayment.expiresAt) }}</dd></div>
              </dl>
              <p class="payment-status" :class="{ paid: latestPayment.status === 'PAID' }">
                {{ t('reservationLookup.payment') }}: {{ paymentStatusText(latestPayment.status) }}
              </p>
              <button class="ghost-btn" type="button" @click="refreshLatestPayment" :disabled="qrLoading">
                {{ qrLoading ? t('reservationLookup.refreshingPayment') : t('reservationLookup.refreshPayment') }}
              </button>
              <button v-if="canRegenerateQr" class="ghost-btn" type="button" @click="regenerateQr" :disabled="qrLoading">
                {{ qrLoading ? t('reservationLookup.creatingQr') : t('reservationLookup.regenerateQr') }}
              </button>
            </div>
            <img :src="latestPayment.qrUrl" :alt="t('reservationLookup.paymentQr')" />
          </article>

          <div v-else-if="canCreateQr" class="payment-action">
            <div>
              <strong>{{ t('reservationLookup.noPaymentQr') }}</strong>
              <p>{{ t('reservationLookup.createQrHint') }}</p>
            </div>
            <button class="primary-btn" type="button" @click="createQr" :disabled="qrLoading">
              {{ qrLoading ? t('reservationLookup.creatingQr') : t('reservationLookup.createQr') }}
            </button>
          </div>

          <section v-if="reservation.preorderItems?.length" class="preorder-box">
            <h3>{{ t('reservationLookup.preorder') }}</h3>
            <div v-for="item in reservation.preorderItems" :key="item.id" class="preorder-row">
              <strong>{{ item.productName }}</strong>
              <span>{{ item.quantity }} x {{ money(item.unitPrice) }}</span>
              <span>{{ money(item.lineTotal) }}</span>
            </div>
          </section>

          <section v-if="canReview" class="review-box">
            <div class="review-head">
              <div>
                <h3>{{ t('reservationLookup.reviewTitle') }}</h3>
                <p>{{ t('reservationLookup.reviewHint') }}</p>
              </div>
              <span v-if="myReview" class="review-done">{{ t('reservationLookup.reviewSent') }}</span>
            </div>
            <div v-if="myReview" class="submitted-review">
              <strong>{{ t('reservationLookup.ratingValue', { rating: myReview.overallRating }) }}</strong>
              <p>{{ myReview.content || t('reservationLookup.noReviewComment') }}</p>
              <small v-if="myReview.adminReply">{{ t('reservationLookup.restaurantReply') }}: {{ myReview.adminReply }}</small>
            </div>
            <form v-else class="review-form" @submit.prevent="submitReview">
              <div class="rating-grid">
                <label>{{ t('reservationLookup.ratingOverall') }}<select v-model.number="reviewForm.overallRating"><option v-for="n in 5" :key="n" :value="n">{{ t('reservationLookup.ratingValue', { rating: n }) }}</option></select></label>
                <label>{{ t('reservationLookup.ratingFood') }}<select v-model.number="reviewForm.foodRating"><option v-for="n in 5" :key="n" :value="n">{{ t('reservationLookup.ratingValue', { rating: n }) }}</option></select></label>
                <label>{{ t('reservationLookup.ratingService') }}<select v-model.number="reviewForm.serviceRating"><option v-for="n in 5" :key="n" :value="n">{{ t('reservationLookup.ratingValue', { rating: n }) }}</option></select></label>
                <label>{{ t('reservationLookup.ratingAmbience') }}<select v-model.number="reviewForm.ambienceRating"><option v-for="n in 5" :key="n" :value="n">{{ t('reservationLookup.ratingValue', { rating: n }) }}</option></select></label>
                <label>{{ t('reservationLookup.ratingCleanliness') }}<select v-model.number="reviewForm.cleanlinessRating"><option v-for="n in 5" :key="n" :value="n">{{ t('reservationLookup.ratingValue', { rating: n }) }}</option></select></label>
              </div>
              <label>
                {{ t('reservationLookup.reviewContent') }}
                <textarea v-model.trim="reviewForm.content" rows="4" :placeholder="t('reservationLookup.reviewPlaceholder')" />
              </label>
              <label class="inline-check">
                <input v-model="reviewForm.anonymous" type="checkbox" />
                {{ t('reservationLookup.anonymousReview') }}
              </label>
              <button class="primary-btn" type="submit" :disabled="reviewLoading">
                {{ reviewLoading ? t('reservationLookup.sending') : t('reservationLookup.submitReview') }}
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
import { useI18n } from 'vue-i18n'
import CustomerLayout from '@/components/CustomerLayout.vue'
import api from '@/services/api'
import { getApiErrorMessage } from '@/services/errorMessage'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const loading = ref(false)
const qrLoading = ref(false)
const error = ref('')
const cancelError = ref('')
const cancelMessage = ref('')
const cancelLoading = ref(false)
const cancellationReceipt = ref(null)
const refundPreview = ref(null)
const previewLoading = ref(false)
const reservation = ref(null)
const form = ref({ code: '', phone: '' })
const cancelForm = ref({ reservationCode: '', customerName: '', customerPhone: '', customerEmail: '', reason: '', contactMethod: 'PHONE', refundBankName: '', refundAccountNumber: '', refundAccountHolder: '' })
const realtimeConnected = ref(false)
const realtimeState = ref('DISCONNECTED')
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
let realtimeConnectTimer = null
let intentionalRealtimeDisconnect = false
const closedCancellationStatuses = ['CANCELLED', 'COMPLETED', 'REJECTED', 'EXPIRED', 'NO_SHOW']

function resetCancellationFeedback() {
  cancelError.value = ''
  cancelMessage.value = ''
  cancellationReceipt.value = null
}

function onCancelFieldInput() {
  refundPreview.value = null
  resetCancellationFeedback()
}

const latestPayment = computed(() => reservation.value?.payments?.[0] || null)
const paymentCapability = computed(() => reservation.value?.reservationCode
  ? sessionStorage.getItem(`reservation-capability:${reservation.value.reservationCode}`)
  : '')
const hasPaymentAccess = computed(() => Boolean(paymentCapability.value || sessionStorage.getItem('token')))
const activeStatuses = ['PENDING', 'CONFIRMED', 'DEPOSIT_REQUIRED', 'DEPOSIT_PENDING', 'DEPOSIT_PAID']
const canSendCancellationRequest = computed(() => Boolean(reservation.value)
  && !cancellationReceipt.value
  && Boolean(refundPreview.value)
  && !closedCancellationStatuses.includes(reservation.value.reservationStatus))
const cancellationAvailabilityMessage = computed(() => {
  if (!reservation.value) return ''
  if (cancellationReceipt.value) {
    return t('reservationLookup.cancellationPending')
  }
  if (closedCancellationStatuses.includes(reservation.value.reservationStatus)) {
    return t('reservationLookup.cancellationTooLate')
  }
  if (!refundPreview.value) return t('reservationLookup.previewRequired')
  return ''
})
const canCreateQr = computed(() => reservation.value
  && Number(reservation.value.amountDueNow || 0) > 0
  && reservation.value.paymentOption !== 'PAY_AT_RESTAURANT'
  && hasPaymentAccess.value
  && activeStatuses.includes(reservation.value.reservationStatus))
const canRegenerateQr = computed(() => latestPayment.value && ['PENDING', 'EXPIRED'].includes(latestPayment.value.status))
const canReview = computed(() => reservation.value?.reservationStatus === 'COMPLETED')
const cancellationStatusText = status => t(`reservationLookup.cancellationStatus.${status || 'PENDING'}`)
const refundRequired = computed(() => Number(refundPreview.value?.expectedRefundAmount || 0) > 0)
const verifiedEmail = computed(() => Boolean(reservation.value?.customerEmail || cancelForm.value.customerEmail))

const money = value => new Intl.NumberFormat(locale.value === 'en' ? 'en-US' : 'vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0
}).format(Number(value || 0))

const formatDateTime = value => value ? new Date(value).toLocaleString(locale.value === 'en' ? 'en-US' : 'vi-VN') : '-'

const realtimeStatusText = computed(() => t(`reservationLookup.realtime.${realtimeState.value.toLowerCase()}`))

function localizedApiMessage(err, fallback) {
  const message = getApiErrorMessage(err, fallback)
  return locale.value === 'en' && /[À-ỹ]/.test(message) ? fallback : message
}

function refundPolicyText(preview) {
  const code = preview?.policyCode
  return code ? t(`reservationLookup.refundPolicy.${code}`) : preview?.policyApplied
}

function refundPreviewMessage(preview) {
  const code = preview?.messageCode
  return code ? t(`reservationLookup.refundMessage.${code}`) : preview?.message
}

function statusText(status) {
  return t(`reservationLookup.reservationStatus.${status || 'UNKNOWN'}`)
}

function paymentStatusText(status) {
  return t(`reservationLookup.paymentStatus.${status || 'UNKNOWN'}`)
}

async function lookupReservation() {
  error.value = ''
  reservation.value = null
  if (!form.value.code || !form.value.phone) {
    error.value = t('reservationLookup.errors.lookupRequired')
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
    error.value = localizedApiMessage(err, t('reservationLookup.errors.lookupNotFound'))
  } finally {
    loading.value = false
  }
}

async function submitCancellationRequest() {
  cancelError.value = ''
  cancelMessage.value = ''
  if (!String(cancelForm.value.reason || '').trim()) {
    cancelError.value = t('reservationLookup.errors.reasonRequired')
    return
  }
  const verificationValues = [
    cancelForm.value.reservationCode,
    cancelForm.value.customerName,
    cancelForm.value.customerPhone,
    cancelForm.value.customerEmail
  ].filter(value => String(value || '').trim())
  if (verificationValues.length < 2) {
    cancelError.value = t('reservationLookup.errors.twoFieldsRequired')
    return
  }
  cancelLoading.value = true
  try {
    const response = await api.post('/api/reservation-cancellations', cancellationRequestPayload())
    cancellationReceipt.value = response.data
    cancelMessage.value = response.data.message || t('reservationLookup.cancellationSubmitted', { code: response.data.requestCode })
  } catch (err) {
    cancelError.value = localizedApiMessage(err, t('reservationLookup.errors.cancellationVerificationFailed'))
  } finally {
    cancelLoading.value = false
  }
}

async function loadRefundPreview() {
  cancelError.value = ''
  refundPreview.value = null
  const verificationValues = [cancelForm.value.reservationCode, cancelForm.value.customerName,
    cancelForm.value.customerPhone, cancelForm.value.customerEmail].filter(value => String(value || '').trim())
  if (verificationValues.length < 2) {
    cancelError.value = t('reservationLookup.errors.twoFieldsRequired')
    return
  }
  previewLoading.value = true
  try {
    const response = await api.post('/api/reservation-cancellations/preview', cancellationRequestPayload())
    refundPreview.value = response.data
  } catch (err) {
    cancelError.value = localizedApiMessage(err, t('reservationLookup.errors.refundPreviewFailed'))
  } finally {
    previewLoading.value = false
  }
}

function cancellationRequestPayload() {
  if (reservation.value) {
    return {
      reservationCode: reservation.value.reservationCode || cancelForm.value.reservationCode,
      customerName: reservation.value.customerName || cancelForm.value.customerName,
      customerPhone: reservation.value.customerPhone || cancelForm.value.customerPhone,
      customerEmail: reservation.value.customerEmail || cancelForm.value.customerEmail,
      reason: cancelForm.value.reason,
      contactMethod: cancelForm.value.contactMethod,
      refundBankName: cancelForm.value.refundBankName,
      refundAccountNumber: cancelForm.value.refundAccountNumber,
      refundAccountHolder: cancelForm.value.refundAccountHolder
    }
  }
  return { ...cancelForm.value }
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
    error.value = localizedApiMessage(err, t('reservationLookup.errors.reviewFailed'))
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
  realtimeMessage.value = event.message || t('reservationLookup.realtime.updated')
  window.clearTimeout(realtimeTimer)
  realtimeTimer = window.setTimeout(() => {
    realtimeMessage.value = ''
  }, 7000)
}

function disconnectRealtime() {
  intentionalRealtimeDisconnect = true
  realtimeConnected.value = false
  window.clearTimeout(realtimeConnectTimer)
  realtimeConnectTimer = null
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
  }
}

function connectRealtime(code) {
  disconnectRealtime()
  if (!code) return
  intentionalRealtimeDisconnect = false
  realtimeState.value = 'CONNECTING'
  stompClient = new Client({
    webSocketFactory: () => new SockJS('/ws'),
    connectHeaders: sessionStorage.getItem('token')
      ? { Authorization: `Bearer ${sessionStorage.getItem('token')}` }
      : {},
    reconnectDelay: 5000,
    onConnect: () => {
      realtimeConnected.value = true
      realtimeState.value = 'CONNECTED'
      window.clearTimeout(realtimeConnectTimer)
      realtimeConnectTimer = null
      const capability = sessionStorage.getItem(`reservation-capability:${code}`)
      const headers = capability ? { 'X-Reservation-Capability': capability } : {}
      stompClient.subscribe(`/topic/reservations/${code}`, message => {
        try {
          handleRealtimeEvent(JSON.parse(message.body))
        } catch {
          realtimeMessage.value = t('reservationLookup.realtime.updatedFallback')
        }
      }, headers)
    },
    onWebSocketClose: () => {
      realtimeConnected.value = false
      if (!intentionalRealtimeDisconnect) realtimeState.value = 'FAILED'
    },
    onStompError: () => {
      realtimeConnected.value = false
      realtimeState.value = 'FAILED'
    }
  })
  stompClient.activate()
  realtimeConnectTimer = window.setTimeout(() => {
    if (realtimeState.value === 'CONNECTING') {
      realtimeState.value = 'FAILED'
      realtimeConnected.value = false
    }
  }, 8000)
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
    error.value = localizedApiMessage(err, t('reservationLookup.errors.createQrFailed'))
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
    error.value = localizedApiMessage(err, t('reservationLookup.errors.regenerateQrFailed'))
  } finally {
    qrLoading.value = false
  }
}

async function refreshLatestPayment() {
  if (!latestPayment.value?.paymentCode || qrLoading.value) return
  qrLoading.value = true
  error.value = ''
  try {
    // PaymentIntent is the payment source of truth. Refresh it directly so a
    // customer is not left showing PENDING when realtime delivery is delayed.
    const res = await api.get(`/api/payments/${latestPayment.value.paymentCode}`, {
      headers: paymentCapability.value ? { 'X-Payment-Capability': paymentCapability.value } : {}
    })
    reservation.value = {
      ...reservation.value,
      payments: [res.data, ...(reservation.value.payments || []).filter(item => item.paymentCode !== res.data.paymentCode)]
    }
    await refreshReservationSilently()
  } catch (err) {
    error.value = localizedApiMessage(err, t('reservationLookup.errors.paymentStatus'))
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
  window.clearTimeout(realtimeConnectTimer)
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

.refund-preview-trigger {
  align-self: end;
  min-height: 44px;
  margin-top: 0;
  box-sizing: border-box;
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
  white-space: pre-line;
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

.realtime-box.failed {
  border-color: var(--color-error);
  background: var(--color-error-container);
  color: var(--color-on-error-container);
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
.cancel-form select {
  border: 1px solid var(--color-outline-variant);
  border-radius: 10px;
  padding: 11px 12px;
  font: inherit;
  background: var(--color-surface-container-lowest);
  min-height: 44px;
  box-sizing: border-box;
}
.cancel-reason { grid-column: 1 / -1; }
.refund-destination {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--color-primary);
  border-radius: 12px;
  background: var(--color-primary-container);
}
.refund-destination h3, .refund-destination p { grid-column: 1 / -1; margin: 0; }
.refund-destination p { color: var(--color-on-primary-container); }
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
  .refund-destination { grid-template-columns: 1fr; }
}
.cancellation-status { display:grid; gap:4px; margin-top:12px; padding:12px; border:1px solid var(--color-outline-variant); border-radius:10px; background:var(--color-surface-container-low); }
.cancellation-status span { color:var(--text-muted); font-size:.85rem; }
.verified-booking, .refund-preview { display:grid; grid-column:1 / -1; gap:8px; padding:12px; border:1px solid var(--color-outline-variant); border-radius:10px; background:var(--color-surface-container-low); }
.verified-booking span, .refund-preview p { color:var(--text-muted); font-size:.86rem; }
.refund-preview dl { display:grid; gap:6px; margin:0; }
.refund-preview dl div { display:flex; justify-content:space-between; gap:12px; border-bottom:1px dashed var(--color-outline-variant); padding-bottom:5px; }
.refund-preview dt { color:var(--text-muted); font-size:.84rem; }
.refund-preview dd { margin:0; text-align:right; font-weight:800; }
.refund-preview p { margin:0; }
.cancellation-message { margin: 0; color: var(--color-primary); font-weight: 700; white-space: pre-line; }
</style>
