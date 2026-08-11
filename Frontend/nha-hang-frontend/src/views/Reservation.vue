<template>
  <CustomerLayout>
    <main class="reservation-page">
      <section class="reservation-shell">
        <header class="reservation-header">
          <div>
            <p class="eyebrow">{{ text.eyebrow }}</p>
            <h1>{{ text.title }}</h1>
            <p>{{ text.subtitle }}</p>
          </div>
          <button class="lang-toggle" type="button" @click="toggleLang">{{ lang === 'vi' ? 'EN' : 'VI' }}</button>
        </header>

        <nav class="stepper" aria-label="Reservation steps">
          <button
            v-for="(label, index) in text.steps"
            :key="label"
            :class="['step-chip', { active: step === index + 1, done: step > index + 1 && !(skipPaymentStep && index === 7), skipped: skipPaymentStep && index === 7 }]"
            type="button"
            @click="goTo(index + 1)"
          >
            <span>{{ index + 1 }}</span>{{ label }}
          </button>
        </nav>

        <section v-if="submitResult" class="success-panel">
          <div>
            <p class="eyebrow">{{ text.doneEyebrow }}</p>
            <h2>{{ text.doneTitle }}</h2>
            <p>{{ text.doneMessage }}</p>
          </div>
          <div class="reservation-code-row">
            <code class="reservation-code">{{ submitResult.reservationCode }}</code>
            <button
              class="code-copy-btn"
              type="button"
              :aria-label="copiedCode === submitResult.reservationCode ? 'Đã sao chép mã đặt bàn' : 'Sao chép mã đặt bàn'"
              @click="copyCode(submitResult.reservationCode)"
            >
              {{ copiedCode === submitResult.reservationCode ? 'Đã sao chép' : 'Sao chép' }}
            </button>
          </div>
          <div class="summary-grid">
            <span>{{ text.total }}</span><strong>{{ money(submitResult.totalAmount) }}</strong>
            <span>{{ text.payableNow }}</span><strong>{{ money(submitResult.depositAmount) }}</strong>
            <span>{{ text.status }}</span><strong>{{ statusLabel(submitResult.reservationStatus) }}</strong>
          </div>
          <article v-if="paymentQr" class="qr-card">
            <div>
              <h3>{{ text.qrTitle }}</h3>
              <p>{{ text.qrHint }}</p>
              <dl>
                <div><dt>{{ text.bank }}</dt><dd>{{ paymentQr.bankCode }}</dd></div>
                <div><dt>{{ text.accountNumber }}</dt><dd>{{ paymentQr.accountNumber }}</dd></div>
                <div><dt>{{ text.accountHolder }}</dt><dd>{{ paymentQr.accountHolder }}</dd></div>
                <div><dt>{{ text.transferContent }}</dt><dd>{{ paymentQr.transferContent }}</dd></div>
                <div><dt>{{ text.expiresAt }}</dt><dd>{{ formatDateTime(paymentQr.expiresAt) }}</dd></div>
              </dl>
            </div>
            <img :src="paymentQr.qrUrl" :alt="text.qrTitle" />
            <button class="secondary-btn" type="button" :disabled="qrLoading" @click="regeneratePaymentQr">
              {{ lang === 'vi' ? 'Tạo lại QR' : 'Regenerate QR' }}
            </button>
          </article>
          <div v-else-if="qrLoading" class="qr-local-state">
            {{ lang === 'vi' ? 'Đang tạo QR...' : 'Creating QR...' }}
          </div>
          <div v-else-if="qrError" class="qr-local-state qr-error-state">
            <p>{{ qrError }}</p>
            <button class="secondary-btn" type="button" @click="createPaymentQr">
              {{ lang === 'vi' ? 'Thử lại QR' : 'Retry QR' }}
            </button>
          </div>
          <button class="primary-btn" type="button" @click="resetForm">{{ text.newBooking }}</button>
        </section>

        <section v-else-if="waitlistResult" class="success-panel">
          <div>
            <p class="eyebrow">{{ text.waitlistEyebrow }}</p>
            <h2>{{ text.waitlistDoneTitle }}</h2>
            <p>{{ text.waitlistDoneMessage }}</p>
          </div>
          <div class="reservation-code-row">
            <code class="reservation-code">{{ waitlistResult.waitlistCode }}</code>
            <button
              class="code-copy-btn"
              type="button"
              :aria-label="copiedCode === waitlistResult.waitlistCode ? 'Đã sao chép mã chờ' : 'Sao chép mã chờ'"
              @click="copyCode(waitlistResult.waitlistCode)"
            >
              {{ copiedCode === waitlistResult.waitlistCode ? 'Đã sao chép' : 'Sao chép' }}
            </button>
          </div>
          <div class="summary-grid">
            <span>{{ text.date }}</span><strong>{{ waitlistResult.reservationDate }}</strong>
            <span>{{ text.time }}</span><strong>{{ waitlistResult.preferredStartTime }} - {{ waitlistResult.preferredEndTime }}</strong>
            <span>{{ text.status }}</span><strong>{{ waitlistResult.status }}</strong>
          </div>
          <button class="primary-btn" type="button" @click="resetForm">{{ text.newBooking }}</button>
        </section>

        <form v-else class="reservation-card" @submit.prevent="submitReservation">
          <section v-show="step === 1" class="panel">
            <h2>{{ text.customerInfo }}</h2>
            <div class="form-grid">
              <label>
                {{ text.fullName }}
                <input v-model.trim="form.customerName" type="text" autocomplete="name" />
                <small v-if="errors.customerName">{{ errors.customerName }}</small>
              </label>
              <label>
                {{ text.phone }}
                <input v-model.trim="form.customerPhone" type="tel" autocomplete="tel" />
                <small v-if="errors.customerPhone">{{ errors.customerPhone }}</small>
              </label>
              <label>
                Email (có thể để trống)
                <input v-model.trim="form.customerEmail" type="email" autocomplete="email" />
                <small v-if="errors.customerEmail">{{ errors.customerEmail }}</small>
              </label>
              <label>
                {{ text.contactNote }}
                <input v-model.trim="form.contactNote" type="text" placeholder="Có thể để trống" />
              </label>
            </div>
          </section>

          <section v-show="step === 2" class="panel">
            <h2>{{ text.timeInfo }}</h2>
            <div class="form-grid">
              <label>
                {{ text.date }}
                <input v-model="form.reservationDate" type="date" :min="today" />
                <small v-if="errors.reservationDate">{{ errors.reservationDate }}</small>
              </label>
              <label>
                {{ text.time }}
                <input v-model="form.arrivalTime" type="time" min="09:00" max="21:59" />
                <small v-if="errors.arrivalTime">{{ errors.arrivalTime }}</small>
              </label>
              <label>
                {{ text.duration }}
                <select v-model.number="form.expectedDurationMinutes">
                  <option :value="90">90 {{ text.minutes }}</option>
                  <option :value="120">120 {{ text.minutes }}</option>
                  <option :value="180">180 {{ text.minutes }}</option>
                </select>
              </label>
            </div>
            <div v-if="lateDiningEndTime" class="late-dining-confirmation" role="alert">
              <strong>Thời gian dùng bữa vượt quá giờ phục vụ</strong>
              <p>Dự kiến kết thúc lúc {{ lateDiningEndTime }}; nhà hàng phục vụ đến 22:00.</p>
              <label>
                <input v-model="lateDiningConfirmed" type="checkbox" />
                Tôi xác nhận vẫn muốn dùng bữa tại nhà hàng theo thời gian đã chọn.
              </label>
              <small v-if="errors.lateDiningConfirmed">{{ errors.lateDiningConfirmed }}</small>
            </div>
          </section>

          <section v-show="step === 3" class="panel">
            <h2>{{ text.guestInfo }}</h2>
            <div class="form-grid compact">
              <label>
                {{ text.guests }}
                <input v-model.number="form.guestCount" type="number" min="1" max="10000" />
                <small v-if="errors.guestCount">{{ errors.guestCount }}</small>
                <small v-if="earlyGroupWarning" class="group-warning">{{ earlyGroupWarning }}</small>
              </label>
            </div>
            <p class="review-note">{{ text.guestHint }}</p>
          </section>

          <section v-show="step === 4" class="panel">
            <div class="panel-row">
              <h2>{{ text.areaInfo }}</h2>
              <button class="ghost-btn" type="button" @click="loadAreas" :disabled="loadingAreas">
                {{ loadingAreas ? text.loading : text.reloadAreas }}
              </button>
            </div>
            <div v-if="areaError" class="error-banner">
              {{ areaError }}
              <button type="button" @click="loadAreas">{{ text.retry }}</button>
            </div>
            <div v-if="loadingAreas" class="skeleton-grid">
              <div v-for="n in 3" :key="n" class="skeleton-card"></div>
            </div>
            <div v-else-if="!activeAreas.length" class="empty-state">{{ text.noAreas }}</div>
            <div v-else class="area-chip-grid">
              <button
                v-for="area in activeAreas"
                :key="area.id"
                type="button"
                :class="['area-chip', { selected: form.areaId === area.id }]"
                :aria-pressed="form.areaId === area.id"
                @click="selectArea(area)"
              >
                <span class="area-chip-title">{{ areaName(area) }}</span>
                <span class="area-chip-description">{{ areaDescription(area) }}</span>
                <span class="area-chip-meta">
                  {{ text.capacity }}: {{ area.capacity || '-' }} · {{ text.availableTables }}: {{ areaAvailableCount(area.id) }}
                </span>
                <span v-if="form.areaId === area.id" class="area-chip-selected">{{ text.selected }}</span>
              </button>
            </div>
          </section>

          <section v-show="step === 5" class="panel">
            <div class="panel-row">
              <h2>{{ text.tableInfo }}</h2>
              <button class="ghost-btn" type="button" @click="loadAvailableTables" :disabled="loadingTables">
                {{ loadingTables ? text.loading : text.reload }}
              </button>
            </div>
            <div v-if="tableError" class="error-banner">
              {{ tableError }}
              <button type="button" @click="loadAvailableTables">{{ text.retry }}</button>
            </div>
            <div v-if="loadingTables" class="skeleton-grid">
              <div v-for="n in 4" :key="n" class="skeleton-card table"></div>
            </div>
            <div v-if="!loadingTables && requiresTableCombination" class="combo-suggestion">
              <strong>Nhóm {{ form.guestCount }} khách cần ghép bàn</strong>
              <p v-if="tableCombo?.available">
                {{ tableCombo.reasons?.join(' ') }}
              </p>
              <template v-if="tableCombo?.available">
                <div class="combo-table-list">
                  <span v-for="table in tableCombo.tables" :key="table.tableId">
                    {{ table.tableName }} ({{ table.capacity }} chỗ)
                  </span>
                </div>
                <button class="primary-btn" type="button" @click="acceptTableCombination">
                  {{ form.tableIds.length ? 'Đã chọn tổ hợp bàn' : 'Chấp nhận ghép bàn' }}
                </button>
              </template>
              <span v-else>{{ tableCombo?.reasons?.[0] || 'Không tìm được tổ hợp bàn phù hợp.' }}</span>
              <button class="secondary-btn" type="button" @click="step = 4">Đổi khu vực</button>
            </div>
            <div v-if="!loadingTables && tables.length && !requiresTableCombination" class="table-map">
              <div class="table-map-header">
                <div>
                  <h3>{{ text.tableMap }}</h3>
                  <p>{{ text.tableMapHint }}</p>
                </div>
                <div class="map-legend">
                  <span class="legend available">{{ text.statusMap.AVAILABLE }}</span>
                  <span class="legend blocked">{{ text.unavailable }}</span>
                </div>
              </div>
              <div class="map-groups">
                <section v-for="group in tableMapGroups" :key="group.name" class="map-group">
                  <strong>{{ group.name }}</strong>
                  <div class="map-grid">
                    <button
                      v-for="table in group.tables"
                      :key="table.id"
                      type="button"
                      :class="['map-seat', tableMapStatusClass(table), { selected: form.tableId === table.id }]"
                      :disabled="table.availabilityStatus !== 'AVAILABLE'"
                      @click="selectTable(table)"
                    >
                      <img
                        :src="table.imageUrl || fallbackTableImage"
                        :alt="`Ảnh ${table.name}`"
                        class="map-seat-image"
                        loading="lazy"
                        @error="replaceTableImage"
                      />
                      <span>{{ table.name }}</span>
                      <small>{{ table.capacity || table.maxCapacity || '-' }} {{ text.seats }}</small>
                    </button>
                  </div>
                </section>
              </div>
            </div>
            <div v-if="!loadingTables && !availableTables.length && !tableCombo?.available" class="empty-state waitlist-offer">
              <strong>{{ text.noTables }}</strong>
              <span>{{ text.waitlistOffer }}</span>
              <button class="primary-btn" type="button" @click="submitWaitlist" :disabled="submitting">
                {{ submitting ? text.submitting : text.joinWaitlist }}
              </button>
            </div>
            <div v-if="!loadingTables && availableTables.length && !requiresTableCombination" class="table-grid">
              <article
                v-for="table in availableTables"
                :key="table.id"
                :class="['table-card', { selected: form.tableId === table.id }]"
              >
                <img :src="table.imageUrl || fallbackTableImage" :alt="table.name" loading="lazy" @error="replaceTableImage" />
                <div class="card-body">
                  <div class="card-title-row">
                    <strong>{{ table.name }}</strong>
                    <span class="status-pill available">{{ tableStatusLabel(table.availabilityStatus) }}</span>
                    <span v-if="suggestionById[table.id]?.best" class="status-pill suggested">{{ text.bestSuggestion }}</span>
                  </div>
                  <p>{{ table.positionDescription || table.areaName || table.floor }}</p>
                  <div class="meta-grid">
                    <span>{{ text.minGuests }}: {{ table.minCapacity || 1 }}</span>
                    <span>{{ text.maxGuests }}: {{ table.maxCapacity || table.capacity }}</span>
                    <span>{{ text.seats }}: {{ table.capacity || table.maxCapacity }}</span>
                    <span>{{ text.fit }}: {{ fitLabel(table.fitScore) }}</span>
                    <span v-if="suggestionById[table.id]">{{ text.score }}: {{ suggestionById[table.id].score }}</span>
                  </div>
                  <ul v-if="suggestionById[table.id]?.reasons?.length" class="reason-list">
                    <li v-for="reason in suggestionById[table.id].reasons" :key="reason">{{ reason }}</li>
                  </ul>
                  <div class="amenities">
                    <span v-if="table.windowSeat">{{ text.windowSeat }}</span>
                    <span v-if="table.privateRoom">{{ text.privateRoom }}</span>
                    <span v-if="table.childFriendly">{{ text.childFriendly }}</span>
                    <span v-if="table.hasView">{{ text.hasView }}</span>
                  </div>
                  <small v-if="table.warning">{{ table.warning }}</small>
                  <button class="primary-btn" type="button" @click="selectTable(table)">
                    {{ form.tableId === table.id ? text.selected : text.chooseTable }}
                  </button>
                </div>
              </article>
            </div>
          </section>

          <section v-show="step === 6" class="panel">
            <h2>{{ text.preorderTitle }}</h2>
            <div class="choice-grid">
              <button type="button" :class="{ selected: form.preorderEnabled }" @click="form.preorderEnabled = true">
                <strong>{{ text.preorderYes }}</strong>
                <span>{{ text.preorderYesHint }}</span>
              </button>
              <button type="button" :class="{ selected: !form.preorderEnabled }" @click="disablePreorder">
                <strong>{{ text.preorderNo }}</strong>
                <span>{{ text.preorderNoHint }}</span>
              </button>
            </div>

            <div v-if="form.preorderEnabled" class="menu-picker">
              <div class="filters">
                <input v-model.trim="menuSearch" type="search" :placeholder="text.searchDish" />
                <select v-model="menuCategory">
                  <option value="">{{ text.allCategories }}</option>
                  <option v-for="category in menuCategories" :key="category" :value="category">{{ category }}</option>
                </select>
                <button class="ghost-btn" type="button" @click="loadPreorderMenu">{{ text.reloadMenu }}</button>
              </div>
              <div v-if="menuError" class="error-banner">{{ menuError }}</div>
              <div v-if="cartItems.length" class="preorder-summary" aria-live="polite">
                <strong>Đã thêm {{ cartItems.length }} món</strong>
                <span>{{ cartQuantity }} phần đã chọn</span>
              </div>
              <div class="dish-grid">
                <article v-for="dish in filteredMenu" :key="dish.id" class="dish-card">
                  <img :src="dish.image || fallbackDishImage" :alt="dishName(dish)" loading="lazy" @error="replaceDishImage" />
                  <div>
                    <strong>{{ dishName(dish) }}</strong>
                    <span>{{ dishCategory(dish) }}</span>
                    <p>{{ dishDescription(dish) }}</p>
                    <b>{{ money(dish.price) }}</b>
                    <small v-if="cartQuantityForDish(dish.id)" class="dish-added">Đã thêm {{ cartQuantityForDish(dish.id) }} phần</small>
                    <button class="primary-btn" type="button" @click="addDish(dish)">{{ text.addDish }}</button>
                  </div>
                </article>
              </div>
              <aside v-if="cartItems.length" class="cart-box">
                <h3>{{ text.selectedDishes }}</h3>
                <div v-for="item in cartItems" :key="item.productId" class="cart-row">
                  <strong>{{ item.name }}</strong>
                  <div class="qty">
                    <button type="button" @click="changeQty(item.productId, -1)">-</button>
                    <span>{{ item.quantity }}</span>
                    <button type="button" @click="changeQty(item.productId, 1)">+</button>
                  </div>
                  <input v-model.trim="item.note" maxlength="300" :placeholder="text.dishNote" />
                  <span>{{ money(item.price * item.quantity) }}</span>
                  <button type="button" class="danger-btn" @click="removeDish(item.productId)">{{ text.remove }}</button>
                </div>
              </aside>
            </div>
          </section>

          <section v-show="step === 7" class="panel">
            <h2>{{ text.requestInfo }}</h2>
            <div class="preference-grid">
              <label v-for="item in visiblePreferences" :key="item">
                <input v-model="selectedPreferences" type="checkbox" :value="item" />
                {{ item }}
              </label>
            </div>
            <label class="wide-label">
              {{ text.specialRequest }}
              <textarea v-model.trim="form.specialRequest" maxlength="500" rows="5"></textarea>
              <small>{{ form.specialRequest.length }}/500</small>
            </label>
          </section>

          <section v-show="step === 8 && hasPreorderItems" class="panel">
            <h2>{{ text.paymentTitle }}</h2>
            <div class="choice-grid">
              <button v-for="option in paymentOptions" :key="option.key" type="button" :class="{ selected: form.paymentOption === option.key }" @click="selectPayment(option.key)">
                <strong>{{ option.label }}</strong>
                <span>{{ option.hint }}</span>
              </button>
            </div>
            <label class="voucher-field">
              {{ text.voucherCode }}
              <div>
                <input v-model.trim="form.voucherCode" type="text" :placeholder="text.voucherPlaceholder" @input="quote = null" />
                <button class="ghost-btn" type="button" @click="loadQuote">{{ text.applyVoucher }}</button>
              </div>
            </label>
            <div v-if="quote" class="review-box">
              <div><span>{{ text.tableAmount }}</span><strong>{{ money(quote.tableAmount) }}</strong></div>
              <div><span>{{ text.foodAmount }}</span><strong>{{ money(quote.foodAmount) }}</strong></div>
              <div v-if="quote.discountAmount > 0"><span>{{ text.originalTotal }}</span><strong>{{ money(quote.originalTotalAmount) }}</strong></div>
              <div v-if="quote.discountAmount > 0"><span>{{ text.discountAmount }} {{ quote.voucherCode ? `(${quote.voucherCode})` : '' }}</span><strong>-{{ money(quote.discountAmount) }}</strong></div>
              <div><span>{{ text.total }}</span><strong>{{ money(quote.totalAmount) }}</strong></div>
              <div v-if="quote.depositPolicy"><span>{{ text.depositPolicy }}</span><strong>{{ quote.depositPolicy.nameVi || quote.depositPolicy.policyCode }}</strong></div>
              <div><span>{{ text.payableNow }}</span><strong>{{ money(quote.payableNow) }}</strong></div>
              <div><span>{{ text.remaining }}</span><strong>{{ money(quote.remainingAmount) }}</strong></div>
            </div>
            <p class="review-note">{{ form.paymentOption === 'PAY_AT_RESTAURANT' ? text.payLaterWarning : text.qrAfterSubmit }}</p>
          </section>

          <section v-show="step === 9" class="panel">
            <h2>{{ text.review }}</h2>
            <div class="review-box">
              <div><span>{{ text.fullName }}</span><strong>{{ form.customerName }}</strong></div>
              <div><span>{{ text.phone }}</span><strong>{{ form.customerPhone }}</strong></div>
              <div><span>{{ text.date }}</span><strong>{{ form.reservationDate }} {{ form.arrivalTime }}</strong></div>
              <div><span>{{ text.guests }}</span><strong>{{ form.guestCount }}</strong></div>
              <div><span>{{ text.areaInfo }}</span><strong>{{ selectedAreaName }}</strong></div>
              <div><span>{{ text.tableInfo }}</span><strong>{{ selectedTable?.name }}</strong></div>
              <div><span>{{ text.selectedDishes }}</span><strong>{{ cartItems.length }}</strong></div>
              <div><span>{{ text.paymentTitle }}</span><strong>{{ paymentOptionLabel(form.paymentOption) }}</strong></div>
              <div><span>{{ text.total }}</span><strong>{{ money(quote?.totalAmount || selectedTable?.reservationPrice || 0) }}</strong></div>
            </div>
          </section>

          <div v-if="serverError" class="error-banner">{{ serverError }}</div>
          <div class="actions">
            <button class="ghost-btn" type="button" @click="step--" :disabled="step === 1 || submitting">{{ text.back }}</button>
            <button v-if="step < 9" class="primary-btn" type="button" @click="nextStep">{{ text.next }}</button>
            <button v-else class="primary-btn" type="submit" :disabled="submitting">
              {{ submitting ? text.submitting : text.submit }}
            </button>
          </div>
        </form>
      </section>
    </main>
  </CustomerLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import CustomerLayout from '@/components/CustomerLayout.vue'
