import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const source = readFileSync(new URL('./Reservation.vue', import.meta.url), 'utf8')

describe('reservation late-dining full-flow contract', () => {
  it('sends the same confirmed state to availability, quote, and final creation', () => {
    const quoteStart = source.indexOf('async function loadQuote()')
    const submitStart = source.indexOf('async function submitReservation()')
    const quoteSource = source.slice(quoteStart, submitStart)

    expect(quoteStart).toBeGreaterThan(-1)
    expect(quoteSource).toContain('lateDiningConfirmed: lateDiningConfirmed.value')
    // The confirmation is sent only to the three contracts that validate the
    // service window: table availability, quote and final reservation creation.
    expect(source.match(/lateDiningConfirmed: lateDiningConfirmed\.value/g)).toHaveLength(3)
  })

  it('binds confirmation validity only to the date, arrival time, and duration signature', () => {
    expect(source).toContain('const currentReservationTimeSignature = computed(() => buildReservationTimeSignature(')
    expect(source).toContain('form.value.reservationDate,')
    expect(source).toContain('form.value.arrivalTime,')
    expect(source).toContain('form.value.expectedDurationMinutes')
    expect(source).not.toContain('lateDiningConfirmed.value = true')
  })

  it('keeps the acknowledgement when the guest changes the preorder choice', () => {
    const disableStart = source.indexOf('function disablePreorder()')
    const disableEnd = source.indexOf('\n}', disableStart) + 2
    const disableSource = source.slice(disableStart, disableEnd)

    expect(disableStart).toBeGreaterThan(-1)
    expect(disableSource).toContain('form.value.preorderEnabled = false')
    expect(disableSource).not.toContain('confirmedLateDiningSignature')
    expect(disableSource).not.toContain('lateDiningConfirmed')
  })

  it('revalidates the same acknowledgement at the preorder step without bypassing it', () => {
    const validationStart = source.indexOf('function validateCurrentStep()')
    const validationEnd = source.indexOf('\n}\n\nwatch(currentReservationTimeSignature', validationStart)
    const validationSource = source.slice(validationStart, validationEnd)

    expect(validationSource).toContain('if (step.value === 6 && lateDiningEndTime.value && !lateDiningConfirmed.value)')
    expect(validationSource).not.toContain('lateDiningConfirmed.value = true')
  })
})
