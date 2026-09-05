import { describe, expect, it } from 'vitest'
import { allowedCheckInTimestamp, checkInAvailability } from './reservationCheckIn'

const booking = {
  reservationDate: '2026-09-05',
  arrivalTime: '19:00:00',
  reservationStatus: 'DEPOSIT_PAID',
}

const atVietnamTime = value => Date.parse(`${value}+07:00`)

describe('admin reservation check-in availability', () => {
  it('opens exactly sixty minutes before arrival', () => {
    expect(allowedCheckInTimestamp(booking)).toBe(atVietnamTime('2026-09-05T18:00:00'))
    expect(checkInAvailability(booking, atVietnamTime('2026-09-05T17:59:00'))).toEqual({
      visible: true,
      allowed: false,
      reason: 'Chưa tới giờ check-in. Có thể check-in từ 18:00 05/09/2026',
    })
    expect(checkInAvailability(booking, atVietnamTime('2026-09-05T18:00:00')).allowed).toBe(true)
    expect(checkInAvailability(booking, atVietnamTime('2026-09-05T19:00:00')).allowed).toBe(true)
  })

  it('keeps tomorrow reservations disabled', () => {
    expect(checkInAvailability(booking, atVietnamTime('2026-09-04T19:00:00')).allowed).toBe(false)
  })

  it('does not expose check-in for backend-ineligible states', () => {
    expect(checkInAvailability({ ...booking, reservationStatus: 'DEPOSIT_PENDING' }).visible).toBe(false)
    expect(checkInAvailability({ ...booking, reservationStatus: 'DEPOSIT_REQUIRED' }).visible).toBe(false)
  })
})
