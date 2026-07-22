package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import poly.edu.quanlynhahang.entity.OrderDetail;

public record OrderDetailResponse(Integer id, BigDecimal price, Double taxRate, BigDecimal taxAmount,
        Integer quantity, Integer status, OrderProductResponse product) {
    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(detail.getId(), money(detail.getPrice()), detail.getTaxRate(), money(detail.getTaxAmount()),
                detail.getQuantity(), detail.getStatus(), OrderProductResponse.from(detail.getProduct()));
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
