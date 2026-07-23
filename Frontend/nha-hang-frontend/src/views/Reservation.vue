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
            :class="['step-chip', { active: step === index + 1, done: step > index + 1 }]"
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
          <div class="reservation-code">{{ submitResult.reservationCode }}</div>
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
          <div class="reservation-code">{{ waitlistResult.waitlistCode }}</div>
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
                Email
                <input v-model.trim="form.customerEmail" type="email" autocomplete="email" />
                <small v-if="errors.customerEmail">{{ errors.customerEmail }}</small>
              </label>
              <label>
                {{ text.contactNote }}
                <input v-model.trim="form.contactNote" type="text" />
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
                <input v-model="form.arrivalTime" type="time" min="09:00" max="22:00" />
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
              <label>
                {{ text.occasion }}
                <select v-model="form.occasion">
                  <option value="">{{ text.selectOptional }}</option>
                  <option v-for="occasion in text.occasions" :key="occasion" :value="occasion">{{ occasion }}</option>
                </select>
              </label>
            </div>
          </section>

          <section v-show="step === 3" class="panel">
            <h2>{{ text.guestInfo }}</h2>
            <div class="form-grid compact">
              <label>
                {{ text.guests }}
                <input v-model.number="form.guestCount" type="number" min="1" max="30" />
                <small v-if="errors.guestCount">{{ errors.guestCount }}</small>
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
            <div v-else class="area-grid">
              <article
                v-for="area in activeAreas"
                :key="area.id"
                :class="['area-card', { selected: form.areaId === area.id }]"
              >
                <img :src="area.imageUrl || fallbackAreaImage" :alt="areaName(area)" loading="lazy" @error="replaceImage" />
                <div class="card-body">
                  <div class="card-title-row">
                    <strong>{{ areaName(area) }}</strong>
                    <span class="status-pill">{{ text.active }}</span>
                  </div>
                  <p>{{ areaDescription(area) }}</p>
                  <div class="meta-grid">
                    <span>{{ text.capacity }}: {{ area.capacity || '-' }}</span>
                    <span>{{ text.areaType }}: {{ area.code || area.nameEn || '-' }}</span>
                    <span>{{ text.price }}: {{ money(area.basePrice) }}</span>
                    <span>{{ text.availableTables }}: {{ areaAvailableCount(area.id) }}</span>
                  </div>
                  <div class="amenities">
                    <span>{{ text.family }}</span>
                    <span>{{ text.quiet }}</span>
                    <span>{{ text.photoReady }}</span>
                  </div>
                  <div class="card-actions">
                    <button class="primary-btn" type="button" @click="selectArea(area)">
                      {{ form.areaId === area.id ? text.selected : text.chooseArea }}
                    </button>
                    <a class="ghost-link" :href="area.imageUrl || fallbackAreaImage" target="_blank" rel="noreferrer">
                      {{ text.viewImage }}
                    </a>
                  </div>
                </div>
              </article>
            </div>
            <section v-if="form.areaId" class="table-preview" aria-live="polite">
              <div class="panel-row">
                <div>
                  <h3>{{ text.tableInfo }}</h3>
                  <p>{{ selectedAreaName }}</p>
                </div>
                <button class="ghost-btn" type="button" :disabled="loadingTables" @click="loadAvailableTables">
                  {{ loadingTables ? text.loading : text.reload }}
                </button>
              </div>
              <div v-if="loadingTables" class="skeleton-grid">
                <div v-for="n in 3" :key="n" class="skeleton-card table"></div>
              </div>
              <div v-else-if="tableError" class="error-banner">
                {{ tableError }}
                <button type="button" @click="loadAvailableTables">{{ text.retry }}</button>
              </div>
              <div v-else-if="!availableTables.length" class="empty-state">{{ text.noTables }}</div>
              <div v-else class="table-preview-grid">
                <button
                  v-for="table in availableTables"
                  :key="table.id"
                  type="button"
                  :class="['preview-table', { selected: form.tableId === table.id }]"
                  :aria-pressed="form.tableId === table.id"
                  :aria-label="`${text.chooseTable}: ${table.name}`"
                  @click="selectTable(table)"
                >
                  <strong>{{ table.name }}</strong>
                  <span>{{ text.capacity }}: {{ table.capacity || table.maxCapacity || '-' }}</span>
                  <span>{{ text.price }}: {{ money(table.reservationPrice) }}</span>
                  <small>{{ form.tableId === table.id ? text.selected : text.chooseTable }}</small>
                </button>
              </div>
            </section>
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
            <div v-if="!loadingTables && tables.length" class="table-map">
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
                      <span>{{ table.name }}</span>
                      <small>{{ table.capacity || table.maxCapacity || '-' }} {{ text.seats }}</small>
                    </button>
                  </div>
                </section>
              </div>
            </div>
            <div v-if="!loadingTables && !availableTables.length" class="empty-state waitlist-offer">
              <strong>{{ text.noTables }}</strong>
              <span>{{ text.waitlistOffer }}</span>
              <button class="primary-btn" type="button" @click="submitWaitlist" :disabled="submitting">
                {{ submitting ? text.submitting : text.joinWaitlist }}
              </button>
            </div>
            <div v-if="!loadingTables && availableTables.length" class="table-grid">
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
                    <span>{{ text.price }}: {{ money(table.reservationPrice) }}</span>
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
              <div class="dish-grid">
                <article v-for="dish in filteredMenu" :key="dish.id" class="dish-card">
                  <img :src="dish.image || fallbackDishImage" :alt="dishName(dish)" loading="lazy" @error="replaceDishImage" />
                  <div>
                    <strong>{{ dishName(dish) }}</strong>
                    <span>{{ dishCategory(dish) }}</span>
                    <p>{{ dishDescription(dish) }}</p>
                    <b>{{ money(dish.price) }}</b>
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
              <label v-for="item in text.preferences" :key="item">
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

          <section v-show="step === 8" class="panel">
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
            <p class="review-note">{{ text.backendPriceNote }}</p>
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
import { useI18n } from 'vue-i18n'
import CustomerLayout from '@/components/CustomerLayout.vue'
import api from '@/services/api'
import { useFormatters } from '@/composables/useFormatters'

