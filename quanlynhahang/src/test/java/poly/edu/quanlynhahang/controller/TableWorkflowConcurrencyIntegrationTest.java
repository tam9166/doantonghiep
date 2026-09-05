package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import poly.edu.quanlynhahang.dto.MergeTablesRequest;
import poly.edu.quanlynhahang.dto.OrderResponse;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.OrderType;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.service.ActivityLogService;
import poly.edu.quanlynhahang.service.TableSessionService;

@SpringBootTest
class TableWorkflowConcurrencyIntegrationTest {
    @Autowired AdminOrderController adminOrderController;
    @Autowired OrderController orderController;
    @Autowired RestaurantTableRepository tableRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean TableSessionService tableSessionService;
    @MockitoBean ActivityLogService activityLogService;
    @MockitoBean SimpMessagingTemplate messagingTemplate;

    private String marker;

    @BeforeEach
    void authenticateWaiter() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "regression-waiter", "n/a", AuthorityUtils.createAuthorityList("ROLE_WAITER")));
    }

    @AfterEach
    void cleanup() {
        if (marker != null) {
            jdbc.update("DELETE FROM Orders WHERE order_code LIKE ?", marker + "%");
            jdbc.update("DELETE FROM restaurant_table WHERE name LIKE ?", marker + "%");
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    @Timeout(30)
    void twoTransfersCompetingForOneTargetAllowExactlyOneOrder() throws Exception {
        marker = marker();
        RestaurantTable firstSource = table("-TRANSFER-S1", 2);
        RestaurantTable secondSource = table("-TRANSFER-S2", 2);
        RestaurantTable target = table("-TRANSFER-T", 0);
        Order firstOrder = order("-TRANSFER-O1", firstSource.getId());
        Order secondOrder = order("-TRANSFER-O2", secondSource.getId());

        List<Attempt> attempts = race(
                () -> adminOrderController.moveOrderToTable(firstOrder.getId(), target.getId()),
                () -> adminOrderController.moveOrderToTable(secondOrder.getId(), target.getId()));

        assertEquals(1, attempts.stream().filter(Attempt::successful).count(), attempts::toString);
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM Orders WHERE table_id = ? AND status NOT IN (3, 4)",
                Integer.class, target.getId()));
        assertEquals(2, jdbc.queryForObject(
                "SELECT is_occupied FROM restaurant_table WHERE id = ?", Integer.class, target.getId()));
        verify(tableSessionService, times(1)).revokeActiveForTable(target.getId());
    }

    @Test
    @Timeout(30)
    void twoMergesCompetingForOneSourceApplyItExactlyOnce() throws Exception {
        marker = marker();
        RestaurantTable source = table("-MERGE-S", 2);
        RestaurantTable firstTarget = table("-MERGE-T1", 2);
        RestaurantTable secondTarget = table("-MERGE-T2", 2);
        Order sourceOrder = order("-MERGE-OS", source.getId());
        order("-MERGE-OT1", firstTarget.getId());
        order("-MERGE-OT2", secondTarget.getId());

        List<Attempt> attempts = race(
                () -> orderController.mergeTables(new MergeTablesRequest(source.getId(), firstTarget.getId())),
                () -> orderController.mergeTables(new MergeTablesRequest(source.getId(), secondTarget.getId())));

        assertEquals(1, attempts.stream().filter(Attempt::successful).count(), attempts::toString);
        assertEquals(3, jdbc.queryForObject(
                "SELECT status FROM Orders WHERE id = ?", Integer.class, sourceOrder.getId()));
        assertEquals(5, jdbc.queryForObject(
                "SELECT is_occupied FROM restaurant_table WHERE id = ?", Integer.class, source.getId()));
        verify(tableSessionService, times(1)).revokeActiveForTable(source.getId());
    }

    @Test
    void twoUnpaidTablesMergeIntoOneOpenInvoiceWithoutResendingKitchenTickets() {
        marker = marker();
        RestaurantTable source = table("-UNPAID-S", 2);
        RestaurantTable target = table("-UNPAID-T", 2);
        Order sourceOrder = financialOrder("-UNPAID-OS", source.getId(), "300000", "0", false, 0);
        Order targetOrder = financialOrder("-UNPAID-OT", target.getId(), "400000", "0", false, 0);

        ResponseEntity<?> response = orderController.mergeTables(
                new MergeTablesRequest(source.getId(), target.getId()));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new BigDecimal("700000.00"), jdbc.queryForObject(
                "SELECT total_amount FROM Orders WHERE id = ?", BigDecimal.class, targetOrder.getId()));
        assertEquals(new BigDecimal("700000"), jdbc.queryForObject(
                "SELECT remaining_amount FROM Orders WHERE id = ?", BigDecimal.class, targetOrder.getId()));
        assertEquals(3, jdbc.queryForObject(
                "SELECT status FROM Orders WHERE id = ?", Integer.class, sourceOrder.getId()));
        assertEquals(target.getId(), jdbc.queryForObject(
                "SELECT merged_into_table_id FROM restaurant_table WHERE id = ?", Integer.class, source.getId()));
        verify(messagingTemplate, never()).convertAndSend("/topic/kitchen", "TABLE_MERGED");
    }

    @Test
    void paidInvoiceRemainsImmutableAndAddOnsTargetTheOtherOpenInvoice() {
        marker = marker();
        RestaurantTable paidTable = table("-MIXED-PAID", 2);
        RestaurantTable openTable = table("-MIXED-OPEN", 2);
        Order paid = financialOrder("-MIXED-OP", paidTable.getId(), "500000", "500000", true, 4);
        Order open = financialOrder("-MIXED-OO", openTable.getId(), "300000", "0", false, 0);

        orderController.mergeTables(new MergeTablesRequest(paidTable.getId(), openTable.getId()));

        assertEquals(new BigDecimal("500000.00"), jdbc.queryForObject(
                "SELECT total_amount FROM Orders WHERE id = ?", BigDecimal.class, paid.getId()));
        assertEquals(new BigDecimal("500000"), jdbc.queryForObject(
                "SELECT paid_amount FROM Orders WHERE id = ?", BigDecimal.class, paid.getId()));
        assertEquals("PAID", jdbc.queryForObject(
                "SELECT payment_status FROM Orders WHERE id = ?", String.class, paid.getId()));
        assertEquals(4, jdbc.queryForObject(
                "SELECT status FROM Orders WHERE id = ?", Integer.class, paid.getId()));
        assertEquals(List.of(open.getId()), orderRepository
                .findOpenDineInOrdersByTableIdWithDetails(openTable.getId()).stream().map(Order::getId).toList());
    }

    @Test
    void twoPaidInvoicesRemainClosedWhileTheirTablesBecomeOneGroup() {
        marker = marker();
        RestaurantTable source = table("-PAID-S", 2);
        RestaurantTable target = table("-PAID-T", 2);
        Order sourceInvoice = financialOrder("-PAID-OS", source.getId(), "250000", "250000", true, 4);
        Order targetInvoice = financialOrder("-PAID-OT", target.getId(), "350000", "350000", true, 4);

        orderController.mergeTables(new MergeTablesRequest(source.getId(), target.getId()));

        assertEquals(new BigDecimal("250000.00"), jdbc.queryForObject(
                "SELECT total_amount FROM Orders WHERE id = ?", BigDecimal.class, sourceInvoice.getId()));
        assertEquals(new BigDecimal("350000.00"), jdbc.queryForObject(
                "SELECT total_amount FROM Orders WHERE id = ?", BigDecimal.class, targetInvoice.getId()));
        assertEquals(2L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM Orders WHERE id IN (?, ?) AND payment_status = 'PAID' AND status = 4",
                Long.class, sourceInvoice.getId(), targetInvoice.getId()));
        assertEquals(0, orderRepository.findOpenDineInOrdersByTableIdWithDetails(target.getId()).size());
        assertEquals(5, jdbc.queryForObject(
                "SELECT is_occupied FROM restaurant_table WHERE id = ?", Integer.class, source.getId()));
        assertEquals(2, jdbc.queryForObject(
                "SELECT is_occupied FROM restaurant_table WHERE id = ?", Integer.class, target.getId()));
    }

    @Test
    void dispatchToKitchenMapsLazyOrderDetailsInsideTheControllerTransaction() {
        marker = marker();
        RestaurantTable table = table("-DISPATCH", 0);
        Order pending = order("-DISPATCH-ORDER", table.getId());
        pending.setPaymentOption(OrderPaymentOption.PAY_AT_RESTAURANT);
        orderRepository.saveAndFlush(pending);

        ResponseEntity<?> response = adminOrderController.dispatchToKitchen(pending.getId());

        assertEquals(200, response.getStatusCode().value());
        OrderResponse body = (OrderResponse) response.getBody();
        assertEquals(pending.getId(), body.id());
        assertEquals(List.of(), body.orderDetails());
        assertEquals(1, jdbc.queryForObject(
                "SELECT status FROM Orders WHERE id = ?", Integer.class, pending.getId()));
    }

    private List<Attempt> race(ControllerCall firstCall, ControllerCall secondCall) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> callAfter(start, firstCall));
            Future<Attempt> second = executor.submit(() -> callAfter(start, secondCall));
            start.countDown();
            return new ArrayList<>(List.of(
                    first.get(20, TimeUnit.SECONDS),
                    second.get(20, TimeUnit.SECONDS)));
        } finally {
            executor.shutdownNow();
        }
    }

    private Attempt callAfter(CountDownLatch start, ControllerCall call) {
        try {
            start.await(10, TimeUnit.SECONDS);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    "regression-waiter", "n/a", AuthorityUtils.createAuthorityList("ROLE_WAITER")));
            ResponseEntity<?> response = call.execute();
            return new Attempt(response.getStatusCode().is2xxSuccessful(), response.getStatusCode().value(), null);
        } catch (Throwable failure) {
            return new Attempt(false, null, failure);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RestaurantTable table(String suffix, int occupied) {
        RestaurantTable table = new RestaurantTable();
        table.setName(marker + suffix);
        table.setFloor("REGRESSION");
        table.setIsOccupied(occupied);
        return tableRepository.saveAndFlush(table);
    }

    private Order order(String suffix, Integer tableId) {
        Order order = new Order();
        order.setOrderCode(marker + suffix);
        order.setOrderType(OrderType.DINE_IN);
        order.setTableId(tableId);
        order.setStatus(0);
        return orderRepository.saveAndFlush(order);
    }

    private Order financialOrder(String suffix, Integer tableId, String total, String paid,
                                 boolean fullyPaid, int status) {
        Order order = new Order();
        order.setOrderCode(marker + suffix);
        order.setOrderType(OrderType.DINE_IN);
        order.setTableId(tableId);
        order.setStatus(status);
        order.setOriginalSubtotal(new BigDecimal(total));
        order.setSubTotal(new BigDecimal(total));
        order.setTaxAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal(total));
        order.setPaidAmount(new BigDecimal(paid));
        order.setRemainingAmount(new BigDecimal(total).subtract(new BigDecimal(paid)));
        order.setIsPaid(fullyPaid);
        order.setPaymentStatus(fullyPaid ? PaymentStatus.PAID
                : new BigDecimal(paid).signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.UNPAID);
        return orderRepository.saveAndFlush(order);
    }

    private String marker() {
        return "REGT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @FunctionalInterface
    private interface ControllerCall {
        ResponseEntity<?> execute();
    }

    private record Attempt(boolean successful, Integer status, Throwable failure) {
        Attempt {
            if (successful) {
                assertNull(failure);
            }
        }
    }
}
