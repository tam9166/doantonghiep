import { describe, expect, it } from 'vitest'
import {
  buildReservationTimeSignature,
  isLateDiningConfirmationCurrent,
  isTimeWithinInclusiveWindow,
  isTimeWithinWindow,
  minuteBefore,
  requiresLateDiningConfirmation
} from './businessHours'

describe('business hours', () => {
  it('uses an exclusive configured cutoff for normal service hours', () => {
    expect(isTimeWithinWindow('09:00', '09:00', '21:30')).toBe(true)
    expect(isTimeWithinWindow('21:29', '09:00', '21:30')).toBe(true)
    expect(isTimeWithinWindow('21:30', '09:00', '21:30')).toBe(false)
  })

  it('supports service windows that pass midnight', () => {
    expect(isTimeWithinWindow('23:30', '23:00', '05:30')).toBe(true)
    expect(isTimeWithinWindow('05:29', '23:00', '05:30')).toBe(true)
    expect(isTimeWithinWindow('12:00', '23:00', '05:30')).toBe(false)
  })

  it('derives the latest minute accepted by a time input', () => {
    expect(minuteBefore('21:30')).toBe('21:29')
    expect(minuteBefore('00:00')).toBe('23:59')
  })

  it('uses an inclusive 09:00-22:00 arrival window for reservations', () => {
    expect(isTimeWithinInclusiveWindow('08:59', '09:00', '22:00')).toBe(false)
    expect(isTimeWithinInclusiveWindow('09:00', '09:00', '22:00')).toBe(true)
    expect(isTimeWithinInclusiveWindow('21:30', '09:00', '22:00')).toBe(true)
    expect(isTimeWithinInclusiveWindow('21:41', '09:00', '22:00')).toBe(true)
    expect(isTimeWithinInclusiveWindow('21:59', '09:00', '22:00')).toBe(true)
    expect(isTimeWithinInclusiveWindow('22:00', '09:00', '22:00')).toBe(true)
    expect(isTimeWithinInclusiveWindow('22:01', '09:00', '22:00')).toBe(false)
  })

  it('keeps late-dining confirmation separate from the arrival window', () => {
    expect(requiresLateDiningConfirmation('21:41', 120, '09:00', '22:00')).toBe(true)
    expect(requiresLateDiningConfirmation('22:00', 120, '09:00', '22:00')).toBe(true)
    expect(requiresLateDiningConfirmation('20:00', 120, '09:00', '22:00')).toBe(false)
  })

  it('keeps a late-dining confirmation through the full no-preorder flow', () => {
    const booking = {
      reservationDate: '2026-09-05',
      arrivalTime: '20:41',
      expectedDurationMinutes: 120,
      guestCount: 2,
      areaId: null,
      tableId: null,
      preorderEnabled: false
    }
    const signature = buildReservationTimeSignature(
      booking.reservationDate,
      booking.arrivalTime,
      booking.expectedDurationMinutes
    )

    booking.guestCount = 4
    booking.areaId = 2
    booking.tableId = 15
    booking.preorderEnabled = false

    expect(isLateDiningConfirmationCurrent(signature, buildReservationTimeSignature(
      booking.reservationDate,
      booking.arrivalTime,
      booking.expectedDurationMinutes
    ))).toBe(true)
  })

  it('keeps a late-dining confirmation through products, quantity, and kitchen-note changes', () => {
    const booking = {
      reservationDate: '2026-09-05',
      arrivalTime: '20:41',
      expectedDurationMinutes: 120,
      preorderEnabled: true,
      preorderItems: [],
      orderNote: ''
    }
    const signature = buildReservationTimeSignature(
      booking.reservationDate,
      booking.arrivalTime,
      booking.expectedDurationMinutes
    )

    booking.preorderItems.push(
      { productId: 1, quantity: 1 },
      { productId: 2, quantity: 2 },
      { productId: 3, quantity: 1 }
    )
    booking.preorderItems[0].quantity = 3
    booking.orderNote = 'Ít cay, phục vụ cùng lúc.'

    expect(isLateDiningConfirmationCurrent(signature, buildReservationTimeSignature(
      booking.reservationDate,
      booking.arrivalTime,
      booking.expectedDurationMinutes
    ))).toBe(true)
  })

  it('invalidates the confirmation when arrival time or duration changes', () => {
    const confirmed = buildReservationTimeSignature('2026-09-05', '20:41', 120)

    expect(isLateDiningConfirmationCurrent(
      confirmed,
      buildReservationTimeSignature('2026-09-05', '21:00', 120)
    )).toBe(false)
    expect(isLateDiningConfirmationCurrent(
      confirmed,
      buildReservationTimeSignature('2026-09-05', '20:41', 180)
    )).toBe(false)
  })
})
