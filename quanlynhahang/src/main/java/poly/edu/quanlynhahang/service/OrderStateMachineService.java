package poly.edu.quanlynhahang.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.OrderStatus;

/** Single authority for operational order-status transitions. */
@Service
public class OrderStateMachineService {
    private static final Map<OrderStatus, EnumSet<OrderStatus>> ALLOWED = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED,
                    OrderStatus.COMPLETED),
            OrderStatus.IN_PREPARATION, EnumSet.of(OrderStatus.READY, OrderStatus.PARTIALLY_READY,
                    OrderStatus.CANCELLED, OrderStatus.COMPLETED),
            OrderStatus.PARTIALLY_READY, EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.READY,
                    OrderStatus.CANCELLED, OrderStatus.COMPLETED),
            // A served/ready table can legitimately add a new dish.  Returning the
            // aggregate order to preparation exposes only the newly queued detail to
            // Kitchen; already served details retain their own terminal status.
            OrderStatus.READY, EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.SERVED,
                    OrderStatus.CANCELLED, OrderStatus.COMPLETED),
            OrderStatus.SCHEDULED, EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.CANCELLED,
                    OrderStatus.COMPLETED),
            OrderStatus.SERVED, EnumSet.of(OrderStatus.IN_PREPARATION, OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED),
            OrderStatus.COMPLETED, EnumSet.of(OrderStatus.CANCELLED),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));

    public OrderStatus current(Order order) {
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng");
        }
        return OrderStatus.fromCode(order.getStatus());
    }

    public void transition(Order order, Integer targetCode) {
        transition(order, OrderStatus.fromCode(targetCode));
    }

    public void initializeFrom(Order newOrder, Order sourceOrder) {
        initialize(newOrder, current(sourceOrder));
    }

    public void initialize(Order newOrder, OrderStatus initialStatus) {
        if (newOrder == null || newOrder.getId() != null) {
            throw conflict("Chỉ được khởi tạo trạng thái cho đơn hàng mới");
        }
        newOrder.setStatus(initialStatus.code());
    }

    public void transition(Order order, OrderStatus target) {
        OrderStatus source = current(order);
        if (source == target) {
            return;
        }
        if (!ALLOWED.get(source).contains(target)) {
            throw conflict("Không thể chuyển đơn từ " + source + " sang " + target);
        }
        validateDishState(order, target);
        if (target == OrderStatus.COMPLETED && !Boolean.TRUE.equals(order.getIsPaid())) {
            throw conflict("Không thể hoàn thành đơn khi chưa thanh toán đủ");
        }
        order.setStatus(target.code());
    }

    public void refreshFromDishStates(Order order) {
        OrderStatus current = current(order);
        if (current == OrderStatus.CANCELLED || current == OrderStatus.COMPLETED
                || current == OrderStatus.SERVED) {
            return;
        }
        List<OrderDetail> active = activeDetails(order);
        if (active.isEmpty()) {
            return;
        }
        boolean allReady = active.stream().allMatch(detail -> detail.getStatus() != null && detail.getStatus() >= 1);
        boolean anyReady = active.stream().anyMatch(detail -> Integer.valueOf(1).equals(detail.getStatus()));
        if (allReady && (current == OrderStatus.IN_PREPARATION || current == OrderStatus.PARTIALLY_READY)) {
            transition(order, OrderStatus.READY);
        } else if (anyReady && current == OrderStatus.IN_PREPARATION) {
            transition(order, OrderStatus.PARTIALLY_READY);
        }
    }

    private void validateDishState(Order order, OrderStatus target) {
        if (target != OrderStatus.READY && target != OrderStatus.SERVED
                && target != OrderStatus.PARTIALLY_READY) {
            return;
        }
        List<OrderDetail> active = activeDetails(order);
        if (active.isEmpty()) {
            throw conflict("Đơn chưa có món để chuyển trạng thái");
        }
        if (target == OrderStatus.READY
                && active.stream().anyMatch(detail -> detail.getStatus() == null || detail.getStatus() < 1)) {
            throw conflict("Chỉ được báo sẵn sàng khi tất cả món đã hoàn thành");
        }
        if (target == OrderStatus.SERVED
                && active.stream().anyMatch(detail -> !Integer.valueOf(2).equals(detail.getStatus()))) {
            throw conflict("Chỉ được báo đã phục vụ khi tất cả món đã được bưng");
        }
    }

    private List<OrderDetail> activeDetails(Order order) {
        if (order.getOrderDetails() == null) {
            return List.of();
        }
        return order.getOrderDetails().stream()
                .filter(detail -> !Integer.valueOf(3).equals(detail.getStatus()))
                .toList();
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
