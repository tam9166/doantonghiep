package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@ExtendWith(MockitoExtension.class)
class InventoryAlertServiceTest {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    @Mock IngredientRepository ingredientRepository;
    @Mock IngredientBatchRepository ingredientBatchRepository;
    @Mock OrderRepository orderRepository;
    @Mock RecipeRepository recipeRepository;
    @InjectMocks InventoryAlertService service;

    @Test
    void twentyFourExpiredBatchesAreVisibleToEveryInventoryConsumerAndNeverReportedSafe() {
        Ingredient rice = ingredient(1L, "Gạo", "kg", "5", "20000");
        List<IngredientBatch> batches = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            batches.add(batch((long) index + 1, rice, "1", -1));
        }
        when(ingredientRepository.findAll()).thenReturn(List.of(rice));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient()).thenReturn(batches);
        when(ingredientBatchRepository.findTopByIngredientIdAndUnitPriceIsNotNullOrderByImportDateDescIdDesc(1L))
                .thenReturn(java.util.Optional.of(batches.getFirst()));
        when(orderRepository.findByStatusSinceWithDetails(eq(OrderStatus.COMPLETED.code()), any(Date.class)))
                .thenReturn(List.of());

        InventoryAlertService.Analysis analysis = service.analyze(3);

        assertEquals(24, analysis.expiredBatchesCount());
        assertEquals(1, analysis.handlingCount());
        assertEquals(1, analysis.totalItems());
        assertEquals(1, analysis.criticalCount());
        assertEquals(1, analysis.suggestions().size());
        assertEquals("expired", analysis.suggestions().getFirst().urgency());
        assertEquals(new BigDecimal("10.0"), analysis.suggestions().getFirst().suggestedAmount());
        assertTrue(analysis.suggestions().getFirst().needsPurchase());
        assertTrue(analysis.toAiContext().contains("expiredBatches=24"));
        assertFalse(analysis.toAiContext().contains("Kho đang an toàn"));
        verify(recipeRepository, never()).findAll();
    }

    @Test
    void validStockUsesCompletedSalesAndRecipesToCalculatePurchaseQuantity() {
        Ingredient rice = ingredient(2L, "Gạo", "kg", "5", "10000");
        IngredientBatch validBatch = batch(25L, rice, "5", 30);
        Product product = new Product();
        product.setId(10);
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(56);
        Order order = new Order();
        order.setOrderDetails(List.of(detail));
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(rice);
        recipe.setAmountRequired(BigDecimal.ONE);

        when(ingredientRepository.findAll()).thenReturn(List.of(rice));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient()).thenReturn(List.of(validBatch));
        when(ingredientBatchRepository.findTopByIngredientIdAndUnitPriceIsNotNullOrderByImportDateDescIdDesc(2L))
                .thenReturn(java.util.Optional.of(validBatch));
        when(orderRepository.findByStatusSinceWithDetails(eq(OrderStatus.COMPLETED.code()), any(Date.class)))
                .thenReturn(List.of(order));
        when(recipeRepository.findByProductIdsWithIngredient(List.of(10))).thenReturn(List.of(recipe));

        InventoryAlertService.Item item = service.analyze(3).suggestions().getFirst();

        assertEquals(new BigDecimal("8.00"), item.dailyConsumption());
        assertEquals(new BigDecimal("51.0"), item.suggestedAmount());
        assertEquals(new BigDecimal("510000"), item.estimatedCost());
        assertEquals(new BigDecimal("10000"), item.previousUnitPrice());
        assertTrue(item.needsPurchase());
        assertEquals("warning", item.urgency());
    }

    @Test
    void nearExpiryStockTriggersReplacementUsingOnlyQuantityConsumableBeforeExpiry() {
        Ingredient beef = ingredient(3L, "Thịt bò", "kg", "5", "180000");
        IngredientBatch nearExpiry = batch(30L, beef, "15", 2);
        IngredientBatch longLived = batch(31L, beef, "5", 30);
        Product product = new Product();
        product.setId(11);
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(28);
        Order order = new Order();
        order.setOrderDetails(List.of(detail));
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(beef);
        recipe.setAmountRequired(BigDecimal.ONE);

        when(ingredientRepository.findAll()).thenReturn(List.of(beef));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient())
                .thenReturn(List.of(nearExpiry, longLived));
        when(ingredientBatchRepository.findTopByIngredientIdAndUnitPriceIsNotNullOrderByImportDateDescIdDesc(3L))
                .thenReturn(java.util.Optional.of(longLived));
        when(orderRepository.findByStatusSinceWithDetails(eq(OrderStatus.COMPLETED.code()), any(Date.class)))
                .thenReturn(List.of(order));
        when(recipeRepository.findByProductIdsWithIngredient(List.of(11))).thenReturn(List.of(recipe));

        InventoryAlertService.Item item = service.analyze(3).suggestions().getFirst();

        assertEquals(new BigDecimal("20"), item.currentStock());
        assertEquals(new BigDecimal("15"), item.nearExpiryStock());
        assertEquals(new BigDecimal("5"), item.longLivedStock());
        assertEquals(new BigDecimal("15.0"), item.suggestedAmount());
        assertTrue(item.needsPurchase());
        assertTrue(item.action().contains("chuẩn bị nhập lô mới"));
    }

    @Test
    void abundantStockWithTwoDaysToExpiryWarnsAboutExpiryWithoutSuggestingPurchase() {
        Ingredient dessert = ingredient(4L, "Nguyên liệu tráng miệng", "kg", "1", "50000");
        IngredientBatch nearExpiry = batch(40L, dessert, "279.4", 2);
        mockDailyConsumption(dessert, 14, 7);
        when(ingredientRepository.findAll()).thenReturn(List.of(dessert));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient()).thenReturn(List.of(nearExpiry));
        when(ingredientBatchRepository.findTopByIngredientIdAndUnitPriceIsNotNullOrderByImportDateDescIdDesc(4L))
                .thenReturn(java.util.Optional.of(nearExpiry));

        InventoryAlertService.Item item = service.analyze(3).suggestions().getFirst();

        assertEquals(279.4, item.daysOfStock(), 0.1);
        assertEquals(2L, item.daysToExpiry());
        assertEquals("expiring", item.urgency());
        assertEquals("Sắp hết hạn", item.urgencyLabel());
        assertEquals(new BigDecimal("0.0"), item.suggestedAmount());
        assertFalse(item.needsPurchase());
        assertTrue(item.reason().contains("Tồn kho hiện tại vẫn còn nhiều"));
    }

    @Test
    void twoDaysOfStockWithLongExpiryWarnsAboutShortageNotExpiry() {
        Ingredient dessert = ingredient(5L, "Nguyên liệu tráng miệng", "kg", "0", "50000");
        IngredientBatch longLived = batch(50L, dessert, "2", 279);
        mockDailyConsumption(dessert, 15, 7);
        when(ingredientRepository.findAll()).thenReturn(List.of(dessert));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient()).thenReturn(List.of(longLived));
        when(ingredientBatchRepository.findTopByIngredientIdAndUnitPriceIsNotNullOrderByImportDateDescIdDesc(5L))
                .thenReturn(java.util.Optional.of(longLived));

        InventoryAlertService.Item item = service.analyze(3).suggestions().getFirst();

        assertEquals(2.0, item.daysOfStock(), 0.1);
        assertEquals(279L, item.daysToExpiry());
        assertEquals("info", item.urgency());
        assertTrue(item.urgencyLabel().startsWith("Sắp thiếu"));
        assertTrue(item.expiringBatches().isEmpty());
        assertTrue(item.needsPurchase());
    }

    @Test
    void abundantLongLivedStockDoesNotCreateAnAlert() {
        Ingredient dessert = ingredient(6L, "Nguyên liệu tráng miệng", "kg", "1", "50000");
        IngredientBatch longLived = batch(60L, dessert, "279.4", 279);
        mockDailyConsumption(dessert, 16, 7);
        when(ingredientRepository.findAll()).thenReturn(List.of(dessert));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient()).thenReturn(List.of(longLived));

        assertTrue(service.analyze(3).suggestions().isEmpty());
    }

    @Test
    void expiredBatchUsesExpirySeverityRegardlessOfDaysOfStock() {
        Ingredient dessert = ingredient(7L, "Nguyên liệu tráng miệng", "kg", "1", "50000");
        IngredientBatch expired = batch(70L, dessert, "10", -1);
        when(ingredientRepository.findAll()).thenReturn(List.of(dessert));
        when(ingredientBatchRepository.findPositiveBatchesWithIngredient()).thenReturn(List.of(expired));
        when(ingredientBatchRepository.findTopByIngredientIdAndUnitPriceIsNotNullOrderByImportDateDescIdDesc(7L))
                .thenReturn(java.util.Optional.of(expired));
        when(orderRepository.findByStatusSinceWithDetails(eq(OrderStatus.COMPLETED.code()), any(Date.class)))
                .thenReturn(List.of());

        InventoryAlertService.Item item = service.analyze(3).suggestions().getFirst();

        assertEquals("expired", item.urgency());
        assertTrue(item.daysToExpiry() <= 0);
        assertEquals(1, item.expiredBatches().size());
    }

    private void mockDailyConsumption(Ingredient ingredient, int productId, int sevenDayQuantity) {
        Product product = new Product();
        product.setId(productId);
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(sevenDayQuantity);
        Order order = new Order();
        order.setOrderDetails(List.of(detail));
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(BigDecimal.ONE);
        when(orderRepository.findByStatusSinceWithDetails(eq(OrderStatus.COMPLETED.code()), any(Date.class)))
                .thenReturn(List.of(order));
        when(recipeRepository.findByProductIdsWithIngredient(List.of(productId))).thenReturn(List.of(recipe));
    }

    private Ingredient ingredient(Long id, String name, String unit, String minStock, String unitPrice) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName(name);
        ingredient.setUnit(unit);
        ingredient.setMinStock(new BigDecimal(minStock));
        ingredient.setUnitPrice(new BigDecimal(unitPrice));
        return ingredient;
    }

    private IngredientBatch batch(Long id, Ingredient ingredient, String quantity, int expiryOffsetDays) {
        IngredientBatch batch = new IngredientBatch();
        batch.setId(id);
        batch.setIngredient(ingredient);
        batch.setQuantity(new BigDecimal(quantity));
        batch.setImportDate(new Date(System.currentTimeMillis() - 86_400_000L));
        batch.setExpirationDate(Date.from(LocalDate.now(BUSINESS_ZONE).plusDays(expiryOffsetDays)
                .atTime(12, 0).atZone(BUSINESS_ZONE).toInstant()));
        batch.setUnitPrice(ingredient.getUnitPrice());
        return batch;
    }
}
