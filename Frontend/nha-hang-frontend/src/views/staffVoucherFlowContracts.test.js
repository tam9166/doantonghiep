import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = name => readFileSync(fileURLToPath(new URL(name, import.meta.url)), 'utf8')

describe('staff and voucher operational contracts', () => {
  it('renders schedule and attendance using the backend employee contract with visible states', () => {
    const staff = source('./AdminStaff.vue')
    expect(staff).toContain("sched.employee?.fullname")
    expect(staff).toContain("tk.employee?.fullname")
    expect(staff).not.toContain('sched.account.fullname')
    expect(staff).not.toContain('tk.account.fullname')
    expect(staff).toContain('scheduleError')
    expect(staff).toContain('timekeepingError')
    expect(staff).toContain('/api/admin/payroll?month=')
    expect(staff).not.toContain("st.role === 'ROLE_KITCHEN' ? 250000")
  })

  it('supports the complete voucher lifecycle instead of a one-use label', () => {
    const voucher = source('./AdminVoucher.vue')
    expect(voucher).toContain('Đã dùng / Giới hạn')
    expect(voucher).toContain('Không giới hạn')
    expect(voucher).toContain("ACTIVE: 'Đang hoạt động'")
    expect(voucher).toContain("PAUSED: 'Tạm dừng'")
    expect(voucher).toContain("EXHAUSTED: 'Hết lượt'")
    expect(voucher).toContain('/reset-usage')
    expect(voucher).toContain('/active?active=')
    expect(voucher).not.toContain("v.isUsed ? 'Đã dùng' : 'Chưa dùng'")
  })
})