import api from '@/services/api'
import { useFormatters } from '@/composables/useFormatters'
import {
  createEventBookingDraft,
  earlyGroupWarning as groupWarningFor,
  hasAvailableSingleTable,
  shouldRedirectToEventBooking,
  waitlistOverflowReason
} from '@/utils/reservationOverflow'

const { locale, tm } = useI18n()
const { formatCurrency, formatDateTime } = useFormatters()
const lang = computed(() => locale.value)
const step = ref(1)
const router = useRouter()
const areas = ref([])
const tables = ref([])
const suggestedTables = ref([])
const tableCombo = ref(null)
const menuItems = ref([])
const cartItems = ref([])
const areaCounts = ref({})
const selectedTable = ref(null)
const selectedPreferences = ref([])
const errors = ref({})
const areaError = ref('')
const tableError = ref('')
const menuError = ref('')
const serverError = ref('')
const loadingAreas = ref(false)
const loadingTables = ref(false)
const submitting = ref(false)
const submitResult = ref(null)
const waitlistResult = ref(null)
const copiedCode = ref('')
const paymentQr = ref(null)
const paymentCapabilityToken = ref('')
const paymentIdempotencyKey = ref(crypto.randomUUID())
const regenerateIdempotencyKey = ref('')
const qrLoading = ref(false)
const qrError = ref('')
const quote = ref(null)
const menuSearch = ref('')
const menuCategory = ref('')
const idempotencyKey = ref(crypto.randomUUID())
const lateDiningConfirmed = ref(false)
let tableRequestSequence = 0

