import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = name => readFileSync(fileURLToPath(new URL(name, import.meta.url)), 'utf8')

describe('kitchen, dispatch and reservation flow contracts', () => {
  it('keeps Kitchen renderable when queue, inventory, menu or realtime fails', () => {
    const kitchen = source('./Kitchen.vue')
    expect(kitchen).toContain("const inventoryError = ref('')")
    expect(kitchen).toContain("const menuError = ref('')")
    expect(kitchen).toContain('Promise.allSettled')
    expect(kitchen).toContain('Không thể tải dữ liệu tồn kho. Vui lòng thử lại.')
    expect(kitchen).toContain("console.warn('Kitchen realtime unavailable; polling remains active.'")
    expect(kitchen).toContain('v-if="activeTab === \'orders\' && loadError"')
  })

  it('uses the dedicated dispatch endpoint and preserves the backend reason', () => {
    const adminOrder = source('./AdminOrder.vue')
    expect(adminOrder).toContain('/dispatch-to-kitchen')
    expect(adminOrder).toContain('getApiErrorMessage(error')
    expect(adminOrder).not.toContain('Vui lòng kiểm tra quyền.')
  })

  it('catches Step 7 quote failures and routes from the returned payable amount', () => {
    const reservation = source('./Reservation.vue')
    expect(reservation).toContain('if (step.value === 7 || step.value === 8) await loadQuote()')
    expect(reservation).toContain("step.value === 7 ? t('reservation.errors.specialRequest') : t('reservation.errors.quote')")
    expect(reservation).toContain('step.value = nextReservationStep(step.value, quote.value)')
    expect(reservation).toContain("paymentOption: form.value.paymentOption || 'DEPOSIT_50'")
    expect(reservation).toContain('serializeRequirementFlags(requirements)')
  })
})
