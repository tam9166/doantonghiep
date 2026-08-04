import { describe, expect, it } from 'vitest'
import { captchaActionForRequest } from './captcha'

describe('captchaActionForRequest', () => {
  it('protects reservation review submissions with a dedicated action', () => {
    expect(captchaActionForRequest({
      method: 'post',
      baseURL: '/',
      url: '/api/reservation-reviews'
    })).toBe('reservation-review-create')
  })

  it('protects reservation waitlist submissions with a dedicated action', () => {
    expect(captchaActionForRequest({
      method: 'post',
      baseURL: '/',
      url: '/api/reservation-waitlist'
    })).toBe('reservation-waitlist-create')
  })
})
