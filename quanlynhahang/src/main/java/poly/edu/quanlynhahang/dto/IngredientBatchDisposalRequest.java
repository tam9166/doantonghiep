package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngredientBatchDisposalRequest(
        @NotBlank @Size(max = 500) String reason) {
}
