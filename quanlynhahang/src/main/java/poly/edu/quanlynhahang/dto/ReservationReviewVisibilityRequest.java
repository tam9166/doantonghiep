package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReservationReviewVisibilityRequest(
        @NotNull Boolean hidden,
        @Size(max = 500) String hiddenReason
) {
}
