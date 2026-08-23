export function createDeliveryCheckoutRequest({ recipientName, recipientPhone, deliveryAddress, deliveryNote, items }) {
  return {
    recipientName,
    recipientPhone,
    deliveryAddress,
    deliveryNote: deliveryNote?.trim() || null,
    orderType: 'DELIVERY',
    paymentOption: 'PREPAID_TRANSFER',
    items
  }
}
