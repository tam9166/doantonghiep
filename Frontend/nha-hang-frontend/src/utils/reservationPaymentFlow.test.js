import { describe, expect, it } from 'vitest'
import { nextReservationStep, previousReservationStep, shouldSkipReservationPayment } from './reservationPaymentFlow'

describe('reservation payment flow', () => {
  it('skips payment when there are no dishes and no deposit', () => {
    const quote = { foodAmount: 0, payableNow: 0, depositPolicy: null }
    expect(shouldSkipReservationPayment(quote)).toBe(true)
    expect(nextReservationStep(7, quote)).toBe(9)
    expect(previousReservationStep(9, quote)).toBe(7)
  })

  it('keeps payment when pre-order dishes create an amount due', () => {
    expect(nextReservationStep(7, { foodAmount: 400000, payableNow: 200000 })).toBe(8)
  })

  it('keeps payment when a deposit policy requires payment', () => {
    expect(nextReservationStep(7, { foodAmount: 0, payableNow: 300000, depositPolicy: { policyCode: 'GROUP' } })).toBe(8)
  })
})
