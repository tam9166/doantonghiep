package poly.edu.quanlynhahang.dto;

import lombok.Data;

@Data
public class OrderDetailRequest {
    private Integer productId;
    private Integer quantity;
}