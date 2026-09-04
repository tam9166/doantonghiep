import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const views = fileURLToPath(new URL('./', import.meta.url))

describe('public view API contracts', () => {
  it('uses the public hot-menu endpoint for unauthenticated dine-in suggestions', () => {
    const source = readFileSync(`${views}/DineInOrder.vue`, 'utf8')

    expect(source).toContain("api.get('/api/menu/hot?limit=4')")
    expect(source).not.toContain("api.get('/api/admin/popular-items/products")
  })

  it('keeps cancellation verification in the request body and phone numbers out of URLs', () => {
    const source = readFileSync(`${views}/ReservationLookup.vue`, 'utf8')

    expect(source).toContain("api.post('/api/reservation-cancellations'")
    expect(source).not.toContain('phone: form.value.phone || undefined')
    expect(source).not.toContain('route.query.phone')
    expect(source).toContain('canSendCancellationRequest')
    expect(source).toContain('cancellationAvailabilityMessage')
    expect(source).toContain("Vui lòng nhập lý do hủy.")
    expect(source).toContain('cancellationRequestPayload')
    expect(source).toContain('reservation.value.customerPhone')
    expect(source).toContain('Phương thức liên lạc mong muốn')
    expect(source).toContain('refundRequired')
    expect(source).toContain('refundBankName')
    expect(source).toContain('refundAccountNumber')
    expect(source).toContain('refundAccountHolder')
    expect(source).not.toContain('v-if="refundPreview" class="danger-btn"')
  })

  it('does not hard-code a 50% manual refund in the cashier cancellation flow', () => {
    const source = readFileSync(`${views}/CashierView.vue`, 'utf8')

    expect(source).toContain('res.data.refundAmount')
    expect(source).toContain('res.data.refundStatus')
    expect(source).not.toContain('hoàn lại 50% tiền cọc')
    expect(source).not.toContain('tự chuyển khoản ngoài')
  })

  it('caps customer cart quantities using backend availableQuantity', () => {
    for (const file of ['ProductMenu.vue', 'DineInOrder.vue']) {
      const source = readFileSync(`${views}/${file}`, 'utf8')
      expect(source).toContain('availableQuantity')
      expect(source).toMatch(/text\.soldOut|t\('dineIn\.quantityLimit'/)
      expect(source).toMatch(/menu\.quantityLimit|dineIn\.quantityLimit/)
    }
  })

  it('connects the no-table state to the public waitlist flow without restoring manual table selection', () => {
    const source = readFileSync(`${views}/Reservation.vue`, 'utf8')

    expect(source).toContain('@click="submitWaitlist"')
    expect(source).toContain("api.post('/api/reservation-waitlist'")
    expect(source).toContain('availableTables.length || tableCombo?.available')
    expect(source).not.toContain('function selectTable(')
    expect(source).not.toContain('function acceptTableCombination(')
  })

  it('keeps payment configuration out of the reservation frontend and uses one preorder note', () => {
    const source = readFileSync(`${views}/Reservation.vue`, 'utf8')

    expect(source).toContain('paymentQr.bankName || paymentQr.bankCode')
    expect(source).toContain('v-model.trim="form.orderNote"')
    const configuredAccount = ['919', '112', '006', '789'].join('')
    expect(source).not.toContain(configuredAccount)
    expect(source).not.toContain('v-model.trim="item.note"')
  })
})
