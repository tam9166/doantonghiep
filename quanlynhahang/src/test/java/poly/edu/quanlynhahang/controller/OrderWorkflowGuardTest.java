package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.service.CustomerInvoiceEmailService;
import poly.edu.quanlynhahang.service.OrderCheckoutService;
import poly.edu.quanlynhahang.service.OrderStateMachineService;
import poly.edu.quanlynhahang.service.KitchenOrderDetailService;
import poly.edu.quanlynhahang.dto.GuestBookingRequest;
import poly.edu.quanlynhahang.dto.SplitTableRequest;
import poly.edu.quanlynhahang.service.OrderFinancialMutationGuardService;
import poly.edu.quanlynhahang.service.TableSessionService;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.service.TableReleaseGuardService;
import poly.edu.quanlynhahang.service.InventoryReservationService;

class OrderWorkflowGuardTest {

    @Test
    void manualCashierPaymentConsumesInventoryHoldBeforeCompletingOrder() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        InventoryReservationService inventory = mock(InventoryReservationService.class);
        Order order = new Order();
        order.setId(31);
        order.setStatus(7);
        order.setIsPaid(false);
        order.setTotalAmount(new BigDecimal("108.00"));
        when(orderRepository.findLockedById(31)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "inventoryReservationService", inventory);
        ReflectionTestUtils.setField(controller, "orderStateMachineService", new OrderStateMachineService());
        ReflectionTestUtils.setField(controller, "activityLogService",
                mock(poly.edu.quanlynhahang.service.ActivityLogService.class));
        ReflectionTestUtils.setField(controller, "messagingTemplate",
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cashier", "n/a"));
        try {
            assertEquals(HttpStatus.OK, controller.payOrder(31).getStatusCode());
        } finally {
            SecurityContextHolder.clearContext();
        }
        verify(inventory).consume(31);
        assertEquals(4, order.getStatus());
    }

    @Test
    void repeatedCashierPaymentIsRejectedBeforeInventoryOrTableMutation() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        InventoryReservationService inventory = mock(InventoryReservationService.class);
        Order order = new Order();
        order.setId(32);
        order.setStatus(4);
        order.setIsPaid(true);
        order.setPaymentStatus(poly.edu.quanlynhahang.entity.PaymentStatus.PAID);
        when(orderRepository.findLockedById(32)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "inventoryReservationService", inventory);

