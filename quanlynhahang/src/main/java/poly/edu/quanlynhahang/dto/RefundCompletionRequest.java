package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Size;

public record RefundCompletionRequest(
        @Size(max = 120) String providerReference,
        @Size(max = 1000) String note) {
}
