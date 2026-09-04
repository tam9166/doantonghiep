import { describe, expect, it } from 'vitest'
import { createDeliveryCheckoutRequest } from './orderCheckout'

describe('order checkout contracts', () => {
  it('defaults a customer delivery checkout to QR transfer', () => {
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

  it('passes through an explicit cash delivery option', () => {
    expect(createDeliveryCheckoutRequest({
      recipientName: 'Nguyễn An',
      recipientPhone: '0901234567',
      deliveryAddress: '123 Main',
      deliveryNote: 'Gọi trước khi giao',
      paymentOption: 'COD',
      items: []
    })).toMatchObject({
      orderType: 'DELIVERY',
      paymentOption: 'COD'
    })
  })
})
