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
        </header>

        <nav class="stepper" :aria-label="text.stepperLabel">
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
        <div class="reservation-progress" role="status" aria-live="polite">
          <span>{{ reservationPhase.label }}</span>
          <strong>{{ reservationPhase.index }}/{{ reservationPhases.length }}</strong>
        </div>

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
              :aria-label="copiedCode === submitResult.reservationCode ? text.copiedBookingCode : text.copyBookingCode"
              @click="copyCode(submitResult.reservationCode)"
            >
              {{ copiedCode === submitResult.reservationCode ? text.copied : text.copy }}
            </button>
          </div>
          <div class="summary-grid">
            <span>{{ text.total }}</span><strong>{{ money(submitResult.totalAmount) }}</strong>
            <span>{{ text.payableNow }}</span><strong>{{ money(submitResult.amountDueNow) }}</strong>
            <span>{{ text.status }}</span><strong>{{ statusLabel(submitResult.reservationStatus) }}</strong>
            <span>{{ text.date }}</span><strong>{{ submitResult.reservationDate || form.reservationDate }}</strong>
            <span>{{ text.time }}</span><strong>{{ submitResult.arrivalTime || form.arrivalTime }}</strong>
            <span>{{ text.guests }}</span><strong>{{ submitResult.guestCount || form.guestCount }}</strong>
            <span>{{ text.areaInfo }}</span><strong>{{ submitResult.areaName || selectedAreaName || text.restaurantArranges }}</strong>
            <span>{{ text.remaining }}</span><strong>{{ money(submitResult.remainingAmount) }}</strong>
          </div>
          <article v-if="submitResult.tables?.length" class="qr-card">
            <div>
              <h3>{{ t('reservation.assignedTable', { table: submitResult.tables[0].tableName }) }}</h3>
              <p>{{ t('reservation.areaCapacity', { area: submitResult.areaName, capacity: submitResult.tables[0].capacity }) }}</p>
            </div>
            <img
              :src="submitResult.tables[0].imageUrl || fallbackTableImage"
              :alt="t('reservation.tableImageAlt', { table: submitResult.tables[0].tableName })"
              @error="replaceTableImage"
            />
          </article>
          <div v-else-if="submitResult.reservationStatus === 'WAITING_TABLE_ASSIGNMENT'" class="staff-info">
            <UiIcon name="info" />
            <p>{{ text.waitingAssignment }}</p>
          </div>
          <article v-if="paymentQr" class="qr-card">
            <div>
              <h3>{{ text.qrTitle }}</h3>
              <p>{{ text.qrHint }}</p>
              <p :class="['payment-state', { paid: paymentQr.status === 'PAID' }]">
                <UiIcon :name="paymentQr.status === 'PAID' ? 'check' : 'clock'" />
                {{ paymentQr.status === 'PAID' ? text.paymentReceived : text.paymentPending }}
              </p>
              <dl>
                <div><dt>{{ text.bank }}</dt><dd>{{ paymentQr.bankName || paymentQr.bankCode }}</dd></div>
                <div><dt>{{ text.accountNumber }}</dt><dd><span>{{ paymentQr.accountNumber }}</span><button class="copy-field-btn" type="button" @click="copyPaymentValue(paymentQr.accountNumber, 'account')"><UiIcon name="copy" />{{ copiedPaymentValue === 'account' ? text.copied : text.copy }}</button></dd></div>
                <div><dt>{{ text.accountHolder }}</dt><dd>{{ paymentQr.accountHolder }}</dd></div>
                <div><dt>{{ text.transferContent }}</dt><dd><span>{{ paymentQr.transferContent }}</span><button class="copy-field-btn" type="button" @click="copyPaymentValue(paymentQr.transferContent, 'content')"><UiIcon name="copy" />{{ copiedPaymentValue === 'content' ? text.copied : text.copy }}</button></dd></div>
                <div><dt>{{ text.expiresAt }}</dt><dd>{{ formatDateTime(paymentQr.expiresAt) }}</dd></div>
              </dl>
            </div>
            <img :src="paymentQr.qrUrl" :alt="text.qrTitle" />
            <div class="qr-actions">
              <button class="qr-action-btn" type="button" :disabled="qrLoading" @click="refreshPaymentStatus">
                {{ qrLoading ? 'Đang kiểm tra...' : text.checkPayment }}
              </button>
              <button class="qr-action-btn outline" type="button" :disabled="qrLoading" @click="regeneratePaymentQr">
                {{ qrLoading ? 'Đang xử lý...' : text.regenerateQr }}
              </button>
            </div>
            <p v-if="qrStatusMessage" class="qr-status-message" role="status">{{ qrStatusMessage }}</p>
          </article>
          <div v-else-if="qrLoading" class="qr-local-state">
            {{ text.creatingQr }}
          </div>
          <div v-else-if="qrError" class="qr-local-state qr-error-state">
            <p>{{ qrError }}</p>
            <button class="secondary-btn" type="button" @click="createPaymentQr">
              {{ text.retryQr }}
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
              :aria-label="copiedCode === waitlistResult.waitlistCode ? text.copiedWaitlistCode : text.copyWaitlistCode"
              @click="copyCode(waitlistResult.waitlistCode)"
            >
              {{ copiedCode === waitlistResult.waitlistCode ? text.copied : text.copy }}
            </button>
          </div>
          <div class="summary-grid">
            <span>{{ text.date }}</span><strong>{{ waitlistResult.reservationDate }}</strong>
            <span>{{ text.time }}</span><strong>{{ waitlistResult.preferredStartTime }} - {{ waitlistResult.preferredEndTime }}</strong>
            <span>{{ text.status }}</span><strong>{{ waitlistResult.status }}</strong>
          </div>
          <button class="primary-btn" type="button" @click="resetForm">{{ text.newBooking }}</button>
        </section>

        <div v-else class="booking-layout">
          <form class="reservation-card" @submit.prevent="submitReservation">
            <section v-show="step === 1" class="panel">
          <div class="section-heading"><span class="section-icon"><UiIcon name="user" /></span><div><h2>{{ text.customerInfo }}</h2><p>{{ text.customerHint }}</p></div></div>
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
                {{ text.emailOptional }}
                <input v-model.trim="form.customerEmail" type="email" autocomplete="email" />
                <small v-if="errors.customerEmail">{{ errors.customerEmail }}</small>
              </label>
              <label>
                {{ text.contactNote }}
                <textarea v-model.trim="form.contactNote" rows="3" :placeholder="text.optionalPlaceholder"></textarea>
              </label>
            </div>
          </section>

          <section v-show="step === 2" class="panel">
            <div class="section-heading"><span class="section-icon">◷</span><div><h2>{{ text.timeInfo }}</h2><p>{{ text.timeHint }}</p></div></div>
            <div class="form-grid time-grid">
              <label>
                {{ text.date }}
                <input v-model="form.reservationDate" type="date" :min="today" />
                <small v-if="errors.reservationDate">{{ errors.reservationDate }}</small>
              </label>
              <label>
                {{ text.time }}
                <input v-model="form.arrivalTime" type="time" :min="businessHours.openingTime" :max="latestArrivalTime" />
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
              <strong>{{ text.lateTitle }}</strong>
              <p>{{ t('reservation.lateDescription', { end: lateDiningEndTime, close: businessHours.closingTime }) }}</p>
              <label>
                <input v-model="lateDiningConfirmed" type="checkbox" />
                {{ text.lateConfirm }}
              </label>
              <small v-if="errors.lateDiningConfirmed">{{ errors.lateDiningConfirmed }}</small>
            </div>
          </section>

          <section v-show="step === 3" class="panel">
            <div class="guest-layout">
              <div>
          <div class="section-heading"><span class="section-icon"><UiIcon name="users" /></span><div><h2>{{ text.guestInfo }}</h2><p>{{ text.guestHint }}</p></div></div>
                <div class="guest-counter">
                  <button type="button" :aria-label="text.decreaseGuests" @click="form.guestCount = Math.max(1, Number(form.guestCount || 1) - 1)">−</button>
                  <label class="guest-count-input">
                    <input
                      v-model.number="form.guestCount"
                      type="number"
                      min="1"
                      max="10000"
                      inputmode="numeric"
                      :aria-label="text.guestInput"
                      @change="form.guestCount = Math.max(1, Number(form.guestCount || 1))"
                    />
                    <small>{{ text.guestWord }}</small>
                  </label>
                  <button type="button" :aria-label="text.increaseGuests" @click="form.guestCount = Number(form.guestCount || 0) + 1">+</button>
                </div>
                <div class="guest-presets">
                  <button v-for="count in [1, 2, 4, 6, 8]" :key="count" type="button" :class="{ selected: form.guestCount === count }" @click="form.guestCount = count">{{ count === 8 ? text.groupEight : t('reservation.peopleCount', { count }) }}</button>
                </div>
                <small v-if="errors.guestCount">{{ errors.guestCount }}</small>
                <small v-if="earlyGroupWarning" class="group-warning">{{ earlyGroupWarning }}</small>
                <p class="guest-tip">{{ text.guestTip }}</p>
              </div>
            </div>
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
                v-for="area in pagedAreas"
                :key="area.id"
                type="button"
                :class="['area-chip', { selected: form.areaId === area.id }]"
                :aria-pressed="form.areaId === area.id"
                @click="selectArea(area)"
              >
                <img class="area-chip-image" :src="areaImage(area)" :alt="t('reservation.areaImageAlt', { area: areaName(area) })" />
                <span class="area-chip-icon"><UiIcon :name="areaIcon(area)" /></span>
                <span class="area-chip-title">{{ areaName(area) }}</span>
                <span class="area-chip-description">{{ areaDescription(area) }}</span>
                <span class="area-chip-meta">
                  {{ text.capacity }}: {{ area.capacity || '-' }} {{ text.people }} · {{ text.availableTables }}: {{ areaAvailableCount(area.id) }}
                </span>
                <span v-if="form.areaId === area.id" class="area-chip-selected">{{ text.selected }}</span>
              </button>
            </div>
            <nav v-if="areaTotalPages > 1" class="area-pagination" aria-label="Phân trang khu vực">
              <button type="button" :disabled="areaPage === 1" @click="areaPage--">‹</button>
              <button v-for="page in areaTotalPages" :key="page" type="button" :class="{ active: page === areaPage }" @click="areaPage = page">{{ page }}</button>
              <button type="button" :disabled="areaPage === areaTotalPages" @click="areaPage++">›</button>
            </nav>
          </section>

          <section v-show="step === 5" class="panel">
            <div class="section-heading">
              <span class="section-icon">⌑</span>
              <div>
                <h2>{{ text.autoLayout }}</h2>
                <p v-if="form.guestCount < largePartyThreshold">
                  {{ text.autoSingle }}
                </p>
                <p v-else>
                  {{ text.autoGroup }}
                </p>
              </div>
            </div>
            <div class="staff-info">
              <UiIcon name="info" />
              <p>{{ text.autoInfo }}</p>
            </div>
            <div v-if="loadingTables" class="skeleton-grid" aria-live="polite">
              <div v-for="n in 2" :key="n" class="skeleton-card"></div>
            </div>
            <div v-else-if="tableError" class="error-banner">
              {{ tableError }}
              <button type="button" @click="loadAvailableTables">{{ text.retry }}</button>
            </div>
            <div v-else-if="availableTables.length || tableCombo?.available" class="staff-info availability-result" role="status">
              <span>✓</span>
              <p v-if="requiresTableCombination">
                {{ t('reservation.combinedAvailable', { count: tableCombo.tables?.length || 2 }) }}
              </p>
              <p v-else>
                {{ t('reservation.matchingAvailable', { count: availableTables.length }) }}
              </p>
            </div>
            <div v-else class="waitlist-offer" role="status">
              <div>
                <strong>{{ text.noSuitableTitle }}</strong>
                <p>{{ text.noSuitableHint }}</p>
              </div>
              <div class="waitlist-actions">
                <button class="primary-btn" type="button" :disabled="submitting" @click="submitWaitlist">
                  {{ submitting ? text.sending : text.joinWaitlist }}
                </button>
                <button class="ghost-btn" type="button" :disabled="submitting" @click="loadAvailableTables">{{ text.checkAgain }}</button>
              </div>
            </div>
          </section>

          <section v-show="step === 6" class="panel">
            <div class="section-heading"><span class="section-icon">⌒</span><div><h2>{{ text.preorderTitle }}</h2><p>{{ text.preorderHint }}</p></div></div>
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

            <div v-if="form.preorderEnabled" class="menu-picker preorder-layout">
              <div class="menu-picker-main">
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
                  <strong>{{ t('reservation.addedDishes', { count: validCartItems.length }) }}</strong>
                  <span>{{ t('reservation.selectedPortions', { count: cartQuantity }) }}</span>
                </div>
                <div class="dish-grid">
                  <article v-for="dish in pagedPreorderMenu" :key="dish.id" :class="['dish-card', { unavailable: !isDishAvailable(dish) }]">
                    <img :src="dish.image || fallbackDishImage" :alt="dishName(dish)" loading="lazy" @error="replaceDishImage" />
                    <div class="dish-card__content">
                      <strong class="dish-card__title" :title="dishName(dish)">{{ dishName(dish) }}</strong>
                      <span class="dish-card__category">{{ dishCategory(dish) }}</span>
                      <p class="dish-card__description" :title="dishDescription(dish)">{{ dishDescription(dish) }}</p>
                      <b class="dish-card__price">{{ money(dish.price) }}</b>
                      <small v-if="cartQuantityForDish(dish.id)" class="dish-added">{{ t('reservation.addedPortions', { count: cartQuantityForDish(dish.id) }) }}</small>
                      <small v-if="dish.inventoryManaged" class="dish-stock-limit">{{ Math.max(0, Number(dish.availableQuantity || 0)) }} {{ t('reservation.portionsRemaining') }}</small>
                      <small v-if="!isDishAvailable(dish)" class="dish-unavailable">Không khả dụng để đặt trước</small>
                      <div class="dish-card__actions">
                        <button class="primary-btn" type="button" :disabled="!isDishAvailable(dish)" @click="addDish(dish)">
                        {{ isDishAvailable(dish) ? text.addDish : 'Tạm hết' }}
                        </button>
                      </div>
                    </div>
                  </article>
                </div>
                <nav v-if="preorderTotalPages > 1" class="menu-pagination compact" aria-label="Phân trang gọi món trước">
                  <button type="button" :disabled="preorderPage === 1" @click="preorderPage--">‹</button>
                  <button
                    v-for="page in preorderPageButtons"
                    :key="page.key"
                    type="button"
                    :disabled="page.ellipsis"
                    :class="{ active: page.value === preorderPage, ellipsis: page.ellipsis }"
                    @click="!page.ellipsis && (preorderPage = page.value)"
                  >{{ page.label }}</button>
                  <button type="button" :disabled="preorderPage === preorderTotalPages" @click="preorderPage++">›</button>
                </nav>
              </div>
              <aside class="cart-box selected-dishes-panel">
                <h3>{{ text.selectedDishes }}</h3>
                <p v-if="!cartItems.length" class="cart-empty">Chưa chọn món nào.</p>
                <div v-for="item in cartItems" :key="item.productId" :class="['cart-row', { invalid: !isCartItemValid(item) }]">
                  <strong>{{ item.name }}</strong>
                  <small v-if="!isCartItemValid(item)" class="cart-invalid-reason">{{ item.invalidReason || 'Không còn khả dụng' }}</small>
                  <div class="qty">
                    <button type="button" :disabled="!isCartItemValid(item)" @click="changeQty(item.productId, -1)">-</button>
                    <input class="qty-input" type="number" min="1" :max="item.availableQuantity || undefined" :disabled="!isCartItemValid(item)" :value="item.quantity" @change="setQty(item, $event.target.value)" @keydown="blockInvalidQuantity" aria-label="Số lượng món" />
                    <button type="button" :disabled="!isCartItemValid(item)" @click="changeQty(item.productId, 1)">+</button>
                  </div>
                  <span>{{ isCartItemValid(item) ? money(item.price * item.quantity) : money(0) }}</span>
                  <button type="button" class="danger-btn" @click="removeDish(item.productId)">{{ text.remove }}</button>
                </div>
                <label class="preorder-note">
                  {{ text.kitchenNote }}
                  <textarea v-model.trim="form.orderNote" maxlength="500" rows="3" :placeholder="text.kitchenNotePlaceholder"></textarea>
                  <small>{{ form.orderNote.length }}/500</small>
                </label>
              </aside>
            </div>
          </section>

          <section v-show="step === 7" class="panel">
          <div class="section-heading"><span class="section-icon"><UiIcon name="note" /></span><div><h2>{{ text.requestInfo }}</h2><p>{{ text.requestHint }}</p></div></div>
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

          <section v-show="step === 8" class="panel">
            <div class="section-heading"><span class="section-icon">▣</span><div><h2>{{ text.paymentTitle }}</h2><p>{{ text.paymentHint }}</p></div></div>
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
            <div class="section-heading"><span class="section-icon">✓</span><div><h2>{{ text.review }}</h2><p>{{ text.reviewHint }}</p></div></div>
            <div class="review-box">
              <div><span>{{ text.fullName }}</span><strong>{{ form.customerName }}</strong></div>
              <div><span>{{ text.phone }}</span><strong>{{ form.customerPhone }}</strong></div>
              <div><span>{{ text.date }}</span><strong>{{ form.reservationDate }} {{ form.arrivalTime }}</strong></div>
              <div><span>{{ text.guests }}</span><strong>{{ form.guestCount }}</strong></div>
              <div><span>{{ text.areaInfo }}</span><strong>{{ selectedAreaName }}</strong></div>
              <div><span>{{ text.tableInfo }}</span><strong>{{ quote?.proposedTableName || selectedTable?.name || text.restaurantArranges }}</strong></div>
              <div><span>{{ text.selectedDishes }}</span><strong>{{ cartItems.length }}</strong></div>
              <div><span>{{ text.requestInfo }}</span><strong>{{ selectedPreferences.join(', ') || text.none }}</strong></div>
              <div><span>{{ text.paymentTitle }}</span><strong>{{ skipPaymentStep ? text.noAdvancePayment : paymentOptionLabel(form.paymentOption) }}</strong></div>
              <div><span>{{ text.total }}</span><strong>{{ money(quote?.totalAmount || selectedTable?.reservationPrice || 0) }}</strong></div>
            </div>
          </section>

          <div v-if="serverError" class="error-banner">{{ serverError }}</div>
          <div class="actions">
            <button class="ghost-btn" type="button" @click="previousStep" :disabled="step === 1 || submitting">{{ text.back }}</button>
            <button v-if="step < 9" class="primary-btn" type="button" :disabled="submitting || navigating" @click="nextStep">
              {{ navigating ? text.processing : text.next }}
            </button>
            <button v-else class="primary-btn" type="submit" :disabled="submitting">
              {{ submitting ? text.submitting : text.submit }}
            </button>
          </div>
          </form>
          <aside class="booking-summary" aria-label="Tóm tắt đặt bàn">
            <h2>{{ text.quickSummary }}</h2>
            <dl>
              <div><dt>{{ text.customer }}</dt><dd>{{ form.customerName || text.notEntered }}</dd></div>
              <div><dt>{{ text.dateTime }}</dt><dd>{{ form.reservationDate }} · {{ form.arrivalTime || '-' }}</dd></div>
              <div><dt>{{ text.guests }}</dt><dd>{{ form.guestCount }}</dd></div>
              <div><dt>{{ text.areaInfo }}</dt><dd>{{ selectedAreaName || '-' }}</dd></div>
              <div><dt>{{ text.selectedDishes }}</dt><dd>{{ cartQuantity }}</dd></div>
              <div><dt>{{ text.total }}</dt><dd>{{ quote ? money(quote.totalAmount) : '-' }}</dd></div>
              <div><dt>{{ text.payableNow }}</dt><dd>{{ quote ? money(quote.payableNow) : '-' }}</dd></div>
              <div><dt>{{ text.remaining }}</dt><dd>{{ quote ? money(quote.remainingAmount) : '-' }}</dd></div>
            </dl>
          </aside>
        </div>
      </section>
    </main>
  </CustomerLayout>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import CustomerLayout from '@/components/CustomerLayout.vue'