const fallbackTableImage = 'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=480&q=80'
const fallbackDishImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80'
const today = new Date().toISOString().slice(0, 10)

const form = ref({
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  contactNote: '',
  reservationDate: today,
  arrivalTime: '',
  expectedDurationMinutes: 120,
  guestCount: 2,
  occasion: '',
  seatingPreference: '',
  specialRequest: '',
  areaId: null,
  tableId: null,
  tableIds: [],
  preorderEnabled: false,
  paymentOption: 'DEPOSIT_50',
  voucherCode: ''
})

const text = computed(() => tm('reservation'))
const activeAreas = computed(() => areas.value.filter(area => (area.status || 'ACTIVE') === 'ACTIVE'))
const suggestionById = computed(() => Object.fromEntries(suggestedTables.value.map(item => [item.tableId, item])))
const availableTables = computed(() => tables.value
  .filter(table => table.availabilityStatus === 'AVAILABLE')
    .sort((a, b) => (suggestionById.value[b.id]?.score || 0) - (suggestionById.value[a.id]?.score || 0)))
const requiresTableCombination = computed(() => {
  const hasSingleFit = hasAvailableSingleTable(tables.value, form.value.guestCount)
  return !hasSingleFit && (tableCombo.value?.combinationRequired || Boolean(tableCombo.value?.available))
})
const earlyGroupWarning = computed(() => groupWarningFor(form.value.guestCount))
const tableMapGroups = computed(() => {
  const groups = new Map()
  tables.value.forEach(table => {
    const groupName = table.floor || table.areaName || selectedAreaName.value || text.value.areaInfo
    if (!groups.has(groupName)) groups.set(groupName, [])
    groups.get(groupName).push(table)
  })
  return [...groups.entries()].map(([name, rows]) => ({
    name,
    tables: rows.slice().sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'vi', { numeric: true }))
  }))
})
const selectedAreaName = computed(() => {
  const area = areas.value.find(item => item.id === form.value.areaId)
  return area ? areaName(area) : ''
})
const menuCategories = computed(() => [...new Set(menuItems.value.map(dishCategory).filter(Boolean))])
const visiblePreferences = computed(() => text.value.preferences.filter(item => !/kh\u00f4ng gian ri\u00eang t\u01b0|private space/i.test(item)))
const filteredMenu = computed(() => {
  const keyword = menuSearch.value.toLowerCase()
  return menuItems.value.filter(item => {
    const matchesKeyword = !keyword || `${dishName(item)} ${dishDescription(item)} ${dishCategory(item)}`.toLowerCase().includes(keyword)
    const matchesCategory = !menuCategory.value || dishCategory(item) === menuCategory.value
    return matchesKeyword && matchesCategory
  })
})
const paymentOptions = computed(() => Object.entries(text.value.paymentOptions).map(([key, value]) => ({ key, label: value[0], hint: value[1] })))
const hasPreorderItems = computed(() => form.value.preorderEnabled && cartItems.value.length > 0)
const cartQuantity = computed(() => cartItems.value.reduce((total, item) => total + item.quantity, 0))
const skipPaymentStep = computed(() => !hasPreorderItems.value)
const lateDiningEndTime = computed(() => {
  const { arrivalTime, expectedDurationMinutes } = form.value
  if (!arrivalTime || !expectedDurationMinutes) return ''

  const [hour, minute] = arrivalTime.split(':').map(Number)
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return ''

  const endMinutes = hour * 60 + minute + Number(expectedDurationMinutes)
  if (endMinutes <= 22 * 60) return ''

  const endHour = Math.floor((endMinutes % (24 * 60)) / 60)
  const endMinute = endMinutes % 60
  const endTime = `${String(endHour).padStart(2, '0')}:${String(endMinute).padStart(2, '0')}`
  return endMinutes >= 24 * 60 ? `${endTime} ngày hôm sau` : endTime
})

