package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuestBookingRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Pattern(regexp = "^[0-9+() -]{8,20}$") String phone,
        @NotBlank @Size(max = 50) String tableName,
        @NotBlank @Size(max = 100) String scheduledTime
) {
}
