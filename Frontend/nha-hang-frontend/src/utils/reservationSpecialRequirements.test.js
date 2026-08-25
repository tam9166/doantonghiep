import { describe, expect, it } from 'vitest'
import { normalizeSpecialRequirements, serializeRequirementFlags } from './reservationSpecialRequirements'

describe('reservation special requirements', () => {
  it('normalizes an empty optional selection without undefined values', () => {
    const result = normalizeSpecialRequirements([], '')
    expect(result).toEqual({
      birthdayDecoration: false,
      childSeat: false,
      birthdayCake: false,
      nonSmoking: false,
      elderlyGuest: false,
      disabledGuest: false,
      foodAllergy: false,
      note: ''
    })
    expect(serializeRequirementFlags(result)).toBe('REQ:BD=0;CS=0;BC=0;NS=0;EG=0;DG=0;FA=0')
  })

  it('preserves one or many requirements, allergy and a trimmed note', () => {
    const result = normalizeSpecialRequirements(
      ['Trang trí sinh nhật', 'Ghế trẻ em', 'Dị ứng thực phẩm'],
      '  Không dùng đậu phộng  '
    )
    expect(result.birthdayDecoration).toBe(true)
    expect(result.childSeat).toBe(true)
    expect(result.foodAllergy).toBe(true)
    expect(result.birthdayCake).toBe(false)
    expect(result.note).toBe('Không dùng đậu phộng')
    expect(serializeRequirementFlags(result)).toContain('FA=1')
  })

  it('keeps the free note within the existing backend limit', () => {
    expect(normalizeSpecialRequirements([], 'x'.repeat(700)).note).toHaveLength(500)
  })
})
