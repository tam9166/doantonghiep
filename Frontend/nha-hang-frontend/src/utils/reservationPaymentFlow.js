export function shouldSkipReservationPayment(quote) {
  return quote != null && Number(quote.payableNow ?? quote.depositAmount ?? 0) <= 0
}

export function nextReservationStep(currentStep, quote) {
  return currentStep === 7 && shouldSkipReservationPayment(quote)
    ? 9
    : Math.min(9, currentStep + 1)
}

export function previousReservationStep(currentStep, quote) {
  return currentStep === 9 && shouldSkipReservationPayment(quote)
    ? 7
    : Math.max(1, currentStep - 1)
}
