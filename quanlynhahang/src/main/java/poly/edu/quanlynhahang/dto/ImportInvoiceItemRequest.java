package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import lombok.Data;
import java.util.Date;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class ImportInvoiceItemRequest {
    @NotNull
    private Long ingredientId;
    @NotNull
    @Positive
    private BigDecimal quantity;
    @DecimalMin("0.00")
    private BigDecimal unitPrice;
    private Date expirationDate;
    private Date importDate;
}
