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
            "780,0.50,250000",
            "721,0.50,250000",
            "720,0.50,250000",
            "719,0,0",
            "-1,0,0"
    })
    void appliesTwelveHourBoundaryToActuallyPaidDeposit(
            long minutesBefore, String expectedRate, String expectedRefund) {
        LocalDateTime bookingAt = LocalDateTime.of(2026, 8, 30, 20, 0);
        Reservation reservation = new Reservation();
        reservation.setReservationDate(bookingAt.toLocalDate());
        reservation.setArrivalTime(bookingAt.toLocalTime());
        ZonedDateTime requestedAt = bookingAt.minusMinutes(minutesBefore)
                .atZone(ReservationCancellationPolicy.BUSINESS_ZONE);

        ReservationCancellationPolicy.Calculation result = policy.calculate(
                reservation, Date.from(requestedAt.toInstant()), new BigDecimal("500000"));

        assertEquals(0, result.refundRate().compareTo(new BigDecimal(expectedRate)));
        assertEquals(0, result.refundAmount().compareTo(new BigDecimal(expectedRefund)));
    }
}
