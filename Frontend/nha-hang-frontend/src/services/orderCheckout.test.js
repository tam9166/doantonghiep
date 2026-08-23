import { describe, expect, it } from 'vitest'
import { createDeliveryCheckoutRequest } from './orderCheckout'

describe('order checkout contracts', () => {
  it('always identifies a customer delivery checkout explicitly', () => {
    expect(createDeliveryCheckoutRequest({
      recipientName: 'Nguyễn An',
      recipientPhone: '0901234567',
      deliveryAddress: '123 Main',
      deliveryNote: 'Gọi trước khi giao',
      items: []
    })).toMatchObject({
      recipientName: 'Nguyễn An',
      recipientPhone: '0901234567',
      deliveryAddress: '123 Main',
      deliveryNote: 'Gọi trước khi giao',
      orderType: 'DELIVERY',
      paymentOption: 'PREPAID_TRANSFER'
    })
  })
})
