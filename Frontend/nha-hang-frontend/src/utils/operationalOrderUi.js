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
