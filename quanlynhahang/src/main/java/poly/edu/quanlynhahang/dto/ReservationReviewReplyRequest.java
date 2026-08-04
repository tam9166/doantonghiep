package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReservationReviewReplyRequest(
        @NotBlank @Size(max = 2_000) String adminReply
) {
}
