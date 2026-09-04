package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.RestaurantTable;

public record OrderResponse(Integer id, String orderCode, LocalDateTime scheduledAt, Date createDate, String address, Integer status,
        BigDecimal originalSubtotal, BigDecimal membershipDiscount, BigDecimal voucherDiscount,
        BigDecimal subTotal, BigDecimal taxAmount, BigDecimal totalAmount, BigDecimal deposit, Integer tableId,
        OrderType orderType, String tableName, Integer areaId, String areaName, String recipientName,
        String recipientPhone, String deliveryAddress, String deliveryNote,
        EmployeeSummaryResponse account, List<OrderDetailResponse> orderDetails, Boolean isPaid,
        OrderPaymentOption paymentOption, PaymentStatus paymentStatus, BigDecimal paidAmount,
        BigDecimal remainingAmount, Boolean invoiceRequested) {
    public static OrderResponse from(Order order) {
        List<OrderDetailResponse> details = order.getOrderDetails() == null ? List.of()
                : order.getOrderDetails().stream().map(OrderDetailResponse::from).toList();
        return from(order, details);
    }

    /** Kitchen receives only details that have not completed their kitchen lifecycle. */
    public static OrderResponse forKitchen(Order order) {
        List<OrderDetailResponse> details = order.getOrderDetails() == null ? List.of()
                : order.getOrderDetails().stream()
                .filter(detail -> detail.getStatus() == null || detail.getStatus() == 0)
                .map(OrderDetailResponse::from)
                .toList();
        return from(order, details);
    }

    private static OrderResponse from(Order order, List<OrderDetailResponse> details) {
        RestaurantTable table = order.getRestaurantTable();
        return new OrderResponse(order.getId(), order.getOrderCode(), order.getScheduledAt(), order.getCreateDate(), order.getAddress(), order.getStatus(),
                money(order.getOriginalSubtotal()), money(order.getMembershipDiscount()), money(order.getVoucherDiscount()),
                money(order.getSubTotal()), money(order.getTaxAmount()), money(order.getTotalAmount()),
                money(order.getDeposit()), order.getTableId(),
                order.getOrderType(), table == null ? null : table.getName(),
                table == null ? null : table.getAreaId(),
                table == null || table.getArea() == null ? null : table.getArea().getNameVi(),
                order.getRecipientName(), order.getRecipientPhone(), order.getDeliveryAddress(), order.getDeliveryNote(),
                order.getAccount() == null ? null : EmployeeSummaryResponse.from(order.getAccount()), details,
                order.getIsPaid(), order.getPaymentOption(), order.getPaymentStatus(), order.getPaidAmount(),
                order.getRemainingAmount(), Boolean.TRUE.equals(order.getInvoiceRequested()));
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
