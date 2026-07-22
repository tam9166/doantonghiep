package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.OrderDetail;

public record OrderDetailResponse(Integer id, Double price, Double taxRate, Double taxAmount,
        Integer quantity, Integer status, OrderProductResponse product) {
    public static OrderDetailResponse from(OrderDetail detail) {
        return new OrderDetailResponse(detail.getId(), detail.getPrice(), detail.getTaxRate(), detail.getTaxAmount(),
                detail.getQuantity(), detail.getStatus(), OrderProductResponse.from(detail.getProduct()));
    }
}
