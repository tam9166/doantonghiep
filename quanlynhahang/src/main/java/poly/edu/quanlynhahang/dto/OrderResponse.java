package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;

public record OrderResponse(Integer id, Date createDate, String address, Integer status,
        Double subTotal, Double taxAmount, Double totalAmount, Double deposit, Integer tableId,
        EmployeeSummaryResponse account, List<OrderDetailResponse> orderDetails, Boolean isPaid,
        OrderPaymentOption paymentOption, PaymentStatus paymentStatus, BigDecimal paidAmount,
        BigDecimal remainingAmount) {
    public static OrderResponse from(Order order) {
        List<OrderDetailResponse> details = order.getOrderDetails() == null ? List.of()
                : order.getOrderDetails().stream().map(OrderDetailResponse::from).toList();
        return new OrderResponse(order.getId(), order.getCreateDate(), order.getAddress(), order.getStatus(),
                order.getSubTotal(), order.getTaxAmount(), order.getTotalAmount(), order.getDeposit(), order.getTableId(),
                order.getAccount() == null ? null : EmployeeSummaryResponse.from(order.getAccount()), details,
                order.getIsPaid(), order.getPaymentOption(), order.getPaymentStatus(), order.getPaidAmount(),
                order.getRemainingAmount());
    }
}
