package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.repository.OrderRepository;

/** Closes paid orders from earlier service days without bypassing payment rules. */
@Service
public class OverdueOrderService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<Integer> TERMINAL_STATUSES = List.of(
            OrderStatus.CANCELLED.code(), OrderStatus.COMPLETED.code());

    private final OrderRepository orderRepository;
    private final OrderStateMachineService stateMachine;
    private final ActivityLogService activityLogService;
    private final PointsLedgerService pointsLedgerService;

    public OverdueOrderService(OrderRepository orderRepository,
                               OrderStateMachineService stateMachine,
                               ActivityLogService activityLogService,
                               PointsLedgerService pointsLedgerService) {
        this.orderRepository = orderRepository;
        this.stateMachine = stateMachine;
        this.activityLogService = activityLogService;
        this.pointsLedgerService = pointsLedgerService;
    }

    @Scheduled(cron = "${restaurant.order.overdue-cron:0 15 2 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int autoCompletePaidOverdueOrders() {
        LocalDateTime cutoff = LocalDate.now(BUSINESS_ZONE).atStartOfDay();
        Date dateCutoff = Date.from(cutoff.atZone(BUSINESS_ZONE).toInstant());
        int completed = 0;
        for (Order order : orderRepository.findOverdueOrders(TERMINAL_STATUSES, cutoff, dateCutoff)) {
            if (!isPaid(order)) {
                continue;
            }
            stateMachine.transition(order, OrderStatus.COMPLETED);
            orderRepository.save(order);
            awardPoints(order);
            activityLogService.log("SYSTEM_AUTO_UPDATE", "Order", String.valueOf(order.getId()),
                    "SYSTEM AUTO UPDATE: tự động hoàn tất đơn quá ngày phục vụ");
            completed++;
        }
        return completed;
    }

    private boolean isPaid(Order order) {
        return Boolean.TRUE.equals(order.getIsPaid()) || PaymentStatus.PAID.equals(order.getPaymentStatus());
    }

    private void awardPoints(Order order) {
        if (order.getAccount() == null) return;
        int points = (order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .divideToIntegralValue(BigDecimal.valueOf(10_000)).intValue();
        if (points > 0) {
            pointsLedgerService.credit(order.getAccount().getUsername(), PointsEventType.ORDER_COMPLETED,
                    "ORDER_COMPLETED:" + order.getId(), points,
                    "Thưởng điểm đơn tự động hoàn tất #" + order.getId());
        }
    }
}
