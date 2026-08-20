package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IngredientBatchCreateRequest(
        @NotNull @Positive BigDecimal quantity,
        @DecimalMin("0.00") BigDecimal unitPrice,
        Date expirationDate) {
}
