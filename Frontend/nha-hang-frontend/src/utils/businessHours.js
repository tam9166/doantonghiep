const MINUTES_PER_DAY = 24 * 60

function toMinutes(value) {
  const match = /^(\d{2}):(\d{2})$/.exec(String(value || ''))
  if (!match) return null
  const hour = Number(match[1])
  const minute = Number(match[2])
  if (hour > 23 || minute > 59) return null
  return hour * 60 + minute
}

export function isTimeWithinWindow(value, start, exclusiveEnd) {
  const time = toMinutes(value)
  const from = toMinutes(start)
  const to = toMinutes(exclusiveEnd)
  if (time === null || from === null || to === null || from === to) return false
  return from < to ? time >= from && time < to : time >= from || time < to
}

export function isTimeWithinInclusiveWindow(value, start, inclusiveEnd) {
  const time = toMinutes(value)
  const from = toMinutes(start)
  const to = toMinutes(inclusiveEnd)
  if (time === null || from === null || to === null || from === to) return false
  return from < to ? time >= from && time <= to : time >= from || time <= to
}

export function requiresLateDiningConfirmation(value, durationMinutes, openingTime, closingTime) {
  const arrival = toMinutes(value)
  const opening = toMinutes(openingTime)
  const closing = toMinutes(closingTime)
  const duration = Number(durationMinutes)
  if (arrival === null || opening === null || closing === null || !Number.isFinite(duration) || duration <= 0) return false
  if (!isTimeWithinInclusiveWindow(value, openingTime, closingTime)) return false
  const serviceSpan = (closing - opening + MINUTES_PER_DAY) % MINUTES_PER_DAY
  const arrivalOffset = (arrival - opening + MINUTES_PER_DAY) % MINUTES_PER_DAY
  return arrivalOffset + duration > serviceSpan
}

export function buildReservationTimeSignature(reservationDate, arrivalTime, durationMinutes) {
  const date = String(reservationDate || '').trim()
  const time = String(arrivalTime || '').trim()
  const duration = Number(durationMinutes)
  if (!date || !time || !Number.isFinite(duration) || duration <= 0) return ''
  return `${date}|${time}|${duration}`
}

export function isLateDiningConfirmationCurrent(confirmedSignature, currentSignature) {
  return Boolean(currentSignature) && confirmedSignature === currentSignature
}

export function minuteBefore(value) {
  const minutes = toMinutes(value)
  if (minutes === null) return ''
  const previous = (minutes - 1 + MINUTES_PER_DAY) % MINUTES_PER_DAY
  return `${String(Math.floor(previous / 60)).padStart(2, '0')}:${String(previous % 60).padStart(2, '0')}`
}
