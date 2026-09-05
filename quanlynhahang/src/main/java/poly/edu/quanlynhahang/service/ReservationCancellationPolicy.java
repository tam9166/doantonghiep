package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.Reservation;

@Service
public class ReservationCancellationPolicy {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final BigDecimal FULL_REFUND_THRESHOLD_HOURS = BigDecimal.valueOf(24);
    private static final BigDecimal HALF_DAY_THRESHOLD_HOURS = BigDecimal.valueOf(12);
    private static final BigDecimal HALF_ORDER_PENALTY_RATE = new BigDecimal("0.50");

    public ReservationCancellationPolicy() {
        // Policy is intentionally fixed in code because refund is money-critical backend business logic.
    }

    public ReservationCancellationPolicy(long ignoredRefundHours, BigDecimal ignoredRefundRate) {
        this();
    }

    public Calculation calculate(Reservation reservation, Date requestedAt, BigDecimal actuallyPaidDeposit) {
        if (reservation == null || reservation.getReservationDate() == null
                || reservation.getArrivalTime() == null || requestedAt == null) {
            throw new IllegalArgumentException("Thiếu thời gian đặt bàn/yêu cầu hủy");
        }
        LocalDateTime bookingAt = LocalDateTime.of(
                reservation.getReservationDate(), reservation.getArrivalTime());
        LocalDateTime requestAt = LocalDateTime.ofInstant(requestedAt.toInstant(), BUSINESS_ZONE);
        long minutesBefore = Duration.between(requestAt, bookingAt).toMinutes();
        BigDecimal hoursBefore = BigDecimal.valueOf(minutesBefore)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal paidAmount = actuallyPaidDeposit == null
                ? BigDecimal.ZERO : actuallyPaidDeposit.max(BigDecimal.ZERO);
        BigDecimal orderTotal = reservation.getTotalAmount() == null
                ? BigDecimal.ZERO : reservation.getTotalAmount().max(BigDecimal.ZERO);
        BigDecimal penaltyAmount;
        String policyApplied;
        String policyCode;
        if (minutesBefore >= FULL_REFUND_THRESHOLD_HOURS.longValue() * 60) {
            penaltyAmount = BigDecimal.ZERO;
            policyApplied = "Hủy trước giờ đặt từ 24 giờ: hoàn 100% số tiền đã thanh toán.";
            policyCode = "FULL_REFUND_24H";
        } else if (minutesBefore >= HALF_DAY_THRESHOLD_HOURS.longValue() * 60) {
            penaltyAmount = orderTotal.multiply(HALF_ORDER_PENALTY_RATE)
                    .setScale(0, RoundingMode.HALF_UP);
            policyApplied = "Hủy trước giờ đặt từ 12 đến dưới 24 giờ: phí hủy bằng 50% giá trị đơn.";
            policyCode = "HALF_ORDER_PENALTY_12H";
        } else {
            penaltyAmount = paidAmount;
            policyApplied = "Hủy dưới 12 giờ trước giờ đặt: giữ nguyên chính sách hiện tại, không hoàn tiền.";
            policyCode = "NO_REFUND_UNDER_12H";
        }
        BigDecimal refundAmount = paidAmount.subtract(penaltyAmount).max(BigDecimal.ZERO).min(paidAmount)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal appliedRate = paidAmount.signum() == 0
                ? BigDecimal.ZERO
                : refundAmount.divide(paidAmount, 2, RoundingMode.HALF_UP);
        boolean eligible = refundAmount.signum() > 0 || minutesBefore >= HALF_DAY_THRESHOLD_HOURS.longValue() * 60;
        return new Calculation(hoursBefore, appliedRate, paidAmount, refundAmount, eligible,
                orderTotal, penaltyAmount.min(paidAmount), policyApplied, policyCode);
    }

    public record Calculation(
            BigDecimal hoursBeforeReservation,
            BigDecimal refundRate,
            BigDecimal paidDepositAmount,
            BigDecimal refundAmount,
            boolean eligible,
            BigDecimal orderTotalAmount,
            BigDecimal penaltyAmount,
            String policyApplied,
            String policyCode) {
    }
}
