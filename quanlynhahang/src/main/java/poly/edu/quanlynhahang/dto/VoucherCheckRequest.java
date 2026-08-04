package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Bounded voucher code supplied by an authenticated customer. */
public record VoucherCheckRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String code) {
}
