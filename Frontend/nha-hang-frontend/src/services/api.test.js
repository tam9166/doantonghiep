// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { api, externalApi } from './api'

function requestInterceptor() {
  return api.interceptors.request.handlers.find(handler => handler?.fulfilled)?.fulfilled
}

describe('internal API client', () => {
  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('adds the bearer token to a relative backend request', async () => {
    sessionStorage.setItem('token', 'internal-token')

    const config = await requestInterceptor()({
      method: 'get',
      url: '/api/products',
      headers: {}
    })

    expect(config.headers.Authorization).toBe('Bearer internal-token')
  })

  it('uses the staff token for an operational backend request', async () => {
    sessionStorage.setItem('token', 'customer-token')
    sessionStorage.setItem('staff_token', 'staff-token')

    const config = await requestInterceptor()({
      method: 'get',
      url: '/api/admin/orders',
      headers: {}
    })

    expect(config.headers.Authorization).toBe('Bearer staff-token')
  })

  it('never falls back to a customer token for a staff request', async () => {
    sessionStorage.setItem('token', 'customer-token')

    const config = await requestInterceptor()({
      method: 'get',
      url: '/api/admin/orders',
      headers: {}
    })

    expect(config.headers.Authorization).toBeUndefined()
  })

  it('never adds application credentials to an external absolute URL', async () => {
    sessionStorage.setItem('token', 'internal-token')

    const config = await requestInterceptor()({
      method: 'get',
      url: 'https://api.open-meteo.com/v1/forecast',
      headers: {}
    })

    expect(config.headers.Authorization).toBeUndefined()
    expect(config.headers['X-Captcha-Token']).toBeUndefined()
    expect(externalApi.interceptors.request.handlers).toHaveLength(0)
  })
})
