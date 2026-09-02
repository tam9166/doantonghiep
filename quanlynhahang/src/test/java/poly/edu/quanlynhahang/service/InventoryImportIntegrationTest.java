package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.dto.ImportInvoiceItemRequest;
import poly.edu.quanlynhahang.dto.ImportInvoiceRequest;
import poly.edu.quanlynhahang.entity.ImportInvoice;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;

@SpringBootTest
@Transactional
class InventoryImportIntegrationTest {

    @Autowired InventoryImportService inventoryImportService;
    @Autowired IngredientRepository ingredientRepository;
    @Autowired IngredientBatchRepository batchRepository;

    @Test
    void createsDetailInReleasedFlywayTableAndTreatsRepeatedSourceRequestAsIdempotent() {
        Ingredient ingredient = ingredientRepository.findAll().stream().findFirst().orElseThrow();
        ImportInvoiceItemRequest item = new ImportInvoiceItemRequest();
        item.setIngredientId(ingredient.getId());
        item.setQuantity(new BigDecimal("10.000"));
        item.setUnitPrice(new BigDecimal("25000.00"));
        item.setExpirationDate(Date.from(Instant.now().plus(20, ChronoUnit.DAYS)));

        ImportInvoiceRequest request = new ImportInvoiceRequest();
        request.setSupplier("Nhà cung cấp kiểm thử");
        request.setNote("Regression mapping ImportInvoiceDetails");
        request.setSourceRequestId("inventory-import-mapping-regression");
        request.setItems(List.of(item));

        ImportInvoice first = inventoryImportService.create(request);
        ImportInvoice duplicate = inventoryImportService.create(request);

        assertNotNull(first.getId());
        assertEquals(first.getId(), duplicate.getId());
        long createdBatches = batchRepository.findAll().stream()
                .filter(batch -> batch.getImportInvoice() != null
                        && first.getId().equals(batch.getImportInvoice().getId()))
                .count();
        assertEquals(1L, createdBatches);
    }
}
