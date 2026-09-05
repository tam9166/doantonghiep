import { describe, expect, it } from 'vitest'
import {
  OPERATIONAL_ORDER_PAGE_SIZE,
  clampOperationalPage,
  dedupeOperationalOrders,
  formatOperationalWait,
  isBeforePreparation,
  isFutureServiceOrder,
  operationalElapsedMinutes,
  operationalTimerStart,
  operationalWaitLabel,
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

  it('never uses createdAt as the preparation timer for a preorder', () => {
    const now = new Date('2026-09-05T19:00:00+07:00')
    const order = {
      preorder: true,
      createDate: '2026-08-01T10:00:00+07:00',
      prepareStartTime: '2026-09-05T18:30:00+07:00',
      orderDetails: [],
    }
    expect(operationalTimerStart(order)?.toISOString()).toBe('2026-09-05T11:30:00.000Z')
    expect(operationalElapsedMinutes(order, now)).toBe(30)
    expect(operationalWaitLabel(order, now)).toBe('Đã chờ: 30 phút')
  })

  it('uses dish lifecycle times for Kitchen and Waiter queues', () => {
    const now = new Date('2026-09-05T19:00:00+07:00')
    const order = {
      createDate: '2026-08-01T10:00:00+07:00',
      orderDetails: [
        { status: 0, queuedAt: '2026-09-05T18:20:00+07:00', startedAt: '2026-09-05T18:45:00+07:00' },
        { status: 1, completedAt: '2026-09-05T18:55:00+07:00' },
      ],
    }
    expect(operationalElapsedMinutes(order, now, 'kitchen')).toBe(15)
    expect(operationalElapsedMinutes(order, now, 'waiter-ready')).toBe(5)
  })

  it('identifies a preorder before its preparation window', () => {
    const order = { preorder: true, prepareStartTime: '2026-09-05T18:30:00+07:00' }
    expect(isBeforePreparation(order, new Date('2026-09-05T18:29:59+07:00'))).toBe(true)
    expect(isBeforePreparation(order, new Date('2026-09-05T18:30:00+07:00'))).toBe(false)
  })

  it('formats operational waits without exposing huge minute/hour counters', () => {
    expect(formatOperationalWait(15)).toBe('Đã chờ: 15 phút')
    expect(formatOperationalWait(59)).toBe('Đã chờ: 59 phút')
    expect(formatOperationalWait(75)).toBe('Đã chờ: 1 giờ 15 phút')
    expect(formatOperationalWait(150)).toBe('Đã chờ: 2 giờ 30 phút')
    expect(formatOperationalWait(15189)).toBe('Đã chờ: trên 24 giờ')
    expect(formatOperationalWait(15189, true)).toBe('Đơn đặt trước')
  })
})
