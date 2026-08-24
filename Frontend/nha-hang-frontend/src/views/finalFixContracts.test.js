import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = relativePath => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8')

describe('final fix UI contracts', () => {
  it('uses readable pastel toast colors and shared SVG icons', () => {
    for (const file of ['../components/ToastGlobal.vue', '../components/ToastNotification.vue']) {
      const source = read(file)
      expect(source).toContain('#FFF1F2')
      expect(source).toContain('#991B1B')
      expect(source).toContain('#F0FDF4')
      expect(source).toContain('<UiIcon')
      expect(source).not.toMatch(/\.toast-(?:success|error|warning)[^}]*color:\s*#fff/is)
    }
  })

  it('bridges legacy browser alerts to non-blocking application toasts', () => {
    const source = read('../main.js')
    expect(source).toContain('window.alert = message => toast.info')
  })

  it('keeps product and category management layouts usable on desktop', () => {
    const products = read('./AdminProduct.vue')
    const categories = read('./AdminCategory.vue')
    expect(products).toContain('minmax(280px, 30%) minmax(0, 70%)')
    expect(products).toContain('min-width: 1200px')
    expect(categories).toContain('const pageSize = 20')
    expect(categories).toContain('pagedCategories.value.slice(0, 10)')
    expect(categories).toContain('currentPage.value - 2')
  })

  it('renders compact AI sources, table heatmap data and CRM summary safely', () => {
    const knowledge = read('./AdminAiKnowledge.vue')
    const tables = read('./AdminTable.vue')
    const staff = read('./AdminStaff.vue')
    expect(knowledge).toContain('slice(0, 140)')
    expect(knowledge).toContain('viewingSource')
    expect(tables).toContain('/api/admin/orders?limit=500')
    expect(tables).toContain('Chưa đủ dữ liệu phân tích')
    expect(staff).toContain('customerTotalSpend')
    expect(staff).toContain('(selectedCustomerOrder.orderDetails || [])')
  })

  it('keeps normal areas free and exposes private-room pricing fields', () => {
    const source = read('./AdminTableArea.vue')
    expect(source).toContain('PRIVATE_ROOM')
    expect(source).toContain('roomFee')
    expect(source).toContain('minimumSpend')
    expect(source).toContain('basePrice: 0')
  })

  it('uses icon-only ingredient actions and one canonical inventory analysis source', () => {
    const ingredients = read('./AdminIngredient.vue')
    const purchases = read('./AdminPurchaseSuggestion.vue')
    expect(ingredients).toContain('class="ingredient-action-btn action-history"')
    expect(ingredients).toContain('title="Xem lịch sử nhập kho"')
    expect(ingredients).toContain('title="Chỉnh sửa nguyên liệu"')
    expect(ingredients).toContain('title="Xóa nguyên liệu"')
    expect(ingredients).not.toContain('<UiIcon name="history" /> Lịch Sử Lô')
    expect(ingredients).toContain('/api/admin/ingredients/analysis?expiringDays=3')
    expect(ingredients).toContain('Không nhập thêm')
    expect(purchases).toContain('summary.expiredBatchesCount')
    expect(purchases).toContain('item.action')
    expect(purchases).toContain('Không nhập · Cần xử lý')
  })
})
