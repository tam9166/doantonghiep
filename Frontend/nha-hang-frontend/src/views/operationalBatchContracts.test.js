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

  it('renders future-service badge and exact friendly guard message', () => {
    for (const source of [view('Waiter.vue'), view('Kitchen.vue')]) {
      expect(source).toContain('Chờ đến ngày')
      expect(source).toContain('Đơn này chưa đến ngày phục vụ.')
    }
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
