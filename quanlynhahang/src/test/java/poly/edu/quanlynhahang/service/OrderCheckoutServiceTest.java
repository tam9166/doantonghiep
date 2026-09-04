package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.OrderDetailRequest;
import poly.edu.quanlynhahang.dto.OrderRequest;
import poly.edu.quanlynhahang.exception.InsufficientInventoryException;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Ingredient;
import poly.edu.quanlynhahang.entity.IngredientBatch;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderVoucherUsage;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderItemOperation;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Recipe;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.Voucher;
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
import poly.edu.quanlynhahang.repository.OrderVoucherUsageRepository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import org.mockito.ArgumentCaptor;

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
    private final OrderVoucherUsageRepository orderVoucherUsageRepository = mock(OrderVoucherUsageRepository.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final OrderPaymentService orderPaymentService = mock(OrderPaymentService.class);
    private final MenuAvailabilityService menuAvailabilityService = mock(MenuAvailabilityService.class);
    private final InventoryReservationService inventoryReservationService = mock(InventoryReservationService.class);
    private final SqlServerApplicationLockService applicationLockService = mock(SqlServerApplicationLockService.class);

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
            orderVoucherUsageRepository,
            activityLogService,
            orderPaymentService,
            menuAvailabilityService,
            inventoryReservationService,
            new OrderStateMachineService(),
            applicationLockService);

    @BeforeEach
    void allowInventoryUnlessTestOverridesIt() {
        when(menuAvailabilityService.availableQuantity(any(Product.class))).thenReturn(100);
        when(applicationLockService.acquireExclusive(any(), any(Integer.class))).thenReturn(0);
        when(inventoryReservationService.defaultExpiry()).thenReturn(new java.util.Date(System.currentTimeMillis() + 60_000));
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order == null) return null;
            if (order.getId() == null) order.setId(22);
            return order;
        });
    }

    @Test
    void duplicateCheckoutKeyReturnsTheExistingOrderAndPaymentIntent() {
        OrderRequest request = request(1, 2);
        request.setOrderType(poly.edu.quanlynhahang.entity.OrderType.DELIVERY);
        request.setPaymentOption(poly.edu.quanlynhahang.entity.OrderPaymentOption.PREPAID_TRANSFER);
        request.setRecipientName("Nguyễn Văn A");
        request.setRecipientPhone("0905123456");
        request.setDeliveryAddress("Đà Nẵng");
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        Recipe recipe = recipe(product, ingredient, 1.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));

        OrderCheckoutService.CheckoutResult first = service.checkout(request, "anonymousUser", "checkout-key-001");
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, org.mockito.Mockito.atLeastOnce()).save(orderCaptor.capture());
        Order saved = orderCaptor.getAllValues().stream()
                .filter(order -> "checkout-key-001".equals(order.getCheckoutIdempotencyKey()))
                .findFirst().orElseThrow();
        when(orderRepository.findByCheckoutIdempotencyKey("checkout-key-001"))
                .thenReturn(Optional.of(saved));

        OrderCheckoutService.CheckoutResult duplicate = service.checkout(
                request, "anonymousUser", "checkout-key-001");

        assertEquals(first.orderId(), duplicate.orderId());
        assertEquals(first.orderCode(), duplicate.orderCode());
        verify(applicationLockService, times(2)).acquireExclusive("order-checkout:checkout-key-001", 10_000);
    }

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
    void rejectsMissingOrderTypeBeforeWritingAnything() {
        OrderRequest request = request(1, 1);
        request.setOrderType(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request, "anonymousUser"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(orderDetailRepository, never()).save(any());
    }

    @Test
    void dineInCheckoutRequiresAnExistingTableIdBeforeWritingAnything() {
        OrderRequest request = request(1, 1);
        request.setOrderType(OrderType.DINE_IN);
        request.setTableId(404);
        request.setAddress("Bàn giả trong chuỗi địa chỉ");
        when(tableRepository.findLockedById(404)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request, "anonymousUser"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(tableRepository).findLockedById(404);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void dineInCheckoutRejectsDisabledTable() {
        OrderRequest request = request(1, 1);
        request.setOrderType(OrderType.DINE_IN);
        request.setTableId(5);
        RestaurantTable table = new RestaurantTable();
        table.setId(5);
        table.setActive(false);
        when(tableRepository.findLockedById(5)).thenReturn(Optional.of(table));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request, "anonymousUser"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void newDineInOrderOnOccupiedTableReturnsTheExactBusinessMessage() {
        OrderRequest request = request(1, 1);
        request.setOrderType(OrderType.DINE_IN);
        request.setTableId(5);
        RestaurantTable table = new RestaurantTable();
        table.setId(5);
        table.setName("B05");
        table.setActive(true);
        table.setIsOccupied(2);
        Order openOrder = new Order();
        openOrder.setId(90);
        when(tableRepository.findLockedById(5)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenDineInOrdersByTableIdWithDetails(5)).thenReturn(List.of(openOrder));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request, "anonymousUser"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("Bàn đã có khách, vui lòng chọn lại bàn trống khác.", error.getReason());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void explicitStaffAddOnCanCreateANewOpenOrderAfterMergedPaidTables() {
        OrderRequest request = request(1, 1);
        request.setOrderType(OrderType.DINE_IN);
        request.setPaymentOption(poly.edu.quanlynhahang.entity.OrderPaymentOption.PAY_AT_RESTAURANT);
        request.setTableId(5);
        request.setAppendToOccupiedTable(true);
        RestaurantTable table = new RestaurantTable();
        table.setId(5);
        table.setName("B05");
        table.setActive(true);
        table.setIsOccupied(2);
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        when(tableRepository.findLockedById(5)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenDineInOrdersByTableIdWithDetails(5)).thenReturn(List.of());
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 1.0)));

        OrderCheckoutService.CheckoutResult result = service.checkout(
                request, "regression-waiter", "post-merge-add-on");

        assertEquals(22, result.orderId());
        assertEquals(2, table.getIsOccupied());
        verify(tableRepository, never()).save(table);
    }

    @Test
    void directDineInCheckoutOccupiesAvailableTableAndReservesInventory() {
        OrderRequest request = request(1, 1);
        request.setOrderType(OrderType.DINE_IN);
        request.setPaymentOption(poly.edu.quanlynhahang.entity.OrderPaymentOption.PAY_AT_RESTAURANT);
        request.setTableId(5);
        RestaurantTable table = new RestaurantTable();
        table.setId(5);
        table.setName("B05");
        table.setActive(true);
        table.setIsOccupied(0);
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        when(tableRepository.findLockedById(5)).thenReturn(Optional.of(table));
        when(orderRepository.findOpenDineInOrdersByTableIdWithDetails(5)).thenReturn(List.of());
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 1.0)));

        OrderCheckoutService.CheckoutResult result = service.checkout(
                request, "anonymousUser", "dinein-checkout-5");

        assertEquals(22, result.orderId());
        assertEquals(1, table.getIsOccupied());
        verify(tableRepository).save(table);
        verify(inventoryReservationService).reserve(any(), any(), any());
    }

    @Test
    void deliveryRequiresStructuredRecipientDataBeforeWritingAnything() {
        OrderRequest request = request(1, 1);
        request.setRecipientPhone("khong-hop-le");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.checkout(request, "anonymousUser"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsInsufficientInventoryWithoutMutatingPhysicalStock() {
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        Recipe recipe = recipe(product, ingredient, 2.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
        doThrow(new InsufficientInventoryException(java.util.Map.of("Thịt bò", "required=2.0, available=1.0")))
                .when(inventoryReservationService).reserve(any(), any(), any());

        InsufficientInventoryException error = assertThrows(InsufficientInventoryException.class,
                () -> service.checkout(request(1, 1), "anonymousUser"));

        assertEquals(1, error.getShortages().size());
        verify(inventoryReservationService).reserve(any(), any(), any());
        verify(batchRepository, never()).saveAll(any());
    }

    @Test
    void rejectsRequestedDishQuantityAboveAvailableServings() {
        Product product = product(1, 100_000.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(menuAvailabilityService.availableQuantity(product)).thenReturn(7);

        InsufficientInventoryException error = assertThrows(InsufficientInventoryException.class,
                () -> service.checkout(request(1, 8), "anonymousUser"));

        assertEquals("requested=8, availableQuantity=7", error.getShortages().get(product.getName()));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void calculatesServerPriceAndReservesInventoryWithoutConsumingIt() {
        Product product = product(1, 100_000.0);
        product.setTaxRate(new BigDecimal("8.00"));
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        Recipe recipe = recipe(product, ingredient, 2.0);
        IngredientBatch batch = batch(10.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
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
        assertEquals(0, new BigDecimal("10.0").compareTo(batch.getQuantity()));
        verify(inventoryReservationService).reserve(any(Order.class),
                org.mockito.ArgumentMatchers.eq(java.util.Map.of(10L, new BigDecimal("4.0"))), any());
        verify(batchRepository, never()).saveAll(any());
        verify(orderDetailRepository).save(any(OrderDetail.class));
    }

    @Test
    void separatesMembershipAndVoucherDiscountAndRecordsOnlyActualVoucherReduction() {
        Account account = new Account();
        account.setUsername("member");
        account.setMembershipTier("Vàng");
        Voucher voucher = new Voucher();
        voucher.setId(5L);
        voucher.setCode("SAVE20");
        voucher.setDiscountPercent(20);
        Product product = product(1, 100_000.0);
        product.setTaxRate(new BigDecimal("8.00"));
        Ingredient ingredient = ingredient(10L, "Thịt bò");
        when(accountRepository.findById("member")).thenReturn(Optional.of(account));
        when(voucherRepository.findLockedByCode("SAVE20")).thenReturn(Optional.of(voucher));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe(product, ingredient, 1.0)));
        OrderRequest request = request(1, 1);
        request.setVoucherCode("SAVE20");

        OrderCheckoutService.CheckoutResult result = service.checkout(request, "member");

        assertEquals(new BigDecimal("100000.00"), result.originalSubtotal());
        assertEquals(new BigDecimal("10000.00"), result.membershipDiscount());
        assertEquals(new BigDecimal("18000.00"), result.voucherDiscount());
        assertEquals(new BigDecimal("72000.00"), result.subTotal());
        assertEquals(new BigDecimal("5760.00"), result.taxAmount());
        assertEquals(new BigDecimal("77760.00"), result.totalAmount());
        ArgumentCaptor<OrderVoucherUsage> usage = ArgumentCaptor.forClass(OrderVoucherUsage.class);
        verify(orderVoucherUsageRepository).save(usage.capture());
        assertEquals(new BigDecimal("18000"), usage.getValue().getDiscountAmount());
        assertEquals(new BigDecimal("90000"), usage.getValue().getOriginalAmount());
    }

    @Test
    void sellsActiveProductWithoutRecipeAsInventoryUnmanaged() {
        Product product = product(1, 0.10);
        product.setTaxRate(new BigDecimal("8.00"));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of());

        assertDoesNotThrow(() -> service.checkout(request(1, 3), "anonymousUser"));
        verify(orderRepository, times(2)).save(any());
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
        when(ingredientRepository.findLockedById(10L)).thenReturn(Optional.of(ingredient));
        when(orderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderCheckoutService.AddItemsResult result = service.addItems(22, request(1, 2), "add-item-key-001");

        assertEquals(2, result.addedItems());
        assertEquals(new BigDecimal("200000.00"), result.subTotal());
        assertEquals(new BigDecimal("16000.00"), result.taxAmount());
        assertEquals(new BigDecimal("216000.00"), result.totalAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(batch.getQuantity()));
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
        operation.setRequestHash("b7511123b954cc151e43d45de96f31da082a7df3af17050ed283d8e9f52977e9");
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
        operation.setRequestHash("b7511123b954cc151e43d45de96f31da082a7df3af17050ed283d8e9f52977e9");
        when(orderRepository.findLockedById(22)).thenReturn(Optional.of(order));
        when(orderItemOperationRepository.findByOrderIdAndIdempotencyKey(22, "add-item-key-001"))
                .thenReturn(Optional.of(operation));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.addItems(22, request(1, 3), "add-item-key-001"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(orderDetailRepository, never()).save(any());
        verify(batchRepository, never()).saveAll(any());
    }

    @Test
    void persistsPerDishNoteAndAllergyWithoutMergingDifferentLines() {
        Product product = product(1, 100_000.0);
        Ingredient ingredient = ingredient(10L, "Thit bo");
        Recipe recipe = recipe(product, ingredient, 1.0);
        IngredientBatch batch = batch(10.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of(recipe));
        when(recipeRepository.findByIngredient(ingredient)).thenReturn(List.of(recipe));
        when(batchRepository.findAvailableBatchesForUpdate(10L)).thenReturn(List.of(batch));
        when(orderRepository.save(any())).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(24);
            return order;
        });

        OrderDetailRequest first = detail(1, 1, "It cay", "Di ung dau phong", 4);
        OrderDetailRequest second = detail(1, 1, "Khong hanh", null, 0);
        OrderRequest request = new OrderRequest();
        request.setRecipientName("Nguyen An");
        request.setRecipientPhone("0901234567");
        request.setDeliveryAddress("123 Duong Test");
        request.setOrderType(OrderType.DELIVERY);
        request.setItems(List.of(first, second));

        service.checkout(request, "anonymousUser");

        ArgumentCaptor<OrderDetail> captor = ArgumentCaptor.forClass(OrderDetail.class);
        verify(orderDetailRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals("It cay", captor.getAllValues().get(0).getNote());
        assertEquals("Di ung dau phong", captor.getAllValues().get(0).getAllergyNote());
        assertEquals("Khong hanh", captor.getAllValues().get(1).getNote());
        assertEquals(0, captor.getAllValues().get(1).getPriority());
    }

    @Test
    void futureReservationPreorderKeepsServiceDateAndPerDishNoteWhileWaiting() {
        LocalDate serviceDate = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
        LocalTime serviceTime = LocalTime.of(18, 30);
        RestaurantTable table = new RestaurantTable();
        table.setId(7);
        table.setName("B07");
        Reservation reservation = new Reservation();
        reservation.setReservationCode("MV-FUTURE-001");
        reservation.setCustomerName("Khách đặt trước");
        reservation.setReservationDate(serviceDate);
        reservation.setArrivalTime(serviceTime);
        reservation.setTable(table);
        reservation.setTotalAmount(new BigDecimal("229000"));
        reservation.setPaidAmount(new BigDecimal("114500"));

        Product product = product(1, 229_000.0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(recipeRepository.findByProduct(product)).thenReturn(List.of());
        ReservationPreorderItem preorder = new ReservationPreorderItem();
        preorder.setProductId(1);
        preorder.setQuantity(1);
        preorder.setLineTotal(new BigDecimal("229000"));
        preorder.setNote("  Ít cay, không hành  ");

        service.dispatchReservationPreorder(reservation, List.of(preorder));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, org.mockito.Mockito.atLeastOnce()).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getAllValues().get(orderCaptor.getAllValues().size() - 1);
        assertEquals(5, savedOrder.getStatus());
        assertEquals(serviceDate.atTime(serviceTime), savedOrder.getScheduledAt());
        ArgumentCaptor<OrderDetail> detailCaptor = ArgumentCaptor.forClass(OrderDetail.class);
        verify(orderDetailRepository).save(detailCaptor.capture());
        assertEquals("Ít cay, không hành", detailCaptor.getValue().getNote());
    }

    private OrderRequest request(int productId, int quantity) {
        OrderDetailRequest detail = detail(productId, quantity, null, null, 0);
        OrderRequest request = new OrderRequest();
        request.setRecipientName("Nguyễn An");
        request.setRecipientPhone("0901234567");
        request.setDeliveryAddress("123 Đường Test");
        request.setOrderType(OrderType.DELIVERY);
        request.setItems(List.of(detail));
        return request;
    }

    private OrderDetailRequest detail(int productId, int quantity, String note, String allergyNote, int priority) {
        OrderDetailRequest detail = new OrderDetailRequest();
        detail.setProductId(productId);
        detail.setQuantity(quantity);
        detail.setNote(note);
        detail.setAllergyNote(allergyNote);
        detail.setPriority(priority);
        return detail;
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
        recipe.setAmountRequired(BigDecimal.valueOf(amount));
        return recipe;
    }

    private IngredientBatch batch(double quantity) {
        IngredientBatch batch = new IngredientBatch();
        batch.setId(1L);
        batch.setQuantity(BigDecimal.valueOf(quantity));
        return batch;
    }
}
