import { describe, expect, it } from 'vitest'
import { captchaActionForRequest } from './captcha'

describe('captcha action mapping', () => {
  it('maps protected auth endpoints', () => {
    expect(captchaActionForRequest({ method: 'post', url: '/api/auth/login' })).toBe('auth')
    expect(captchaActionForRequest({ method: 'post', url: '/api/auth/staff/login' })).toBe('auth')
    expect(captchaActionForRequest({ method: 'post', url: '/api/auth/signup' })).toBe('auth')
  })

  it('maps public write endpoints protected by backend CAPTCHA', () => {
    expect(captchaActionForRequest({ method: 'post', url: '/api/reservations' })).toBe('reservation-create')
    expect(captchaActionForRequest({ method: 'post', url: '/api/applications/upload' })).toBe('application-upload')
    expect(captchaActionForRequest({ method: 'post', url: '/api/chatbot/chat' })).toBe('chatbot')
  })

  it('does not require CAPTCHA for reservation quote calls', () => {
    expect(captchaActionForRequest({ method: 'post', url: '/api/reservations/quote' })).toBeNull()
  })
})
