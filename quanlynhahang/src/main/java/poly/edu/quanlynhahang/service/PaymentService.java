package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.config.PaymentProperties;
import poly.edu.quanlynhahang.dto.PaymentQrRequest;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;

@Service
public class PaymentService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ReservationRepository reservationRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ReservationRealtimeService realtimeService;
    private final ReservationStateMachine stateMachine;
    private final PaymentProperties paymentProperties;

    public PaymentService(ReservationRepository reservationRepository,
                          PaymentIntentRepository paymentIntentRepository,
                          ReservationRealtimeService realtimeService,
                          ReservationStateMachine stateMachine,
                          PaymentProperties paymentProperties) {
        this.reservationRepository = reservationRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.realtimeService = realtimeService;
        this.stateMachine = stateMachine;
        this.paymentProperties = paymentProperties;
    }

    @Transactional
    public PaymentQrResponse createQr(PaymentQrRequest request) {
        if (request == null || request.getReservationCode() == null || request.getReservationCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Thiếu mã đặt bàn");
        }
        Reservation reservation = reservationRepository.findByReservationCode(request.getReservationCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặt bàn"));
        if (isClosedReservation(reservation.getReservationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể tạo QR cho đặt bàn đã kết thúc hoặc bị hủy");
        }
        expireOldPendingPayments(reservation);
        PaymentOption option = request.getPaymentOption() == null ? reservation.getPaymentOption() : request.getPaymentOption();
        BigDecimal amount = payableAmount(reservation, option);
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Hình thức thanh toán này không cần tạo QR");
        }

        PaymentIntent intent = new PaymentIntent();
        intent.setReservation(reservation);
        intent.setPaymentCode(nextPaymentCode());
        intent.setPaymentOption(option);
        intent.setAmount(amount);
        intent.setBankCode(paymentProperties.getBankCode().trim().toUpperCase(Locale.ROOT));
        intent.setAccountNumber(paymentProperties.getAccountNumber().trim());
        intent.setAccountHolder(paymentProperties.getAccountHolder().trim().toUpperCase(Locale.ROOT));
        intent.setTransferContent(buildTransferContent(reservation.getReservationCode(), intent.getPaymentCode()));
        intent.setExpiresAt(Date.from(Instant.now().plusSeconds(
                paymentProperties.getQrExpirationMinutes() * 60L)));
        intent.setQrUrl(buildVietQrUrl(intent));
        return toResponse(paymentIntentRepository.save(intent));
    }

    @Transactional
    public PaymentQrResponse regenerate(String paymentCode) {
        PaymentIntent existing = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch thanh toán"));
        if (PaymentStatus.PAID.equals(existing.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã thanh toán");
        }
        if (isClosedReservation(existing.getReservation().getReservationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể tạo lại QR cho đặt bàn đã kết thúc hoặc bị hủy");
        }
        existing.setStatus(PaymentStatus.EXPIRED);
        paymentIntentRepository.save(existing);

        PaymentQrRequest request = new PaymentQrRequest();
        request.setReservationCode(existing.getReservation().getReservationCode());
        request.setPaymentOption(existing.getPaymentOption());
        return createQr(request);
    }

    @Transactional
    public PaymentQrResponse confirm(String paymentCode, String bankTransactionCode, String note) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch thanh toán"));
        if (bankTransactionCode != null && !bankTransactionCode.isBlank()) {
            paymentIntentRepository.findByBankTransactionCode(bankTransactionCode)
                    .filter(existing -> !existing.getId().equals(intent.getId()))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã giao dịch ngân hàng đã được xử lý");
                    });
        }
        return confirmIntent(intent, bankTransactionCode, note, currentUsername());
    }

    @Transactional
    public PaymentQrResponse confirmFromWebhook(String paymentCode, String providerTransactionId, BigDecimal paidAmount) {
        PaymentIntent intent = paymentIntentRepository.findByPaymentCode(paymentCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch thanh toán"));
        if (PaymentStatus.PAID.equals(intent.getStatus())) {
            if (providerTransactionId != null && providerTransactionId.equals(intent.getBankTransactionCode())) {
                return toResponse(intent);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã được xác nhận trước đó");
        }
        if (!PaymentStatus.PENDING.equals(intent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch không còn ở trạng thái chờ thanh toán");
        }
        if (intent.getExpiresAt() != null && intent.getExpiresAt().before(new Date())) {
            intent.setStatus(PaymentStatus.EXPIRED);
            paymentIntentRepository.save(intent);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PAYMENT_QR_EXPIRED");
        }
        if (paidAmount == null || paidAmount.compareTo(intent.getAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_AMOUNT_MISMATCH");
        }
        paymentIntentRepository.findByBankTransactionCode(providerTransactionId)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "PAYMENT_TRANSACTION_DUPLICATED");
                });
        return confirmIntent(intent, providerTransactionId, "Webhook xác nhận thanh toán", "WEBHOOK");
    }

    private PaymentQrResponse confirmIntent(PaymentIntent intent, String bankTransactionCode, String note, String confirmedBy) {
        if (PaymentStatus.PAID.equals(intent.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã được xác nhận trước đó");
        }
        if (intent.getExpiresAt() != null && intent.getExpiresAt().before(new Date())) {
            intent.setStatus(PaymentStatus.EXPIRED);
            paymentIntentRepository.save(intent);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Giao dịch đã hết hạn");
        }
        intent.setStatus(PaymentStatus.PAID);
        intent.setPaidAt(new Date());
        intent.setConfirmedBy(confirmedBy);
        intent.setBankTransactionCode(bankTransactionCode);
        intent.setNote(note);

        Reservation reservation = intent.getReservation();
        ReservationStatus oldStatus = reservation.getReservationStatus();
        reservation.setPaymentStatus(PaymentStatus.PAID);
        reservation.setRemainingAmount(reservation.getTotalAmount().subtract(intent.getAmount()).max(BigDecimal.ZERO));
        ReservationStatus nextStatus;
        if (reservation.getRemainingAmount().signum() == 0 || PaymentOption.FULL.equals(intent.getPaymentOption())) {
            nextStatus = ReservationStatus.FULLY_PAID;
        } else {
            nextStatus = ReservationStatus.DEPOSIT_PAID;
        }
        stateMachine.assertCanTransition(oldStatus, nextStatus);
        reservation.setReservationStatus(nextStatus);
        reservationRepository.save(reservation);
        PaymentQrResponse response = toResponse(paymentIntentRepository.save(intent));
        realtimeService.publish(
                "PAYMENT_CONFIRMED",
                reservation.getReservationCode(),
                oldStatus,
                reservation.getReservationStatus(),
                "Thanh toán đã được xác nhận",
                null);
        return response;
    }

    private BigDecimal payableAmount(Reservation reservation, PaymentOption option) {
        if (PaymentOption.PAY_AT_RESTAURANT.equals(option)) {
            return BigDecimal.ZERO;
        }
        if (PaymentOption.FULL.equals(option)) {
            return reservation.getTotalAmount();
        }
        return reservation.getDepositAmount();
    }

    private void expireOldPendingPayments(Reservation reservation) {
        Date now = new Date();
        paymentIntentRepository.findByReservationIdAndStatusOrderByCreatedAtDesc(reservation.getId(), PaymentStatus.PENDING)
                .forEach(intent -> {
                    if (intent.getExpiresAt() == null || intent.getExpiresAt().after(now)) {
                        intent.setStatus(PaymentStatus.EXPIRED);
                        paymentIntentRepository.save(intent);
                    }
                });
    }

    private boolean isClosedReservation(ReservationStatus status) {
        return ReservationStatus.CANCELLED.equals(status)
                || ReservationStatus.REJECTED.equals(status)
                || ReservationStatus.EXPIRED.equals(status)
                || ReservationStatus.COMPLETED.equals(status)
                || ReservationStatus.NO_SHOW.equals(status);
    }

    private String nextPaymentCode() {
        byte[] randomBytes = new byte[18];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "PAY-" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).toUpperCase(Locale.ROOT);
    }

    private String buildTransferContent(String reservationCode, String paymentCode) {
        String billCode = reservationCode == null
                ? "DB"
                : reservationCode.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        String paymentSuffix = paymentCode.replace("PAY-", "");
        paymentSuffix = paymentSuffix.substring(0, Math.min(8, paymentSuffix.length()));
        return "TT " + billCode + " " + paymentSuffix;
    }

    private String buildVietQrUrl(PaymentIntent intent) {
        String addInfo = URLEncoder.encode(intent.getTransferContent(), StandardCharsets.UTF_8);
        String accountName = URLEncoder.encode(intent.getAccountHolder(), StandardCharsets.UTF_8);
        return "https://img.vietqr.io/image/" + intent.getBankCode() + "-" + intent.getAccountNumber()
                + "-compact2.png?amount=" + intent.getAmount().toPlainString()
                + "&addInfo=" + addInfo
                + "&accountName=" + accountName;
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
        response.setQrUrl(buildVietQrUrl(intent));
        response.setExpiresAt(intent.getExpiresAt());
        return response;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "SYSTEM";
    }

}
