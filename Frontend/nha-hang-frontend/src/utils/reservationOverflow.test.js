import { describe, expect, it } from 'vitest'
import {
  createEventBookingDraft,
  earlyGroupWarning,
  hasAvailableSingleTable,
  shouldRedirectToEventBooking,
  waitlistOverflowReason
} from './reservationOverflow'

describe('reservation overflow workflow', () => {
  it('shows an early warning only when the party exceeds the largest single-table capacity', () => {
    expect(earlyGroupWarning(20)).toBe('')
    expect(earlyGroupWarning(21)).toContain('thử ghép bàn')
  })

  it('keeps a fitting group in the normal table flow when a single table or combination is available', () => {
    expect(hasAvailableSingleTable([{ availabilityStatus: 'AVAILABLE', maxCapacity: 24 }], 21)).toBe(true)
    expect(shouldRedirectToEventBooking({ available: true }, 21)).toBe(false)
    expect(waitlistOverflowReason(21)).toBe('GROUP_TOO_LARGE')
  })

  it('redirects an oversized group to event booking and preserves its draft when no combination is available', () => {
    const form = {
      customerName: 'Nguyen Van A', customerPhone: '0912345678', customerEmail: 'a@example.com',
      reservationDate: '2026-08-20', arrivalTime: '18:00', guestCount: 25
    }

    expect(hasAvailableSingleTable([{ availabilityStatus: 'AVAILABLE', maxCapacity: 12 }], form.guestCount)).toBe(false)
    expect(shouldRedirectToEventBooking({ available: false }, form.guestCount)).toBe(true)
    expect(createEventBookingDraft(form)).toEqual(form)
  })
})