import UiIcon from '@/components/UiIcon.vue'
import api from '@/services/api'
import { useFormatters } from '@/composables/useFormatters'
import { toBusinessDate } from '@/utils/businessDate'
import { isTimeWithinWindow, minuteBefore } from '@/utils/businessHours'
import { nextReservationStep, previousReservationStep, shouldSkipReservationPayment } from '@/utils/reservationPaymentFlow'
import { normalizeSpecialRequirements, serializeRequirementFlags } from '@/utils/reservationSpecialRequirements'
import { getApiErrorMessage } from '@/services/errorMessage'
import { useToast } from '@/composables/useToast'
import indoorAreaImage from '@/assets/reservation-areas/reservation-area-indoor.png'
import privateAreaImage from '@/assets/reservation-areas/reservation-area-private.png'
import gardenAreaImage from '@/assets/reservation-areas/reservation-area-garden.png'
import {
  createEventBookingDraft,
  earlyGroupWarning as groupWarningFor,
  hasAvailableSingleTable,
  shouldRedirectToEventBooking,
  waitlistOverflowReason
} from '@/utils/reservationOverflow'

const { locale, tm, t } = useI18n()
const { formatCurrency, formatDateTime } = useFormatters()
const toast = useToast()
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
const largePartyThreshold = ref(10)
const selectedPreferences = ref([])
const errors = ref({})
const areaError = ref('')
const tableError = ref('')
const menuError = ref('')
const serverError = ref('')
const loadingAreas = ref(false)
const loadingTables = ref(false)
const submitting = ref(false)
const navigating = ref(false)
const submitResult = ref(null)
const waitlistResult = ref(null)
const copiedCode = ref('')
const copiedPaymentValue = ref('')
const paymentQr = ref(null)
const paymentCapabilityToken = ref('')
const paymentIdempotencyKey = ref(crypto.randomUUID())
const regenerateIdempotencyKey = ref('')
const qrLoading = ref(false)
const qrError = ref('')
const qrStatusMessage = ref('')
const quote = ref(null)
const menuSearch = ref('')
const menuCategory = ref('')
const areaPage = ref(1)
const preorderPage = ref(1)
const AREA_PAGE_SIZE = 3
const PREORDER_PAGE_SIZE = 10
const idempotencyKey = ref(crypto.randomUUID())
const lateDiningConfirmed = ref(false)
const businessHours = ref({ openingTime: '09:00', closingTime: '22:00', lastOrderTime: '21:30' })
let tableRequestSequence = 0