function toggleLang() {
  locale.value = lang.value === 'vi' ? 'en' : 'vi'
}

const money = formatCurrency

function statusLabel(status) {
  return text.value.statusMap[status] || status
}

function tableStatusLabel(status) {
  return text.value.statusMap[status] || status
}

function tableMapStatusClass(table) {
  if (table.availabilityStatus === 'AVAILABLE') return 'available'
  if (table.availabilityStatus === 'TOO_SMALL') return 'too-small'
  if (table.availabilityStatus === 'RESERVED' || table.availabilityStatus === 'PENDING') return 'reserved'
  return 'blocked'
}

function fitLabel(score) {
  if (score === 0) return lang.value === 'vi' ? 'Vừa đủ' : 'Exact'
  if (score <= 2) return lang.value === 'vi' ? `Dư ${score} chỗ` : `${score} extra seats`
  return lang.value === 'vi' ? 'Bàn lớn' : 'Large table'
}

function paymentOptionLabel(key) {
  return text.value.paymentOptions[key]?.[0] || key
}

function areaName(area) {
  return lang.value === 'vi' ? area.nameVi : (area.nameEn || area.nameVi)
}

function areaDescription(area) {
  return lang.value === 'vi' ? area.descriptionVi : (area.descriptionEn || area.descriptionVi)
}

function areaAvailableCount(areaId) {
  const value = areaCounts.value[areaId]
  return value === undefined ? '-' : value
}

function dishName(dish) {
  return lang.value === 'vi' ? dish.nameVi : (dish.nameEn || dish.nameVi)
}

function dishCategory(dish) {
  return lang.value === 'vi' ? dish.categoryNameVi : (dish.categoryNameEn || dish.categoryNameVi)
}

function dishDescription(dish) {
  return lang.value === 'vi' ? dish.descriptionVi : (dish.descriptionEn || dish.descriptionVi)
}

function replaceTableImage(event) {
  event.target.src = fallbackTableImage
}

function replaceDishImage(event) {
  event.target.src = fallbackDishImage
}

function reservationTimeError() {
  const { reservationDate, arrivalTime } = form.value
  if (!reservationDate || !arrivalTime) return ''

  if (arrivalTime < '09:00' || arrivalTime >= '22:00') {
    return lang.value === 'vi'
      ? 'Vui l\u00f2ng ch\u1ecdn gi\u1edd trong khung ph\u1ee5c v\u1ee5 09:00-22:00.'
      : 'Please select a time between 09:00 and 22:00.'
  }

  const selectedTime = new Date(`${reservationDate}T${arrivalTime}:00`)
  if (reservationDate === today && !Number.isNaN(selectedTime.getTime()) && selectedTime <= new Date()) {
    return lang.value === 'vi'
      ? 'Th\u1eddi gian \u0111\u1eb7t b\u00e0n \u0111\u00e3 qua. Vui l\u00f2ng ch\u1ecdn gi\u1edd kh\u00e1c.'
      : 'The selected reservation time has already passed. Please choose another time.'
  }

  return ''
}

function isTimeValidationError(error) {
  const message = String(error?.response?.data?.message || error?.response?.data || '').toLowerCase()
  return /ngo\u00e0i gi\u1edd|gi\u1edd ho\u1ea1t \u0111\u1ed9ng|qu\u00e1 kh\u1ee9|past|operating hours|service hours/.test(message)
}

function validateCurrentStep() {
  errors.value = {}
  serverError.value = ''
  if (step.value === 1) {
    if (!form.value.customerName.trim()) errors.value.customerName = lang.value === 'vi' ? 'Vui lòng nhập họ tên' : 'Full name is required'
    if (!/^(0|\+84)(3|5|7|8|9)[0-9]{8}$/.test(form.value.customerPhone.replace(/\s/g, ''))) errors.value.customerPhone = lang.value === 'vi' ? 'Số điện thoại Việt Nam không hợp lệ' : 'Invalid Vietnamese phone'
    if (form.value.customerEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.customerEmail)) errors.value.customerEmail = 'Invalid email'
  }
  if (step.value === 2) {
    if (!form.value.reservationDate) errors.value.reservationDate = lang.value === 'vi' ? 'Vui lòng chọn ngày' : 'Date is required'
    if (!form.value.arrivalTime) errors.value.arrivalTime = lang.value === 'vi' ? 'Vui lòng chọn giờ' : 'Time is required'
  }
  if (step.value === 3 && (!form.value.guestCount || form.value.guestCount < 1)) errors.value.guestCount = lang.value === 'vi' ? 'Số khách không hợp lệ' : 'Invalid party size'
  if (step.value === 4 && !form.value.areaId) serverError.value = lang.value === 'vi' ? 'Vui lòng chọn khu vực' : 'Please select an area'
  if (step.value === 5 && !form.value.tableId) serverError.value = lang.value === 'vi' ? 'Vui lòng chọn bàn còn trống' : 'Please select an available table'
  if (step.value === 2 && form.value.arrivalTime) {
    const timeError = reservationTimeError()
    if (timeError) errors.value.arrivalTime = timeError
    if (lateDiningEndTime.value && !lateDiningConfirmed.value) {
      errors.value.lateDiningConfirmed = 'Vui lòng xác nhận thời gian dùng bữa sau giờ phục vụ.'
    }
  }
  return Object.keys(errors.value).length === 0 && !serverError.value
}

