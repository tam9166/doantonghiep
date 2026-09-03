import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const source = name => readFileSync(new URL(`./${name}`, import.meta.url), 'utf8')

describe('invoice, inventory and admin UI regression contracts', () => {
  it('prints only the selected admin invoice through the isolated print utility', () => {
    const order = source('AdminOrder.vue')
    const staff = source('AdminStaff.vue')
    expect(order).toContain("printElement('admin-order-invoice'")
    expect(staff).toContain("printElement('admin-customer-invoice'")
    expect(order).not.toContain('const exportToPDF = () => { window.print()')
    expect(order).toMatch(/\.print-table th \{[^}]*color: #FFFFFF !important;/)
    expect(readFileSync(new URL('../utils/printElement.js', import.meta.url), 'utf8'))
      .toContain("frame.setAttribute('aria-hidden', 'true')")
  })

  it('requires actual purchase data and aggregates batch approval', () => {
    const purchase = source('AdminPurchaseSuggestion.vue')
    expect(purchase).toContain('Nhà cung cấp dùng chung *')
    expect(purchase).toContain('Đơn giá thực tế')
    expect(purchase).toContain('Đơn giá nhập trước đó')
    expect(purchase).toContain('priceDeltaText(row)')
    expect(purchase).toContain('Hạn sử dụng *')
    expect(purchase).toContain("'/api/admin/purchase-suggestions/approve-batch'")
    expect(purchase).not.toContain('for (const item of purchasable)')
    expect(purchase).not.toContain('message: JSON.stringify({')
    expect(purchase).toContain('requestId: requestId(`suggestion-${item.ingredientId}`)')
    expect(source('AdminIngredient.vue')).not.toContain('message: JSON.stringify(analysis)')
    expect(source('AdminIngredient.vue')).toContain('Duyệt & Nhập tất cả')
    expect(source('AdminIngredient.vue')).toContain('invoiceRequiresCompleteRows')
  })

  it('keeps admin navigation wrappers and removes the area gallery editor', () => {
    expect(source('AdminKitchenProposals.vue')).toContain('<AdminLayout>')
    expect(source('Staff.vue')).toContain(':is="profileLayout"')
    expect(source('AdminTableArea.vue')).not.toContain('Gallery (mỗi URL một dòng)')
  })

  it('uses a one-time temporary-password flow without exposing existing passwords', () => {
    const staff = source('AdminStaff.vue')
    expect(staff).toContain('{ generateTemporary: true }')
    expect(staff).toContain('Mật khẩu này sẽ không được hiển thị lại')
    expect(staff).toContain('reset-password-action')
    expect(staff).toContain('staff-locked-username')
    expect(staff).not.toContain('Mật Khẩu (Để trống nếu không đổi)')
  })
})
