import { describe, expect, it } from 'vitest'
import { kitchenQuantity, normalizeKitchenCollection } from './kitchenData'

describe('kitchen data normalization', () => {
  it('keeps valid API lists and converts invalid payloads to an empty state', () => {
    const rows = [{ id: 1 }]
    expect(normalizeKitchenCollection(rows)).toBe(rows)
    expect(normalizeKitchenCollection(null)).toEqual([])
    expect(normalizeKitchenCollection({ content: rows })).toEqual([])
  })

  it('formats decimal strings safely without a render exception', () => {
    expect(kitchenQuantity('12.5000')).toBe(12.5)
    expect(kitchenQuantity(undefined)).toBe(0)
    expect(kitchenQuantity('invalid')).toBe(0)
  })
})
