export function getApiErrorMessage(error, fallback = 'Yêu cầu không thể xử lý.') {
  const payload = error?.response?.data
  if (typeof payload === 'string' && payload.trim()) return payload
  if (!payload || typeof payload !== 'object') return fallback

  const fieldMessage = Object.values(payload.fieldErrors || {}).find(Boolean)
  if (payload.message && fieldMessage) return `${payload.message}: ${fieldMessage}`
  return payload.message || fieldMessage || fallback
}
