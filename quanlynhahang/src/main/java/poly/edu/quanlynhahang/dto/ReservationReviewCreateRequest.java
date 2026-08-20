package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReservationReviewCreateRequest(
        @NotBlank @Size(max = 64) String reservationCode,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{8,20}$") String customerPhone,
        @Min(1) @Max(5) Integer overallRating,
        @Min(1) @Max(5) Integer foodRating,
        @Min(1) @Max(5) Integer serviceRating,
        @Min(1) @Max(5) Integer ambienceRating,
        @Min(1) @Max(5) Integer cleanlinessRating,
        @Size(max = 1_000)
        @Pattern(regexp = "^[\\P{Cc}\\r\\n\\t]*$", message = "content contains unsupported control characters")
        String content,
        @Pattern(regexp = "^https?://[^\\s]{1,480}$", message = "imageUrl must be an HTTP(S) URL") String imageUrl,
        Boolean anonymous
) {
}
