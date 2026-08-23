package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancellationDecisionRequest(
        @NotBlank @Size(max = 1000) String note) {
}
