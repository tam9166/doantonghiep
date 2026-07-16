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

@Service
public class OrderPaymentService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PaymentIntentRepository intentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProperties properties;
    private final ActivityLogService activityLogService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestaurantTableRepository tableRepository;

    public OrderPaymentService(PaymentIntentRepository intentRepository,
                               OrderRepository orderRepository,
                               PaymentProperties properties,
                               ActivityLogService activityLogService,
                               SimpMessagingTemplate messagingTemplate,
                               RestaurantTableRepository tableRepository) {
        this.intentRepository = intentRepository;
        this.orderRepository = orderRepository;
        this.properties = properties;
        this.activityLogService = activityLogService;
        this.messagingTemplate = messagingTemplate;
        this.tableRepository = tableRepository;
    }

    @Transactional
    public PaymentQrResponse createForOrder(Order order) {
        if (order == null || order.getId() == null || order.getTotalAmount() == null
                || order.getTotalAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn hàng chưa có tổng tiền hợp lệ");
        }
        if (!OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())) {
            return null;
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

        BigDecimal amount = BigDecimal.valueOf(order.getTotalAmount()).setScale(0, RoundingMode.HALF_UP);
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
        intent.setAccountHolder(properties.getAccountHolder().trim().toUpperCase(Locale.ROOT));
        intent.setQrProvider(properties.getQrProvider().trim().toUpperCase(Locale.ROOT));
        intent.setTransferContent(transferContent(order.getId(), intent.getPaymentCode()));
        intent.setExpiresAt(Date.from(Instant.now().plusSeconds(properties.getQrExpirationMinutes() * 60L)));
        intent.setIdempotencyKey("ORDER-CHECKOUT-" + order.getId());
        intent.setRequestHash(hash("CREATE|ORDER|" + order.getId() + "|FULL|" + amount));
        intent.setCreatedBy(order.getAccount() == null ? "GUEST" : order.getAccount().getUsername());
        intent.setQrUrl(vietQrUrl(intent));
        PaymentIntent saved = intentRepository.save(intent);
        activityLogService.log("CREATE_PAYMENT_INTENT", "Order", String.valueOf(order.getId()),
                "Tạo QR thanh toán cho đơn hàng");
        return toResponse(saved);
    }

    @Transactional
    public Order confirmManualDispatch(Integer orderId) {
        Order order = orderRepository.findLockedById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Đơn hàng không tồn tại"));
        if (Integer.valueOf(3).equals(order.getStatus()) || Boolean.TRUE.equals(order.getIsPaid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể xác nhận đơn đã hủy hoặc đã thanh toán");
        }
        if (OrderPaymentOption.PREPAID_TRANSFER.equals(order.getPaymentOption())
                && !PaymentStatus.PAID.equals(order.getPaymentStatus())
                && !PaymentStatus.OVERPAID.equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn chuyển khoản chưa được xác nhận đủ tiền");
        }
        if (!OrderPaymentOption.COD.equals(order.getPaymentOption())
                && !OrderPaymentOption.PAY_AT_RESTAURANT.equals(order.getPaymentOption())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn này không dùng luồng xác nhận thủ công");
        }
        if (!Integer.valueOf(0).equals(order.getStatus()) && !Integer.valueOf(5).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trạng thái đơn không cho phép xác nhận");
        }
        order.setStatus(1);
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
        BigDecimal total = BigDecimal.valueOf(order.getTotalAmount() == null ? 0.0 : order.getTotalAmount())
                .setScale(0, RoundingMode.HALF_UP);
        order.setPaidAmount(aggregatePaid.max(BigDecimal.ZERO));
        order.setRemainingAmount(total.subtract(aggregatePaid).max(BigDecimal.ZERO));
        order.setPaymentStatus(status);
        boolean fullyPaid = PaymentStatus.PAID.equals(status) || PaymentStatus.OVERPAID.equals(status);
        order.setIsPaid(fullyPaid);
        if (fullyPaid && Integer.valueOf(0).equals(order.getStatus())) {
            order.setStatus(1);
            messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
        }
        return orderRepository.save(order);
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
