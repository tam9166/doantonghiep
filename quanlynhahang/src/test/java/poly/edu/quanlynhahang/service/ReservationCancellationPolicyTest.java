package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import poly.edu.quanlynhahang.entity.Reservation;

class ReservationCancellationPolicyTest {
    private final ReservationCancellationPolicy policy =
            new ReservationCancellationPolicy(12, new BigDecimal("0.50"));

    @ParameterizedTest
    @CsvSource({
            "1500,0,500000,1.00,500000",
            "1440,0,500000,1.00,500000",
            "1080,500000,500000,0,0",
            "1080,500000,1000000,0.50,500000",
            "719,500000,500000,0,0",
            "-1,500000,500000,0,0"
    })
    void appliesCurrentRefundPolicyToPaidAmount(
            long minutesBefore, String expectedPenalty, String paidAmount, String expectedRate, String expectedRefund) {
        LocalDateTime bookingAt = LocalDateTime.of(2026, 8, 30, 20, 0);
        Reservation reservation = new Reservation();
        reservation.setReservationDate(bookingAt.toLocalDate());
        reservation.setArrivalTime(bookingAt.toLocalTime());
        reservation.setTotalAmount(new BigDecimal("1000000"));
        ZonedDateTime requestedAt = bookingAt.minusMinutes(minutesBefore)
                .atZone(ReservationCancellationPolicy.BUSINESS_ZONE);

        ReservationCancellationPolicy.Calculation result = policy.calculate(
                reservation, Date.from(requestedAt.toInstant()), new BigDecimal(paidAmount));

        assertEquals(0, result.penaltyAmount().compareTo(new BigDecimal(expectedPenalty)));
        assertEquals(0, result.refundRate().compareTo(new BigDecimal(expectedRate)));
        assertEquals(0, result.refundAmount().compareTo(new BigDecimal(expectedRefund)));
    }
}
