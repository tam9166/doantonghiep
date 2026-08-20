package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ReservationReviewVisibilityRequest(
        @NotNull Boolean hidden,
        @Size(max = 500)
        @Pattern(regexp = "^[\\P{Cc}\\r\\n\\t]*$", message = "hiddenReason contains unsupported control characters")
        String hiddenReason
) {
}