async function nextStep() {
  if (!validateCurrentStep()) return
  if (step.value === 3) await refreshAreaCounts()
  if (step.value === 4) await loadAvailableTables()
  if (step.value === 5 && !menuItems.value.length) await loadPreorderMenu()
  if (step.value === 6 && !hasPreorderItems.value) {
    step.value = 9
    return
  }
  if (step.value === 7 || step.value === 8) await loadQuote()
  step.value = Math.min(9, step.value + 1)
}

function goTo(target) {
  if (target === 8 && skipPaymentStep.value) return
  if (target < step.value) step.value = target
}

function selectArea(area) {
  if (form.value.areaId === area.id && tables.value.length) return
  form.value.areaId = area.id
  form.value.tableId = null
  form.value.tableIds = []
  selectedTable.value = null
  tables.value = []
  suggestedTables.value = []
  tableCombo.value = null
  quote.value = null
}

function selectTable(table) {
  selectedTable.value = table
  form.value.tableId = table.id
  form.value.tableIds = [table.id]
  quote.value = null
}

function acceptTableCombination() {
  const selected = tableCombo.value?.tables || []
  if (!selected.length) return
  form.value.tableIds = selected.map(table => table.tableId)
  form.value.tableId = selected.find(table => table.primary)?.tableId || selected[0].tableId
  selectedTable.value = tables.value.find(table => table.id === form.value.tableId) || null
  quote.value = null
}

async function loadAreas() {
  loadingAreas.value = true
  areaError.value = ''
  try {
    const res = await api.get('/api/areas')
    areas.value = Array.isArray(res.data) ? res.data : []
    if (areas.value.length && !form.value.areaId) form.value.areaId = areas.value[0].id
    await refreshAreaCounts()
  } catch (err) {
    areaError.value = err.response?.data?.message || (lang.value === 'vi' ? 'Không tải được danh sách khu vực' : 'Could not load areas')
  } finally {
    loadingAreas.value = false
  }
}

async function refreshAreaCounts() {
  if (!form.value.reservationDate || !form.value.arrivalTime || !form.value.guestCount) return
  const entries = await Promise.allSettled(activeAreas.value.map(async area => {
    const res = await api.get('/api/tables/available', {
      params: {
        date: form.value.reservationDate,
        time: form.value.arrivalTime,
        durationMinutes: form.value.expectedDurationMinutes,
        guestCount: form.value.guestCount,
        areaId: area.id
      }
    })
    return [area.id, res.data.filter(table => table.availabilityStatus === 'AVAILABLE').length]
  }))
  areaCounts.value = Object.fromEntries(entries.filter(item => item.status === 'fulfilled').map(item => item.value))
}

async function loadAvailableTables() {
  if (!form.value.reservationDate || !form.value.arrivalTime || !form.value.areaId) return
  const requestSequence = ++tableRequestSequence
  loadingTables.value = true
  tableError.value = ''
  try {
    const res = await api.get('/api/tables/available', {
      params: {
        date: form.value.reservationDate,
        time: form.value.arrivalTime,
        durationMinutes: form.value.expectedDurationMinutes,
        guestCount: form.value.guestCount,
        areaId: form.value.areaId
      }
    })
    if (requestSequence !== tableRequestSequence) return
    tables.value = Array.isArray(res.data) ? res.data : []
    await loadTableSuggestions()
    await loadTableCombination()
    if (requestSequence !== tableRequestSequence) return
    if (selectedTable.value && !tables.value.some(table => table.id === selectedTable.value.id && table.availabilityStatus === 'AVAILABLE')) {
      selectedTable.value = null
      form.value.tableId = null
      form.value.tableIds = []
      serverError.value = lang.value === 'vi'
        ? 'Bàn đã chọn không còn phù hợp. Vui lòng chọn lại.'
        : 'The selected table is no longer suitable. Please choose another table.'
    }
  } catch (err) {
    if (isTimeValidationError(err)) {
      step.value = 2
      errors.value = {
        ...errors.value,
        arrivalTime: String(err.response?.data?.message || err.response?.data || reservationTimeError())
      }
      serverError.value = ''
      return
    }
    if (requestSequence !== tableRequestSequence) return
    tableError.value = err.response?.data?.message || (lang.value === 'vi' ? 'Không tải được bàn phù hợp' : 'Could not load matching tables')
  } finally {
    if (requestSequence === tableRequestSequence) loadingTables.value = false
  }
}

watch(
  () => [
    form.value.reservationDate,
    form.value.arrivalTime,
    form.value.expectedDurationMinutes,
    form.value.guestCount,
    form.value.areaId
  ],
  async (current, previous) => {
    if (!previous || current.every((value, index) => value === previous[index])) return
    if (!form.value.areaId || !form.value.reservationDate || !form.value.arrivalTime || !form.value.guestCount) return

    if (current.slice(0, 4).some((value, index) => value !== previous[index])) {
      form.value.tableId = null
      form.value.tableIds = []
      selectedTable.value = null
      tableCombo.value = null
      quote.value = null
      lateDiningConfirmed.value = false
    }
    await loadAvailableTables()
  }
)

async function loadTableSuggestions() {
  try {
    const res = await api.post('/api/reservations/table-suggestions', {
      reservationDate: form.value.reservationDate,
      arrivalTime: form.value.arrivalTime,
      durationMinutes: form.value.expectedDurationMinutes,
      guestCount: form.value.guestCount,
      areaId: form.value.areaId,
      seatingPreference: selectedPreferences.value.join(', '),
      customerPhone: form.value.customerPhone
    })
    suggestedTables.value = Array.isArray(res.data) ? res.data : []
  } catch {
    suggestedTables.value = []
  }
}

async function loadTableCombination() {
  tableCombo.value = null
  const hasSingleFit = hasAvailableSingleTable(tables.value, form.value.guestCount)
  if (hasSingleFit) return
  try {
    const res = await api.post('/api/reservations/table-combinations', {
      reservationDate: form.value.reservationDate,
      arrivalTime: form.value.arrivalTime,
      durationMinutes: form.value.expectedDurationMinutes,
      guestCount: form.value.guestCount,
      areaId: form.value.areaId,
      seatingPreference: selectedPreferences.value.join(', '),
      customerPhone: form.value.customerPhone
    })
    tableCombo.value = res.data || null
    if (shouldRedirectToEventBooking(tableCombo.value, form.value.guestCount)) redirectToEventBooking()
  } catch {
    tableCombo.value = { available: false, combinationRequired: false, reasons: [] }
    if (shouldRedirectToEventBooking(tableCombo.value, form.value.guestCount)) redirectToEventBooking()
  }
}

function redirectToEventBooking() {
  sessionStorage.setItem('event-booking-draft', JSON.stringify(createEventBookingDraft(form.value)))
  router.push('/dat-su-kien')
}

async function loadPreorderMenu() {
  menuError.value = ''
  try {
    const res = await api.get('/api/menu-items/preorder')
    menuItems.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    menuError.value = err.response?.data?.message || (lang.value === 'vi' ? 'Không tải được danh sách món' : 'Could not load menu')
  }
}

function addDish(dish) {
  const existing = cartItems.value.find(item => item.productId === dish.id)
  if (existing) {
    existing.quantity += 1
    return
  }
  cartItems.value.push({ productId: dish.id, name: dishName(dish), price: Number(dish.price || 0), quantity: 1, note: '' })
  quote.value = null
}

function changeQty(productId, delta) {
  const item = cartItems.value.find(row => row.productId === productId)
  if (!item) return
  item.quantity += delta
  if (item.quantity <= 0) removeDish(productId)
  quote.value = null
}

function removeDish(productId) {
  cartItems.value = cartItems.value.filter(item => item.productId !== productId)
  quote.value = null
}

function disablePreorder() {
  form.value.preorderEnabled = false
  cartItems.value = []
  quote.value = null
}

function selectPayment(option) {
  form.value.paymentOption = option
  quote.value = null
}

function preorderPayload() {
  return form.value.preorderEnabled ? cartItems.value.map(item => ({
    productId: item.productId,
    quantity: item.quantity,
    note: item.note
  })) : []
}

