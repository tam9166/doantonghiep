package poly.edu.quanlynhahang.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PurchaseSuggestionBatchRequest(@NotEmpty List<@Valid Item> items) {
    public record Item(
            @NotNull Long ingredientId,
            @NotNull @Valid PurchaseSuggestionApprovalRequest approval) {
    }
}
