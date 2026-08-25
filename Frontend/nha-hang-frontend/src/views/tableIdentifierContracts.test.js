import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'

const readView = name => readFileSync(new URL(`./${name}.vue`, import.meta.url), 'utf8')

describe('waiter and cashier table identifier contracts', () => {
  it('renders the shared identifier prominently on both cards', () => {
    const cashier = readView('CashierView')
    const waiter = readView('Waiter')

    expect(cashier).toContain('class="cashier-table-code" :title="tableIdentifier(table)"')
    expect(cashier).toContain('{{ tableIdentifier(table) }}')
    expect(waiter).toContain('class="table-code" :title="tableIdentifier(table)"')
    expect(waiter).toContain('{{ tableIdentifier(table) }}')
  })

  it('keeps the card identifier in cashier detail and waiter modal', () => {
    const cashier = readView('CashierView')
    const waiter = readView('Waiter')

    expect(cashier).toContain('Thanh toán — {{ selectedTableIdentifier }}')
    expect(cashier).toContain('selectedTableIdentifier.value = tableIdentifier(table)')
    expect(waiter).toContain('Chi Tiết — {{ tableIdentifier(detailTable) }}')
  })
})
