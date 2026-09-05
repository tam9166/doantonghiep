import { toBusinessDate } from '@/utils/businessDate'

export const OPERATIONAL_ORDER_PAGE_SIZE = 8

export const dedupeOperationalOrders = (orders = []) => {
  const byOrderId = new Map()
  ;(Array.isArray(orders) ? orders : []).forEach(order => {
    if (order?.id == null) return
    const existing = byOrderId.get(order.id) || {}
    const detailMap = new Map()
    ;[...(existing.orderDetails || []), ...(order.orderDetails || [])].forEach(detail => {
      if (detail?.id != null) detailMap.set(detail.id, { ...(detailMap.get(detail.id) || {}), ...detail })
    })
    byOrderId.set(order.id, { ...existing, ...order, orderDetails: [...detailMap.values()] })
  })
  return [...byOrderId.values()]
}

export const totalOperationalPages = (count, pageSize = OPERATIONAL_ORDER_PAGE_SIZE) =>
  Math.max(1, Math.ceil(Math.max(0, Number(count) || 0) / pageSize))

export const clampOperationalPage = (page, count, pageSize = OPERATIONAL_ORDER_PAGE_SIZE) =>
  Math.min(Math.max(1, Number(page) || 1), totalOperationalPages(count, pageSize))

export const paginateOperationalOrders = (orders, page, pageSize = OPERATIONAL_ORDER_PAGE_SIZE) => {
  const source = Array.isArray(orders) ? orders : []
  const safePage = clampOperationalPage(page, source.length, pageSize)
  const start = (safePage - 1) * pageSize
  return source.slice(start, start + pageSize)
}

export const operationalPageButtons = totalPages =>
  Array.from({ length: Math.max(1, Number(totalPages) || 1) }, (_, index) => index + 1)

export const isFutureServiceOrder = order => {
  const raw = String(order?.scheduledAt || '').trim()
  const serviceDate = raw.match(/^\d{4}-\d{2}-\d{2}/)?.[0]
  return Boolean(serviceDate && serviceDate > toBusinessDate())
}

const validDate = value => {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date
}

const earliestDetailTime = (order, field, predicate = () => true) => {
  const timestamps = (order?.orderDetails || [])
    .filter(predicate)
    .map(detail => validDate(detail?.[field]))
    .filter(Boolean)
    .sort((left, right) => left - right)
  return timestamps[0] || null
}

/** Timers represent operational work, never the time an advance order was created. */
export const operationalTimerStart = (order, queue = 'kitchen') => {
  if (queue === 'waiter-ready') {
    return earliestDetailTime(order, 'completedAt', detail => Number(detail?.status) === 1)
      || validDate(order?.prepareStartTime)
  }
  return earliestDetailTime(order, 'startedAt', detail => Number(detail?.status ?? 0) === 0)
    || validDate(order?.prepareStartTime)
    || earliestDetailTime(order, 'queuedAt')
}

export const operationalElapsedMinutes = (order, now = new Date(), queue = 'kitchen') => {
  const start = operationalTimerStart(order, queue)
  if (!start) return 0
  return Math.max(0, Math.floor((new Date(now) - start) / 60000))
}

export const formatOperationalWait = (minutes, preorder = false) => {
  const safeMinutes = Math.max(0, Math.floor(Number(minutes) || 0))
  if (safeMinutes >= 24 * 60) {
    return preorder ? 'Đơn đặt trước' : 'Đã chờ: trên 24 giờ'
  }
  if (safeMinutes >= 60) {
    return `Đã chờ: ${Math.floor(safeMinutes / 60)} giờ ${safeMinutes % 60} phút`
  }
  return `Đã chờ: ${safeMinutes} phút`
}

export const operationalWaitLabel = (order, now = new Date(), queue = 'kitchen') =>
  formatOperationalWait(operationalElapsedMinutes(order, now, queue), Boolean(order?.preorder))

export const isBeforePreparation = (order, now = new Date()) => {
  const prepareStart = validDate(order?.prepareStartTime)
  return Boolean(order?.preorder && prepareStart && new Date(now) < prepareStart)
}
