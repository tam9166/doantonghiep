package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import lombok.Data;
import java.util.Date;

@Data
public class ImportInvoiceItemRequest {
    private Long ingredientId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private Date expirationDate;
}
