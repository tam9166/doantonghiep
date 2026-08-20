package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.exception.InsufficientInventoryException;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;

@SpringBootTest
class OrderCheckoutPersistenceIntegrationTest {

    @Autowired private OrderCheckoutService checkoutService;
    @Autowired private ProductRepository productRepository;
    @Autowired private IngredientRepository ingredientRepository;
    @Autowired private IngredientBatchRepository batchRepository;
    @Autowired private RecipeRepository recipeRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoBean
    private ActivityLogService activityLogService;

    private final List<Fixture> fixtures = new ArrayList<>();

    @AfterEach
    void cleanFixtures() {
        reset(activityLogService);
        for (Fixture fixture : fixtures.reversed()) {
            jdbcTemplate.update("DELETE FROM order_item_operations WHERE order_id IN (?, ?)",
                    fixture.firstOrderId(), fixture.secondOrderId());
            jdbcTemplate.update("DELETE FROM order_details WHERE order_id IN (?, ?)",
                    fixture.firstOrderId(), fixture.secondOrderId());
            jdbcTemplate.update("DELETE FROM orders WHERE id IN (?, ?)",
                    fixture.firstOrderId(), fixture.secondOrderId());
            jdbcTemplate.update("DELETE FROM recipes WHERE product_id = ?", fixture.productId());
            jdbcTemplate.update("DELETE FROM ingredient_batches WHERE ingredient_id = ?", fixture.ingredientId());
            jdbcTemplate.update("DELETE FROM products WHERE id = ?", fixture.productId());
            jdbcTemplate.update("DELETE FROM ingredients WHERE id = ?", fixture.ingredientId());
        }
        fixtures.clear();
    }

    @Test
    void lateFailureRollsBackDetailsTotalsInventoryAndIdempotencyRecord() {
        Fixture fixture = fixture(10.0, 1.0);
        doThrow(new IllegalStateException("forced late failure"))
                .when(activityLogService).log(eq("UPDATE"), eq("Order"),
                        eq(String.valueOf(fixture.firstOrderId())), anyString());

        assertThrows(IllegalStateException.class,
                () -> checkoutService.addItems(fixture.firstOrderId(), request(fixture.productId(), 2),
                        "rollback-key-001"));

        assertOrderState(fixture.firstOrderId(), 0, "0.00");
        assertEquals(0, count("order_item_operations", fixture.firstOrderId()));
        assertEquals(10.0, batchQuantity(fixture.batchId()), 0.000001);
        assertEquals(10.0, ingredientQuantity(fixture.ingredientId()), 0.000001);
    }

    @Test
    void insufficientInventoryCreatesNothingAndLeavesTotalsAndStockUnchanged() {
        Fixture fixture = fixture(0.5, 1.0);

        assertThrows(InsufficientInventoryException.class,
                () -> checkoutService.addItems(fixture.firstOrderId(), request(fixture.productId(), 1),
                        "shortage-key-001"));

        assertOrderState(fixture.firstOrderId(), 0, "0.00");
        assertEquals(0, count("order_item_operations", fixture.firstOrderId()));
        assertEquals(0.5, batchQuantity(fixture.batchId()), 0.000001);
        assertEquals(0.5, ingredientQuantity(fixture.ingredientId()), 0.000001);
    }

    @Test
    void retryWithTheSameIdempotencyKeyAddsAndConsumesOnlyOnce() {
        Fixture fixture = fixture(10.0, 1.0);
        OrderRequest request = request(fixture.productId(), 2);

        OrderCheckoutService.AddItemsResult first = checkoutService.addItems(
                fixture.firstOrderId(), request, "retry-key-0001");
        OrderCheckoutService.AddItemsResult retry = checkoutService.addItems(
                fixture.firstOrderId(), request, "retry-key-0001");

        assertEquals(first, retry);
        assertOrderState(fixture.firstOrderId(), 1, "216.00");
        assertEquals(1, count("order_item_operations", fixture.firstOrderId()));
        assertEquals(8.0, batchQuantity(fixture.batchId()), 0.000001);
        assertEquals(8.0, ingredientQuantity(fixture.ingredientId()), 0.000001);
    }

    @Test
    void fractionalInventoryAndTaxColumnsUseExactDecimalArithmetic() {
        Fixture fixture = fixture(0.3, 0.1);

        checkoutService.addItems(fixture.firstOrderId(), request(fixture.productId(), 3),
                "decimal-key-001");

        assertEquals(new BigDecimal("0.0000"), decimalQuantity("ingredient_batches", fixture.batchId()));
        assertEquals(new BigDecimal("0.0000"), decimalQuantity("ingredients", fixture.ingredientId()));
        List<String> columnTypes = jdbcTemplate.queryForList("""
                SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE (TABLE_NAME = 'ingredients' AND COLUMN_NAME IN ('quantity', 'min_stock'))
                   OR (TABLE_NAME = 'ingredient_batches' AND COLUMN_NAME = 'quantity')
                   OR (TABLE_NAME = 'recipes' AND COLUMN_NAME = 'amount_required')
                   OR (TABLE_NAME = 'products' AND COLUMN_NAME = 'tax_rate')
                   OR (TABLE_NAME = 'order_details' AND COLUMN_NAME = 'tax_rate')
                """, String.class);
        assertEquals(6, columnTypes.size());
        assertTrue(columnTypes.stream().allMatch("decimal"::equalsIgnoreCase));
    }

