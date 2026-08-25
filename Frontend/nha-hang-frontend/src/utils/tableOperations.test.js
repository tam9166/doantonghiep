import { describe, expect, it } from 'vitest'
import { findAwaitingPaymentOrder, groupTablesByFloorAndArea, isAwaitingPayment, tableIdentifier } from './tableOperations'

describe('shared Waiter/Cashier table operations', () => {
  it('keeps a legacy table order visible to both workspaces until financially paid', () => {
    const legacy = { id: 30, tableId: 3, orderType: null, status: 7, isPaid: false, paymentStatus: 'PENDING' }
    expect(isAwaitingPayment(legacy)).toBe(true)
    expect(findAwaitingPaymentOrder([legacy], 3)).toBe(legacy)
    expect(isAwaitingPayment({ ...legacy, isPaid: true, paymentStatus: 'PAID' })).toBe(false)
  })

  it('groups tables using database floor and area names', () => {
    const grouped = groupTablesByFloorAndArea([
      { id: 3, name: 'B03', floor: 'Tầng 1', areaName: 'Khu trong nhà' },
      { id: 4, name: 'VIP01', floor: 'Tầng 1', areaName: 'Phòng VIP' },
      { id: 8, name: 'B08', floor: 'Tầng 2', areaName: 'Sân vườn' },
    ])
    expect(grouped['Tầng 1']['Khu trong nhà'][0].name).toBe('B03')
    expect(grouped['Tầng 1']['Phòng VIP'][0].name).toBe('VIP01')
    expect(grouped['Tầng 2']['Sân vườn'][0].name).toBe('B08')
  })

  it('uses one business identifier without exposing the database id', () => {
    expect(tableIdentifier({ id: 3, name: 'B03' })).toBe('B03')
    expect(tableIdentifier({ id: 8, code: 'VIP01', name: 'Bàn nội bộ' })).toBe('VIP01')
    expect(tableIdentifier({ id: 15 })).toBe('Chưa đặt mã bàn')
  })
})
