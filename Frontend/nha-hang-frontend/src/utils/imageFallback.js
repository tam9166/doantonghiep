export const FOOD_IMAGE_FALLBACK = 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=600&q=80'
export const INGREDIENT_IMAGE_FALLBACK = 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=600&q=80'
export const POST_IMAGE_FALLBACK = 'https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=900&q=80'

export function foodImage(src) {
  return cleanImage(src) || FOOD_IMAGE_FALLBACK
}

export function ingredientImage(src) {
  return cleanImage(src) || INGREDIENT_IMAGE_FALLBACK
}

export function postImage(src) {
  return cleanImage(src) || POST_IMAGE_FALLBACK
}

export function replaceFoodImage(event) {
  replaceImage(event, FOOD_IMAGE_FALLBACK)
}

export function replaceIngredientImage(event) {
  replaceImage(event, INGREDIENT_IMAGE_FALLBACK)
}

export function replacePostImage(event) {
  replaceImage(event, POST_IMAGE_FALLBACK)
}

function cleanImage(src) {
  if (!src || typeof src !== 'string') return ''
  const value = src.trim()
  if (!value) return ''
  const lower = value.toLowerCase()
  if (
    lower.includes('placeholder') ||
    lower.includes('placehold.co') ||
    lower.includes('via.placeholder') ||
    lower.includes('example.com')
  ) {
    return ''
  }
  return value
}

function replaceImage(event, fallback) {
  const target = event?.target
  if (!target || target.dataset.fallbackApplied === 'true') return
  target.dataset.fallbackApplied = 'true'
  target.src = fallback
}
