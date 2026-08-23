import { describe, expect, it } from 'vitest'
import { toBusinessDate } from './businessDate'

describe('Vietnam business dates', () => {
  it('does not use the previous UTC date after midnight in Vietnam', () => {
    expect(toBusinessDate(new Date('2026-08-20T17:30:00.000Z'))).toBe('2026-08-21')
  })

  it('keeps the Vietnam date before local midnight', () => {
    expect(toBusinessDate(new Date('2026-08-20T16:59:59.000Z'))).toBe('2026-08-20')
  })
})
