import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = name => readFileSync(fileURLToPath(new URL(name, import.meta.url)), 'utf8')

describe('second UI and business refinement contracts', () => {
  it('shows the latest exact ingredient import unit price without prefilling the new batch price', () => {
    const ingredients = source('./AdminIngredient.vue')
    expect(ingredients).toContain('Đơn giá nhập gần nhất')
    expect(ingredients).toContain('latestImportPriceText')
    expect(ingredients).toContain('loadLatestImportPrice')
    expect(ingredients).toContain("`/api/admin/ingredients/${ingredientId}/batches`")
    expect(ingredients).toContain('Chưa có dữ liệu')
    expect(ingredients).not.toMatch(/batchForm\.value\.unitPrice\s*=\s*latestImportPrice/)
  })

  it('keeps ingredient pagination, recipe search and import history detail controls explicit', () => {
    const ingredients = source('./AdminIngredient.vue')
    expect(ingredients).toContain('const ingredientPageSize = 7')
    expect(ingredients).toContain('v-for="ing in pagedIngredients"')
    expect(ingredients).toContain('recipeIngredientSearch')
    expect(ingredients).toContain('availableRecipeIngredients')
    expect(ingredients).toContain('usedIds.has')
    expect(ingredients).toContain('class="btn-sm btn-secondary invoice-detail-btn"')
  })

  it('keeps staff/customer edit modals readable and password reset one-time only', () => {
    const staff = source('./AdminStaff.vue')
    expect(staff).toContain('.staff-edit-modal input.g-form-control.staff-locked-username')
    expect(staff).toContain('-webkit-text-fill-color: #FFFFFF')
    expect(staff).toContain('customer-edit-modal')
    expect(staff).toContain('customer-edit-header')
    expect(staff).toContain('customer-edit-footer')
    expect(staff).toContain('temporaryPassword')
    expect(staff).not.toContain('currentPassword')
  })

  it('separates reservation form and summary, paginates areas/preorders, and preserves selected state', () => {
    const reservation = source('./Reservation.vue')
    expect(reservation).toContain('class="booking-layout"')
    expect(reservation).toContain('<form class="reservation-card"')
    expect(reservation).toContain('<aside class="booking-summary"')
    expect(reservation).not.toContain('class="quick-summary"')
    expect(reservation).toContain('const AREA_PAGE_SIZE = 3')
    expect(reservation).toContain('v-for="area in pagedAreas"')
    expect(reservation).toContain('const PREORDER_PAGE_SIZE = 10')
    expect(reservation).toContain('v-for="dish in pagedPreorderMenu"')
    expect(reservation).toContain('cartItems.value')
  })

  it('keeps dine-in full menu compact and sorts available dishes before unavailable dishes', () => {
    const dineIn = source('./DineInOrder.vue')
    expect(dineIn).toContain('const FULL_MENU_PAGE_SIZE = 10')
    expect(dineIn).toContain('v-for="product in pagedFullMenuProducts"')
    expect(dineIn).toContain('sortedFullMenuProducts')
    expect(dineIn).toContain('aAvailable - bAvailable')
    expect(dineIn).toContain('class="menu-pagination"')
  })

  it('shows booking readiness diagnostics to admins and keeps table admin on the admin area endpoint', () => {
    const areas = source('./AdminTableArea.vue')
    const tables = source('./AdminTable.vue')
    expect(areas).toContain('bookingReady')
    expect(areas).toContain('bookingReadyReason')
    expect(areas).toContain('Bàn hoạt động:')
    expect(areas).toContain('Tổng chỗ bàn:')
    expect(tables).toContain("/api/areas/admin")
  })
})
