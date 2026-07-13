import { describe, expect, it } from 'vitest'
import {
  FOOD_IMAGE_FALLBACK,
  foodImage,
  replaceFoodImage,
} from './imageFallback'

describe('imageFallback', () => {
  it('uses the food fallback for empty and placeholder URLs', () => {
    expect(foodImage('')).toBe(FOOD_IMAGE_FALLBACK)
    expect(foodImage('https://via.placeholder.com/150')).toBe(FOOD_IMAGE_FALLBACK)
  })

  it('keeps a valid image URL', () => {
    const imageUrl = 'https://cdn.example.org/food/pho.jpg'
    expect(foodImage(imageUrl)).toBe(imageUrl)
  })

  it('replaces a broken image only once', () => {
    const target = { src: 'broken.jpg', dataset: {} }
    replaceFoodImage({ target })

    expect(target.src).toBe(FOOD_IMAGE_FALLBACK)
    expect(target.dataset.fallbackApplied).toBe('true')

    target.src = 'second-broken.jpg'
    replaceFoodImage({ target })
    expect(target.src).toBe('second-broken.jpg')
  })
})
