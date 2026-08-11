export const LARGEST_SINGLE_TABLE_CAPACITY = 20

export function exceedsLargestSingleTableCapacity(guestCount, capacity = LARGEST_SINGLE_TABLE_CAPACITY) {
  return Number(guestCount || 0) > Number(capacity || LARGEST_SINGLE_TABLE_CAPACITY)
}

export function earlyGroupWarning(guestCount, capacity = LARGEST_SINGLE_TABLE_CAPACITY) {
  return exceedsLargestSingleTableCapacity(guestCount, capacity)
    ? 'Nhóm đông: hệ thống sẽ thử ghép bàn; nếu không đủ sẽ chuyển sang đặt sảnh sự kiện.'
    : ''
}

export function hasAvailableSingleTable(tables, guestCount) {
  return (tables || []).some(table => table.availabilityStatus === 'AVAILABLE'
    && Number(table.maxCapacity || table.capacity || 0) >= Number(guestCount || 0))
}

export function shouldRedirectToEventBooking(tableCombination, guestCount, capacity = LARGEST_SINGLE_TABLE_CAPACITY) {
  return exceedsLargestSingleTableCapacity(guestCount, capacity) && !tableCombination?.available
}

export function createEventBookingDraft(form) {
  return {
    customerName: form.customerName,
    customerPhone: form.customerPhone,
    customerEmail: form.customerEmail,
    reservationDate: form.reservationDate,
    arrivalTime: form.arrivalTime,
    guestCount: form.guestCount
  }
}

export function waitlistOverflowReason(guestCount, capacity = LARGEST_SINGLE_TABLE_CAPACITY) {
  return exceedsLargestSingleTableCapacity(guestCount, capacity) ? 'GROUP_TOO_LARGE' : null
}
