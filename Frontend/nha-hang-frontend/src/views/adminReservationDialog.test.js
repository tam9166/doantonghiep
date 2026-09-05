import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const source = readFileSync(
  fileURLToPath(new URL('./AdminReservation.vue', import.meta.url)),
  'utf8',
)

describe('admin reservation workflow dialogs', () => {
  it('uses the shared dialog and promotes waitlist records into the table-assignment lifecycle', () => {
    expect(source).toContain("import { useDialog } from '@/composables/useDialog'")
    expect(source).toContain('const { promptDialog } = useDialog()')
    expect(source).not.toMatch(/window\.(prompt|confirm)\s*\(/)
    expect(source).not.toContain('<section class="waitlist-panel">')
    expect(source).toContain("reservationStatus: 'WAITING_TABLE_ASSIGNMENT'")
    expect(source).toContain('const displayReservations = computed(() =>')
    expect(source).toContain("/api/admin/reservation-waitlist/${item.waitlistId}/promote")
  })

  it('loads global status totals and renders centered, themed reservation pagination', () => {
    expect(source).toContain("api.get('/api/admin/reservations/stats')")
    expect(source).toContain("status === 'WAITING_TABLE_ASSIGNMENT' ? activeWaitlist.value.length : 0")
    expect(source).toContain('await Promise.all([fetchReservations(), fetchStatusCounts(), fetchWaitlist()])')
    expect(source).toContain('const reservationPageNumbers = computed(() =>')
    expect(source).toContain('class="pagination-nav"')
    expect(source).toContain('class="pagination-page"')
    expect(source).toContain('grid-template-columns: minmax(92px, 1fr) auto minmax(92px, 1fr)')
    expect(source).toContain('.pagination-page.active')
  })

  it('disables early check-in using the shared sixty-minute availability rule', () => {
    expect(source).toContain("import { checkInAvailability } from '@/utils/reservationCheckIn'")
    expect(source).toContain(':disabled="!checkInState(item).allowed"')
    expect(source).toContain('class="check-in-hint"')
    expect(source).toContain('if (!checkInState(item).allowed) return')
  })
})
