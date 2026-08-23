package poly.edu.quanlynhahang.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import poly.edu.quanlynhahang.dto.MergeTablesRequest;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderType;
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

    @AfterEach
    void cleanup() {
        if (marker != null) {
            jdbc.update("DELETE FROM Orders WHERE order_code LIKE ?", marker + "%");
            jdbc.update("DELETE FROM restaurant_table WHERE name LIKE ?", marker + "%");
        }
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
