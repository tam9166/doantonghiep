import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const source = readFileSync(fileURLToPath(new URL('./Waiter.vue', import.meta.url)), 'utf8')

describe('waiter dish lifecycle regression contract', () => {
  it('derives cooking and ready queues from OrderDetail.status', () => {
    expect(source).toContain('const isReadyDetail = detail => Number(detail?.status) === 1')
    expect(source).toContain('const isCookingDetail = detail =>')
    expect(source).toContain('const readyOrders = computed(() => orders.value.filter(hasReadyDetails))')
    expect(source).toContain('const cookingOrders = computed(() => orders.value.filter(order => cookingDetails(order).length > 0))')
    expect(source).toContain('v-if="isReadyDetail(detail)"')
    expect(source).toContain('v-if="isCookingDetail(detail)"')
  })

  it('keeps served and cancelled dishes out of the waiter queues', () => {
    expect(source).toContain("return status === 0 && !detail?.completedAt && !detail?.cancelledAt")
    expect(source).toContain("Number(detail?.status) === 1 && !detail?.cancelledAt")
    expect(source).toContain("const map = { 0: ' Đang làm', 1: ' Cần bưng', 2: ' Đã bưng', 3: ' Đã hủy' }")
  })

  it('refreshes after every dish lifecycle event without duplicating details', () => {
    for (const event of ['DISH_STARTED', 'DISH_READY', 'DISH_SERVED', 'DISH_CANCELLED']) {
      expect(source).toContain(event)
    }
    expect(source).toContain('mergeOrderDetails(existing.orderDetails, order.orderDetails)')
  })
})
