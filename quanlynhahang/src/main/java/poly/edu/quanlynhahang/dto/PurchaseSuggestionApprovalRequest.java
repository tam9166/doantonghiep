package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PurchaseSuggestionApprovalRequest(
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitPrice,
        @NotNull @Future Date expirationDate,
        @NotBlank @Size(max = 255) String supplier,
        @Size(max = 500) String note,
        @NotBlank @Size(max = 64) String requestId) {
}
