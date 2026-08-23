import { describe, expect, it } from 'vitest'
import { getRegistrationValidationError, normalizeRegistration } from './registrationValidation'

const validForm = {
  username: 'mocvi.user',
  password: 'MocVi-2026-Secure',
  fullname: 'Nguyễn Văn A',
  email: 'customer@example.com',
}

describe('registration validation contract', () => {
  it('normalizes the fields normalized by the backend before submission', () => {
    expect(normalizeRegistration({
      ...validForm,
      username: '  MocVi.User  ',
      fullname: '  Nguyễn Văn A  ',
      email: '  Customer@Example.COM  ',
    })).toEqual({
      ...validForm,
      username: 'mocvi.user',
      fullname: 'Nguyễn Văn A',
      email: 'customer@example.com',
    })
  })

  it('requires email and rejects malformed email before submit', () => {
    expect(getRegistrationValidationError({ ...validForm, email: '' })).toContain('bắt buộc')
    expect(getRegistrationValidationError({ ...validForm, email: 'not-an-email' })).toContain('Email')
  })

  it('matches backend username and password rules', () => {
    expect(getRegistrationValidationError({ ...validForm, username: 'bad user' })).toContain('Tên đăng nhập')
    expect(getRegistrationValidationError({ ...validForm, password: 'password123' })).toContain('quá phổ biến')
    expect(getRegistrationValidationError({ ...validForm, password: 'short' })).toContain('10 đến 72')
    expect(getRegistrationValidationError(validForm)).toBe('')
  })
})
