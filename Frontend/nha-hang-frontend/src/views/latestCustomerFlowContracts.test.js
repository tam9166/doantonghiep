import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const read = (name) => readFileSync(new URL(name, import.meta.url), 'utf8')

describe('latest customer flow contracts', () => {
  it('keeps the customer history modal high contrast and fully opaque', () => {
    const source = read('./AdminStaff.vue')
    expect(source).toContain('customer-history-overlay')
    expect(source).toContain('customer-history-table')
    expect(source).toContain('color: var(--text-primary, #2B171A)')
    expect(source).not.toContain('style="color: gold; font-weight: bold;"')
  })

  it('sends stable idempotency keys for delivery and direct dine-in checkout', () => {
    const menu = read('./ProductMenu.vue')
    const dineIn = read('./DineInOrder.vue')
    expect(menu).toContain("'X-Idempotency-Key': checkoutIdempotencyKey.value")
    expect(dineIn).toContain("'X-Idempotency-Key': checkoutIdempotencyKey.value")
    expect(dineIn).not.toContain('Đơn tại bàn phải được tạo từ mã QR hợp lệ')
  })

  it('collects party size, favorite groups, palate and allergies before suggesting real items', () => {
    const dineIn = read('./DineInOrder.vue')
    expect(dineIn).toContain('favoriteOptions')
    expect(dineIn).toContain('palateOptions')
    expect(dineIn).toContain('allergyInput')
    expect(dineIn).toContain("'/api/customer/ai/menu-suggestion'")
    expect(dineIn).toContain('item.availableQuantity > 0')
  })

  it('renders six desktop columns and paginates the filtered list', () => {
    const menu = read('./ProductMenu.vue')
    expect(menu).toContain('v-for="product in paginatedProducts"')
    expect(menu).toContain('repeat(6, minmax(0, 1fr))')
    expect(menu).toContain("t('menu.remaining', { count: product.availableQuantity })")
    expect(menu).toContain('class="menu-pagination"')
  })
})