    @Test
    @Timeout(20)
    void concurrentRequestsForTheLastStockAllowExactlyOneSuccess() throws Exception {
        Fixture fixture = fixture(1.0, 1.0);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = executor.submit(() -> invokeAfter(start, fixture.firstOrderId(),
                    fixture.productId(), "race-key-first"));
            Future<Throwable> second = executor.submit(() -> invokeAfter(start, fixture.secondOrderId(),
                    fixture.productId(), "race-key-second"));
            start.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            outcomes.add(first.get(15, TimeUnit.SECONDS));
            outcomes.add(second.get(15, TimeUnit.SECONDS));
            long successes = outcomes.stream().filter(outcome -> outcome == null).count();
            List<Throwable> failures = outcomes.stream().filter(outcome -> outcome != null).toList();

            assertEquals(1, successes);
            assertEquals(1, failures.size());
            assertInstanceOf(InsufficientInventoryException.class, failures.getFirst());
            assertEquals(1, count("order_details", fixture.firstOrderId())
                    + count("order_details", fixture.secondOrderId()));
            assertEquals(0.0, batchQuantity(fixture.batchId()), 0.000001);
            assertTrue(ingredientQuantity(fixture.ingredientId()) >= 0.0);
        } finally {
            executor.shutdownNow();
        }
    }

    private Throwable invokeAfter(CountDownLatch start, Integer orderId, Integer productId, String key) {
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            checkoutService.addItems(orderId, request(productId, 1), key);
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private Fixture fixture(double stock, double amountRequired) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Fixture fixture = transaction.execute(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);

            Ingredient ingredient = new Ingredient();
            ingredient.setName("reg_inventory_" + suffix);
            ingredient.setUnit("unit");
            ingredient.setQuantity(BigDecimal.valueOf(stock));
            ingredient.setMinStock(BigDecimal.ZERO);
            ingredient.setUnitPrice(BigDecimal.ONE);
            ingredient = ingredientRepository.save(ingredient);

            IngredientBatch batch = new IngredientBatch();
            batch.setIngredient(ingredient);
            batch.setQuantity(BigDecimal.valueOf(stock));
            batch.setImportDate(new Date());
            batch.setExpirationDate(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)));
            batch.setUnitPrice(BigDecimal.ONE);
            batch = batchRepository.save(batch);

            Product product = new Product();
            product.setName("reg_product_" + suffix);
            product.setPrice(new BigDecimal("100.00"));
            product.setTaxRate(new BigDecimal("8.00"));
            product = productRepository.save(product);

            Recipe recipe = new Recipe();
            recipe.setProduct(product);
            recipe.setIngredient(ingredient);
            recipe.setAmountRequired(BigDecimal.valueOf(amountRequired));
            recipeRepository.save(recipe);

            Order firstOrder = orderRepository.save(openOrder("reg_order_a_" + suffix));
            Order secondOrder = orderRepository.save(openOrder("reg_order_b_" + suffix));
            return new Fixture(product.getId(), ingredient.getId(), batch.getId(),
                    firstOrder.getId(), secondOrder.getId());
        });
        fixtures.add(fixture);
        return fixture;
    }

    private Order openOrder(String address) {
        Order order = new Order();
        order.setAddress(address);
        order.setStatus(0);
        order.setSubTotal(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);
        return order;
    }

    private OrderRequest request(Integer productId, int quantity) {
        OrderDetailRequest item = new OrderDetailRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);
        OrderRequest request = new OrderRequest();
        request.setOrderType(poly.edu.quanlynhahang.entity.OrderType.TAKEAWAY);
        request.setItems(List.of(item));
        return request;
    }

    private void assertOrderState(Integer orderId, int detailCount, String total) {
        assertEquals(detailCount, count("order_details", orderId));
        BigDecimal actual = jdbcTemplate.queryForObject(
                "SELECT total_amount FROM orders WHERE id = ?", BigDecimal.class, orderId);
        assertEquals(new BigDecimal(total), actual.setScale(2));
    }

    private int count(String table, Integer orderId) {
        if (!List.of("order_details", "order_item_operations").contains(table)) {
            throw new IllegalArgumentException("Unsupported table");
        }
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE order_id = ?", Integer.class, orderId);
    }

    private double batchQuantity(Long batchId) {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM ingredient_batches WHERE id = ?", Double.class, batchId);
    }

    private double ingredientQuantity(Long ingredientId) {
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM ingredients WHERE id = ?", Double.class, ingredientId);
    }

    private BigDecimal decimalQuantity(String table, Long id) {
        if (!List.of("ingredient_batches", "ingredients").contains(table)) {
            throw new IllegalArgumentException("Unsupported table");
        }
        return jdbcTemplate.queryForObject(
                "SELECT quantity FROM " + table + " WHERE id = ?", BigDecimal.class, id).setScale(4);
    }

    private record Fixture(Integer productId, Long ingredientId, Long batchId,
                           Integer firstOrderId, Integer secondOrderId) {
    }
}
