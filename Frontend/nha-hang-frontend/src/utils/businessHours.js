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

export function minuteBefore(value) {
  const minutes = toMinutes(value)
  if (minutes === null) return ''
  const previous = (minutes - 1 + MINUTES_PER_DAY) % MINUTES_PER_DAY
  return `${String(Math.floor(previous / 60)).padStart(2, '0')}:${String(previous % 60).padStart(2, '0')}`
}