async function loadQuote() {
  if (!form.value.tableId) return
  const res = await api.post('/api/reservations/quote', {
    areaId: form.value.areaId,
    tableId: form.value.tableId,
    reservationDate: form.value.reservationDate,
    arrivalTime: form.value.arrivalTime,
    durationMinutes: form.value.expectedDurationMinutes,
    guestCount: form.value.guestCount,
    preorderItems: preorderPayload(),
    paymentOption: form.value.paymentOption,
    voucherCode: form.value.voucherCode
  })
  quote.value = res.data
}

async function submitReservation() {
  if (!validateCurrentStep()) return
  submitting.value = true
  serverError.value = ''
  paymentQr.value = null
  qrError.value = ''
  try {
    await loadQuote()
    const payload = {
      ...form.value,
      customerPhone: form.value.customerPhone.replace(/\s/g, ''),
      seatingPreference: selectedPreferences.value.join(', '),
      lateDiningConfirmed: lateDiningConfirmed.value,
      preorderItems: preorderPayload()
    }
    const res = await api.post('/api/reservations', payload, {
      headers: { 'X-Idempotency-Key': idempotencyKey.value }
    })
    submitResult.value = res.data
    paymentCapabilityToken.value = res.data.paymentCapabilityToken || ''
    if (paymentCapabilityToken.value && res.data.reservationCode) {
      sessionStorage.setItem(`reservation-capability:${res.data.reservationCode}`, paymentCapabilityToken.value)
    }
    idempotencyKey.value = crypto.randomUUID()
    if (form.value.paymentOption !== 'PAY_AT_RESTAURANT' && Number(res.data.depositAmount || 0) > 0) {
      await createPaymentQr()
    }
  } catch (err) {
    if (isTimeValidationError(err)) {
      step.value = 2
      errors.value = {
        ...errors.value,
        arrivalTime: String(err.response?.data?.message || err.response?.data || reservationTimeError())
      }
      serverError.value = ''
      return
    }
    serverError.value = err.response?.data?.message || err.response?.data || (lang.value === 'vi' ? 'Không gửi được yêu cầu đặt bàn' : 'Could not submit reservation')
  } finally {
    submitting.value = false
  }
}

function cartQuantityForDish(productId) {
  return cartItems.value.find(item => item.productId === productId)?.quantity || 0
}

async function copyCode(code) {
  if (!code) return

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(code)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = code
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    copiedCode.value = code
    window.setTimeout(() => {
      if (copiedCode.value === code) copiedCode.value = ''
    }, 1800)
  } catch (error) {
    console.error('Could not copy reservation code', error)
  }
}

async function createPaymentQr() {
  if (!submitResult.value?.reservationCode || qrLoading.value) return
  qrLoading.value = true
  qrError.value = ''
  try {
    const qrRes = await api.post('/api/payments/qr', {
      reservationCode: submitResult.value.reservationCode,
      paymentOption: form.value.paymentOption
    }, {
      headers: {
        'X-Payment-Capability': paymentCapabilityToken.value,
        'X-Idempotency-Key': paymentIdempotencyKey.value
      }
    })
    paymentQr.value = qrRes.data
  } catch (err) {
    qrError.value = err.response?.data?.message
      || (lang.value === 'vi' ? 'Không tạo được QR thanh toán.' : 'Could not create payment QR.')
  } finally {
    qrLoading.value = false
  }
}

async function regeneratePaymentQr() {
  if (!paymentQr.value?.paymentCode || qrLoading.value) return
  if (!regenerateIdempotencyKey.value) {
    regenerateIdempotencyKey.value = crypto.randomUUID()
  }
  qrLoading.value = true
  qrError.value = ''
  try {
    const qrRes = await api.post(`/api/payments/${paymentQr.value.paymentCode}/regenerate`, null, {
      headers: {
        'X-Payment-Capability': paymentCapabilityToken.value,
        'X-Idempotency-Key': regenerateIdempotencyKey.value
      }
    })
    paymentQr.value = qrRes.data
    regenerateIdempotencyKey.value = ''
  } catch (err) {
    qrError.value = err.response?.data?.message
      || (lang.value === 'vi' ? 'Không thể tạo lại QR.' : 'Could not regenerate QR.')
  } finally {
    qrLoading.value = false
  }
}

async function submitWaitlist() {
  const originalStep = step.value
  for (const targetStep of [1, 2, 3, 4]) {
    step.value = targetStep
    if (!validateCurrentStep()) {
      return
    }
  }
  step.value = originalStep
  submitting.value = true
  serverError.value = ''
  try {
    const [hour, minute] = form.value.arrivalTime.split(':').map(Number)
    const endDate = new Date()
    endDate.setHours(hour, minute + Number(form.value.expectedDurationMinutes || 120), 0, 0)
    const preferredEndTime = `${String(endDate.getHours()).padStart(2, '0')}:${String(endDate.getMinutes()).padStart(2, '0')}`
    const res = await api.post('/api/reservation-waitlist', {
      customerName: form.value.customerName,
      customerPhone: form.value.customerPhone.replace(/\s/g, ''),
      customerEmail: form.value.customerEmail,
      reservationDate: form.value.reservationDate,
      preferredStartTime: form.value.arrivalTime,
      preferredEndTime,
      guestCount: form.value.guestCount,
      areaId: form.value.areaId,
      seatingPreference: selectedPreferences.value.join(', '),
      specialRequest: form.value.specialRequest,
      overflowReason: waitlistOverflowReason(form.value.guestCount)
    })
    waitlistResult.value = res.data
  } catch (err) {
    serverError.value = err.response?.data?.message || err.response?.data || (lang.value === 'vi' ? 'Không tạo được danh sách chờ' : 'Could not join waitlist')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  submitResult.value = null
  waitlistResult.value = null
  copiedCode.value = ''
  lateDiningConfirmed.value = false
  paymentQr.value = null
  paymentCapabilityToken.value = ''
  paymentIdempotencyKey.value = crypto.randomUUID()
  regenerateIdempotencyKey.value = ''
  qrError.value = ''
  quote.value = null
  step.value = 1
  form.value.tableId = null
  form.value.tableIds = []
  tableCombo.value = null
  selectedTable.value = null
  selectedPreferences.value = []
  cartItems.value = []
  form.value.voucherCode = ''
  idempotencyKey.value = crypto.randomUUID()
}

onMounted(async () => {
  await loadAreas()
  await loadPreorderMenu()
})
</script>

<style scoped>
.reservation-page {
  min-height: 100vh;
  background: #DED8C2;
  color: #201D14;
  padding: 32px 16px 56px;
}

.reservation-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.reservation-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #5A6E45;
  font-weight: 800;
  text-transform: uppercase;
  font-size: 0.78rem;
}

.reservation-header h1 {
  margin: 0;
  font-size: 2.35rem;
  color: #22301B;
}

.reservation-header p {
  max-width: 720px;
  color: #7A7460;
}

.stepper {
  display: grid;
  grid-template-columns: repeat(9, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 18px;
}

.step-chip {
  border: 1px solid #CFC7A8;
  background: #FFFFFF;
  border-radius: 8px;
  min-height: 48px;
  padding: 6px;
  color: #7A7460;
  font-weight: 700;
  cursor: pointer;
}

.step-chip span {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #E7E3D2;
  color: #5A6E45;
  margin-right: 6px;
}

.step-chip.active,
.step-chip.done {
  border-color: #5A6E45;
  color: #201D14;
}

.reservation-card,
.success-panel {
  background: #FFFFFF;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  box-shadow: 0 18px 40px rgba(35, 48, 43, 0.08);
  padding: 24px;
}

.combo-suggestion {
  display: grid;
  gap: 12px;
  max-width: 620px;
  margin: 20px auto;
  padding: 22px;
  border: 1px solid #5A6E45;
  border-radius: 10px;
  background: #F5F4E9;
  color: #22301B;
}

.combo-table-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.combo-table-list span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #FFFFFF;
  border: 1px solid #CFC7A8;
}

.panel h2,
.success-panel h2 {
  margin: 0 0 18px;
  color: #22301B;
}

.panel-row,
.card-title-row,
.actions,
.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-grid.compact {
  max-width: 360px;
}

