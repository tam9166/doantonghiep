package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import poly.edu.quanlynhahang.dto.ImportInvoiceRequest;
import poly.edu.quanlynhahang.dto.PurchaseSuggestionApprovalRequest;
import poly.edu.quanlynhahang.entity.ImportInvoice;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.service.InventoryAlertService;
import poly.edu.quanlynhahang.service.InventoryImportService;

class PurchaseSuggestionControllerTest {

    @Test
    void approvalCarriesRealSupplierPriceExpiryAndIdempotencyKeyIntoImport() {
        IngredientRepository ingredients = mock(IngredientRepository.class);
        IngredientBatchRepository batches = mock(IngredientBatchRepository.class);
        InventoryImportService imports = mock(InventoryImportService.class);
        Ingredient ingredient = new Ingredient();
        ingredient.setId(7L);
        ingredient.setName("Bánh phở");
        ingredient.setUnit("kg");
        ImportInvoice invoice = new ImportInvoice();
        invoice.setId(91L);
        when(ingredients.findById(7L)).thenReturn(Optional.of(ingredient));
        when(imports.create(any())).thenReturn(invoice);
        when(batches.sumAvailableByIngredientId(7L)).thenReturn(new BigDecimal("14.5"));
        PurchaseSuggestionController controller = new PurchaseSuggestionController(
                ingredients, batches, mock(InventoryAlertService.class), imports);
        Date expiry = Date.from(Instant.now().plus(20, ChronoUnit.DAYS));

        controller.approveSuggestion(7L, new PurchaseSuggestionApprovalRequest(
                new BigDecimal("10"), new BigDecimal("25000"), expiry,
                "Nhà cung cấp A", "Phiếu tuần", "req-7-001"));

        ArgumentCaptor<ImportInvoiceRequest> captor = ArgumentCaptor.forClass(ImportInvoiceRequest.class);
        verify(imports).create(captor.capture());
        ImportInvoiceRequest request = captor.getValue();
        assertEquals("Nhà cung cấp A", request.getSupplier());
        assertEquals("req-7-001", request.getSourceRequestId());
        assertEquals(new BigDecimal("25000"), request.getItems().getFirst().getUnitPrice());
        assertEquals(expiry, request.getItems().getFirst().getExpirationDate());
    }
}
