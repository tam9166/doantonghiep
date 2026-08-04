package poly.edu.quanlynhahang.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreorderItemResponse {
    private Long id;
    private Integer productId;
    private String productName;
    private String productImage;
    private String categoryName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String note;
    private BigDecimal lineTotal;
    private String status;
}
