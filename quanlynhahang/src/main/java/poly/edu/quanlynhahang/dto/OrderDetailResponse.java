package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import poly.edu.quanlynhahang.entity.OrderDetail;

public record OrderDetailResponse(Integer id, BigDecimal price, BigDecimal taxRate, BigDecimal taxAmount,
        Integer quantity, Integer status, String note, String allergyNote, Integer priority,
        Date queuedAt, Date startedAt, Date completedAt, Date cancelledAt, String cancelReason,
        OrderProductResponse product) {
    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(detail.getId(), money(detail.getPrice()), detail.getTaxRate(), money(detail.getTaxAmount()),
                detail.getQuantity(), detail.getStatus(), detail.getNote(), detail.getAllergyNote(), detail.getPriority(),
                detail.getQueuedAt(), detail.getStartedAt(), detail.getCompletedAt(), detail.getCancelledAt(),
                detail.getCancelReason(), OrderProductResponse.from(detail.getProduct()));
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
