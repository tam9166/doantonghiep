import { describe, expect, it } from 'vitest'
import vi from './vi.json'
import en from './en.json'
import productMenuMessages from './productMenuMessages'
import reservationMessages from './reservationMessages'

function sortedKeys(value) {
  return Object.keys(value).sort().map(key => [
    key,
    value[key] && typeof value[key] === 'object' && !Array.isArray(value[key])
      ? sortedKeys(value[key])
      : null
  ])
}

describe('locale dictionaries', () => {
  it('loads Vietnamese and English dictionaries', () => {
    expect(Object.keys(vi).length).toBeGreaterThan(0)
    expect(Object.keys(en).length).toBeGreaterThan(0)
  })

  it('keeps key parity for top-level namespaces', () => {
    expect(Object.keys(en).sort()).toEqual(Object.keys(vi).sort())
  })

  it('keeps reservation translation keys in sync', () => {
    expect(sortedKeys(reservationMessages.en)).toEqual(sortedKeys(reservationMessages.vi))
  })

  it('keeps product menu translation keys in sync', () => {
    expect(sortedKeys(productMenuMessages.en)).toEqual(sortedKeys(productMenuMessages.vi))
  })
})
