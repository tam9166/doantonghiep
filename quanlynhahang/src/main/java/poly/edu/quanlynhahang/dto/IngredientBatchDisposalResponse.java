package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.util.Date;

import poly.edu.quanlynhahang.entity.IngredientBatchDisposal;

public record IngredientBatchDisposalResponse(
        Long id, Long ingredientId, String ingredientName, Long batchId, String lotCode,
        BigDecimal quantityDisposed, Date expiryDate, Date disposalDate, String reason,
        String confirmedBy) {
    public static IngredientBatchDisposalResponse from(IngredientBatchDisposal disposal) {
        return new IngredientBatchDisposalResponse(disposal.getId(), disposal.getIngredient().getId(),
                disposal.getIngredient().getName(), disposal.getBatch().getId(), disposal.getLotCode(),
                disposal.getQuantityDisposed(), disposal.getExpiryDate(), disposal.getDisposalDate(),
                disposal.getReason(), disposal.getConfirmedBy());
    }
}
