import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const views = fileURLToPath(new URL('./', import.meta.url))

describe('route resource cleanup', () => {
  it('clears AdminOrder scheduled polling when the route unmounts', () => {
    const source = readFileSync(`${views}/AdminOrder.vue`, 'utf8')
    expect(source).toContain('onUnmounted(() =>')
    expect(source).toContain('window.clearInterval(scheduledActivationInterval)')
  })

  it('keeps persistent kitchen and waiter timers paired with cleanup', () => {
    for (const name of ['Kitchen.vue', 'Waiter.vue']) {
      const source = readFileSync(`${views}/${name}`, 'utf8')
      expect(source).toContain('onUnmounted(() =>')
      expect(source).toContain('clearInterval(timerInterval)')
    }
  })

  it('delegates a table move to the single atomic backend endpoint', () => {
    const source = readFileSync(`${views}/Waiter.vue`, 'utf8')
    expect(source).toContain('/api/admin/orders/${activeOrder.id}/table?newTableId=${newTable.id}')
    expect(source).not.toContain('/api/tables/${newTable.id}/status?status=2')
    expect(source).not.toContain('/api/tables/${movingTable.value.id}/status?status=0')
  })
})
