package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WaitlistLookupRequest(
        @NotBlank @Size(max = 30) String waitlistCode,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{8,20}$") String customerPhone) {
}