const fallbackTableImage = 'https://images.unsplash.com/photo-1515003197210-e0cd71810b5f?auto=format&fit=crop&w=480&q=80'
const fallbackDishImage = 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=900&q=80'
const today = toBusinessDate()

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
  orderNote: sessionStorage.getItem('reservation-order-note') || '',
  paymentOption: null,
  voucherCode: ''
})

const text = computed(() => tm('reservation'))
const reservationPhases = [
  { from: 1, to: 2, label: 'Thông tin & thời gian' },
  { from: 3, to: 5, label: 'Số khách & chỗ ngồi' },
  { from: 6, to: 7, label: 'Món đặt trước & yêu cầu' },
  { from: 8, to: 9, label: 'Thanh toán & xác nhận' }
]
const reservationPhase = computed(() => {
  const index = reservationPhases.findIndex(phase => step.value >= phase.from && step.value <= phase.to)
  const phase = reservationPhases[index < 0 ? reservationPhases.length - 1 : index]
  return { ...phase, index: (index < 0 ? reservationPhases.length : index + 1) }
})
const activeAreas = computed(() => areas.value.filter(area => (area.status || 'ACTIVE') === 'ACTIVE' && area.bookingReady !== false))
const areaTotalPages = computed(() => Math.max(1, Math.ceil(activeAreas.value.length / AREA_PAGE_SIZE)))
const pagedAreas = computed(() => {
  const start = (areaPage.value - 1) * AREA_PAGE_SIZE
  return activeAreas.value.slice(start, start + AREA_PAGE_SIZE)
})
const suggestionById = computed(() => Object.fromEntries(suggestedTables.value.map(item => [item.tableId, item])))
const availableTables = computed(() => tables.value
  .filter(table => table.availabilityStatus === 'AVAILABLE')
    .sort((a, b) => (suggestionById.value[b.id]?.score || 0) - (suggestionById.value[a.id]?.score || 0)))
const requiresTableCombination = computed(() => {
  const hasSingleFit = hasAvailableSingleTable(tables.value, form.value.guestCount)
  return !hasSingleFit && (tableCombo.value?.combinationRequired || Boolean(tableCombo.value?.available))
})
const earlyGroupWarning = computed(() => groupWarningFor(form.value.guestCount, undefined, text.value.groupWarning))
const selectedAreaName = computed(() => {
  const area = areas.value.find(item => item.id === form.value.areaId)
  return area ? areaName(area) : ''
})
const menuCategories = computed(() => [...new Set(menuItems.value.map(dishCategory).filter(Boolean))])
const visiblePreferences = computed(() => text.value.preferences.filter(item => !/kh\u00f4ng gian ri\u00eang t\u01b0|private space/i.test(item)))
const alcoholKeywords = ['bia', 'rượu', 'beer', 'lager', 'wine', 'vang', 'whisky', 'whiskey', 'vodka', 'gin', 'rum', 'soju', 'sake', 'cocktail', 'champagne', 'cognac', 'hennessy', 'chivas', 'johnnie', 'martell', 'remy']
const isAlcoholicDish = dish => {
  const content = `${dishName(dish)} ${dishDescription(dish)} ${dishCategory(dish)}`.toLocaleLowerCase('vi-VN')
  return alcoholKeywords.some(keyword => content.includes(keyword))
}
const filteredMenu = computed(() => {
  const keyword = menuSearch.value.toLowerCase()
  return menuItems.value.filter(item => {
    const matchesKeyword = !keyword || `${dishName(item)} ${dishDescription(item)} ${dishCategory(item)}`.toLowerCase().includes(keyword)
    const matchesCategory = !menuCategory.value || dishCategory(item) === menuCategory.value
    return matchesKeyword && matchesCategory
  }).sort((a, b) => Number(isDishAvailable(b)) - Number(isDishAvailable(a))
    || Number(isAlcoholicDish(a)) - Number(isAlcoholicDish(b))
    || dishName(a).localeCompare(dishName(b), 'vi'))
})
const preorderTotalPages = computed(() => Math.max(1, Math.ceil(filteredMenu.value.length / PREORDER_PAGE_SIZE)))
const pagedPreorderMenu = computed(() => {
  const start = (preorderPage.value - 1) * PREORDER_PAGE_SIZE
  return filteredMenu.value.slice(start, start + PREORDER_PAGE_SIZE)
})
const preorderPageButtons = computed(() => compactPageButtons(preorderPage.value, preorderTotalPages.value))
const paymentOptions = computed(() => Object.entries(text.value.paymentOptions).map(([key, value]) => ({ key, label: value[0], hint: value[1] })))
const validCartItems = computed(() => cartItems.value.filter(isCartItemValid))
const cartQuantity = computed(() => validCartItems.value.reduce((total, item) => total + item.quantity, 0))
const skipPaymentStep = computed(() => shouldSkipReservationPayment(quote.value))
const latestArrivalTime = computed(() => minuteBefore(businessHours.value.lastOrderTime))
const lateDiningEndTime = computed(() => {
  const { arrivalTime, expectedDurationMinutes } = form.value
  if (!arrivalTime || !expectedDurationMinutes) return ''

  const [hour, minute] = arrivalTime.split(':').map(Number)
  if (!Number.isFinite(hour) || !Number.isFinite(minute)) return ''

  const endMinutes = hour * 60 + minute + Number(expectedDurationMinutes)
  const [closingHour, closingMinute] = businessHours.value.closingTime.split(':').map(Number)
  const closingMinutes = closingHour * 60 + closingMinute
  if (endMinutes <= closingMinutes) return ''

  const endHour = Math.floor((endMinutes % (24 * 60)) / 60)
  const endMinute = endMinutes % 60
  const endTime = `${String(endHour).padStart(2, '0')}:${String(endMinute).padStart(2, '0')}`
  return endMinutes >= 24 * 60 ? `${endTime} ${text.value.nextDay}` : endTime
})