label {
  display: grid;
  gap: 7px;
  font-weight: 700;
  color: #55503E;
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  padding: 11px 12px;
  background: #FFFFFF;
  color: #201D14;
}

input:focus,
select:focus,
textarea:focus {
  outline: 3px solid #E7E3D2;
  border-color: #5A6E45;
}

small {
  color: #B23B2E;
}

.lang-toggle,
.ghost-btn,
.primary-btn,
.danger-btn {
  min-height: 40px;
  border-radius: 8px;
  border: 1px solid #CFC7A8;
  padding: 0 16px;
  font-weight: 800;
  cursor: pointer;
}

.lang-toggle,
.ghost-btn {
  background: #FFFFFF;
  color: #201D14;
}

.primary-btn {
  background: #5A6E45;
  color: #FFFFFF;
  border-color: #5A6E45;
}

.table-card .primary-btn {
  width: 100%;
  white-space: nowrap;
  margin-top: auto;
}

.danger-btn {
  background: #FFFFFF;
  color: #B23B2E;
  border-color: #E8C9C4;
}

.primary-btn:disabled,
.ghost-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.area-grid,
.table-grid,
.dish-grid,
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.area-card,
.table-card,
.dish-card {
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  overflow: hidden;
  background: #FFFFFF;
}

.area-chip-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.area-chip {
  min-width: min(100%, 260px);
  flex: 1 1 240px;
  display: grid;
  gap: 5px;
  padding: 14px;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  background: #FFFFFF;
  color: #55503E;
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.area-chip:hover,
.area-chip:focus-visible {
  border-color: #5A6E45;
  outline: none;
}

.area-chip.selected {
  border-color: #5A6E45;
  box-shadow: 0 0 0 3px #E7E3D2;
}

.area-chip-title { font-weight: 900; color: #22301B; }
.area-chip-description { color: #7A7460; font-size: 0.86rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.area-chip-meta { color: #5A6E45; font-size: 0.78rem; font-weight: 700; }
.area-chip-selected { color: #5A6E45; font-size: 0.78rem; font-weight: 800; }

.table-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.table-card > .card-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.dish-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.dish-card > div {
  flex: 1;
  min-width: 0;
}

.dish-card .primary-btn {
  width: 100%;
  white-space: nowrap;
  flex-shrink: 0;
}

.area-card.selected,
.table-card.selected {
  border-color: #5A6E45;
  box-shadow: 0 0 0 3px #E7E3D2;
}

.area-card img,
.table-card img,
.dish-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}

.table-map {
  border: 1px solid #E7E3D2;
  border-radius: 8px;
  background: #DED8C2;
  padding: 16px;
  margin-bottom: 18px;
}

.table-map-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.table-map-header h3 {
  margin: 0 0 4px;
  color: #22301B;
}

.table-map-header p {
  margin: 0;
  color: #7A7460;
}

.map-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.legend {
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 0.78rem;
  font-weight: 900;
}

.legend.available {
  background: #E7E3D2;
  color: #22301B;
}

.legend.blocked {
  background: #DED8C2;
  color: #55503E;
}

.map-groups {
  display: grid;
  gap: 14px;
}

.map-group {
  display: grid;
  gap: 8px;
}

.map-group > strong {
  color: #55503E;
}

.map-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 10px;
}

.map-seat {
  min-height: 132px;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  background: #FFFFFF;
  color: #55503E;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 5px;
  cursor: pointer;
  font: inherit;
  overflow: hidden;
}

.map-seat span,
.map-seat small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-seat span {
  font-weight: 900;
}

.map-seat small {
  color: #7A7460;
  font-size: 0.76rem;
}

.map-seat.available {
  border-color: #B9D8C2;
  background: #F3F7F0;
  color: #22301B;
}

.map-seat.reserved {
  border-color: #E7D5B8;
  background: #F5F0E4;
  color: #8A641F;
}

.map-seat.too-small {
  border-color: #D7E3ED;
  background: #EEF3F6;
  color: #5A6E45;
}

.map-seat.blocked,
.map-seat:disabled {
  background: #DED8C2;
  color: #7A7460;
  cursor: not-allowed;
  opacity: 0.75;
}

.map-seat.selected {
  outline: 3px solid #5A6E45;
  outline-offset: 2px;
}

.card-body,
.dish-card > div {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.card-body p,
.dish-card p,
.review-note {
  color: #7A7460;
  margin: 0;
}

.meta-grid,
.summary-grid,
.review-box {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.voucher-field {
  display: grid;
  gap: 8px;
  margin: 16px 0;
}

.voucher-field > div {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.meta-grid span,
.review-box > div,
.summary-grid > span,
.summary-grid > strong {
  background: #DED8C2;
  border-radius: 8px;
  padding: 10px;
}

.amenities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.amenities span,
.status-pill {
  background: #E7E3D2;
  color: #5A6E45;
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
}

.status-pill.available {
  background: #E7E3D2;
  color: #2F8F5B;
}

.status-pill.suggested {
  background: #EEF3F6;
  color: #5A6E45;
}

.reason-list {
  margin: 0;
  padding-left: 18px;
  color: #55503E;
  font-size: 0.86rem;
}

.reason-list li + li {
  margin-top: 3px;
}

.ghost-link {
  color: #5A6E45;
  font-weight: 800;
  text-decoration: none;
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.choice-grid button {
  text-align: left;
  display: grid;
  gap: 8px;
  min-height: 110px;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  background: #FFFFFF;
  padding: 16px;
  cursor: pointer;
}

.choice-grid button.selected {
  border-color: #5A6E45;
  background: #E7E3D2;
}

.filters {
  display: grid;
  grid-template-columns: 1fr minmax(180px, 240px) auto;
  gap: 10px;
  margin: 18px 0;
}

.map-seat-image {
  width: calc(100% - 12px);
  height: 62px;
  object-fit: cover;
  border-radius: 5px;
  pointer-events: none;
}

.cart-box {
  margin-top: 18px;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  padding: 14px;
  background: #DED8C2;
}

.cart-row {
  display: grid;
  grid-template-columns: 1.3fr 110px 1.4fr 120px auto;
  gap: 10px;
  align-items: center;
  margin-top: 10px;
}

.qty {
  display: grid;
  grid-template-columns: 32px 1fr 32px;
  align-items: center;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  overflow: hidden;
  text-align: center;
}

.qty button {
  border: 0;
  background: #E7E3D2;
  height: 34px;
  cursor: pointer;
}

.wide-label {
  margin-top: 16px;
}

.preference-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.preference-grid label {
  display: flex;
  align-items: center;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  padding: 10px;
}

.preference-grid input {
  width: auto;
}

.error-banner,
.empty-state {
  background: #F5F0E4;
  color: #8A641F;
  border: 1px solid #E7D5B8;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 14px;
}

.empty-state {
  background: #DED8C2;
  color: #7A7460;
  border-color: #CFC7A8;
}

.waitlist-offer {
  display: grid;
  gap: 10px;
  justify-items: start;
}

.skeleton-card {
  height: 260px;
  border-radius: 8px;
  background: linear-gradient(90deg, #DED8C2, #CFC7A8, #DED8C2);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

.skeleton-card.table {
  height: 300px;
}

.reservation-code-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
}

.preorder-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid var(--primary);
  border-radius: 8px;
  background: var(--color-surface-container-low);
  color: var(--text-primary);
}

.preorder-summary span,
.dish-added {
  color: var(--primary);
  font-weight: 700;
}

.dish-added {
  display: block;
  margin-top: 8px;
}

.late-dining-confirmation {
  margin-top: 16px;
  border: 1px solid #D1A13A;
  border-radius: 8px;
  background: #FFF8E6;
  color: #4A3410;
  padding: 14px;
}

.late-dining-confirmation p {
  margin: 6px 0 10px;
}

.late-dining-confirmation label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
}

.late-dining-confirmation input {
  width: auto;
  min-height: auto;
  margin-top: 3px;
}

.late-dining-confirmation small {
  display: block;
  margin-top: 8px;
}

.reservation-code {
  display: inline-flex;
  margin: 0;
  background: #22301B;
  color: #FFFFFF;
  border-radius: 8px;
  padding: 10px 14px;
  font-weight: 900;
  letter-spacing: 1px;
}

.reservation-code::selection {
  background: #F7D77B;
  color: #1B1212;
}

.reservation-code::-moz-selection {
  background: #F7D77B;
  color: #1B1212;
}

.code-copy-btn {
  min-height: 44px;
  padding: 8px 12px;
  border: 1px solid var(--primary);
  border-radius: 8px;
  background: var(--bg-card);
  color: var(--primary);
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.code-copy-btn:hover,
.code-copy-btn:focus-visible {
  background: var(--primary);
  color: var(--color-on-primary);
}

.qr-card {
  display: grid;
  grid-template-columns: 1fr 240px;
  gap: 18px;
  margin: 18px 0;
  padding: 18px;
  border: 1px solid #CFC7A8;
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
}

.qr-card dl div {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 10px;
}

.qr-card dt {
  color: #7A7460;
}

.table-preview {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #CFC7A8;
}

.table-preview h3,
.table-preview p {
  margin: 0;
}

.table-preview p {
  color: #6A6657;
  margin-top: 4px;
}

.table-preview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.preview-table {
  display: grid;
  gap: 6px;
  min-height: 132px;
  padding: 16px;
  text-align: left;
  color: #201D14;
  background: #FFFFFF;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  cursor: pointer;
}

.preview-table:hover,
.preview-table:focus-visible,
.preview-table.selected {
  border-color: #5A6E45;
  box-shadow: 0 0 0 3px #E7E3D2;
  outline: none;
}

.preview-table span,
.preview-table small {
  color: #6A6657;
}

.qr-card > .secondary-btn {
  grid-column: 1 / -1;
  justify-self: end;
}

.qr-local-state {
  margin: 18px 0;
  padding: 18px;
  border: 1px solid #CFC7A8;
  background: #FFFFFF;
}

.qr-error-state {
  border-color: #A74335;
  color: #7C2E24;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

@media (max-width: 1024px) {
  .reservation-page { padding: 28px 12px 48px; }
  .stepper {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .table-map-header {
    display: grid;
  }

  .map-legend {
    justify-content: flex-start;
  }

  .map-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .cart-row {
    grid-template-columns: minmax(0, 1fr) 120px;
  }

  .cart-row .danger-btn {
    grid-column: 1 / -1;
  }

  .qr-card {
    grid-template-columns: minmax(0, 1fr) 220px;
  }

  .qr-card img {
    width: 100%;
    height: auto;
  }
}

@media (max-width: 640px) {
  .reservation-page,
  .reservation-page * { box-sizing: border-box; }
  .reservation-page { overflow-x: hidden; padding: 20px 8px 40px; }
  .reservation-header { align-items: stretch; flex-direction: column; }
  .reservation-header h1 { font-size: 1.85rem; }
  .reservation-header p { margin-bottom: 0; }
  .lang-toggle { align-self: flex-end; }
  .stepper { gap: 6px; }
  .step-chip { min-width: 0; min-height: 52px; padding: 5px 3px; font-size: 0.78rem; }
  .step-chip span { width: 22px; height: 22px; margin-right: 3px; }
  .reservation-card,
  .success-panel { padding: 18px 12px; }
  .panel h2,
  .success-panel h2 { font-size: 1.35rem; }
  .panel-row,
  .card-title-row,
  .card-actions { align-items: flex-start; flex-wrap: wrap; }
  .form-grid,
  .meta-grid,
  .review-box,
  .voucher-field > div,
  .summary-grid,
  .filters,
  .cart-row,
  .qr-card,
  .qr-card dl div { grid-template-columns: 1fr; }
  .form-grid.compact { max-width: none; }
  input,
  select,
  textarea,
  .lang-toggle,
  .ghost-btn,
  .primary-btn,
  .danger-btn { min-height: 44px; }
  .area-grid,
  .table-grid,
  .table-preview-grid,
  .dish-grid,
  .skeleton-grid,
  .choice-grid,
  .preference-grid { grid-template-columns: 1fr; }
  .area-card img,
  .table-card img,
  .dish-card img { height: 180px; }
  .table-map { padding: 12px; }
  .map-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .card-actions .primary-btn,
  .actions .primary-btn,
  .actions .ghost-btn { flex: 1 1 100%; }
  .actions { align-items: stretch; flex-direction: column; }
  .cart-row .danger-btn { grid-column: auto; width: 100%; }
  .qty { min-height: 44px; }
  .qty button { min-height: 44px; }
  .qr-card { padding: 14px; }
  .qr-card img { max-width: 260px; margin: 0 auto; }
  .qr-card dl div { gap: 4px; }
}

/* GustoPro reservation workspace. The wizard logic and API payloads remain unchanged. */
.reservation-page { background: var(--color-surface); padding: 42px 24px 72px; }
.reservation-shell { max-width: 1240px; margin: 0 auto; }
.reservation-header { align-items: flex-start; margin-bottom: 24px; }
.eyebrow { color: var(--primary); letter-spacing: 0.08em; }
.reservation-header h1 { font-family: var(--font-display); color: var(--text-primary); font-size: 2.5rem; }
.reservation-header p { color: var(--text-secondary); }
.lang-toggle, .ghost-btn { border-color: var(--color-outline-variant); background: var(--bg-card); color: var(--text-primary); }
.lang-toggle:hover, .ghost-btn:hover { border-color: var(--primary); color: var(--primary); background: var(--color-surface-container-low); }
.stepper { gap: 8px; margin-bottom: 22px; }
.step-chip { border-color: var(--color-outline-variant); background: var(--bg-card); border-radius: var(--radius-sm); color: var(--text-secondary); }
.step-chip span { background: var(--color-surface-container); color: var(--primary); }
.step-chip.active, .step-chip.done { border-color: var(--primary); color: var(--primary); background: var(--color-surface-container-low); }
.step-chip.active span, .step-chip.done span { background: var(--primary); color: var(--color-on-primary); }
.reservation-card, .success-panel { background: var(--bg-card); border-color: var(--color-outline-variant); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); padding: 28px; }
.panel h2, .success-panel h2 { font-family: var(--font-display); color: var(--text-primary); }
label { color: var(--text-primary); }
input, select, textarea { border-color: var(--color-outline-variant); background: var(--color-surface-container-lowest); color: var(--text-primary); }
input:focus, select:focus, textarea:focus { outline-color: var(--primary-glow); border-color: var(--primary); }
.primary-btn { background: var(--primary); border-color: var(--primary); color: var(--color-on-primary); }
.primary-btn:hover:not(:disabled) { background: var(--primary-dark); }
.area-card, .table-card, .dish-card, .preview-table { border-color: var(--color-outline-variant); background: var(--bg-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.area-card.selected, .table-card.selected, .preview-table.selected { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
.status-pill { background: var(--color-surface-container); color: var(--primary); }
.table-map { background: var(--color-surface-container-low); border-color: var(--color-outline-variant); }
.map-seat { border-color: var(--color-outline-variant); background: var(--bg-card); color: var(--text-primary); }
.map-seat.available { border-color: #8dc6a4; background: #eff9f1; color: #176b3a; }
.map-seat.reserved { border-color: #f1c46a; background: #fff5db; color: #805300; }
.map-seat.blocked, .map-seat:disabled { background: var(--color-surface-container); }
.summary-grid > div, .review-box, .qr-card { border-color: var(--color-outline-variant); background: var(--color-surface-container-low); }
.reservation-code { background: var(--primary); color: var(--color-on-primary); }
@media (max-width: 1024px) { .reservation-page { padding: 32px 18px 56px; } }
@media (max-width: 640px) { .reservation-page { padding: 24px 12px 48px; } .reservation-card, .success-panel { padding: 18px 14px; } .reservation-header h1 { font-size: 2rem; } }
@media (max-width: 1024px) {
  .reservation-header, .panel-row { min-width: 0; }
  .area-grid, .table-grid, .dish-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .stepper { grid-template-columns: repeat(3, minmax(0, 1fr)); overflow: hidden; }
  .step-chip { font-size: 0.7rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .step-chip span { flex: 0 0 22px; }
  .area-grid, .table-grid, .dish-grid, .table-preview-grid { grid-template-columns: 1fr; }
  .area-card, .table-card, .dish-card, .preview-table { min-width: 0; }
  .card-title-row, .meta-grid, .reason-list, .summary-grid { min-width: 0; overflow-wrap: anywhere; }
  .qr-card img { width: min(100%, 260px); }
}
</style>
