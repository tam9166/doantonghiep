import { describe, expect, it } from 'vitest'
import { MENU_PAGE_SIZE, paginateMenu } from './menuPagination'

describe('customer menu pagination', () => {
  it('paginates the already filtered result at 12 items per page', () => {
    const beerItems = Array.from({ length: 30 }, (_, index) => ({ id: index + 1, category: 'Bia' }))
    expect(MENU_PAGE_SIZE).toBe(12)
    expect(paginateMenu(beerItems, 1).items).toHaveLength(12)
    expect(paginateMenu(beerItems, 2).items.map(item => item.id)).toEqual([13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24])
  })

  it('clamps a stale page after search/category filtering without touching cart state', () => {
    const cart = [{ productId: 99, quantity: 2 }]
    const result = paginateMenu([{ id: 1 }, { id: 2 }], 4)
    expect(result.page).toBe(1)
    expect(result.items).toHaveLength(2)
    expect(cart).toEqual([{ productId: 99, quantity: 2 }])
  })
})
