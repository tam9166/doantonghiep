package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import poly.edu.quanlynhahang.entity.CookingMethod;
import poly.edu.quanlynhahang.entity.DietType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductUpsertRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @DecimalMin("0") BigDecimal price,
        @DecimalMin("0") @DecimalMax("100") BigDecimal taxRate,
        @Size(max = 255) String image,
        @Size(max = 20_000) String description,
        Boolean status,
        Boolean available,
        DietType dietType,
        CookingMethod cookingMethod,
        @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(3) Integer spicyLevel,
        Boolean isSignatureDish,
        @NotNull @Valid CategoryReference category) {

    public record CategoryReference(@NotNull @Positive Integer id) {
    }
}
