export function normalizeKitchenCollection(payload) {
  return Array.isArray(payload) ? payload : []
}

export function kitchenQuantity(value) {
  const quantity = Number(value)
  return Number.isFinite(quantity) ? quantity : 0
}
