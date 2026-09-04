package poly.edu.quanlynhahang.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationStatus;

@SpringBootTest
class ReservationAdminPagingIntegrationTest {
    @Autowired
    private ReservationRepository reservations;

    @Test
    @Transactional
    void adminPageOrdersNewestReservationsBeforeApplyingTenRowPagination() {
        Date base = new Date(4_102_444_800_000L); // 2100-01-01, newer than normal application fixtures.
        for (int index = 1; index <= 11; index++) {
            Reservation reservation = new Reservation();
            reservation.setReservationCode("ADMIN-PAGING-" + String.format("%02d", index));
            reservation.setCustomerName("Kiểm thử phân trang " + index);
            reservation.setCustomerPhone("0900000" + String.format("%03d", index));
            reservation.setReservationDate(LocalDate.of(2100, 1, 2));
            reservation.setArrivalTime(LocalTime.of(18, 0));
            reservation.setExpectedDurationMinutes(120);
            reservation.setGuestCount(2);
            reservation.setReservationStatus(ReservationStatus.REJECTED);
            reservation.setCreatedAt(new Date(base.getTime() + index * 60_000L));
            reservation.setUpdatedAt(reservation.getCreatedAt());
            reservations.save(reservation);
        }
        reservations.flush();

        var firstPage = reservations.findAdminPage(
                ReservationStatus.REJECTED, null, PageRequest.of(0, 10));
        var secondPage = reservations.findAdminPage(
                ReservationStatus.REJECTED, null, PageRequest.of(1, 10));

        List<String> firstPageCodes = firstPage.getContent().stream()
                .map(Reservation::getReservationCode)
                .filter(code -> code.startsWith("ADMIN-PAGING-"))
                .toList();

        assertEquals(10, firstPageCodes.size());
        assertEquals("ADMIN-PAGING-11", firstPageCodes.getFirst());
        assertEquals("ADMIN-PAGING-02", firstPageCodes.getLast());
        assertEquals(List.of("ADMIN-PAGING-01"), secondPage.getContent().stream()
                .map(Reservation::getReservationCode)
                .filter(code -> code.startsWith("ADMIN-PAGING-"))
                .toList());
    }

    @Test
    @Transactional
    void adminPagePrioritizesUnprocessedStatusesBeforeNewerProcessedReservations() {
        Date base = new Date(4_102_530_000_000L); // Isolated future timestamp for this priority fixture.
        List<Reservation> fixtures = List.of(
                priorityReservation("PRIORITY-PROCESSED", ReservationStatus.DEPOSIT_PAID, base, 5),
                priorityReservation("PRIORITY-DEPOSIT", ReservationStatus.DEPOSIT_REQUIRED, base, 4),
                priorityReservation("PRIORITY-WAITING", ReservationStatus.WAITING_TABLE_ASSIGNMENT, base, 3),
                priorityReservation("PRIORITY-PENDING", ReservationStatus.PENDING, base, 2));
        reservations.saveAll(fixtures);
        reservations.flush();

        List<String> codes = reservations.findAdminPage(null, "PRIORITY-", PageRequest.of(0, 10))
                .getContent().stream()
                .map(Reservation::getReservationCode)
                .toList();

        assertEquals(List.of("PRIORITY-PENDING", "PRIORITY-WAITING", "PRIORITY-DEPOSIT", "PRIORITY-PROCESSED"), codes);
    }

    private Reservation priorityReservation(String code, ReservationStatus status, Date base, int minuteOffset) {
        Reservation reservation = new Reservation();
        reservation.setReservationCode(code);
        reservation.setCustomerName("Priority fixture " + code);
        reservation.setCustomerPhone("0911000" + minuteOffset);
        reservation.setReservationDate(LocalDate.of(2100, 1, 3));
        reservation.setArrivalTime(LocalTime.of(18, 0));
        reservation.setExpectedDurationMinutes(120);
        reservation.setGuestCount(2);
        reservation.setReservationStatus(status);
        reservation.setCreatedAt(new Date(base.getTime() + minuteOffset * 60_000L));
        reservation.setUpdatedAt(reservation.getCreatedAt());
        return reservation;
    }
}
