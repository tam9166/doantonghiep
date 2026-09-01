package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Size;

public record KitchenProposalReviewRequest(@Size(max = 1000) String note) {
}
