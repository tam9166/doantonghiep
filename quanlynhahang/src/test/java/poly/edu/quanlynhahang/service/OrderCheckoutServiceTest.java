package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.exception.InsufficientInventoryException;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderItemOperation;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.IngredientBatchRepository;
import poly.edu.quanlynhahang.repository.IngredientRepository;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderItemOperationRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCheckoutServiceTest {
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderDetailRepository orderDetailRepository = mock(OrderDetailRepository.class);
    private final OrderItemOperationRepository orderItemOperationRepository = mock(OrderItemOperationRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
    private final RecipeRepository recipeRepository = mock(RecipeRepository.class);
    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final IngredientBatchRepository batchRepository = mock(IngredientBatchRepository.class);
    private final VoucherRepository voucherRepository = mock(VoucherRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final OrderPaymentService orderPaymentService = mock(OrderPaymentService.class);

    private final OrderCheckoutService service = new OrderCheckoutService(
            orderRepository,
            orderDetailRepository,
            orderItemOperationRepository,
            productRepository,
            accountRepository,
            tableRepository,
            recipeRepository,
            ingredientRepository,
            batchRepository,
            voucherRepository,
            activityLogService,
            orderPaymentService);

    @Test
    void rejectsEmptyCartBeforeWritingAnything() {
        OrderRequest request = new OrderRequest();
        request.setItems(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request, "anonymousUser"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(orderDetailRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownProductInsteadOfSilentlySkippingIt() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request(999, 1), "anonymousUser"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(orderDetailRepository, never()).save(any());
    }

    @Test
    void rejectsInsufficientInventoryBeforeCreatingOrder() {
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        Recipe recipe = recipe(product, ingredient, 2.0);
        IngredientBatch batch = batch(1.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
        when(batchRepository.findAvailableBatchesForUpdate(10L)).thenReturn(List.of(batch));

        InsufficientInventoryException error = assertThrows(InsufficientInventoryException.class,
                () -> service.checkout(request(1, 1), "anonymousUser"));

        assertEquals(1, error.getShortages().size());
        verify(orderRepository, never()).save(any());
        verify(batchRepository, never()).saveAll(any());
    }

    @Test
    void calculatesServerPriceAndConsumesLockedInventory() {
        Product product = product(1, 100_000.0);
        product.setTaxRate(8.0);
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        Recipe recipe = recipe(product, ingredient, 2.0);
        IngredientBatch batch = batch(10.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
        when(recipeRepository.findByIngredient(ingredient)).thenReturn(List.of(recipe));
        when(batchRepository.findAvailableBatchesForUpdate(10L)).thenReturn(List.of(batch));
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(22);
            return order;
        });
        when(orderDetailRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCheckoutService.CheckoutResult result = service.checkout(request(1, 2), "anonymousUser");

        assertEquals(22, result.orderId());
        assertEquals(0, result.status());
        assertEquals(new BigDecimal("200000.00"), result.subTotal());
        assertEquals(new BigDecimal("16000.00"), result.taxAmount());
        assertEquals(new BigDecimal("216000.00"), result.totalAmount());
        assertEquals(6.0, batch.getQuantity());
        assertEquals(6.0, ingredient.getQuantity());
        verify(batchRepository).findAvailableBatchesForUpdate(10L);
        verify(batchRepository).saveAll(List.of(batch));
        verify(orderDetailRepository).save(any(OrderDetail.class));
    }

    @Test
    void roundsDecimalTaxBeforePersistingOrderTotals() {
        Product product = product(1, 0.10);
        product.setTaxRate(8.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of());
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(23);
            return order;
        });

        OrderCheckoutService.CheckoutResult result = service.checkout(request(1, 3), "anonymousUser");

        assertEquals(new BigDecimal("0.30"), result.subTotal());
        assertEquals(new BigDecimal("0.02"), result.taxAmount());
        assertEquals(new BigDecimal("0.32"), result.totalAmount());
    }

    @Test
    void addItemsLocksOrderAndValidatesInventoryBeforeWritingDetails() {
        Order order = new Order();
        order.setId(22);
        order.setStatus(0);
        order.setSubTotal(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thit bo");
        Recipe recipe = recipe(product, ingredient, 2.0);
        IngredientBatch batch = batch(4.0);
        when(orderRepository.findLockedById(22)).thenReturn(Optional.of(order));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
        when(recipeRepository.findByIngredient(ingredient)).thenReturn(List.of(recipe));
        when(batchRepository.findAvailableBatchesForUpdate(10L)).thenReturn(List.of(batch));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCheckoutService.AddItemsResult result = service.addItems(22, request(1, 2), "add-item-key-001");

        assertEquals(2, result.addedItems());
        assertEquals(new BigDecimal("200000.00"), result.subTotal());
        assertEquals(new BigDecimal("16000.00"), result.taxAmount());
        assertEquals(new BigDecimal("216000.00"), result.totalAmount());
        assertEquals(0.0, batch.getQuantity());
        verify(orderRepository).findLockedById(22);
        verify(orderDetailRepository).save(any(OrderDetail.class));
        verify(batchRepository).saveAll(List.of(batch));
        verify(orderItemOperationRepository).save(any(OrderItemOperation.class));
    }

    @Test
    void addItemsReturnsStoredResultForTheSameIdempotencyKey() {
        Order order = new Order();
        order.setId(22);
        OrderItemOperation operation = new OrderItemOperation();
        operation.setOrderId(22);
        operation.setIdempotencyKey("add-item-key-001");
        operation.setRequestHash("7acaa2e3bafe499aedd41ea9500f071b2c661bfe50ad8a07ccb3953cf836fcdc");
        operation.setAddedItems(2);
        operation.setSubTotal(new BigDecimal("200000.00"));
        operation.setTaxAmount(new BigDecimal("16000.00"));
        operation.setTotalAmount(new BigDecimal("216000.00"));
        when(orderRepository.findLockedById(22)).thenReturn(Optional.of(order));
        when(orderItemOperationRepository.findByOrderIdAndIdempotencyKey(22, "add-item-key-001"))
                .thenReturn(Optional.of(operation));

        OrderCheckoutService.AddItemsResult result = service.addItems(22, request(1, 2), "add-item-key-001");

        assertEquals(2, result.addedItems());
        assertEquals(new BigDecimal("216000.00"), result.totalAmount());
        verify(orderDetailRepository, never()).save(any());
        verify(batchRepository, never()).saveAll(any());
    }

    @Test
    void addItemsRejectsMissingIdempotencyKeyBeforeLockingTheOrder() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addItems(22, request(1, 2), " "));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("IDEMPOTENCY_KEY_REQUIRED", error.getReason());
        verify(orderRepository, never()).findLockedById(any());
    }

    @Test
    void addItemsRejectsReusedIdempotencyKeyWithDifferentPayload() {
        Order order = new Order();
        order.setId(22);
        OrderItemOperation operation = new OrderItemOperation();
        operation.setOrderId(22);
        operation.setIdempotencyKey("add-item-key-001");
        operation.setRequestHash("7acaa2e3bafe499aedd41ea9500f071b2c661bfe50ad8a07ccb3953cf836fcdc");
        when(orderRepository.findLockedById(22)).thenReturn(Optional.of(order));
        when(orderItemOperationRepository.findByOrderIdAndIdempotencyKey(22, "add-item-key-001"))
                .thenReturn(Optional.of(operation));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addItems(22, request(1, 3), "add-item-key-001"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderDetailRepository, never()).save(any());
        verify(batchRepository, never()).saveAll(any());
    }

    private OrderRequest request(int productId, int quantity) {
        OrderDetailRequest detail = new OrderDetailRequest();
        detail.setProductId(productId);
        detail.setQuantity(quantity);
        OrderRequest request = new OrderRequest();
        request.setAddress("Giao hàng");
        request.setItems(List.of(detail));
        return request;
    }

    private Product product(int id, double price) {
        Product product = new Product();
        product.setId(id);
        product.setName("Món test");
        product.setPrice(BigDecimal.valueOf(price));
        product.setStatus(true);
        product.setAvailable(true);
        return product;
    }

    private Ingredient ingredient(long id, String name) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName(name);
        return ingredient;
    }

    private Recipe recipe(Product product, Ingredient ingredient, double amount) {
        Recipe recipe = new Recipe();
        recipe.setProduct(product);
        recipe.setIngredient(ingredient);
        recipe.setAmountRequired(amount);
        return recipe;
    }

    private IngredientBatch batch(double quantity) {
        IngredientBatch batch = new IngredientBatch();
        batch.setId(1L);
        batch.setQuantity(quantity);
        return batch;
    }
}
