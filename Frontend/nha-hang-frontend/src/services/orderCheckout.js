export function createDeliveryCheckoutRequest({
  recipientName,
  recipientPhone,
  deliveryAddress,
  deliveryNote,
  paymentOption = 'PREPAID_TRANSFER',
  items
}) {
  return {
    recipientName,
    recipientPhone,
    deliveryAddress,
    deliveryNote: deliveryNote?.trim() || null,
    orderType: 'DELIVERY',
    paymentOption,
    items
  }
}
