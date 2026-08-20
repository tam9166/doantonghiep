package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredientUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String unit,
        @DecimalMin("0.0") BigDecimal minStock,
        @DecimalMin("0.00") BigDecimal unitPrice,
        @Size(max = 500) String image,
        @Min(1) @Max(3650) Integer shelfLifeDays) {
}
