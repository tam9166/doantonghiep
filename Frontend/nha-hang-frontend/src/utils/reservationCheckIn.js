const CHECK_IN_EARLY_MINUTES = 60
const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh'
const BUSINESS_UTC_OFFSET = '+07:00'
const CHECK_IN_STATES = new Set(['CONFIRMED', 'DEPOSIT_PAID', 'FULLY_PAID'])

function reservationDateTime(reservation) {
  const date = String(reservation?.reservationDate || '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  const time = String(reservation?.arrivalTime || '').match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?/)
  if (!date || !time) return null

  const hour = time[1].padStart(2, '0')
  const second = time[3] || '00'
  const timestamp = Date.parse(`${date[1]}-${date[2]}-${date[3]}T${hour}:${time[2]}:${second}${BUSINESS_UTC_OFFSET}`)
  return Number.isFinite(timestamp) ? timestamp : null
}

export function allowedCheckInTimestamp(reservation) {
  const arrivalTimestamp = reservationDateTime(reservation)
  return arrivalTimestamp === null ? null : arrivalTimestamp - CHECK_IN_EARLY_MINUTES * 60_000
}

export function formatAllowedCheckInTime(timestamp) {
  const values = Object.fromEntries(new Intl.DateTimeFormat('vi-VN', {
    timeZone: BUSINESS_TIME_ZONE,
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).formatToParts(new Date(timestamp)).map(part => [part.type, part.value]))
  return `${values.hour}:${values.minute} ${values.day}/${values.month}/${values.year}`
}

export function checkInAvailability(reservation, now = Date.now()) {
  if (!CHECK_IN_STATES.has(reservation?.reservationStatus)) {
    return { visible: false, allowed: false, reason: '' }
  }

  const allowedAt = allowedCheckInTimestamp(reservation)
  if (allowedAt === null) {
    return { visible: true, allowed: false, reason: 'Thiếu ngày hoặc giờ đặt bàn.' }
  }
  if (now < allowedAt) {
    return {
      visible: true,
      allowed: false,
      reason: `Chưa tới giờ check-in. Có thể check-in từ ${formatAllowedCheckInTime(allowedAt)}`,
    }
  }
  return { visible: true, allowed: true, reason: '' }
}
