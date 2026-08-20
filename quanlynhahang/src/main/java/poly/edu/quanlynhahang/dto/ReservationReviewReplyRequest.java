package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record ReservationReviewReplyRequest(
        @NotBlank @Size(max = 1_000)
        @Pattern(regexp = "^[\\P{Cc}\\r\\n\\t]*$", message = "adminReply contains unsupported control characters")
        String adminReply
) {
}
