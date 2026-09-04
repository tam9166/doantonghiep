import { describe, expect, it } from 'vitest'
import {
  OPERATIONAL_ORDER_PAGE_SIZE,
  clampOperationalPage,
  dedupeOperationalOrders,
  isFutureServiceOrder,
  paginateOperationalOrders,
  totalOperationalPages,
} from './operationalOrderUi'

describe('operational order pagination', () => {
  const orders = count => Array.from({ length: count }, (_, index) => ({ id: index + 1, orderDetails: [] }))

  it('uses eight order cards per page, independent of detail quantity', () => {
    const source = orders(9)
    source[0].orderDetails = Array.from({ length: 7 }, (_, index) => ({ id: index + 1 }))
    expect(OPERATIONAL_ORDER_PAGE_SIZE).toBe(8)
    expect(paginateOperationalOrders(source, 1)).toHaveLength(8)
    expect(paginateOperationalOrders(source, 2)).toHaveLength(1)
  })

  it('supports 17 and 26 order matrices and clamps a page after realtime shrink', () => {
    expect([1, 2, 3].map(page => paginateOperationalOrders(orders(17), page).length)).toEqual([8, 8, 1])
    expect([1, 2, 3, 4].map(page => paginateOperationalOrders(orders(26), page).length)).toEqual([8, 8, 8, 2])
    expect(totalOperationalPages(17)).toBe(3)
    expect(clampOperationalPage(4, 9)).toBe(2)
  })

  it('deduplicates order cards and details after websocket plus refresh merges', () => {
    const merged = dedupeOperationalOrders([
      { id: 1, orderDetails: [{ id: 10, status: 0 }] },
      { id: 1, orderDetails: [{ id: 10, status: 1 }, { id: 11, status: 0 }] },
    ])
    expect(merged).toHaveLength(1)
    expect(merged[0].orderDetails).toHaveLength(2)
    expect(merged[0].orderDetails.find(detail => detail.id === 10).status).toBe(1)
  })

  it('marks only a real future scheduled service date', () => {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const localTomorrow = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`
    expect(isFutureServiceOrder({ scheduledAt: `${localTomorrow}T18:00:00` })).toBe(true)
    expect(isFutureServiceOrder({ scheduledAt: null, createDate: `${localTomorrow}T18:00:00` })).toBe(false)
  })
})
