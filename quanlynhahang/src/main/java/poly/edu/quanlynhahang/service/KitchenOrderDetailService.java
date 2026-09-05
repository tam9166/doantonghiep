package poly.edu.quanlynhahang.service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.OrderDetail;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderStatus;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.entity.InventoryReservationStatus;
import poly.edu.quanlynhahang.repository.OrderDetailRepository;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.RecipeRepository;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;

@Service
public class KitchenOrderDetailService {
    private final OrderDetailRepository orderDetailRepository;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final RecipeRepository recipeRepository;
    private final RefundTransactionRepository refundRepository;
    private final InventoryReservationService inventoryReservationService;
    private final OrderStateMachineService orderStateMachineService;
    private final OrderServiceDateGuardService serviceDateGuard;

    public KitchenOrderDetailService(OrderDetailRepository orderDetailRepository,
                                     ActivityLogService activityLogService,
                                     SimpMessagingTemplate messagingTemplate,
                                     OrderRepository orderRepository,
                                     RecipeRepository recipeRepository,
                                     RefundTransactionRepository refundRepository,
                                     InventoryReservationService inventoryReservationService,
                                     OrderStateMachineService orderStateMachineService,
                                     OrderServiceDateGuardService serviceDateGuard) {
        this.orderDetailRepository = orderDetailRepository;
        this.activityLogService = activityLogService;
        this.messagingTemplate = messagingTemplate;
        this.orderRepository = orderRepository;
        this.recipeRepository = recipeRepository;
        this.refundRepository = refundRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.orderStateMachineService = orderStateMachineService;
        this.serviceDateGuard = serviceDateGuard;
    }

    @Transactional
    public OrderDetail start(Integer detailId) {
        OrderDetail detail = detail(detailId);
        serviceDateGuard.assertPreparationReached(detail.getOrder());
        if (isCancelled(detail) || isReadyOrServed(detail)) {
            throw conflict("Món không còn ở trạng thái chờ chế biến");
        }
        if (detail.getStartedAt() != null) {
            throw conflict("Món đã được bắt đầu chế biến");
        }
        detail.setStartedAt(new Date());
        OrderDetail saved = orderDetailRepository.save(detail);
        Order order = detail.getOrder();
        if (order != null) {
            poly.edu.quanlynhahang.entity.OrderStatus current = orderStateMachineService.current(order);
            if (current == poly.edu.quanlynhahang.entity.OrderStatus.PENDING
                    || current == poly.edu.quanlynhahang.entity.OrderStatus.SCHEDULED) {
                orderStateMachineService.transition(
                        order, poly.edu.quanlynhahang.entity.OrderStatus.IN_PREPARATION);
                orderRepository.save(order);
            }
        }
        publish("DISH_STARTED", saved);
        return saved;
    }

    @Transactional
    public OrderDetail complete(Integer detailId) {
        OrderDetail detail = detail(detailId);
        serviceDateGuard.assertPreparationReached(detail.getOrder());
        if (isCancelled(detail) || isReadyOrServed(detail)) {
            throw conflict("Món không còn ở trạng thái có thể hoàn thành");
        }
        if (detail.getStartedAt() == null) {
            throw conflict("Cần bắt đầu chế biến trước khi hoàn thành món");
        }
        detail.setStatus(1);
        detail.setCompletedAt(new Date());
        OrderDetail saved = orderDetailRepository.save(detail);
        if (detail.getOrder() != null) {
            orderStateMachineService.refreshFromDishStates(detail.getOrder());
            orderRepository.save(detail.getOrder());
        }
        publish("DISH_READY", saved);
        return saved;
    }

    @Transactional
    public OrderDetail serve(Integer detailId) {
        OrderDetail detail = orderDetailRepository.findLockedWithOrderAndProductById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món trong đơn"));
        serviceDateGuard.assertPreparationReached(detail.getOrder());
        if (!Integer.valueOf(1).equals(detail.getStatus()) || detail.getCompletedAt() == null) {
            throw conflict("Món này đã hoàn thành hoặc đã được xử lý.");
        }
        Order order = detail.getOrder();
        boolean completesServing = order != null && order.getOrderDetails() != null
                && order.getOrderDetails().stream()
                        .filter(item -> !Integer.valueOf(3).equals(item.getStatus()))
                        .filter(item -> !detailId.equals(item.getId()))
                        .allMatch(item -> Integer.valueOf(2).equals(item.getStatus()));
        OrderStatus current = order == null ? null : orderStateMachineService.current(order);
        if (completesServing && current != OrderStatus.COMPLETED && current != OrderStatus.READY) {
            throw conflict("Trạng thái đơn không cho phép hoàn tất phục vụ món này");
        }
        detail.setStatus(2);
        OrderDetail saved = orderDetailRepository.save(detail);
        if (completesServing && current != OrderStatus.COMPLETED) {
            orderStateMachineService.transition(order, OrderStatus.SERVED);
            orderRepository.save(order);
        }
        publish("DISH_SERVED", saved);
        return saved;
    }

