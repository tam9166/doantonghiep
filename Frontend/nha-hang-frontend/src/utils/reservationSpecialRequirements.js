const REQUIREMENTS = [
  ['birthdayDecoration', /trang trí sinh nhật|birthday decoration/i],
  ['childSeat', /ghế trẻ em|child (seat|chair)/i],
  ['birthdayCake', /bánh sinh nhật|birthday cake/i],
  ['nonSmoking', /không hút thuốc|non-smoking/i],
  ['elderlyGuest', /người lớn tuổi|elderly guest/i],
  ['disabledGuest', /người khuyết tật|disabled guest/i],
  ['foodAllergy', /dị ứng thực phẩm|food allergy/i]
]

export function normalizeSpecialRequirements(selected = [], note = '') {
  const values = Array.isArray(selected) ? selected.map(value => String(value)) : []
  return Object.fromEntries([
    ...REQUIREMENTS.map(([key, pattern]) => [key, values.some(value => pattern.test(value))]),
    ['note', String(note || '').trim().slice(0, 500)]
  ])
}

export function serializeRequirementFlags(requirements) {
  const codes = ['BD', 'CS', 'BC', 'NS', 'EG', 'DG', 'FA']
  return `REQ:${REQUIREMENTS.map(([key], index) => `${codes[index]}=${requirements?.[key] ? 1 : 0}`).join(';')}`
}
