package poly.edu.quanlynhahang.dto;

import java.util.List;

import lombok.Data;

@Data
public class OrderRequest {
    private String address;
    private List<OrderDetailRequest> items; // Danh sách các món ăn trong giỏ
}