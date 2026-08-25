import { describe, expect, it } from 'vitest'
import vi from './vi.json'
import en from './en.json'

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

  it('keeps recursive key parity for JSON dictionaries', () => {
    expect(sortedKeys(en)).toEqual(sortedKeys(vi))
  })

  it('contains the complete customer menu and booking dictionaries', () => {
    expect(Object.keys(en.menu).length).toBeGreaterThan(40)
    expect(Object.keys(en.reservation).length).toBeGreaterThan(40)
  })
})
