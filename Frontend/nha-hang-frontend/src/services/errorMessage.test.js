import { describe, expect, it } from 'vitest'
import { getApiErrorMessage } from './errorMessage'

describe('getApiErrorMessage', () => {
  it('reads the standardized API error and first field error', () => {
    const message = getApiErrorMessage({
      response: {
        data: {
          message: 'Dữ liệu không hợp lệ',
          fieldErrors: { password: 'kích thước phải từ 10 đến 72' },
        },
      },
    })

    expect(message).toContain('Dữ liệu không hợp lệ')
    expect(message).toContain('10')
  })

  it('keeps legacy string errors readable', () => {
    expect(getApiErrorMessage({ response: { data: 'Sai mật khẩu' } })).toBe('Sai mật khẩu')
  })
})
