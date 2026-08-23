package poly.edu.quanlynhahang.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.entity.RefundTransaction.RefundReason;
import poly.edu.quanlynhahang.entity.RefundTransaction.RefundStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentDirection;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * Handles refund processing when reservations/orders are cancelled
 * after payment has been made.
 */
@Service
public class RefundService {
    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundTransactionRepository refundRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ReservationRepository reservationRepository;
    private final DepositPolicyService depositPolicyService;
    private final ActivityLogService activityLogService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public RefundService(RefundTransactionRepository refundRepository,
                         PaymentIntentRepository paymentIntentRepository,
                         ReservationRepository reservationRepository,
                         DepositPolicyService depositPolicyService,
                         ActivityLogService activityLogService,
                         PaymentTransactionRepository paymentTransactionRepository) {
        this.refundRepository = refundRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.reservationRepository = reservationRepository;
        this.depositPolicyService = depositPolicyService;
        this.activityLogService = activityLogService;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    /**
     * Process refund for a cancelled reservation.
     * Calculates forfeiture based on deposit policy, then creates refund record.
     */
    @Transactional
    public RefundTransaction processReservationRefund(Reservation reservation, RefundReason reason,
                                                       String reasonDetail, String processedBy) {
        if (reservation == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation không tồn tại");
        }

        BigDecimal paidAmount = nvl(reservation.getPaidAmount());
        if (paidAmount.signum() <= 0) {
            log.info("Reservation {} has no paid amount, skipping refund", reservation.getReservationCode());
            return null;
        }

        // Tính số tiền được giữ lại theo policy (no-show forfeiture logic)
        BigDecimal forfeited = depositPolicyService.calculateNoShowForfeiture(reservation);
        BigDecimal refundAmount = paidAmount.subtract(forfeited).max(BigDecimal.ZERO);

        if (refundAmount.signum() <= 0) {
            log.info("Reservation {}: paid={}, forfeited={}, no refund due",
                    reservation.getReservationCode(), paidAmount, forfeited);
        }

        // Tìm payment intent để liên kết
        List<PaymentIntent> intents = paymentIntentRepository
                .findByReservationIdAndStatusOrderByCreatedAtDesc(
                        reservation.getId(), PaymentStatus.PAID);

        RefundTransaction refund = new RefundTransaction();
        refund.setReservationId(reservation.getId());
        refund.setAmount(refundAmount);
        refund.setForfeitedAmount(forfeited);
        refund.setReason(reason);
        refund.setReasonDetail(reasonDetail);
        refund.setStatus(RefundStatus.PENDING);
        refund.setProcessedBy(processedBy);
        refund.setCreatedAt(new Date());

        if (!intents.isEmpty()) {
            refund.setPaymentIntentId(intents.get(0).getId());
        }

        RefundTransaction saved = refundRepository.save(refund);

        activityLogService.log("REFUND", "Reservation", String.valueOf(reservation.getId()),
                "Tạo hoàn tiền " + refundAmount + " cho " + reservation.getReservationCode()
                + " (giữ lại " + forfeited + "), lý do: " + reason);

        return saved;
    }

    @Transactional
    public RefundTransaction requestReservationRefund(Reservation reservation,
                                                       BigDecimal amount,
                                                       BigDecimal forfeitedAmount,
                                                       RefundReason reason,
                                                       String reasonDetail,
                                                       String processedBy) {
        if (reservation == null || reservation.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation không tồn tại");
        }
        BigDecimal safeAmount = nvl(amount).max(BigDecimal.ZERO);
        if (safeAmount.signum() <= 0) return null;
        if (refundRepository.existsByReservationIdAndStatusIn(reservation.getId(),
                java.util.EnumSet.of(RefundStatus.PENDING, RefundStatus.COMPLETED))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đặt bàn đã có yêu cầu hoàn tiền");
        }
        RefundTransaction refund = new RefundTransaction();
        refund.setReservationId(reservation.getId());
        refund.setAmount(safeAmount);
        refund.setForfeitedAmount(nvl(forfeitedAmount).max(BigDecimal.ZERO));
        refund.setReason(reason);
        refund.setReasonDetail(reasonDetail);
        refund.setStatus(RefundStatus.PENDING);
        refund.setProcessedBy(processedBy);
        refund.setCreatedAt(new Date());
        paymentIntentRepository.findByReservationIdAndStatusOrderByCreatedAtDesc(
                reservation.getId(), PaymentStatus.PAID).stream().findFirst()
                .ifPresent(intent -> refund.setPaymentIntentId(intent.getId()));
        RefundTransaction saved = refundRepository.save(refund);
        activityLogService.log("REFUND_PENDING", "Reservation", String.valueOf(reservation.getId()),
                "Đã tạo yêu cầu hoàn tiền " + safeAmount + "; chờ xác nhận thực tế");
        return saved;
    }

    @Transactional
    public RefundTransaction markCompleted(RefundTransaction refund, String notes) {
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setProcessedAt(new Date());
        if (notes != null) {
            refund.setNotes(notes);
        }
        return refundRepository.save(refund);
    }

    @Transactional
    public RefundTransaction confirmCompleted(Long refundId, String providerReference,
                                              String notes, String processedBy) {
        RefundTransaction refund = refundRepository.findLockedById(refundId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy yêu cầu hoàn tiền"));
        String reference = providerReference == null ? null : providerReference.trim();
        if (reference == null || reference.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng nhập mã giao dịch hoàn tiền thực tế");
        }
        if (RefundStatus.COMPLETED.equals(refund.getStatus())) {
            PaymentTransaction recorded = paymentTransactionRepository.findByProviderTransactionId(reference)
                    .filter(transaction -> isMatchingCompletedRefund(transaction, refund))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Mã giao dịch không khớp với lần hoàn tiền đã hoàn tất"));
            log.debug("Idempotent refund confirmation {} using ledger transaction {}",
                    refundId, recorded.getId());
            return refund;
        }
        if (!RefundStatus.PENDING.equals(refund.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Yêu cầu hoàn tiền không ở trạng thái chờ xử lý");
        }
        paymentTransactionRepository.findByProviderTransactionId(reference).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mã giao dịch hoàn tiền đã được ghi nhận");
        });
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentIntentId(refund.getPaymentIntentId());
        transaction.setAggregateType(refund.getReservationId() != null ? "RESERVATION" : "ORDER");
        transaction.setAggregateId(refund.getReservationId() != null
                ? refund.getReservationId() : refund.getOrderId().longValue());
        transaction.setProvider("MANUAL_REFUND_CONFIRMATION");
        transaction.setProviderTransactionId(reference);
        transaction.setAmount(refund.getAmount());
        transaction.setDirection(PaymentDirection.REFUND);
        transaction.setStatus(PaymentTransactionStatus.SUCCESS);
        transaction.setRawReference(limit(notes, 200));
        paymentTransactionRepository.save(transaction);
        refund.setProcessedBy(processedBy);
        RefundTransaction completed = markCompleted(refund, notes);
        activityLogService.log("REFUND_COMPLETED", "RefundTransaction", String.valueOf(refundId),
                "Đã đối soát hoàn tiền bằng giao dịch " + reference);
        return completed;
    }

    private boolean isMatchingCompletedRefund(PaymentTransaction transaction, RefundTransaction refund) {
        String aggregateType = refund.getReservationId() != null ? "RESERVATION" : "ORDER";
        Long aggregateId = refund.getReservationId() != null
                ? refund.getReservationId() : refund.getOrderId().longValue();
        return PaymentDirection.REFUND.equals(transaction.getDirection())
                && PaymentTransactionStatus.SUCCESS.equals(transaction.getStatus())
                && aggregateType.equals(transaction.getAggregateType())
                && aggregateId.equals(transaction.getAggregateId())
                && nvl(refund.getAmount()).compareTo(nvl(transaction.getAmount())) == 0;
    }

    @Transactional
    public RefundTransaction markFailed(RefundTransaction refund, String failureReason) {
        refund.setStatus(RefundStatus.FAILED);
        refund.setFailureReason(failureReason);
        refund.setProcessedAt(new Date());
        return refundRepository.save(refund);
    }

    public List<RefundTransaction> getRefundsForReservation(Long reservationId) {
        return refundRepository.findByReservationIdOrderByCreatedAtDesc(reservationId);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(max, trimmed.length()));
    }
}
