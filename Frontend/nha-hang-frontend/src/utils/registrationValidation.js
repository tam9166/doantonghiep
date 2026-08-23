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

export function getRegistrationValidationError(form) {
  const normalized = normalizeRegistration(form)

  if (!normalized.username || !normalized.fullname || !normalized.email || !normalized.password) {
    return 'Vui lòng điền đầy đủ thông tin bắt buộc!'
  }
  if (normalized.username.length < 4 || normalized.username.length > 50) {
    return 'Tên đăng nhập phải có từ 4 đến 50 ký tự!'
  }
  if (!USERNAME_PATTERN.test(normalized.username)) {
    return 'Tên đăng nhập chỉ được chứa chữ, số, dấu chấm, gạch dưới hoặc gạch ngang!'
  }
  if (normalized.fullname.length > 100) {
    return 'Họ và tên không được vượt quá 100 ký tự!'
  }
  if (normalized.email.length > 100 || !EMAIL_PATTERN.test(normalized.email)) {
    return 'Email không đúng định dạng hoặc vượt quá 100 ký tự!'
  }
  if (normalized.password.length < 10 || normalized.password.length > 72) {
    return 'Mật khẩu phải có từ 10 đến 72 ký tự!'
  }
  if (COMMON_PASSWORDS.has(normalized.password.toLowerCase())) {
    return 'Mật khẩu quá phổ biến, vui lòng chọn mật khẩu khác!'
  }
  return ''
}
