import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = relativePath => readFileSync(fileURLToPath(new URL(relativePath, import.meta.url)), 'utf8')

describe('final fix UI contracts', () => {
  it('keeps history readable as labelled mobile cards and checkout usable as a mobile sheet', () => {
    const history = read('./OrderHistory.vue')
    const menu = read('./ProductMenu.vue')
    expect(history).toContain('data-label=')
    expect(history).toContain('@media (max-width: 700px)')
    expect(history).toContain('.history-table td::before')
    expect(menu).toContain('checkout-modal')
    expect(menu).toContain('checkout-header')
    expect(menu).toContain('.checkout-actions')
    expect(menu).toContain('max-height: 100dvh')
  })

  it('does not let non-list API error payloads crash the menu computed state', () => {
    const menu = read('./ProductMenu.vue')
    expect(menu).toContain('if (!Array.isArray(response.data))')
    expect(menu).toContain('if (Array.isArray(response.data) && response.data.length > 0)')
    expect(menu).toContain('products.value.filter')
  })

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
    expect(products).toContain('grid-template-columns: 320px minmax(0, 1fr)')
    expect(products).toContain('min-width: 1200px')
    expect(products).toContain('const pageSize = 10')
    expect(products).toContain('v-for="p in pagedProducts"')
    expect(products).toContain('Giá bán tối thiểu:')
    expect(products).toContain('minimumSalePriceForCost')
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
    expect(knowledge).toContain('displayedSources')
    expect(knowledge).toContain('showAllFaqs')
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
    expect(purchases).not.toContain('>Đóng</button><button class="g-btn-primary"')
  })

  it('uses themed dialogs and toasts for inventory destructive actions', () => {
    const ingredients = read('./AdminIngredient.vue')
    expect(ingredients).toContain("import { useDialog } from '@/composables/useDialog'")
    expect(ingredients).toContain('await confirmDialog(')
    expect(ingredients).not.toMatch(/\b(?:alert|confirm)\s*\(/)
  })

  it('uses the shared prompt dialog for review moderation reasons', () => {
    const reviews = read('./AdminReservationReview.vue')
    expect(reviews).toContain("import { useDialog } from '@/composables/useDialog'")
    expect(reviews).toContain('await promptDialog(')
    expect(reviews).not.toMatch(/\bprompt\s*\(/)
    expect(reviews).not.toMatch(/\balert\s*\(/)
  })

  it('uses themed feedback and confirmation for voucher administration', () => {
    const vouchers = read('./AdminVoucher.vue')
    expect(vouchers).toContain("import { useDialog } from '@/composables/useDialog'")
    expect(vouchers).toContain('await confirmDialog(')
    expect(vouchers).not.toMatch(/\balert\s*\(/)
    expect(vouchers).not.toMatch(/\bconfirm\s*\(/)
  })

  it('uses shared dialogs for category, product and post administration', () => {
    for (const file of ['./AdminCategory.vue', './AdminProduct.vue', './AdminPost.vue']) {
      const source = read(file)
      expect(source).toContain('useDialog')
      expect(source).toContain('await confirmDialog(')
      expect(source).not.toMatch(/\balert\s*\(/)
      expect(source).not.toMatch(/\bconfirm\s*\(/)
    }
  })

  it('uses shared feedback for staff lifecycle, scheduling and zone actions', () => {
    const staff = read('./AdminStaff.vue')
    expect(staff).toContain("import { useDialog } from '@/composables/useDialog'")
    expect(staff).toContain('await confirmDialog(')
    expect(staff).not.toMatch(/\balert\s*\(/)
    expect(staff).not.toMatch(/\bconfirm\s*\(/)
  })

  it('uses shared feedback for area and purchase-suggestion actions', () => {
    for (const file of ['./AdminTableArea.vue', './AdminPurchaseSuggestion.vue']) {
      const source = read(file)
      expect(source).toContain('useDialog')
      expect(source).toContain('await confirmDialog(')
      expect(source).not.toMatch(/\balert\s*\(/)
      expect(source).not.toMatch(/\bconfirm\s*\(/)
    }
  })

  it('uses shared feedback for knowledge, deposit-policy and customer-history admin flows', () => {
    for (const file of ['./AdminAiKnowledge.vue', './AdminDepositPolicy.vue', './AdminCustomerHistory.vue']) {
      const source = read(file)
      expect(source).toContain('useToast')
      expect(source).not.toMatch(/\balert\s*\(/)
      expect(source).not.toMatch(/\bconfirm\s*\(/)
    }
    expect(read('./AdminAiKnowledge.vue')).toContain('await confirmDialog(')
    expect(read('./AdminDepositPolicy.vue')).toContain('await confirmDialog(')
  })

  it('uses non-blocking toasts for customer profile feedback', () => {
    const profile = read('./CustomerProfile.vue')
    expect(profile).toContain("import { useToast } from '@/composables/useToast'")
    expect(profile).not.toMatch(/\balert\s*\(/)
  })

  it('uses non-blocking toasts for menu and order-history feedback', () => {
    for (const file of ['./ProductMenu.vue', './OrderHistory.vue']) {
      const source = read(file)
      expect(source).toContain('useToast')
      expect(source).not.toMatch(/\balert\s*\(/)
    }
  })

  it('keeps customer home and router access feedback non-blocking', () => {
    for (const file of ['./Home.vue', '../router/index.js']) {
      const source = read(file)
      expect(source).toContain('useToast')
      expect(source).not.toMatch(/\balert\s*\(/)
    }
  })

  it('uses a shared dialog for staff logout', () => {
    const source = read('../components/AdminLayout.vue')
    expect(source).toContain('useDialog')
    expect(source).toContain('await confirmDialog(')
    expect(source).not.toMatch(/\bconfirm\s*\(/)
  })
})
