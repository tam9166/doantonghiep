// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { api, externalApi } from './api'

function requestInterceptor() {
  return api.interceptors.request.handlers.find(handler => handler?.fulfilled)?.fulfilled
}

describe('internal API client', () => {
  afterEach(() => {
    localStorage.clear()
  })

  it('adds the bearer token to a relative backend request', async () => {
    localStorage.setItem('token', 'internal-token')

    const config = await requestInterceptor()({
      method: 'get',
      url: '/api/products',
      headers: {}
    })

    expect(config.headers.Authorization).toBe('Bearer internal-token')
  })

  it('never adds application credentials to an external absolute URL', async () => {
    localStorage.setItem('token', 'internal-token')

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
