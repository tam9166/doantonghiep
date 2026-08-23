import { describe, expect, it } from 'vitest'
import { isTimeWithinWindow, minuteBefore } from './businessHours'

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
})
