import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const views = fileURLToPath(new URL('./', import.meta.url))

describe('cancellation refund workflow contracts', () => {
  it('keeps refund destination conditional and sends it with the verified cancellation payload', () => {
    const source = readFileSync(`${views}/ReservationLookup.vue`, 'utf8')
    expect(source).toContain('v-if="refundRequired" class="refund-destination"')
    expect(source).toContain('contactMethod: cancelForm.value.contactMethod')
    expect(source).toContain('refundBankName: cancelForm.value.refundBankName')
    expect(source).toContain(':disabled="!verifiedEmail"')
  })

  it('paginates the filtered admin request list and does not request a manual refund reference', () => {
    const source = readFileSync(`${views}/AdminCancellationRequests.vue`, 'utf8')
    expect(source).toContain('const pageSize = 10')
    expect(source).toContain('v-for="item in pagedRequests"')
    expect(source).toContain('watch([search, statusFilter], () => { page.value = 1 })')
    expect(source).toContain('Mã giao dịch sẽ được hệ thống tự tạo')
    expect(source).not.toContain('v-model.trim="providerReference"')
  })
})
