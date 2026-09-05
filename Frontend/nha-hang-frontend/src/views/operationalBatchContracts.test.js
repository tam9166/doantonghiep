import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const view = name => readFileSync(new URL(`./${name}`, import.meta.url), 'utf8')

describe('operational batch UI contracts', () => {
  it('keeps independent waiter and kitchen pagination states', () => {
    const waiter = view('Waiter.vue')
    const kitchen = view('Kitchen.vue')
    expect(waiter).toContain('readyCurrentPage')
    expect(waiter).toContain('cookingCurrentPage')
    expect(kitchen).toContain('workingCurrentPage')
    expect(kitchen).toContain('completedCurrentPage')
  })

  it('renders preorder timing and the preparation-window guard message', () => {
    for (const source of [view('Waiter.vue'), view('Kitchen.vue')]) {
      expect(source).toContain('Đơn đặt trước')
      expect(source).toContain('Khách đến:')
      expect(source).toContain('Chuẩn bị từ:')
      expect(source).toContain('Đơn đặt trước chưa đến thời gian chuẩn bị.')
      expect(source).not.toContain('getElapsedTime(order.createDate)')
    }
  })

  it('drives the kitchen queue from pending dish details instead of parent order status', () => {
    const kitchen = view('Kitchen.vue')
    expect(kitchen).toContain('const newPending = dedupedOrders.filter(hasKitchenWork)')
    expect(kitchen).toContain('const kitchenWorkDetails = order =>')
    expect(kitchen).toContain('v-for="detail in kitchenWorkDetails(order)"')
    expect(kitchen).not.toContain('dedupedOrders.filter(o => o.status === 5 || o.status === 1 || o.status === 6)')
  })

  it('keeps waiter action/state controls aligned in a responsive shared grid column', () => {
    const waiter = view('Waiter.vue')
    expect(waiter).toContain('class="btn-dish-served dish-operation"')
    expect(waiter).toContain('class="dish-operation cooking-state"')
    expect(waiter).toContain('grid-template-columns: 24px minmax(0, 1fr) auto minmax(92px, auto) minmax(82px, auto)')
    expect(waiter).toContain('@media (max-width: 600px)')
  })

  it('shows cashier reservation service data and preorder empty state', () => {
    const cashier = view('CashierView.vue')
    expect(cashier).toContain('/api/cashier/tables/${table.id}/reservation')
    expect(cashier).toContain('Ngày phục vụ')
    expect(cashier).toContain('Món đặt trước')
    expect(cashier).toContain('Chưa đặt món trước')
  })

  it('clamps kitchen notes and exposes a detail dialog only for long content', () => {
    const kitchen = view('Kitchen.vue')
    expect(kitchen).toContain('-webkit-line-clamp: 2')
    expect(kitchen).toContain('hasLongDishNote(detail)')
    expect(kitchen).toContain('Xem chi tiết')
    expect(kitchen).toContain('aria-modal="true"')
  })
})