const money = formatCurrency

function statusLabel(status) {
  return text.value.statusMap[status] || status
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

function areaKind(area) {
  const value = `${area?.nameVi || ''} ${area?.nameEn || ''} ${area?.areaType || ''}`.toLowerCase()
  if (/vip|riêng|private/.test(value)) return 'private'
  if (/vườn|vuon|garden|outdoor|sân/.test(value)) return 'garden'
  return 'indoor'
}

function areaImage(area) {
  return { indoor: indoorAreaImage, private: privateAreaImage, garden: gardenAreaImage }[areaKind(area)]
}

function areaIcon(area) {
  return { indoor: 'indoor', private: 'private', garden: 'garden' }[areaKind(area)]
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

  if (!isTimeWithinWindow(arrivalTime, businessHours.value.openingTime, businessHours.value.lastOrderTime)) {
    const hoursLabel = `${businessHours.value.openingTime}-${businessHours.value.lastOrderTime}`
    return t('reservation.validation.bookingWindow', { hours: hoursLabel })
  }

  const selectedTime = new Date(`${reservationDate}T${arrivalTime}:00`)
  if (reservationDate === today && !Number.isNaN(selectedTime.getTime()) && selectedTime <= new Date()) {
    return t('reservation.validation.pastTime')
  }

  return ''
}

function isTimeValidationError(error) {
  const message = String(error?.response?.data?.message || error?.response?.data || '').toLowerCase()
  return /ngo\u00e0i gi\u1edd|gi\u1edd ho\u1ea1t \u0111\u1ed9ng|qu\u00e1 kh\u1ee9|past|operating hours|service hours/.test(message)
}

function syncStep2ValidationErrors() {
  if (step.value !== 2) return

  if (!form.value.reservationDate || !form.value.arrivalTime) {
    delete errors.value.arrivalTime
    delete errors.value.lateDiningConfirmed
    return
  }

  const timeError = reservationTimeError()
  if (timeError) errors.value.arrivalTime = timeError
  else delete errors.value.arrivalTime

  if (lateDiningEndTime.value && !lateDiningConfirmed.value) {
    errors.value.lateDiningConfirmed = t('reservation.validation.lateConfirm')
  } else {
    delete errors.value.lateDiningConfirmed
  }
}

function validateCurrentStep() {
  errors.value = {}
  serverError.value = ''
  if (step.value === 1) {
    if (!form.value.customerName.trim()) errors.value.customerName = t('reservation.validation.name')
    if (!/^(0|\+84)(3|5|7|8|9)[0-9]{8}$/.test(form.value.customerPhone.replace(/\s/g, ''))) errors.value.customerPhone = t('reservation.validation.phone')
    if (form.value.customerEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.customerEmail)) errors.value.customerEmail = t('reservation.validation.email')
  }
  if (step.value === 2) {
    if (!form.value.reservationDate) errors.value.reservationDate = t('reservation.validation.date')
    if (!form.value.arrivalTime) errors.value.arrivalTime = t('reservation.validation.time')
  }
  if (step.value === 3 && (!form.value.guestCount || form.value.guestCount < 1)) errors.value.guestCount = t('reservation.validation.guests')
  if (step.value === 4 && !form.value.areaId) serverError.value = t('reservation.validation.area')
  if (step.value === 8 && !form.value.paymentOption) serverError.value = t('reservation.validation.payment')
  if (step.value === 2 && form.value.arrivalTime) {
    const timeError = reservationTimeError()
    if (timeError) errors.value.arrivalTime = timeError
    if (lateDiningEndTime.value && !lateDiningConfirmed.value) {
      errors.value.lateDiningConfirmed = t('reservation.validation.lateConfirm')
    }
  }
  return Object.keys(errors.value).length === 0 && !serverError.value
}

watch(
  () => [
    form.value.reservationDate,
    form.value.arrivalTime,
    form.value.expectedDurationMinutes,
    lateDiningConfirmed.value,
    step.value
  ],
  () => {
    if (step.value === 2) syncStep2ValidationErrors()
  }
)

async function nextStep() {
  if (navigating.value || !validateCurrentStep()) return
  navigating.value = true
  try {
    if (step.value === 3) await refreshAreaCounts()
    if (step.value === 4) await loadAvailableTables()
    if (step.value === 5 && !menuItems.value.length) await loadPreorderMenu()
    if (step.value === 6) {
      await loadPreorderMenu()
      if (!validateSelectedDishes()) return
      await loadQuote()
    }
    if (step.value === 7 || step.value === 8) await loadQuote()
    step.value = nextReservationStep(step.value, quote.value)
  } catch (error) {
    const fallback = step.value === 7 ? t('reservation.errors.specialRequest') : t('reservation.errors.quote')
    serverError.value = lang.value === 'vi' ? getApiErrorMessage(error, fallback) : fallback
    if (step.value === 6) {
      markUnavailableFromServerMessage(serverError.value)
      menuError.value = serverError.value
    }
    toast.error(serverError.value)
  } finally {
    navigating.value = false
  }
}