const { locale, tm } = useI18n()
const { formatCurrency, formatDateTime } = useFormatters()
const lang = computed(() => locale.value)
const step = ref(1)
const areas = ref([])
const tables = ref([])
const suggestedTables = ref([])
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
let tableRequestSequence = 0

const fallbackAreaImage = 'https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=900&q=80'
const fallbackTableImage = 'https://images.unsplash.com/photo-1521017432531-fbd92d768814?auto=format&fit=crop&w=900&q=80'
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
const filteredMenu = computed(() => {
  const keyword = menuSearch.value.toLowerCase()
  return menuItems.value.filter(item => {
    const matchesKeyword = !keyword || `${dishName(item)} ${dishDescription(item)} ${dishCategory(item)}`.toLowerCase().includes(keyword)
    const matchesCategory = !menuCategory.value || dishCategory(item) === menuCategory.value
    return matchesKeyword && matchesCategory
  })
})
const paymentOptions = computed(() => Object.entries(text.value.paymentOptions).map(([key, value]) => ({ key, label: value[0], hint: value[1] })))

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

function replaceImage(event) {
  event.target.src = fallbackAreaImage
}

function replaceTableImage(event) {
  event.target.src = fallbackTableImage
}

function replaceDishImage(event) {
  event.target.src = fallbackDishImage
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
  return Object.keys(errors.value).length === 0 && !serverError.value
}

async function nextStep() {
  if (!validateCurrentStep()) return
  if (step.value === 3) await refreshAreaCounts()
  if (step.value === 4) await loadAvailableTables()
  if (step.value === 5 && !menuItems.value.length) await loadPreorderMenu()
  if (step.value === 7 || step.value === 8) await loadQuote()
  step.value = Math.min(9, step.value + 1)
}

function goTo(target) {
  if (target < step.value) step.value = target
}

function selectArea(area) {
  if (form.value.areaId === area.id && tables.value.length) return
  form.value.areaId = area.id
  form.value.tableId = null
  selectedTable.value = null
  tables.value = []
  suggestedTables.value = []
  quote.value = null
}

function selectTable(table) {
  selectedTable.value = table
  form.value.tableId = table.id
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
    if (requestSequence !== tableRequestSequence) return
    if (selectedTable.value && !tables.value.some(table => table.id === selectedTable.value.id && table.availabilityStatus === 'AVAILABLE')) {
      selectedTable.value = null
      form.value.tableId = null
      serverError.value = lang.value === 'vi'
        ? 'Bàn đã chọn không còn phù hợp. Vui lòng chọn lại.'
        : 'The selected table is no longer suitable. Please choose another table.'
    }
  } catch (err) {
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
      selectedTable.value = null
      quote.value = null
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
    serverError.value = err.response?.data?.message || err.response?.data || (lang.value === 'vi' ? 'Không gửi được yêu cầu đặt bàn' : 'Could not submit reservation')
  } finally {
    submitting.value = false
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
      specialRequest: form.value.specialRequest
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
  paymentQr.value = null
  paymentCapabilityToken.value = ''
  paymentIdempotencyKey.value = crypto.randomUUID()
  regenerateIdempotencyKey.value = ''
  qrError.value = ''
  quote.value = null
  step.value = 1
  form.value.tableId = null
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
  min-height: 72px;
  border: 1px solid #CFC7A8;
  border-radius: 8px;
  background: #FFFFFF;
  color: #55503E;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 3px;
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

.reservation-code {
  display: inline-flex;
  margin: 16px 0;
  background: #22301B;
  color: #FFFFFF;
  border-radius: 8px;
  padding: 10px 14px;
  font-weight: 900;
  letter-spacing: 1px;
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
</style>
