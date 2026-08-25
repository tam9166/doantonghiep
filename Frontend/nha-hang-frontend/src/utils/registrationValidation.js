const COMMON_PASSWORDS = new Set([
  '1234567890',
  'password123',
  'qwerty12345',
  'admin12345',
  'restaurant123',
  'nhahang123',
  'matkhau123',
])

const USERNAME_PATTERN = /^[a-zA-Z0-9._-]+$/
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function normalizeRegistration(form) {
  return {
    username: String(form.username ?? '').trim().toLowerCase(),
    password: String(form.password ?? ''),
    fullname: String(form.fullname ?? '').trim(),
    email: String(form.email ?? '').trim().toLowerCase(),
  }
}

const DEFAULT_VALIDATION_MESSAGES = {
  required: 'Vui lòng điền đầy đủ thông tin bắt buộc!',
  usernameLength: 'Tên đăng nhập phải có từ 4 đến 50 ký tự!',
  usernamePattern: 'Tên đăng nhập chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang!',
  fullNameLength: 'Họ và tên không được vượt quá 100 ký tự!',
  email: 'Email không đúng định dạng hoặc vượt quá 100 ký tự!',
  passwordLength: 'Mật khẩu phải có từ 10 đến 72 ký tự!',
  commonPassword: 'Mật khẩu quá phổ biến, vui lòng chọn mật khẩu khác!',
}

export function getRegistrationValidationError(form, translate = key => DEFAULT_VALIDATION_MESSAGES[key]) {
  const normalized = normalizeRegistration(form)

  if (!normalized.username || !normalized.fullname || !normalized.email || !normalized.password) {
    return translate('required')
  }
  if (normalized.username.length < 4 || normalized.username.length > 50) {
    return translate('usernameLength')
  }
  if (!USERNAME_PATTERN.test(normalized.username)) {
    return translate('usernamePattern')
  }
  if (normalized.fullname.length > 100) {
    return translate('fullNameLength')
  }
  if (normalized.email.length > 100 || !EMAIL_PATTERN.test(normalized.email)) {
    return translate('email')
  }
  if (normalized.password.length < 10 || normalized.password.length > 72) {
    return translate('passwordLength')
  }
  if (COMMON_PASSWORDS.has(normalized.password.toLowerCase())) {
    return translate('commonPassword')
  }
  return ''
}