function previousStep() {
  step.value = previousReservationStep(step.value, quote.value)
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

async function loadAreas() {
  loadingAreas.value = true
  areaError.value = ''
  try {
    const res = await api.get('/api/areas')
    areas.value = Array.isArray(res.data) ? res.data : []
    areaPage.value = Math.min(areaPage.value, areaTotalPages.value)
    if (activeAreas.value.length && (!form.value.areaId || !activeAreas.value.some(area => area.id === form.value.areaId))) {
      form.value.areaId = activeAreas.value[0].id
    }
    await refreshAreaCounts()
  } catch (err) {
    areaError.value = lang.value === 'vi' && err.response?.data?.message ? err.response.data.message : t('reservation.errors.areas')
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
        areaId: area.id,
        lateDiningConfirmed: lateDiningConfirmed.value
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
        areaId: form.value.areaId,
        lateDiningConfirmed: lateDiningConfirmed.value
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
      serverError.value = t('reservation.errors.tableChanged')
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
    tableError.value = lang.value === 'vi' && err.response?.data?.message ? err.response.data.message : t('reservation.errors.tables')
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
    form.value.areaId,
    lateDiningConfirmed.value
  ],
  async (current, previous) => {
    if (!previous || current.every((value, index) => value === previous[index])) return
    if (!form.value.areaId || !form.value.reservationDate || !form.value.arrivalTime || !form.value.guestCount) return

    syncStep2ValidationErrors()

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
    const requirements = normalizeSpecialRequirements(selectedPreferences.value, form.value.specialRequest)
    const res = await api.post('/api/reservations/table-suggestions', {
      reservationDate: form.value.reservationDate,
      arrivalTime: form.value.arrivalTime,
      durationMinutes: form.value.expectedDurationMinutes,
      guestCount: form.value.guestCount,
      areaId: form.value.areaId,
      seatingPreference: serializeRequirementFlags(requirements),
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
    const requirements = normalizeSpecialRequirements(selectedPreferences.value, form.value.specialRequest)
    const res = await api.post('/api/reservations/table-combinations', {
      reservationDate: form.value.reservationDate,
      arrivalTime: form.value.arrivalTime,
      durationMinutes: form.value.expectedDurationMinutes,
      guestCount: form.value.guestCount,
      areaId: form.value.areaId,
      seatingPreference: serializeRequirementFlags(requirements),
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
    syncCartAvailability()
    preorderPage.value = Math.max(1, Math.min(preorderPage.value, preorderTotalPages.value))
  } catch (err) {
    menuError.value = lang.value === 'vi' && err.response?.data?.message ? err.response.data.message : t('reservation.errors.menu')
  }
}

function addDish(dish) {
  if (!isDishAvailable(dish)) {
    menuError.value = `${dishName(dish)} không còn khả dụng để đặt trước`
    markCartItemInvalid(dish.id, menuError.value)
    return
  }
  const existing = cartItems.value.find(item => item.productId === dish.id)
  const limit = dish.inventoryManaged ? Number(dish.availableQuantity || 0) : Infinity
  if (limit <= 0 || (existing && existing.quantity >= limit)) {
    menuError.value = t('reservation.errors.stockLimit', { count: Math.max(0, limit) })
    return
  }
  if (existing) {
    existing.quantity += 1
    existing.availableQuantity = dish.inventoryManaged ? Number(dish.availableQuantity || 0) : -1
    existing.invalidReason = ''
    return
  }
  cartItems.value.push({ productId: dish.id, name: dishName(dish), price: Number(dish.price || 0), quantity: 1, availableQuantity: dish.inventoryManaged ? Number(dish.availableQuantity || 0) : -1, invalidReason: '' })
  quote.value = null
}

function changeQty(productId, delta) {
  const item = cartItems.value.find(row => row.productId === productId)
  if (!item || !isCartItemValid(item)) return
  const dish = menuItems.value.find(row => row.id === productId)
  const limit = dish?.inventoryManaged ? Number(dish.availableQuantity || 0) : Infinity
  if (delta > 0 && item.quantity >= limit) {
    menuError.value = t('reservation.errors.stockLimit', { count: Math.max(0, limit) })
    return
  }
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
  form.value.orderNote = ''
  quote.value = null
}

function isDishAvailable(dish) {
  if (!dish || dish.available === false) return false
  if (dish.inventoryManaged) return Number(dish.availableQuantity || 0) > 0
  return true
}

function isCartItemValid(item) {
  return Boolean(item && !item.invalidReason && (item.availableQuantity == null || item.availableQuantity < 0 || item.quantity <= item.availableQuantity))
}

function markCartItemInvalid(productId, reason) {
  const item = cartItems.value.find(row => row.productId === productId)
  if (item) item.invalidReason = reason
}

function markUnavailableFromServerMessage(message) {
  const normalized = String(message || '').toLowerCase()
  cartItems.value.forEach(item => {
    if (normalized.includes(String(item.name || '').toLowerCase())) {
      item.invalidReason = message
    }
  })
}

function syncCartAvailability() {
  cartItems.value.forEach(item => {
    const dish = menuItems.value.find(row => row.id === item.productId)
    if (!dish || !isDishAvailable(dish)) {
      item.availableQuantity = 0
      item.invalidReason = `${item.name} không còn khả dụng để đặt trước`
      return
    }
    item.availableQuantity = dish.inventoryManaged ? Number(dish.availableQuantity || 0) : -1
    if (dish.inventoryManaged && item.quantity > item.availableQuantity) {
      item.invalidReason = `${item.name} chỉ còn ${item.availableQuantity} phần`
      item.quantity = Math.max(1, item.availableQuantity)
      return
    }
    item.invalidReason = ''
  })
}

function validateSelectedDishes() {
  syncCartAvailability()
  const invalid = cartItems.value.filter(item => !isCartItemValid(item))
  if (!invalid.length) return true
  menuError.value = invalid.map(item => item.invalidReason || `${item.name} không còn khả dụng`).join('; ')
  serverError.value = ''
  toast.error(menuError.value)
  return false
}

function compactPageButtons(current, total) {
  const pages = new Set([1, total, current - 1, current, current + 1])
  const valid = [...pages].filter(page => page >= 1 && page <= total).sort((a, b) => a - b)
  const result = []
  valid.forEach((page, index) => {
    const previous = valid[index - 1]
    if (previous && page - previous > 1) {
      result.push({ key: `ellipsis-${previous}-${page}`, label: '…', ellipsis: true })
    }
    result.push({ key: `page-${page}`, label: String(page), value: page, ellipsis: false })
  })
  return result
}

watch([menuSearch, menuCategory], () => {
  preorderPage.value = 1
})

watch(activeAreas, () => {
  areaPage.value = Math.min(areaPage.value, areaTotalPages.value)
})

watch(filteredMenu, () => {
  preorderPage.value = Math.max(1, Math.min(preorderPage.value, preorderTotalPages.value))
})

function setQty(item, rawValue) {
  if (!isCartItemValid(item)) return
  const parsed = Number.parseInt(rawValue, 10)
  const limit = item.availableQuantity == null || item.availableQuantity < 0 ? Infinity : Number(item.availableQuantity)
  if (!Number.isInteger(parsed) || parsed < 1) { item.quantity = 1; return }
  if (parsed > limit) { item.quantity = limit; menuError.value = t('reservation.errors.stockLimit', { count: limit }); return }
  item.quantity = parsed; quote.value = null
}

function blockInvalidQuantity(event) {
  if (['e', 'E', '+', '-', '.', ','].includes(event.key)) event.preventDefault()
}

function selectPayment(option) {
  form.value.paymentOption = option
  quote.value = null
}

function preorderPayload() {
  return form.value.preorderEnabled ? validCartItems.value.map(item => ({
    productId: item.productId,
    quantity: item.quantity
  })) : []
}

async function loadQuote() {
  const res = await api.post('/api/reservations/quote', {
    areaId: form.value.areaId,
    tableId: form.value.tableId,
    reservationDate: form.value.reservationDate,
    arrivalTime: form.value.arrivalTime,
    durationMinutes: form.value.expectedDurationMinutes,
    guestCount: form.value.guestCount,
    preorderItems: preorderPayload(),
    paymentOption: form.value.paymentOption || 'DEPOSIT_50',
    voucherCode: form.value.voucherCode
  })
  quote.value = res.data
  form.value.tableId = res.data.proposedTableId || null
  form.value.tableIds = res.data.proposedTableId ? [res.data.proposedTableId] : []
}

async function submitReservation() {
  if (!validateCurrentStep()) return
  submitting.value = true
  serverError.value = ''
  paymentQr.value = null
  qrError.value = ''
  try {
    await loadQuote()
    const requirements = normalizeSpecialRequirements(selectedPreferences.value, form.value.specialRequest)
    const payload = {
      ...form.value,
      customerPhone: form.value.customerPhone.replace(/\s/g, ''),
      seatingPreference: serializeRequirementFlags(requirements),
      specialRequest: requirements.note,
      paymentOption: form.value.paymentOption || quote.value?.paymentOption || 'DEPOSIT_50',
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
    serverError.value = lang.value === 'vi' && (err.response?.data?.message || typeof err.response?.data === 'string')
      ? (err.response?.data?.message || err.response.data) : t('reservation.errors.submit')
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

async function copyPaymentValue(value, key) {
  await copyCode(value)
  copiedPaymentValue.value = key
  window.setTimeout(() => {
    if (copiedPaymentValue.value === key) copiedPaymentValue.value = ''
  }, 1800)
}

async function createPaymentQr() {
  if (!submitResult.value?.reservationCode || qrLoading.value) return
  qrLoading.value = true
  qrError.value = ''
  qrStatusMessage.value = ''
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
    qrError.value = lang.value === 'vi' && err.response?.data?.message
      ? err.response.data.message : t('reservation.errors.createQr')
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
  qrStatusMessage.value = ''
  try {
    const qrRes = await api.post(`/api/payments/${paymentQr.value.paymentCode}/regenerate`, null, {
      headers: {
        'X-Payment-Capability': paymentCapabilityToken.value,
        'X-Idempotency-Key': regenerateIdempotencyKey.value
      }
    })
    paymentQr.value = qrRes.data
    regenerateIdempotencyKey.value = ''
    qrStatusMessage.value = 'Đã tạo lại QR thanh toán mới.'
  } catch (err) {
    qrError.value = lang.value === 'vi' && err.response?.data?.message
      ? err.response.data.message : t('reservation.errors.regenerateQr')
  } finally {
    qrLoading.value = false
  }
}

async function refreshPaymentStatus() {
  if (!paymentQr.value?.paymentCode || qrLoading.value) return
  qrLoading.value = true
  qrError.value = ''
  qrStatusMessage.value = ''
  try {
    const response = await api.get(`/api/payments/${paymentQr.value.paymentCode}`, {
      headers: { 'X-Payment-Capability': paymentCapabilityToken.value }
    })
    paymentQr.value = response.data
    qrStatusMessage.value = paymentStatusMessage(response.data?.status)
  } catch (err) {
    qrError.value = lang.value === 'vi' && err.response?.data?.message ? err.response.data.message : t('reservation.errors.paymentStatus')
  } finally {
    qrLoading.value = false
  }
}

function paymentStatusMessage(status) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'PAID' || normalized === 'SUCCESS') return 'Đã ghi nhận thanh toán.'
  if (normalized === 'EXPIRED') return 'QR đã hết hạn.'
  if (normalized === 'FAILED') return 'Thanh toán thất bại.'
  return 'Chưa ghi nhận thanh toán.'
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
    const requirements = normalizeSpecialRequirements(selectedPreferences.value, form.value.specialRequest)
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
      seatingPreference: serializeRequirementFlags(requirements),
      specialRequest: requirements.note,
      overflowReason: waitlistOverflowReason(form.value.guestCount)
    })
    waitlistResult.value = res.data
  } catch (err) {
    serverError.value = lang.value === 'vi' && (err.response?.data?.message || typeof err.response?.data === 'string')
      ? (err.response?.data?.message || err.response.data) : t('reservation.errors.waitlist')
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
  qrStatusMessage.value = ''
  quote.value = null
  step.value = 1
  form.value.tableId = null
  form.value.tableIds = []
  tableCombo.value = null
  selectedTable.value = null
  selectedPreferences.value = []
  cartItems.value = []
  form.value.orderNote = ''
  sessionStorage.removeItem('reservation-order-note')
  form.value.voucherCode = ''
  idempotencyKey.value = crypto.randomUUID()
}

onMounted(async () => {
  try {
    const response = await api.get('/api/settings/public')
    largePartyThreshold.value = Number(response.data?.largePartyThreshold || 10)
    businessHours.value = {
      openingTime: response.data?.openingTime || businessHours.value.openingTime,
      closingTime: response.data?.closingTime || businessHours.value.closingTime,
      lastOrderTime: response.data?.lastOrderTime || businessHours.value.lastOrderTime
    }
  } catch {
    largePartyThreshold.value = 10
  }
  await loadAreas()
  await loadPreorderMenu()
})

watch(() => form.value.orderNote, value => {
  if (value) sessionStorage.setItem('reservation-order-note', value)
  else sessionStorage.removeItem('reservation-order-note')
})
</script>

<style scoped>
.reservation-page {
  min-height: 100vh;
  background: var(--bg-card2);
  color: var(--text-primary);
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
  color: var(--secondary);
  font-weight: 800;
  text-transform: uppercase;
  font-size: 0.78rem;
}

.reservation-header h1 {
  margin: 0;
  font-size: 2.35rem;
  color: var(--color-on-secondary-container);
}

.reservation-header p {
  max-width: 720px;
  color: var(--text-muted);
}

.stepper {
  display: grid;
  grid-template-columns: repeat(9, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 18px;
}

.step-chip {
  border: 1px solid var(--border);
  background: #FFFFFF;
  border-radius: 8px;
  min-height: 48px;
  padding: 6px;
  color: var(--text-muted);
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
  background: var(--bg-hover);
  color: var(--secondary);
  margin-right: 6px;
}

.step-chip.active,
.step-chip.done {
  border-color: var(--secondary);
  color: var(--text-primary);
}

.reservation-card,
.success-panel {
  background: #FFFFFF;
  border: 1px solid var(--border);
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
  border: 1px solid var(--secondary);
  border-radius: 10px;
  background: #F5F4E9;
  color: var(--color-on-secondary-container);
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
  border: 1px solid var(--border);
}

.panel h2,
.success-panel h2 {
  margin: 0 0 18px;
  color: var(--color-on-secondary-container);
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
  color: var(--text-secondary);
}

input,
select,
textarea {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 11px 12px;
  background: #FFFFFF;
  color: var(--text-primary);
}

input:focus,
select:focus,
textarea:focus {
  outline: 3px solid var(--bg-hover);
  border-color: var(--secondary);
}

small {
  color: var(--primary);
}

.lang-toggle,
.ghost-btn,
.primary-btn,
.danger-btn {
  min-height: 40px;
  border-radius: 8px;
  border: 1px solid var(--border);
  padding: 0 16px;
  font-weight: 800;
  cursor: pointer;
}

.lang-toggle,
.ghost-btn {
  background: #FFFFFF;
  color: var(--text-primary);
}

.primary-btn {
  background: var(--secondary);
  color: #FFFFFF;
  border-color: var(--secondary);
}

.table-card .primary-btn {
  width: 100%;
  white-space: nowrap;
  margin-top: auto;
}

.danger-btn {
  background: #FFFFFF;
  color: var(--primary);
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
  align-items: stretch;
}

.area-card,
.table-card,
.dish-card {
  border: 1px solid var(--border);
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
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #FFFFFF;
  color: var(--text-secondary);
  cursor: pointer;
  font: inherit;
  text-align: left;
}

.area-chip:hover,
.area-chip:focus-visible {
  border-color: var(--secondary);
  outline: none;
}

.area-chip.selected {
  border-color: var(--secondary);
  box-shadow: 0 0 0 3px var(--bg-hover);
}

.area-chip-title { font-weight: 900; color: var(--color-on-secondary-container); }
.area-chip-description { color: var(--text-muted); font-size: 0.86rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.area-chip-meta { color: var(--secondary); font-size: 0.78rem; font-weight: 700; }
.area-chip-selected { color: var(--secondary); font-size: 0.78rem; font-weight: 800; }

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
  height: 100%;
}

.dish-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  gap: 10px;
}

.dish-card__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: auto;
}

.dish-card__title,
.dish-card__description {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.dish-card__title {
  -webkit-line-clamp: 2;
  line-height: 1.35;
  min-height: calc(1.35em * 2);
}

.dish-card__description {
  -webkit-line-clamp: 3;
  line-height: 1.45;
  min-height: calc(1.45em * 3);
}

.dish-card__category,
.dish-card__price,
.dish-added,
.dish-stock-limit,
.dish-unavailable {
  min-height: 1.25em;
}

.dish-card__category {
  color: var(--text-muted);
  font-size: 0.78rem;
  font-weight: 700;
  line-height: 1.2;
}

.dish-card__price {
  color: var(--wine);
  font-size: 1.05rem;
  font-weight: 900;
  line-height: 1.2;
}

.dish-card .primary-btn {
  width: 100%;
  white-space: nowrap;
  flex-shrink: 0;
}

.area-card.selected,
.table-card.selected {
  border-color: var(--secondary);
  box-shadow: 0 0 0 3px var(--bg-hover);
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
  border: 1px solid var(--bg-hover);
  border-radius: 8px;
  background: var(--bg-card2);
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
  color: var(--color-on-secondary-container);
}

.table-map-header p {
  margin: 0;
  color: var(--text-muted);
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
  background: var(--bg-hover);
  color: var(--color-on-secondary-container);
}

.legend.blocked {
  background: var(--bg-card2);
  color: var(--text-secondary);
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
  color: var(--text-secondary);
}

.map-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(104px, 1fr));
  gap: 10px;
}

.map-seat {
  min-height: 132px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #FFFFFF;
  color: var(--text-secondary);
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
  color: var(--text-muted);
  font-size: 0.76rem;
}

.map-seat.available {
  border-color: color-mix(in srgb, var(--success) 45%, var(--border));
  background: color-mix(in srgb, var(--success) 9%, var(--bg-card));
  color: var(--success);
}

.map-seat.reserved {
  border-color: color-mix(in srgb, var(--warning) 35%, var(--border));
  background: color-mix(in srgb, var(--warning) 9%, var(--bg-card));
  color: var(--warning);
}

.map-seat.too-small {
  border-color: var(--color-outline-variant);
  background: var(--color-secondary-fixed);
  color: var(--secondary);
}

.map-seat.blocked,
.map-seat:disabled {
  background: var(--bg-card2);
  color: var(--text-muted);
  cursor: not-allowed;
  opacity: 0.75;
}

.map-seat.selected {
  outline: 3px solid var(--secondary);
  outline-offset: 2px;
}

.card-body {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.dish-card__content {
  padding: 14px;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

.card-body p,
.dish-card p,
.review-note {
  color: var(--text-muted);
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
  background: var(--bg-card2);
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
  background: var(--bg-hover);
  color: var(--secondary);
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 800;
}

.status-pill.available {
  background: var(--bg-hover);
  color: var(--success);
}

.status-pill.suggested {
  background: var(--color-secondary-fixed);
  color: var(--secondary);
}

.reason-list {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.reason-list li + li {
  margin-top: 3px;
}

.ghost-link {
  color: var(--secondary);
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
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #FFFFFF;
  padding: 16px;
  cursor: pointer;
}

.choice-grid button.selected {
  border-color: var(--secondary);
  background: var(--bg-hover);
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
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  background: var(--bg-card2);
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
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  text-align: center;
}

.qty button {
  border: 0;
  background: var(--bg-hover);
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
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
}

.preference-grid input {
  width: auto;
}

.error-banner,
.empty-state {
  background: color-mix(in srgb, var(--warning) 9%, var(--bg-card));
  color: var(--warning);
  border: 1px solid color-mix(in srgb, var(--warning) 35%, var(--border));
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 14px;
}

.empty-state {
  background: var(--bg-card2);
  color: var(--text-muted);
  border-color: var(--border);
}

.waitlist-offer {
  display: grid;
  gap: 10px;
  justify-items: start;
  padding: 18px;
  border: 1px solid color-mix(in srgb, var(--warning) 40%, var(--border));
  border-radius: 12px;
  background: color-mix(in srgb, var(--warning) 8%, var(--bg-card));
}

.waitlist-offer p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.waitlist-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.availability-result {
  margin-top: 14px;
}

.skeleton-card {
  height: 260px;
  border-radius: 8px;
  background: linear-gradient(90deg, var(--bg-card2), var(--border), var(--bg-card2));
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
  border: 1px solid var(--warning);
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
  background: var(--color-on-secondary-container);
  color: #FFFFFF;
  border-radius: 8px;
  padding: 10px 14px;
  font-weight: 900;
  letter-spacing: 1px;
}

.reservation-code::selection {
  background: var(--color-tertiary-fixed);
  color: var(--text-primary);
}

.reservation-code::-moz-selection {
  background: var(--color-tertiary-fixed);
  color: var(--text-primary);
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
  border: 1px solid var(--border);
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
  color: var(--text-muted);
}

.table-preview {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
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
  color: var(--text-primary);
  background: #FFFFFF;
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
}

.preview-table:hover,
.preview-table:focus-visible,
.preview-table.selected {
  border-color: var(--secondary);
  box-shadow: 0 0 0 3px var(--bg-hover);
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
  border: 1px solid var(--border);
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
.map-seat.available { border-color: color-mix(in srgb, var(--success) 45%, var(--border)); background: color-mix(in srgb, var(--success) 9%, var(--bg-card)); color: var(--success); }
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

/* Reservation phase 2 — visual system matched to the approved mockups. */
.reservation-page {
  --wine: #be0b2f;
  --wine-dark: #9f0927;
  --ink: #201719;
  --muted: #706568;
  --line: #edd2d3;
  --blush: #fff7f6;
  min-height: calc(100vh - 68px);
  padding: 44px 40px 72px;
  background:
    radial-gradient(circle at 86% 8%, rgba(190, 11, 47, .035), transparent 27%),
    linear-gradient(180deg, #fffdfc 0%, #fff9f8 100%);
}
.reservation-shell { max-width: 1480px; }
.reservation-header { min-height: 128px; position: relative; }
.reservation-header::after { content: ''; position: absolute; right: 72px; top: 28px; color: #f3ddda; font-size: 98px; transform: rotate(-18deg); pointer-events: none; }
.reservation-header .eyebrow { margin: 0 0 10px; color: #b46d76; font-size: .9rem; font-weight: 800; text-transform: uppercase; }
.reservation-header h1 { margin: 0; color: var(--ink); font-family: inherit; font-size: clamp(2.8rem, 4vw, 4.35rem); font-weight: 900; letter-spacing: -.045em; line-height: 1; }
.reservation-header p:not(.eyebrow) { margin-top: 16px; font-size: 1rem; }
.lang-toggle { position: relative; z-index: 1; min-width: 58px; min-height: 48px; border-color: var(--line); border-radius: 10px; }
.stepper { display: flex; grid-template-columns: none; gap: 8px; overflow-x: auto; padding: 2px 0 4px; scrollbar-width: thin; }
.step-chip { flex: 1 1 112px; min-width: 104px; min-height: 42px; border: 1px solid var(--line); border-radius: 999px; background: rgba(255,255,255,.82); color: var(--ink); font-size: .78rem; padding: 4px 7px; }
.step-chip span { width: 20px; height: 20px; margin-right: 4px; background: #fff0ef; color: var(--wine); font-size: .72rem; }
.step-chip.active { color: #fff; border-color: var(--wine); background: linear-gradient(135deg, var(--wine), #d20c39); box-shadow: 0 8px 20px rgba(190,11,47,.16); }
.step-chip.active span { color: var(--wine); background: #fff; }
.step-chip.done { border-color: #e6b8bd; color: var(--ink); background: #fff; }
.step-chip.done span { color: #fff; background: var(--wine); }
.booking-layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 24px; align-items: start; margin-top: 18px; }
.reservation-card, .success-panel { margin-top: 18px; padding: 28px 30px; border: 1px solid var(--line); border-radius: 16px; box-shadow: 0 12px 28px rgba(96,43,48,.07); }
.booking-layout .reservation-card { margin-top: 0; min-width: 0; }
.section-heading { display: flex; align-items: center; gap: 18px; margin-bottom: 24px; }
.section-heading h2 { margin: 0 0 4px; font-family: inherit; font-size: 1.55rem; font-weight: 850; }
.section-heading p { margin: 0; color: var(--muted); }
.section-icon { display: grid; flex: 0 0 48px; width: 48px; height: 48px; place-items: center; border-radius: 50%; background: #fff0ef; color: var(--wine); font-size: 1.6rem; font-weight: 800; }
.form-grid { gap: 22px 34px; }
.time-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
label { gap: 9px; color: var(--ink); }
input, select, textarea { min-height: 50px; border-color: #e7cbca; border-radius: 9px; padding: 12px 16px; }
textarea { resize: vertical; }
input:focus, select:focus, textarea:focus { outline: 3px solid rgba(190,11,47,.09); border-color: var(--wine); }
.late-dining-confirmation { display: grid; grid-template-columns: auto 1fr; margin-top: 28px; padding: 26px 30px; border-color: var(--warning); border-radius: 10px; background: color-mix(in srgb, var(--warning) 8%, var(--bg-card)); }
.late-dining-confirmation::before { content: ''; grid-row: 1 / 4; display: grid; width: 112px; height: 112px; place-items: center; margin-right: 24px; border-radius: 50%; background: #fff; color: var(--wine); font-size: 3rem; }
.late-dining-confirmation label { border-top: 1px dashed var(--warning); padding-top: 14px; }
.guest-layout { display: grid; grid-template-columns: minmax(0, 2fr) minmax(280px, .9fr); gap: 28px; }
.guest-counter { display: flex; max-width: 590px; align-items: center; justify-content: space-between; margin: 10px auto 24px; padding: 18px 30px; border: 1px solid #edc8c9; border-radius: 20px; box-shadow: 0 5px 13px rgba(190,11,47,.08); }
.guest-counter button { width: 58px; height: 58px; border: 0; border-radius: 50%; background: #fff0ef; color: var(--wine); font-size: 2rem; cursor: pointer; }
.guest-count-input { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.guest-count-input input { width: 130px; min-height: 56px; padding: 4px 10px; border: 0; background: transparent; color: var(--wine); font-size: 3rem; font-weight: 600; line-height: 1; text-align: center; appearance: textfield; }
.guest-count-input input::-webkit-inner-spin-button, .guest-count-input input::-webkit-outer-spin-button { appearance: none; margin: 0; }
.guest-count-input input:focus { outline: 2px solid rgba(190,11,47,.12); border-radius: 10px; }
.guest-count-input small { color: var(--ink); font-size: 1rem; font-weight: 700; }
.guest-presets { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.guest-presets button { min-height: 54px; border: 1px solid var(--line); border-radius: 12px; background: #fff; cursor: pointer; font-weight: 700; }
.guest-presets button.selected { color: var(--wine); border-color: var(--wine); background: #fff6f5; }
.guest-tip { margin: 26px 0 0; padding: 16px 20px; border-radius: 10px; background: #fff8f3; color: var(--muted); }
.quick-summary { padding: 24px; border: 1px solid var(--line); border-radius: 16px; background: #fffdfa; }
.quick-summary h3 { margin: 0 0 16px; font-size: 1.3rem; }
.quick-summary div { display: grid; gap: 4px; padding: 13px 0; border-bottom: 1px dashed var(--line); }
.quick-summary span { color: var(--muted); font-size: .85rem; }
.quick-summary p { padding: 15px; border-radius: 10px; background: #fff4e8; }
.area-chip-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 20px; }
.area-chip { position: relative; min-height: 190px; padding: 28px 24px; border-color: var(--color-outline-variant); border-radius: 13px; align-content: center; }
.area-chip-icon { display: grid; width: 66px; height: 66px; place-items: center; border-radius: 50%; background: var(--color-primary-fixed); color: var(--primary); font-size: 2rem; }
.area-chip-title { position: absolute; left: 112px; top: 40px; font-size: 1.15rem; }
.area-chip-description { margin-left: 88px; white-space: normal; }
.area-chip-meta { margin-top: 12px; padding: 10px; border-radius: 8px; background: #faf8f2; color: #51483d; }
.area-chip.selected { border-color: var(--primary); background: var(--color-surface-container-low); box-shadow: var(--shadow-glow); }
.area-chip-selected { position: absolute; right: 18px; top: 18px; width: 28px; height: 28px; overflow: hidden; color: transparent; border-radius: 50%; background: var(--primary); }
.area-chip-selected::after { content: '✓'; display: grid; height: 100%; place-items: center; color: #fff; }
.area-pagination, .menu-pagination { display: flex; justify-content: center; align-items: center; gap: 8px; margin-top: 18px; }
.area-pagination button, .menu-pagination button { min-width: 38px; min-height: 38px; border: 1px solid var(--color-outline-variant); border-radius: 9px; background: var(--bg-card); color: var(--text-primary); font: inherit; font-weight: 800; cursor: pointer; }
.area-pagination button.active, .menu-pagination button.active { background: var(--primary); border-color: var(--primary); color: #fff; }
.area-pagination button:disabled, .menu-pagination button:disabled { opacity: .45; cursor: not-allowed; }
.menu-pagination.compact { flex-wrap: wrap; max-width: 100%; }
.menu-pagination button.ellipsis { cursor: default; background: transparent; border-color: transparent; color: var(--text-muted); opacity: 1; }
.choice-grid button { min-height: 128px; border-color: #e7d4bd; border-radius: 12px; }
.choice-grid button.selected { border-color: var(--primary); background: var(--color-surface-container-low); }
.menu-picker { display: grid; grid-template-columns: minmax(0, 1fr) minmax(300px, 340px); gap: 20px; margin-top: 24px; align-items: start; }
.menu-picker-main { min-width: 0; }
.menu-picker-main .filters { margin-bottom: 14px; }
.menu-picker-main .error-banner, .menu-picker-main .preorder-summary { margin-bottom: 14px; }
.dish-grid { grid-column: auto; grid-template-columns: repeat(3, minmax(0,1fr)); }
.dish-card { border-radius: 13px; overflow: hidden; }
.dish-card.unavailable { opacity: .72; }
.dish-card img { height: 180px; }
.dish-card .primary-btn { border-radius: 9px; }
.dish-unavailable, .cart-invalid-reason { color: var(--danger); font-weight: 800; }
.cart-box { grid-column: 2; position: sticky; top: 88px; margin-top: 0; border-color: var(--line); border-radius: 13px; background: #fffdfa; }
.cart-empty { margin: 8px 0 0; color: var(--text-muted); }
.cart-row { grid-template-columns: 1fr 96px; padding: 12px 0; border-top: 1px solid var(--line); }
.cart-row.invalid { background: color-mix(in srgb, var(--danger) 7%, transparent); border-radius: 10px; padding-inline: 8px; }
.cart-row input, .cart-row .danger-btn { grid-column: 1 / -1; }
.preference-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; }
.preference-grid label { min-height: 64px; justify-content: space-between; border-color: var(--line); border-radius: 10px; padding: 14px; }
.preference-grid input { order: 2; accent-color: var(--wine); }
.voucher-field { margin-top: 24px; }
.review-box { gap: 2px 22px; padding: 14px; border-radius: 12px; background: #fff9f8; }
.review-box > div { display: flex; min-height: 54px; align-items: center; justify-content: space-between; background: rgba(255,255,255,.55); }
.actions { margin-top: 22px; padding-top: 20px; border-top: 1px solid var(--line); }
.actions .ghost-btn { min-width: 170px; }
.actions .primary-btn { min-width: 190px; }
.primary-btn { min-height: 50px; border-radius: 9px; background: linear-gradient(135deg, var(--wine), #d30a38); border-color: var(--wine); }
.primary-btn:hover:not(:disabled) { background: var(--wine-dark); }
.success-panel { max-width: 980px; margin-right: auto; margin-left: auto; text-align: center; }
.success-panel .summary-grid { margin: 24px 0; text-align: left; }
.reservation-code { background: var(--wine); }
@media (max-width: 1100px) {
  .reservation-page { padding: 34px 22px 56px; }
  .booking-layout, .guest-layout, .menu-picker { grid-template-columns: 1fr; }
  .cart-box { grid-column: 1; position: static; }
  .dish-grid { grid-template-columns: repeat(2, minmax(0,1fr)); }
  .preference-grid { grid-template-columns: repeat(3, minmax(0,1fr)); }
}
@media (max-width: 700px) {
  .reservation-page { padding: 24px 12px 40px; }
  .reservation-header { min-height: 132px; }
  .reservation-header::after { display: none; }
  .reservation-header h1 { font-size: 2.65rem; }
  .stepper { display: flex; overflow-x: auto; }
  .step-chip { flex: 0 0 108px; }
  .reservation-card, .success-panel { padding: 20px 14px; }
  .time-grid, .area-chip-grid, .dish-grid, .preference-grid { grid-template-columns: 1fr; }
  .guest-presets { grid-template-columns: repeat(2, 1fr); }
  .guest-counter { padding: 14px; }
  .guest-counter strong { font-size: 2.4rem; }
  .late-dining-confirmation { grid-template-columns: 1fr; }
  .late-dining-confirmation::before { display: none; }
  .filters { grid-template-columns: 1fr; }
}

/* Reservation UX: visual areas, five-column menu and compact shared notes. */
.area-chip { padding: 0 18px 20px; overflow: hidden; align-content: start; }
.area-chip-image { width: calc(100% + 36px); height: 132px; margin: 0 -18px 16px; object-fit: cover; }
.area-chip-icon { position: absolute; top: 108px; left: 18px; width: 44px; height: 44px; border: 3px solid #fff; }
.area-chip-title { position: static; display: block; margin: 0 0 6px 52px; text-align: left; }
.area-chip-description { display: block; min-height: 42px; margin: 0; text-align: left; }
.area-chip-meta { display: block; text-align: left; }
.area-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.preorder-note { display: grid; grid-column: 1 / -1; gap: 8px; margin-top: 8px; text-align: left; }
.preorder-note small { justify-self: end; color: var(--text-muted); }
.dish-grid { grid-template-columns: repeat(5, minmax(0, 1fr)); }
.dish-card img { height: 132px; }
.payment-state, .copy-field-btn, .qr-actions { display: flex; align-items: center; gap: 8px; }
.payment-state { width: fit-content; padding: 8px 12px; border-radius: 999px; background: var(--color-primary-fixed); color: var(--primary); font-weight: 800; }
.payment-state.paid { background: color-mix(in srgb, var(--success) 12%, #fff); color: var(--success); }
.qr-card dd { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 0; }
.copy-field-btn { min-height: 34px; padding: 5px 9px; border: 1px solid var(--border); border-radius: 8px; background: #fff; color: var(--primary); cursor: pointer; }
.copy-field-btn .ui-icon { width: 16px; height: 16px; flex-basis: 16px; }
.qr-actions { grid-column: 1 / -1; justify-content: flex-end; }
.qr-action-btn { min-height: 42px; padding: 9px 14px; border: 1px solid var(--primary); border-radius: 10px; background: var(--primary); color: var(--color-on-primary); font: inherit; font-weight: 850; cursor: pointer; transition: background .18s ease, border-color .18s ease, transform .18s ease; }
.qr-action-btn.outline { background: #fff; color: var(--primary); }
.qr-action-btn:hover:not(:disabled) { background: var(--primary-dark); border-color: var(--primary-dark); color: var(--color-on-primary); transform: translateY(-1px); }
.qr-action-btn:focus-visible { outline: 3px solid var(--primary-glow); outline-offset: 2px; }
.qr-action-btn:disabled { opacity: .58; cursor: wait; transform: none; }
.qr-status-message { grid-column: 1 / -1; margin: 4px 0 0; color: var(--primary); font-weight: 800; text-align: right; }
@media (max-width: 1100px) and (min-width: 701px) {
  .dish-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 700px) {
  .area-grid { grid-template-columns: 1fr; }
  .dish-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .dish-card img { height: 150px; }
  .qr-actions { align-items: stretch; flex-direction: column; }
}
@media (max-width: 430px) {
  .dish-grid { grid-template-columns: 1fr; }
}

/* Keep preorder quantity controls compact despite the shared mobile input rule. */
.dish-card__title,
.dish-card__description {
  overflow-wrap: anywhere;
}
.dish-card__category,
.dish-card__price,
.dish-added,
.dish-stock-limit,
.dish-unavailable {
  overflow-wrap: anywhere;
}
.cart-row .qty {
  display: inline-grid;
  grid-template-columns: 32px 52px 32px;
  width: max-content;
  min-width: 116px;
  height: 36px;
}
.cart-row .qty button {
  width: 32px;
  min-width: 32px;
  height: 36px;
  min-height: 36px;
  padding: 0;
}
.cart-row .qty-input {
  grid-column: auto;
  width: 52px;
  min-width: 0;
  height: 36px;
  min-height: 36px;
  padding: 4px;
  text-align: center;
}
.reservation-progress { display:flex; align-items:center; justify-content:space-between; gap:12px; margin:-12px 0 16px; padding:8px 12px; border:1px solid var(--color-outline-variant); border-radius:10px; background:var(--color-surface-container-low); color:var(--text-secondary); font-size:.85rem; }
.reservation-progress strong { color:var(--primary); white-space:nowrap; }
.booking-summary { position:sticky; top:16px; z-index:2; width:100%; margin:0; padding:16px; border:1px solid var(--color-outline-variant); border-radius:14px; background:var(--bg-card); box-shadow:var(--shadow-sm); }
.booking-summary h2 { margin:0 0 12px; font-size:1rem; }
.booking-summary dl { display:grid; gap:8px; margin:0; }
.booking-summary dl div { display:flex; justify-content:space-between; gap:10px; border-bottom:1px dashed var(--color-outline-variant); padding-bottom:6px; }
.booking-summary dt { color:var(--text-muted); font-size:.78rem; }
.booking-summary dd { margin:0; text-align:right; font-weight:700; font-size:.82rem; overflow-wrap:anywhere; }
@media (max-width: 900px) { .booking-summary { position:static; width:auto; margin:0; } }
@media (max-width: 520px) { .reservation-progress { font-size:.78rem; } .booking-summary { padding:12px; } }
</style>
