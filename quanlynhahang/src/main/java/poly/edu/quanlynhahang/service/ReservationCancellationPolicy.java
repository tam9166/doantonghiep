package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.Reservation;

@Service
public class ReservationCancellationPolicy {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final long refundHours;
    private final BigDecimal refundRate;

    public ReservationCancellationPolicy(
            @Value("${restaurant.cancellation.refund-hours:12}") long refundHours,
            @Value("${restaurant.cancellation.refund-rate:0.50}") BigDecimal refundRate) {
        if (refundHours < 0 || refundRate == null
                || refundRate.signum() < 0 || refundRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Cấu hình chính sách hủy không hợp lệ");
        }
        this.refundHours = refundHours;
        this.refundRate = refundRate;
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
        BigDecimal paidDeposit = actuallyPaidDeposit == null
                ? BigDecimal.ZERO : actuallyPaidDeposit.max(BigDecimal.ZERO);
        boolean eligible = minutesBefore >= refundHours * 60;
        BigDecimal appliedRate = eligible ? refundRate : BigDecimal.ZERO;
        BigDecimal refundAmount = paidDeposit.multiply(appliedRate).setScale(0, RoundingMode.HALF_UP);
        return new Calculation(hoursBefore, appliedRate, paidDeposit, refundAmount, eligible);
    }

    public record Calculation(
            BigDecimal hoursBeforeReservation,
            BigDecimal refundRate,
            BigDecimal paidDepositAmount,
            BigDecimal refundAmount,
            boolean eligible) {
    }
}
