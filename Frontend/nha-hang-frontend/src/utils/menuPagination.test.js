import { describe, expect, it } from 'vitest'
import { MENU_PAGE_SIZE, paginateMenu } from './menuPagination'

describe('customer menu pagination', () => {
  it('paginates the already filtered result at 24 items per page', () => {
    const beerItems = Array.from({ length: 30 }, (_, index) => ({ id: index + 1, category: 'Bia' }))
    expect(MENU_PAGE_SIZE).toBe(24)
    expect(paginateMenu(beerItems, 1).items).toHaveLength(24)
    expect(paginateMenu(beerItems, 2).items.map(item => item.id)).toEqual([25, 26, 27, 28, 29, 30])
  })

  it('clamps a stale page after search/category filtering without touching cart state', () => {
    const cart = [{ productId: 99, quantity: 2 }]
    const result = paginateMenu([{ id: 1 }, { id: 2 }], 4)
    expect(result.page).toBe(1)
    expect(result.items).toHaveLength(2)
    expect(cart).toEqual([{ productId: 99, quantity: 2 }])
  })
})
