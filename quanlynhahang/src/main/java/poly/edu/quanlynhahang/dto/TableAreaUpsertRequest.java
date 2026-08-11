package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import poly.edu.quanlynhahang.entity.AreaType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TableAreaUpsertRequest(
        @NotBlank @Size(max = 150) String nameVi,
        @Size(max = 150) String nameEn,
        @Size(max = 500) String descriptionVi,
        @Size(max = 500) String descriptionEn,
        @Size(max = 500) String imageUrl,
        @DecimalMin("0") BigDecimal basePrice,
        @Min(0) @Max(1000) Integer capacity,
        @Size(max = 30) String status,
        AreaType areaType,
        @Min(1) @Max(10000) Integer minGuestCount,
        @Min(1) @Max(10000) Integer maxGuestCount,
        @Min(1) @Max(72) Integer minBookingHours,
        @DecimalMin("0") BigDecimal hourlyRate,
        @DecimalMin("0") BigDecimal packagePrice) {
}
