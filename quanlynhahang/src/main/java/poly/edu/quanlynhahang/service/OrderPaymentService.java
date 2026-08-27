package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.config.PaymentProperties;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.entity.Order;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.repository.OrderRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderPaymentService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PaymentIntentRepository intentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProperties properties;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestaurantTableRepository tableRepository;
    private final InventoryReservationService inventoryReservationService;
    private final OrderStateMachineService orderStateMachineService;

    public OrderPaymentService(PaymentIntentRepository intentRepository,
                               OrderRepository orderRepository,
                               PaymentProperties properties,
                               ActivityLogService activityLogService,
                               SimpMessagingTemplate messagingTemplate,
                               RestaurantTableRepository tableRepository,
                               InventoryReservationService inventoryReservationService,
                               OrderStateMachineService orderStateMachineService) {
        this.intentRepository = intentRepository;
        this.orderRepository = orderRepository;
        this.properties = properties;
        this.activityLogService = activityLogService;
        this.messagingTemplate = messagingTemplate;
        this.tableRepository = tableRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.orderStateMachineService = orderStateMachineService;
    }

    @Transactional
    public PaymentQrResponse createForOrder(Order order) {
        validatePayableOrder(order);
        if (!OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
            return null;
        }

        return createOrReuse(order, "ORDER-CHECKOUT-" + order.getId(),
                hash("CREATE|ORDER|" + order.getId() + "|FULL|" + payableAmount(order)));
    }

    @Transactional
    public PaymentQrResponse createForExistingOrder(Integer orderId) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        validatePayableOrder(order);
        if (Integer.valueOf(3).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể tạo QR cho đơn đã hủy");
        }
        if (Boolean.TRUE.equals(order.getIsPaid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng đã thanh toán");
        }

        if (!OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
            order.setPaymentOption(OrderPaymentOption.PREPAID_TRANSFER);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setPaidAmount(BigDecimal.ZERO);
            order.setRemainingAmount(payableAmount(order));
            orderRepository.save(order);
        }

        String requestHash = hash("CREATE|ORDER|" + order.getId() + "|FULL|" + payableAmount(order));
        return createOrReuse(order, "ORDER-QR-" + order.getId() + "-" + UUID.randomUUID(), requestHash);
    }

    @Transactional
    public PaymentQrResponse regenerate(Integer orderId, String paymentCode, String idempotencyKey) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        validatePayableOrder(order);
        if (Integer.valueOf(3).equals(order.getStatus()) || Boolean.TRUE.equals(order.getIsPaid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể tạo lại QR cho đơn đã hủy hoặc đã thanh toán");
        }

        PaymentIntent existing = intentRepository.findLockedByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy giao dịch thanh toán"));
        if (existing.getOrder() == null || !orderId.equals(existing.getOrder().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Giao dịch không thuộc hóa đơn này");
        }

        String requestHash = hash("REGENERATE|ORDER|" + orderId + "|" + paymentCode);
        Optional<PaymentIntent> idempotent = intentRepository.findByIdempotencyKey(normalizedKey);
        if (idempotent.isPresent()) {
            PaymentIntent replacement = idempotent.get();
            if (replacement.getOrder() == null
                    || !orderId.equals(replacement.getOrder().getId())
                    || replacement.getRequestHash() == null
                    || !MessageDigest.isEqual(replacement.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                            requestHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT");
            }
            return toResponse(replacement);
        }
        if (PaymentStatus.PAID.equals(existing.getStatus())
                || PaymentStatus.OVERPAID.equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã thanh toán");
        }
        if (existing.getPaidAmount() != null && existing.getPaidAmount().signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Giao dịch đã nhận một phần tiền, cần đối soát thủ công");
        }
        if (PaymentStatus.REPLACED.equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PAYMENT_INTENT_ALREADY_REPLACED");
        }
        if (PaymentStatus.CANCELLED.equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã bị hủy");
        }

        existing.setStatus(PaymentStatus.REPLACED);
        intentRepository.saveAndFlush(existing);
        inventoryReservationService.renew(orderId, nextExpiry());
        PaymentIntent replacement = createIntent(order, normalizedKey, requestHash);
        existing.setReplacedById(replacement.getId());
        intentRepository.save(existing);
        activityLogService.log("REGENERATE_QR", "PaymentIntent", String.valueOf(existing.getId()),
                "Tạo lại QR cho đơn hàng #" + orderId);
        return toResponse(replacement);
    }

    private PaymentQrResponse createOrReuse(Order order, String idempotencyKey, String requestHash) {
        PaymentIntent partiallyPaid = intentRepository
                .findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
                        order.getId(), PaymentOption.FULL, PaymentStatus.PARTIALLY_PAID)
                .orElse(null);
        if (partiallyPaid != null) {
            return toResponse(partiallyPaid);
        }

        PaymentIntent active = intentRepository
                .findFirstByOrderIdAndPaymentOptionAndStatusOrderByCreatedAtDesc(
                        order.getId(), PaymentOption.FULL, PaymentStatus.PENDING)
                .orElse(null);
        if (active != null && active.getExpiresAt() != null && active.getExpiresAt().after(new Date())) {
            return toResponse(active);
        }
        if (active != null) {
            active.setStatus(PaymentStatus.EXPIRED);
            intentRepository.saveAndFlush(active);
        }
        inventoryReservationService.renew(order.getId(), nextExpiry());
        return toResponse(createIntent(order, idempotencyKey, requestHash));
    }

    private PaymentIntent createIntent(Order order, String idempotencyKey, String requestHash) {
        BigDecimal amount = payableAmount(order);
        PaymentIntent intent = new PaymentIntent();
        intent.setOrder(order);
        intent.setReservation(null);
        intent.setAggregateType("ORDER");
        intent.setAggregateId(order.getId().longValue());
        intent.setAggregateCode("ORDER-" + order.getId());
        intent.setPurpose("FULL");
        intent.setPaymentCode(nextPaymentCode());
        intent.setPaymentOption(PaymentOption.FULL);
        intent.setStatus(PaymentStatus.PENDING);
        intent.setAmount(amount);
        intent.setPaidAmount(BigDecimal.ZERO);
        intent.setRemainingAmount(amount);
        intent.setCurrency("VND");
        intent.setBankCode(properties.getBankCode().trim().toUpperCase(Locale.ROOT));
        intent.setBankBin(properties.getBankBin());
        intent.setAccountNumber(properties.getAccountNumber().trim());
        intent.setAccountHolder(properties.getAccountHolder().trim());
        intent.setQrProvider(properties.getQrProvider().trim().toUpperCase(Locale.ROOT));
        intent.setTransferContent(transferContent(order.getId(), intent.getPaymentCode()));
        intent.setExpiresAt(nextExpiry());
        intent.setIdempotencyKey(idempotencyKey);
        intent.setRequestHash(requestHash);
        intent.setCreatedBy(order.getAccount() == null ? "GUEST" : order.getAccount().getUsername());
        intent.setQrUrl(vietQrUrl(intent));
        PaymentIntent saved = intentRepository.save(intent);
        activityLogService.log("CREATE_PAYMENT_INTENT", "Order", String.valueOf(order.getId()),
                "Tạo QR thanh toán cho đơn hàng");
        return saved;
    }

    @Transactional
    public Order confirmManualDispatch(Integer orderId) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        if (Integer.valueOf(poly.edu.quanlynhahang.entity.OrderStatus.CANCELLED.code()).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể chuyển đơn đã hủy xuống bếp");
        }
        if (Integer.valueOf(poly.edu.quanlynhahang.entity.OrderStatus.IN_PREPARATION.code()).equals(order.getStatus())
                || Integer.valueOf(poly.edu.quanlynhahang.entity.OrderStatus.PARTIALLY_READY.code()).equals(order.getStatus())
                || Integer.valueOf(poly.edu.quanlynhahang.entity.OrderStatus.READY.code()).equals(order.getStatus())
                || Integer.valueOf(poly.edu.quanlynhahang.entity.OrderStatus.SERVED.code()).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đã được chuyển xuống bếp trước đó");
        }
        if (order.getPaymentOption() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đơn legacy thiếu hình thức thanh toán; vui lòng cập nhật trước khi chuyển bếp");
        }
        if (OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
            if (!PaymentStatus.PAID.equals(order.getPaymentStatus())
                    && !PaymentStatus.OVERPAID.equals(order.getPaymentStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Đơn chuyển khoản chưa được xác nhận đủ tiền");
            }
        } else if (!OrderPaymentOption.COD.equals(order.getPaymentOption())
                && !OrderPaymentOption.PAY_AT_RESTAURANT.equals(order.getPaymentOption())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn này không dùng luồng xác nhận thủ công");
        }
        if (!Integer.valueOf(0).equals(order.getStatus()) && !Integer.valueOf(5).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái đơn không cho phép xác nhận");
        }
        inventoryReservationService.consume(orderId);
        orderStateMachineService.transition(order, poly.edu.quanlynhahang.entity.OrderStatus.IN_PREPARATION);
        Order saved = orderRepository.save(order);
        if (order.getTableId() != null) {
            tableRepository.findById(order.getTableId()).ifPresent(table -> {
                table.setIsOccupied(2);
                table.setReservedTime("Đơn đang phục vụ: #" + order.getId());
                tableRepository.save(table);
            });
        }
        activityLogService.log("MANUAL_ORDER_CONFIRM", "Order", String.valueOf(orderId),
                "Xác nhận thủ công đơn COD/tại quán và chuyển xuống bếp");
        messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
        return saved;
    }

    @Transactional
    public Order applyLedgerPayment(Integer orderId, BigDecimal aggregatePaid, PaymentStatus status) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        BigDecimal total = money(order.getTotalAmount()).setScale(0, RoundingMode.HALF_UP);
        order.setPaidAmount(aggregatePaid.max(BigDecimal.ZERO));
        order.setRemainingAmount(total.subtract(aggregatePaid).max(BigDecimal.ZERO));
        PaymentStatus effectiveStatus = paymentStatus(aggregatePaid, total);
        order.setPaymentStatus(effectiveStatus);
        boolean fullyPaid = PaymentStatus.PAID.equals(effectiveStatus)
                || PaymentStatus.OVERPAID.equals(effectiveStatus);
        order.setIsPaid(fullyPaid);
        if (fullyPaid && Integer.valueOf(0).equals(order.getStatus())) {
            inventoryReservationService.consume(orderId);
            orderStateMachineService.transition(order, poly.edu.quanlynhahang.entity.OrderStatus.IN_PREPARATION);
            messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
        }
        return orderRepository.save(order);
    }

    @Transactional
    public void expireInventoryHold(Integer orderId) {
        inventoryReservationService.release(orderId,
                poly.edu.quanlynhahang.entity.InventoryReservationStatus.EXPIRED);
    }

    private Date nextExpiry() {
        return Date.from(Instant.now().plusSeconds(properties.getQrExpirationMinutes() * 60L));
    }

    private void validatePayableOrder(Order order) {
        if (order == null || order.getId() == null || order.getTotalAmount() == null
                || order.getTotalAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng chưa có tổng tiền hợp lệ");
        }
    }

    private BigDecimal payableAmount(Order order) {
        return money(order.getTotalAmount()).setScale(0, RoundingMode.HALF_UP);
    }

    private PaymentStatus paymentStatus(BigDecimal paidAmount, BigDecimal expectedAmount) {
        int comparison = paidAmount.compareTo(expectedAmount);
        if (paidAmount.signum() <= 0) return PaymentStatus.UNPAID;
        if (comparison < 0) return PaymentStatus.PARTIALLY_PAID;
        if (comparison == 0) return PaymentStatus.PAID;
        return PaymentStatus.OVERPAID;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REQUIRED");
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() < 8 || normalized.length() > 100) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_INVALID");
        }
        return normalized;
    }

    private String nextPaymentCode() {
        byte[] bytes = new byte[18];
        SECURE_RANDOM.nextBytes(bytes);
        return "PAY-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toUpperCase(Locale.ROOT);
    }

    private String transferContent(Integer orderId, String paymentCode) {
        String suffix = paymentCode.replace("PAY-", "");
        return "TT DH" + orderId + " " + suffix.substring(0, Math.min(8, suffix.length()));
    }

    private String vietQrUrl(PaymentIntent intent) {
        String addInfo = URLEncoder.encode(intent.getTransferContent(), StandardCharsets.UTF_8);
        String accountName = URLEncoder.encode(intent.getAccountHolder(), StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/" + intent.getBankCode() + "-" + intent.getAccountNumber()
                + "-compact2.png?amount=" + intent.getAmount().toPlainString()
                + "&addInfo=" + addInfo + "&accountName=" + accountName;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private PaymentQrResponse toResponse(PaymentIntent intent) {
        PaymentQrResponse response = new PaymentQrResponse();
        response.setPaymentCode(intent.getPaymentCode());
        response.setAmount(intent.getAmount());
        response.setPaymentOption(intent.getPaymentOption());
        response.setStatus(intent.getStatus());
        response.setBankCode(intent.getBankCode());
        response.setAccountNumber(intent.getAccountNumber());
        response.setAccountHolder(intent.getAccountHolder());
        response.setTransferContent(intent.getTransferContent());
        response.setQrUrl(vietQrUrl(intent));
        response.setExpiresAt(intent.getExpiresAt());
        return response;
    }
}
