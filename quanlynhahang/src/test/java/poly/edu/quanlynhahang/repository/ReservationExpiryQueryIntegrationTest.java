package poly.edu.quanlynhahang.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.ReservationStatus;

@SpringBootTest
class ReservationExpiryQueryIntegrationTest {
    @Autowired
    private ReservationRepository reservations;

    @Test
    @Transactional(readOnly = true)
    void sqlServerCanCompareArrivalTimeWithoutTimeDatetimeTypeMismatch() {
        List<Long> ids = reservations.findExpiryCandidateIds(
                new Date(),
                new Date(System.currentTimeMillis() - 15 * 60_000L),
                LocalDate.now(),
                LocalTime.now(),
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.WAITING_TABLE_ASSIGNMENT,
                        ReservationStatus.DEPOSIT_REQUIRED, ReservationStatus.DEPOSIT_PENDING),
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.DEPOSIT_REQUIRED,
                        ReservationStatus.DEPOSIT_PENDING),
                EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.DEPOSIT_PAID,
                        ReservationStatus.FULLY_PAID),
                PageRequest.of(0, 200));

        assertNotNull(ids);
        assertTrue(ids.size() <= 200);
    }
}
