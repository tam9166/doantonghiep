package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentDirection;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.entity.RefundTransaction.RefundReason;
import poly.edu.quanlynhahang.entity.RefundTransaction.RefundStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;

@Service
public class OrderRefundService {
    private final OrderRepository orderRepository;
    private final RefundTransactionRepository refundRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PointsLedgerService pointsLedgerService;
    private final ActivityLogService activityLogService;
    private final TableLifecycleService tableLifecycleService;
    private final InventoryReservationService inventoryReservationService;
    private final OrderStateMachineService orderStateMachineService;
    private final RefundService refundService;

    public OrderRefundService(OrderRepository orderRepository,
                              RefundTransactionRepository refundRepository,
                              PaymentTransactionRepository paymentTransactionRepository,
                              PointsLedgerService pointsLedgerService,
                              ActivityLogService activityLogService,
                              TableLifecycleService tableLifecycleService,
                              InventoryReservationService inventoryReservationService,
                              OrderStateMachineService orderStateMachineService,
                              RefundService refundService) {
        this.orderRepository = orderRepository;
        this.refundRepository = refundRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.pointsLedgerService = pointsLedgerService;
        this.activityLogService = activityLogService;
        this.tableLifecycleService = tableLifecycleService;
        this.inventoryReservationService = inventoryReservationService;
        this.orderStateMachineService = orderStateMachineService;
        this.refundService = refundService;
    }

    @Transactional
    public OrderRefundResult cancelAndRequestRefund(Integer orderId, String processedBy) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));

        RefundTransaction activeRefund = refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(refund -> RefundStatus.PENDING.equals(refund.getStatus())
                        || RefundStatus.COMPLETED.equals(refund.getStatus()))
                .max(Comparator.comparing(RefundTransaction::getCreatedAt))
                .orElse(null);
        if (activeRefund != null) {
            return result(order, activeRefund, true);
        }

        reversePoints(order);
        inventoryReservationService.release(orderId, InventoryReservationStatus.RELEASED);
        orderStateMachineService.transition(order, poly.edu.quanlynhahang.entity.OrderStatus.CANCELLED);
        BigDecimal refundable = refundablePaidAmount(order);
        if (refundable.signum() <= 0) {
            order.setPaymentStatus(PaymentStatus.CANCELLED);
            order.setRemainingAmount(BigDecimal.ZERO);
            orderRepository.save(order);
            releaseTable(order);
            activityLogService.log("CANCEL_ORDER", "Order", String.valueOf(orderId),
                    "Hủy đơn không phát sinh hoàn tiền");
            return new OrderRefundResult(orderId, BigDecimal.ZERO, PaymentStatus.CANCELLED,
                    null, false, "Hủy đơn thành công, không có khoản thanh toán cần hoàn");
        }

        RefundTransaction refund = new RefundTransaction();
        refund.setOrderId(orderId);
        refund.setAmount(refundable);
        refund.setForfeitedAmount(BigDecimal.ZERO);
        refund.setReason(RefundReason.CANCELLED_BY_RESTAURANT);
        refund.setReasonDetail("Đơn hàng bị hủy bởi nhân sự");
        refund.setStatus(RefundStatus.PENDING);
        refund.setProcessedBy(processedBy);
        refund.setCreatedAt(new Date());
        RefundTransaction saved = refundRepository.save(refund);

        order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        order.setRemainingAmount(BigDecimal.ZERO);
        orderRepository.save(order);
        activityLogService.log("REQUEST_ORDER_REFUND", "Order", String.valueOf(orderId),
                "Tạo yêu cầu hoàn tiền " + refundable + ", chờ cổng thanh toán xác nhận");
        return result(order, saved, false);
    }

    private BigDecimal refundablePaidAmount(Order order) {
        if (OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
            List<PaymentTransaction> ledger = paymentTransactionRepository
                    .findByAggregateTypeAndAggregateIdAndStatus(
                            "ORDER", order.getId().longValue(), PaymentTransactionStatus.SUCCESS);
            return ledger.stream().reduce(BigDecimal.ZERO,
                    (total, transaction) -> PaymentDirection.REFUND.equals(transaction.getDirection())
                            ? total.subtract(transaction.getAmount())
                            : total.add(transaction.getAmount()),
                    BigDecimal::add).max(BigDecimal.ZERO);
        }
        boolean manuallyConfirmed = order.getPaymentConfirmedAt() != null
                && (PaymentStatus.PAID.equals(order.getPaymentStatus())
                        || PaymentStatus.OVERPAID.equals(order.getPaymentStatus())
                        || PaymentStatus.PARTIALLY_PAID.equals(order.getPaymentStatus()));
        return manuallyConfirmed && order.getPaidAmount() != null
                ? order.getPaidAmount().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
    }

    private void reversePoints(Order order) {
        if (order.getAccount() == null || order.getId() == null) return;
        pointsLedgerService.reverseIfPresent(
                order.getAccount().getUsername(),
                "ORDER_COMPLETED:" + order.getId(),
                "ORDER_REFUNDED:" + order.getId(),
                "Thu hồi điểm do hủy/hoàn tiền đơn #" + order.getId());
    }

    private void releaseTable(Order order) {
        if (order.getTableId() == null) return;
        tableLifecycleService.release(order.getTableId());
    }

    @Transactional
    public OrderRefundResult completeRefund(Integer orderId, String providerReference,
                                            String note, String processedBy) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        RefundTransaction refund = refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .filter(candidate -> RefundStatus.PENDING.equals(candidate.getStatus())
                        || RefundStatus.COMPLETED.equals(candidate.getStatus()))
                .max(Comparator.comparing(RefundTransaction::getCreatedAt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Đơn không có yêu cầu hoàn tiền đang chờ"));
        boolean idempotent = RefundStatus.COMPLETED.equals(refund.getStatus());
        RefundTransaction completed = refundService.confirmCompleted(
                refund.getId(), providerReference, note, processedBy);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);
        releaseTable(order);
        return result(order, completed, idempotent);
    }

    private OrderRefundResult result(Order order, RefundTransaction refund, boolean idempotent) {
        return new OrderRefundResult(order.getId(), refund.getAmount(), order.getPaymentStatus(),
                refund.getStatus(), idempotent,
                "Yêu cầu hoàn tiền đã được ghi nhận và đang chờ xử lý");
    }

    public record OrderRefundResult(Integer orderId,
                                    BigDecimal refundAmount,
                                    PaymentStatus paymentStatus,
                                    RefundStatus refundStatus,
                                    boolean idempotent,
                                    String message) {
    }
}