        assertEquals(HttpStatus.CONFLICT, controller.payOrder(32).getStatusCode());
        verify(inventory, never()).consume(32);
    }

    @Test
    void rejectsDishStatusOutsideTheDefinedWorkflow() {
        OrderController controller = new OrderController();
        OrderDetailRepository detailRepository = mock(OrderDetailRepository.class);
        ReflectionTestUtils.setField(controller, "orderDetailRepository", detailRepository);

        assertEquals(HttpStatus.BAD_REQUEST,
                controller.updateOrderDetailStatus(11, 99).getStatusCode());
    }

    @Test
    void waiterServingAnOrderUsesTheServedStatusInsteadOfCancellation() {
        WaiterController controller = new WaiterController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Order order = new Order();
        order.setId(15);
        order.setStatus(2);
        OrderDetail ready = new OrderDetail();
        ready.setStatus(1);
        order.setOrderDetails(List.of(ready));
        when(orderRepository.findLockedById(15)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "orderStateMachineService", new OrderStateMachineService());

        assertEquals(HttpStatus.OK, controller.confirmServed(15).getStatusCode());
        assertEquals(7, order.getStatus());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void rejectsCompletingAnOrderWithoutAnyDishes() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Order order = new Order();
        order.setId(21);
        order.setOrderDetails(List.of());
        when(orderRepository.findById(21)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "orderStateMachineService", new OrderStateMachineService());

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.updateOrderStatus(21, 2));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void servingTheWholeTableMarksEveryReadyDishAsServed() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderDetail first = new OrderDetail();
        first.setStatus(1);
        OrderDetail second = new OrderDetail();
        second.setStatus(2);
        Order order = new Order();
        order.setId(22);
        order.setStatus(2);
        order.setOrderDetails(List.of(first, second));
        when(orderRepository.findById(22)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "activityLogService",
                mock(poly.edu.quanlynhahang.service.ActivityLogService.class));
        ReflectionTestUtils.setField(controller, "messagingTemplate",
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class));
        ReflectionTestUtils.setField(controller, "orderStateMachineService", new OrderStateMachineService());

        var response = controller.updateOrderStatus(22, 7);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, first.getStatus());
        assertEquals(2, second.getStatus());
        assertEquals(7, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void findsAnOpenOrderByItsTableIdWhenTheAddressFormatDiffers() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        RestaurantTable table = new RestaurantTable();
        table.setId(5);
        table.setName("B05");
        Order order = new Order();
        order.setId(23);
        order.setTableId(5);
        order.setAddress("Định dạng địa chỉ cũ");
        order.setStatus(7);
        order.setIsPaid(false);
        when(orderRepository.findOpenDineInOrdersByTableIdWithDetails(5)).thenReturn(List.of(order));
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);

        assertEquals(HttpStatus.OK, controller.getOpenDineInOrder(5).getStatusCode());
    }

    @Test
    void addingItemsStillSucceedsWhenWebSocketBroadcastFails() {
        OrderController controller = new OrderController();
        OrderCheckoutService checkoutService = mock(OrderCheckoutService.class);
        org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate =
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);
        doThrow(new RuntimeException("broker unavailable"))
                .when(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
        when(checkoutService.addItems(org.mockito.ArgumentMatchers.eq(18), any(), org.mockito.ArgumentMatchers.eq("key-18")))
                .thenReturn(new OrderCheckoutService.AddItemsResult(18, 1,
                        java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, java.math.BigDecimal.valueOf(11)));
        ReflectionTestUtils.setField(controller, "orderCheckoutService", checkoutService);
        ReflectionTestUtils.setField(controller, "messagingTemplate", messagingTemplate);

        var response = controller.addItemsToOrder(18, new poly.edu.quanlynhahang.dto.OrderRequest(), "key-18");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
        verify(messagingTemplate).convertAndSend("/topic/waiter", "DISH_STATUS_CHANGED");
    }

    @Test
    void publicDineInCheckoutNoLongerRequiresQrAndNotifiesKitchen() {
        OrderController controller = new OrderController();
        OrderCheckoutService checkoutService = mock(OrderCheckoutService.class);
        TableSessionService tableSessionService = mock(TableSessionService.class);
        org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate =
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class);
        poly.edu.quanlynhahang.dto.OrderRequest request = new poly.edu.quanlynhahang.dto.OrderRequest();
        request.setOrderType(OrderType.DINE_IN);
        request.setTableId(5);
        when(checkoutService.checkout(request, null, "checkout-key-5"))
                .thenReturn(new OrderCheckoutService.CheckoutResult(81, "ORD-DINEIN", 0,
                        BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
                        BigDecimal.ZERO, BigDecimal.TEN,
                        poly.edu.quanlynhahang.entity.OrderPaymentOption.PAY_AT_RESTAURANT,
                        poly.edu.quanlynhahang.entity.PaymentStatus.UNPAID, null));
        ReflectionTestUtils.setField(controller, "orderCheckoutService", checkoutService);
        ReflectionTestUtils.setField(controller, "tableSessionService", tableSessionService);
        ReflectionTestUtils.setField(controller, "messagingTemplate", messagingTemplate);
        SecurityContextHolder.clearContext();

        var response = controller.checkout(request, null, "checkout-key-5");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(tableSessionService, never()).requireForTable(any(), any());
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void rejectsServingADishThatTheKitchenHasNotCompleted() {
        OrderController controller = new OrderController();
        KitchenOrderDetailService kitchenService = mock(KitchenOrderDetailService.class);
        doThrow(new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "Món chưa hoàn thành"))
                .when(kitchenService).serve(44);
        ReflectionTestUtils.setField(controller, "kitchenOrderDetailService", kitchenService);

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.updateOrderDetailStatus(44, 2));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void splitRejectsAnUnknownDetailInsteadOfSilentlySkippingIt() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderDetailRepository detailRepository = mock(OrderDetailRepository.class);
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        RestaurantTable sourceTable = new RestaurantTable();
        sourceTable.setId(1);
        RestaurantTable targetTable = new RestaurantTable();
        targetTable.setId(2);
        targetTable.setActive(true);
        Order source = new Order();
        source.setId(10);
        source.setStatus(1);
        when(tableRepository.findLockedByIdIn(List.of(1, 2)))
                .thenReturn(List.of(sourceTable, targetTable));
        when(orderRepository.findOpenDineInOrdersByTableIdWithDetails(1)).thenReturn(List.of(source));
        when(orderRepository.findOpenDineInOrdersByTableIdWithDetails(2)).thenReturn(List.of());
        when(orderRepository.findLockedById(10)).thenReturn(Optional.of(source));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(11);
            return saved;
        });
        when(detailRepository.findAllById(any())).thenReturn(List.of());
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "orderDetailRepository", detailRepository);
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "orderFinancialMutationGuardService",
                mock(OrderFinancialMutationGuardService.class));
        ReflectionTestUtils.setField(controller, "orderStateMachineService", new OrderStateMachineService());

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.splitTable(new SplitTableRequest(1, 2, List.of(999))));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(detailRepository, never()).save(any(OrderDetail.class));
    }

    @Test
    void movingAnOrderLocksTablesRejectsConflictsAndRevokesOldAndTargetQrSessions() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        TableSessionService tableSessions = mock(TableSessionService.class);
        poly.edu.quanlynhahang.service.TableLifecycleService tableLifecycle =
                mock(poly.edu.quanlynhahang.service.TableLifecycleService.class);
        Order order = new Order();
        order.setId(20);
        order.setStatus(1);
        order.setOrderType(OrderType.DINE_IN);
        order.setTableId(1);
        RestaurantTable source = new RestaurantTable();
        source.setId(1);
        source.setIsOccupied(2);
        RestaurantTable target = new RestaurantTable();
        target.setId(2);
        target.setActive(true);
        target.setIsOccupied(0);
        when(orderRepository.findById(20)).thenReturn(Optional.of(order));
        when(orderRepository.findLockedById(20)).thenReturn(Optional.of(order));
        when(tableRepository.findLockedByIdIn(List.of(1, 2))).thenReturn(List.of(source, target));
        when(orderRepository.existsActiveOrderForTableExcludingOrder(2, 20)).thenReturn(false);
        when(orderRepository.existsActiveOrderForTableExcludingOrder(1, 20)).thenReturn(false);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "tableSessionService", tableSessions);
        org.mockito.Mockito.doAnswer(invocation -> {
            source.setIsOccupied(0);
            return source;
        }).when(tableLifecycle).release(1);
        ReflectionTestUtils.setField(controller, "tableLifecycleService", tableLifecycle);
        ReflectionTestUtils.setField(controller, "activityLogService",
                mock(poly.edu.quanlynhahang.service.ActivityLogService.class));

        assertEquals(HttpStatus.OK, controller.moveOrderToTable(20, 2).getStatusCode());
        assertEquals(2, order.getTableId());
        assertEquals(0, source.getIsOccupied());
        assertEquals(2, target.getIsOccupied());
        verify(tableSessions).revokeActiveForTable(1);
        verify(tableSessions).revokeActiveForTable(2);
        verify(tableRepository).findLockedByIdIn(List.of(1, 2));
    }

    @Test
    void rejectsMakingATableEmptyWhileItHasAnUnpaidOrder() {
        RestaurantTableController controller = new RestaurantTableController();
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        RestaurantTable table = new RestaurantTable();
        table.setId(8);
        when(tableRepository.findById(8)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenUnpaidOrderForTable(8)).thenReturn(true);
        TableReleaseGuardService releaseGuard = mock(TableReleaseGuardService.class);
        poly.edu.quanlynhahang.service.TableLifecycleService tableLifecycle =
                mock(poly.edu.quanlynhahang.service.TableLifecycleService.class);
        doThrow(new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "Bàn còn hóa đơn chưa thanh toán đủ"))
                .when(releaseGuard).prepareForRelease(8);
        doThrow(new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "Bàn còn hóa đơn chưa thanh toán đủ"))
                .when(tableLifecycle).release(8);
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "tableReleaseGuardService", releaseGuard);
        ReflectionTestUtils.setField(controller, "tableLifecycleService", tableLifecycle);

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.updateStatus(8, 0, null));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(tableRepository, never()).save(any(RestaurantTable.class));
    }

    @Test
    void rejectsMarkingATableForCleaningWhileItHasAnUnpaidOrder() {
        RestaurantTableController controller = new RestaurantTableController();
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        RestaurantTable table = new RestaurantTable();
        table.setId(9);
        table.setName("B09");
        table.setIsOccupied(2);
        when(tableRepository.findById(9)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenUnpaidOrderForTable(9)).thenReturn(true);
        TableReleaseGuardService releaseGuard = mock(TableReleaseGuardService.class);
        doThrow(new org.springframework.web.server.ResponseStatusException(
                HttpStatus.CONFLICT, "Bàn còn hóa đơn chưa thanh toán đủ"))
                .when(releaseGuard).prepareForRelease(9);
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "tableReleaseGuardService", releaseGuard);

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.updateStatus(9, 3, null));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(2, table.getIsOccupied());
        verify(tableRepository, never()).save(any(RestaurantTable.class));
    }

    @Test
    void allowsPaidTableToMoveFromOccupiedToCleaningAndThenEmpty() {
        RestaurantTableController controller = new RestaurantTableController();
        RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
        OrderRepository orderRepository = mock(OrderRepository.class);
        RestaurantTable table = new RestaurantTable();
        table.setId(10);
        table.setName("B10");
        table.setIsOccupied(2);
        when(tableRepository.findById(10)).thenReturn(Optional.of(table));
        when(orderRepository.existsOpenUnpaidOrderForTable(10)).thenReturn(false);
        ReflectionTestUtils.setField(controller, "tableRepository", tableRepository);
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "tableReleaseGuardService", mock(TableReleaseGuardService.class));
        poly.edu.quanlynhahang.service.TableLifecycleService tableLifecycle =
                mock(poly.edu.quanlynhahang.service.TableLifecycleService.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            table.setIsOccupied(0);
            return table;
        }).when(tableLifecycle).release(10);
        ReflectionTestUtils.setField(controller, "tableLifecycleService", tableLifecycle);

        var cleaningResponse = controller.updateStatus(10, 3, null);
        assertEquals(HttpStatus.OK, cleaningResponse.getStatusCode());
        assertEquals(3, table.getIsOccupied());

        var emptyResponse = controller.updateStatus(10, 0, null);
        assertEquals(HttpStatus.OK, emptyResponse.getStatusCode());
        assertEquals(0, table.getIsOccupied());
        verify(tableRepository, times(1)).save(table);
        verify(tableLifecycle).release(10);
    }

    @Test
    void recordsAnInvoiceRequestOnlyForThePaidOrderOwner() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Account account = new Account();
        account.setUsername("customer-owner");
        account.setEmail("owner@example.com");
        Order order = new Order();
        order.setAccount(account);
        order.setIsPaid(true);
        when(orderRepository.findLockedById(31)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        CustomerInvoiceEmailService invoiceEmailService = mock(CustomerInvoiceEmailService.class);
        when(invoiceEmailService.sendPaidInvoiceNotice(order, "owner@example.com"))
                .thenReturn(CustomerInvoiceEmailService.DeliveryStatus.NOT_CONFIGURED);
        ReflectionTestUtils.setField(controller, "customerInvoiceEmailService", invoiceEmailService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("customer-owner", null, List.of()));

        try {
            var response = controller.requestInvoice(31, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(order.getInvoiceRequested());
            assertEquals("owner@example.com", order.getInvoiceEmail());
            verify(orderRepository).save(order);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void rejectsInvoiceRequestForAnotherCustomerOrUnpaidOrder() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Account account = new Account();
        account.setUsername("customer-owner");
        account.setEmail("owner@example.com");
        Order order = new Order();
        order.setAccount(account);
        order.setIsPaid(true);
        when(orderRepository.findLockedById(32)).thenReturn(Optional.of(order));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("another-customer", null, List.of()));

        try {
            assertEquals(HttpStatus.FORBIDDEN, controller.requestInvoice(32, null).getStatusCode());
            verify(orderRepository, never()).save(any(Order.class));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void deprecatedGuestBookingCannotStoreGuestDataInOrderAddress() {
        OrderController controller = new OrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        when(orderRepository.findAll()).thenReturn(List.of());
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);

        var response = controller.guestBooking(
                new GuestBookingRequest("Nguyễn Thị An", "0901234567", "Bàn B01", "19:00"));

        assertEquals(HttpStatus.GONE, response.getStatusCode());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void scheduledActivationUsesIndexedStructuredTimeInsteadOfScanningOrParsingAddress() {
        AdminOrderController controller = new AdminOrderController();
        OrderRepository orderRepository = mock(OrderRepository.class);
        Order due = new Order();
        due.setId(41);
        due.setStatus(5);
        due.setAddress("free-form text that must not be parsed");
        when(orderRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                org.mockito.ArgumentMatchers.eq(5),
                any(java.time.LocalDateTime.class))).thenReturn(List.of(due));
        ReflectionTestUtils.setField(controller, "orderRepository", orderRepository);
        ReflectionTestUtils.setField(controller, "messagingTemplate",
                mock(org.springframework.messaging.simp.SimpMessagingTemplate.class));
        ReflectionTestUtils.setField(controller, "orderStateMachineService", new OrderStateMachineService());

        var response = controller.activateScheduledOrders();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, due.getStatus());
        verify(orderRepository, never()).findAll();
        verify(orderRepository).save(due);
    }
}
