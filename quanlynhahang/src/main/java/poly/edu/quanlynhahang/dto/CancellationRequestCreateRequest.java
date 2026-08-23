package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CancellationRequestCreateRequest(
        @Size(max = 30) String reservationCode,
        @Size(max = 150) String customerName,
        @Size(max = 30) String customerPhone,
        @Email @Size(max = 150) String customerEmail,
        @Size(max = 1000) String reason) {
}