    @Transactional
    public OrderDetail cancel(Integer detailId, String reason, String cancelledBy) {
        OrderDetail detail = orderDetailRepository.findLockedWithOrderAndProductById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món trong đơn"));
        Order order = orderRepository.findLockedById(detail.getOrder().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (isCancelled(detail) || isReadyOrServed(detail)) {
            throw conflict("Món không còn ở trạng thái có thể hủy");
        }
        Map<Long, BigDecimal> requirements = cancellationRequirements(detail);
        inventoryReservationService.adjustForCancelledItem(
                order.getId(), requirements, detail.getStartedAt() == null);
        detail.setStatus(3);
        detail.setCancelledAt(new Date());
        detail.setCancelReason(reason.trim());
        detail.setCancelledBy(cancelledBy == null || cancelledBy.isBlank() ? "SYSTEM" : cancelledBy);
        OrderDetail saved = orderDetailRepository.save(detail);
        recalculateOrderAndRefund(order, detail, saved.getCancelledBy());
        publish("DISH_CANCELLED", saved);
        return saved;
    }

    private Map<Long, BigDecimal> cancellationRequirements(OrderDetail detail) {
        Map<Long, BigDecimal> requirements = new LinkedHashMap<>();
        recipeRepository.findByProduct(detail.getProduct()).forEach(recipe -> {
            if (recipe.getIngredient() != null && recipe.getIngredient().getId() != null
                    && recipe.getAmountRequired() != null && recipe.getAmountRequired().signum() > 0) {
                BigDecimal amount = recipe.getAmountRequired().multiply(BigDecimal.valueOf(detail.getQuantity()));
                requirements.merge(recipe.getIngredient().getId(), amount, BigDecimal::add);
            }
        });
        if (requirements.isEmpty()) {
            throw conflict("Món không có công thức để điều chỉnh tồn kho an toàn");
        }
        return requirements;
    }

    private void recalculateOrderAndRefund(Order order, OrderDetail detail, String processedBy) {
        BigDecimal subTotal = money(order.getSubTotal()).subtract(money(detail.getPrice())).max(BigDecimal.ZERO);
        BigDecimal tax = money(order.getTaxAmount()).subtract(money(detail.getTaxAmount())).max(BigDecimal.ZERO);
        BigDecimal total = subTotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal paid = order.getPaidAmount() == null ? BigDecimal.ZERO : order.getPaidAmount();
        BigDecimal roundedTotal = total.setScale(0, RoundingMode.HALF_UP);
        BigDecimal alreadyRefundedOrPending = refundRepository.findByOrderIdOrderByCreatedAtDesc(order.getId()).stream()
                .filter(refund -> RefundTransaction.RefundStatus.PENDING.equals(refund.getStatus())
                        || RefundTransaction.RefundStatus.COMPLETED.equals(refund.getStatus()))
                .map(RefundTransaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refundAmount = paid.subtract(roundedTotal).subtract(alreadyRefundedOrPending)
                .max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);

        order.setSubTotal(subTotal);
        order.setTaxAmount(tax);
        order.setTotalAmount(total);
        order.setRemainingAmount(roundedTotal.subtract(paid).max(BigDecimal.ZERO));
        if (refundAmount.signum() > 0) {
            RefundTransaction refund = new RefundTransaction();
            refund.setOrderId(order.getId());
            refund.setAmount(refundAmount);
            refund.setForfeitedAmount(BigDecimal.ZERO);
            refund.setReason(RefundTransaction.RefundReason.CANCELLED_BY_RESTAURANT);
            refund.setReasonDetail("Điều chỉnh do bếp hủy món #" + detail.getId());
            refund.setStatus(RefundTransaction.RefundStatus.PENDING);
            refund.setProcessedBy(processedBy);
            refundRepository.save(refund);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        } else if (alreadyRefundedOrPending.signum() == 0) {
            order.setPaymentStatus(paymentStatus(paid, roundedTotal));
        }
        if (orderDetailRepository.countByOrderIdAndStatusNot(order.getId(), 3) == 0) {
            orderStateMachineService.transition(order, poly.edu.quanlynhahang.entity.OrderStatus.CANCELLED);
            inventoryReservationService.release(order.getId(), InventoryReservationStatus.RELEASED);
        }
        orderRepository.save(order);
    }

    private PaymentStatus paymentStatus(BigDecimal paid, BigDecimal total) {
        if (paid.signum() <= 0) return PaymentStatus.UNPAID;
        int comparison = paid.compareTo(total);
        if (comparison < 0) return PaymentStatus.PARTIALLY_PAID;
        if (comparison == 0) return PaymentStatus.PAID;
        return PaymentStatus.OVERPAID;
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private OrderDetail detail(Integer detailId) {
        return orderDetailRepository.findById(detailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món trong đơn"));
    }

    private boolean isCancelled(OrderDetail detail) {
        return Integer.valueOf(3).equals(detail.getStatus());
    }

    private boolean isReadyOrServed(OrderDetail detail) {
        return detail.getStatus() != null && detail.getStatus() >= 1;
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private void publish(String event, OrderDetail detail) {
        String detailId = String.valueOf(detail.getId());
        activityLogService.log(event, "OrderDetail", detailId, event + " cho món #" + detailId);
        Runnable send = () -> {
            messagingTemplate.convertAndSend("/topic/kitchen", event);
            messagingTemplate.convertAndSend("/topic/waiter", event);
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }
}
