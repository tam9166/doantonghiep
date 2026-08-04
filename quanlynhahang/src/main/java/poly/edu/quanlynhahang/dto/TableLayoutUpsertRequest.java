package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Bounded payload accepted from the administration floor-plan editor. */
public record TableLayoutUpsertRequest(
        @NotNull @Positive Integer tableId,
        @Positive Integer areaId,
        @Size(max = 80) String floorName,
        @DecimalMin("0.00") @DecimalMax("10000.00") BigDecimal xPosition,
        @DecimalMin("0.00") @DecimalMax("10000.00") BigDecimal yPosition,
        @DecimalMin("40.00") @DecimalMax("1000.00") BigDecimal width,
        @DecimalMin("40.00") @DecimalMax("1000.00") BigDecimal height,
        @Size(max = 30) String shape,
        @DecimalMin("-360.00") @DecimalMax("360.00") BigDecimal rotation) {
}
