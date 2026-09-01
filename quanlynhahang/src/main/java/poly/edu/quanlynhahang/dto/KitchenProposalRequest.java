package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KitchenProposalRequest(
        @NotBlank @Size(max = 20) String proposalType,
        @NotBlank @Size(max = 10000) String payload,
        @NotBlank @Size(max = 1000) String reason) {
}
