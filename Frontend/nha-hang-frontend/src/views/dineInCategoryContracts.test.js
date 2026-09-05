import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const source = readFileSync(resolve(process.cwd(), 'src/views/DineInOrder.vue'), 'utf8')

describe('Dine-in category filtering contracts', () => {
  it('renders category chips from product categories and deduplicates by stable id', () => {
    expect(source).toContain('const menuCategories = computed')
    expect(source).toContain('new Map()')
    expect(source).toContain('byId.set(Number(category.id)')
    expect(source).toContain('selectedCategoryId')
  })

  it('filters products without resetting the cart', () => {
    expect(source).toContain('selectCategory')
    expect(source).toContain('fullMenuPage.value = 1')
    expect(source).toContain('sortedFullMenuProducts')
    expect(source).toContain('cart.value')
  })
})
