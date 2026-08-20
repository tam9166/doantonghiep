package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;

class StaffOperationsAssistantServiceTest {
    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final IngredientBatchRepository ingredientBatchRepository = mock(IngredientBatchRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StaffOperationsAssistantService service = new StaffOperationsAssistantService(
            ingredientRepository, ingredientBatchRepository, productRepository);

    @Test
    void reportsAvailableProductsWithoutTrustingClientSuppliedMenu() {
        Product available = new Product();
        available.setName("Cơm rang");
        available.setStatus(true);
        available.setAvailable(true);
        Product unavailable = new Product();
        unavailable.setName("Lẩu hết hàng");
        unavailable.setStatus(true);
        unavailable.setAvailable(false);
        when(productRepository.findAll()).thenReturn(List.of(available, unavailable));

        String reply = service.answer("Món nào khả dụng?");

        assertTrue(reply.contains("Cơm rang"));
        assertTrue(!reply.contains("Lẩu hết hàng"));
    }

    @Test
    void reportsLowStockFromRepositoryQuantity() {
        Ingredient ingredient = ingredient("Rau cải", 2D, 5D, 3);
        when(ingredientRepository.findAll()).thenReturn(List.of());
        when(ingredientRepository.findLowStockIngredients()).thenReturn(List.of(ingredient));

        String reply = service.answer("Số lượng tồn kho");

        assertTrue(reply.contains("Rau cải"));
        assertTrue(reply.contains("2.00"));
    }

    @Test
    void reportsExpiryFromIngredientBatchesInsteadOfConfiguredShelfLifeOnly() {
        Ingredient ingredient = ingredient("Thịt bò", 8D, 5D, 2);
        IngredientBatch batch = new IngredientBatch();
        batch.setIngredient(ingredient);
        batch.setQuantity(new BigDecimal("3.0000"));
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.AUGUST, 5, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        batch.setExpirationDate(calendar.getTime());
        when(ingredientBatchRepository.findExpiringBatches(org.mockito.ArgumentMatchers.any(Date.class)))
                .thenReturn(List.of(batch));

        String reply = service.answer("Hạn sử dụng thịt bò");

        assertTrue(reply.contains("Thịt bò"));
        assertTrue(reply.contains("05/08/2026"));
        assertTrue(reply.contains("3.00 kg"));
    }

    private Ingredient ingredient(String name, Double quantity, Double minStock, Integer shelfLifeDays) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setQuantity(BigDecimal.valueOf(quantity));
        ingredient.setMinStock(BigDecimal.valueOf(minStock));
        ingredient.setUnit("kg");
        ingredient.setShelfLifeDays(shelfLifeDays);
        return ingredient;
    }
}
