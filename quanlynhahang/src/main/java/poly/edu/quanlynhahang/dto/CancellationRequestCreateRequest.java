package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CancellationRequestCreateRequest(
        @Size(max = 30) String reservationCode,
        @Size(max = 150) String customerName,
        @Size(max = 30) String customerPhone,
        @Email @Size(max = 150) String customerEmail,
        @Size(max = 1000) String reason,
        @Size(max = 20) String contactMethod,
        @Size(max = 120) String refundBankName,
        @Size(max = 40) String refundAccountNumber,
        @Size(max = 150) String refundAccountHolder) {

    /** Backward-compatible constructor for existing callers that only verify a booking. */
    public CancellationRequestCreateRequest(String reservationCode, String customerName,
            String customerPhone, String customerEmail, String reason) {
        this(reservationCode, customerName, customerPhone, customerEmail, reason,
                null, null, null, null);
    }
}
