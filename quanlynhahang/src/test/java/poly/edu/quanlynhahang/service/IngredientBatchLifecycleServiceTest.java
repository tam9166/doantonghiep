package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.IngredientBatchDisposal;
import poly.edu.quanlynhahang.entity.IngredientBatchStatus;
import poly.edu.quanlynhahang.repository.IngredientBatchDisposalRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;

@ExtendWith(MockitoExtension.class)
class IngredientBatchLifecycleServiceTest {
    @Mock IngredientBatchRepository batchRepository;
    @Mock IngredientBatchDisposalRepository disposalRepository;
    @Mock IngredientRepository ingredientRepository;
    @Mock MenuAvailabilityService menuAvailabilityService;
    @Mock ActivityLogService activityLogService;
    @InjectMocks IngredientBatchLifecycleService service;

    @Test
    void disposalKeepsBatchAndWritesLossAuditWhileValidStockRemainsUsable() {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(10L);
        ingredient.setName("Thịt bò");
        ingredient.setUnit("kg");
        IngredientBatch batch = new IngredientBatch();
        batch.setId(20L);
        batch.setIngredient(ingredient);
        batch.setQuantity(new BigDecimal("3.5000"));
        batch.setExpirationDate(new Date(System.currentTimeMillis() - 86_400_000L));
        batch.setStatus(IngredientBatchStatus.EXPIRED);

        when(batchRepository.findLockedById(20L)).thenReturn(Optional.of(batch));
        when(disposalRepository.save(any(IngredientBatchDisposal.class))).thenAnswer(invocation -> {
            IngredientBatchDisposal saved = invocation.getArgument(0);
            saved.setId(30L);
            return saved;
        });
        when(batchRepository.sumAvailableByIngredientId(10L)).thenReturn(new BigDecimal("7.0000"));

        var response = service.dispose(20L, "Quá hạn sử dụng");

        assertEquals(new BigDecimal("3.5000"), response.quantityDisposed());
        assertEquals(BigDecimal.ZERO, batch.getQuantity());
        assertEquals(IngredientBatchStatus.DISPOSED, batch.getStatus());
        assertEquals(new BigDecimal("7.0000"), ingredient.getQuantity());
        verify(batchRepository).save(batch);
        verify(menuAvailabilityService).refreshForIngredient(ingredient);
        verify(activityLogService).log("DISPOSE", "IngredientBatch", "20",
                "Tiêu hủy 3.5000 kg - Quá hạn sử dụng");
    }

    @Test
    void validBatchCannotBeDisposedThroughExpiredLossFlow() {
        IngredientBatch batch = new IngredientBatch();
        batch.setId(21L);
        batch.setExpirationDate(new Date(System.currentTimeMillis() + 86_400_000L));
        batch.setStatus(IngredientBatchStatus.AVAILABLE);
        when(batchRepository.findLockedById(21L)).thenReturn(Optional.of(batch));

        assertThrows(ResponseStatusException.class, () -> service.dispose(21L, "Không hợp lệ"));
    }
}
