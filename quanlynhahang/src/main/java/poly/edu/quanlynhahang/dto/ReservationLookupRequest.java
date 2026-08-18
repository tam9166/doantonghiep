package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReservationLookupRequest(
        @NotBlank @Size(min = 1, max = 30) String reservationCode,
        @NotBlank @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ") String customerPhone
) {}
