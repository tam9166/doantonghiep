export function isFinanciallyPaid(order) {
  return Boolean(order?.isPaid) || ['PAID', 'OVERPAID'].includes(String(order?.paymentStatus || ''))
}

export function isCancelledOrder(order) {
  return Number(order?.status) === 3
}

export function isTableOrder(order) {
  return order?.tableId !== null && order?.tableId !== undefined
}

export function isAwaitingPayment(order) {
  return isTableOrder(order) && !isCancelledOrder(order) && !isFinanciallyPaid(order)
}

export function orderLifecycleLabel(order) {
  if (!order) return 'Chưa gọi món'
  if (isFinanciallyPaid(order)) return 'Đã thanh toán'
  if (Number(order.status) === 7 || Number(order.status) === 4) return 'Chờ thanh toán'
  if ([1, 2, 6].includes(Number(order.status))) return 'Đang phục vụ'
  return 'Chưa gọi món'
}

export function findAwaitingPaymentOrder(orders, tableId) {
  return (Array.isArray(orders) ? orders : [])
    .filter(order => Number(order?.tableId) === Number(tableId) && isAwaitingPayment(order))
    .sort((left, right) => Number(right?.id || 0) - Number(left?.id || 0))[0] || null
}

export function tableFloor(table) {
  return String(table?.floor || '').trim() || 'Chưa phân tầng'
}

export function tableArea(table) {
  return String(table?.areaName || '').trim() || 'Khu vực chung'
}

export function groupTablesByFloorAndArea(tables) {
  const grouped = {}
  for (const table of Array.isArray(tables) ? tables : []) {
    const floor = tableFloor(table)
    const area = tableArea(table)
    grouped[floor] ||= {}
    grouped[floor][area] ||= []
    grouped[floor][area].push(table)
  }
  return Object.fromEntries(Object.entries(grouped)
    .sort(([left], [right]) => left.localeCompare(right, 'vi'))
    .map(([floor, areas]) => [floor, Object.fromEntries(Object.entries(areas)
      .sort(([left], [right]) => left.localeCompare(right, 'vi')))]))
}
