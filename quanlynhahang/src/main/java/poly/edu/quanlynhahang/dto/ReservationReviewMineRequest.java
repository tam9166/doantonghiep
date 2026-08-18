package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationReviewMineRequest(
        @NotBlank @Size(max = 30) String reservationCode,
        @NotBlank @Size(max = 20) String customerPhone
) {}
