package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.util.Date;

import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.IngredientBatchStatus;

public record IngredientBatchResponse(Long id, BigDecimal quantity, Date importDate, Date expirationDate,
                                      BigDecimal unitPrice, Long ingredientId, IngredientBatchStatus status) {
    public static IngredientBatchResponse from(IngredientBatch batch) {
        return new IngredientBatchResponse(batch.getId(), batch.getQuantity(), batch.getImportDate(),
                batch.getExpirationDate(), batch.getUnitPrice(),
                batch.getIngredient() == null ? null : batch.getIngredient().getId(), batch.getStatus());
    }
}
