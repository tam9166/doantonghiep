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
import poly.edu.quanlynhahang.repository.RefundTransactionRepository;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
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

    public RefundService(RefundTransactionRepository refundRepository,
                         PaymentIntentRepository paymentIntentRepository,
                         ReservationRepository reservationRepository,
                         DepositPolicyService depositPolicyService,
                         ActivityLogService activityLogService) {
        this.refundRepository = refundRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.reservationRepository = reservationRepository;
        this.depositPolicyService = depositPolicyService;
        this.activityLogService = activityLogService;
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

        // Trong thực tế, gọi API ngân hàng/VNPay ở đây
        // Sau đó cập nhật status thành COMPLETED hoặc FAILED
        markCompleted(saved, "Tự động hoàn tiền qua cổng thanh toán");

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
}