package poly.edu.quanlynhahang.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.repository.CustomerReservationHistoryRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/customer-reservation-history")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
public class CustomerReservationHistoryController {
    private final CustomerReservationHistoryRepository historyRepository;
    private final ReservationRepository reservationRepository;

    public CustomerReservationHistoryController(CustomerReservationHistoryRepository historyRepository,
                                                ReservationRepository reservationRepository) {
        this.historyRepository = historyRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping
    public ResponseEntity<?> getCustomerHistory() {
        return ResponseEntity.ok(historyRepository.findAllByOrderByLastReservationAtDesc());
    }

    @GetMapping("/{phone}/reservations")
    public ResponseEntity<?> getCustomerReservations(@PathVariable String phone) {
        return ResponseEntity.ok(reservationRepository.findByCustomerPhoneOrderByCreatedAtDesc(phone)
                .stream()
                .map(reservation -> Map.of(
                        "id", reservation.getId(),
                        "reservationCode", reservation.getReservationCode(),
                        "reservationDate", reservation.getReservationDate(),
                        "arrivalTime", reservation.getArrivalTime(),
                        "guestCount", reservation.getGuestCount(),
                        "reservationStatus", reservation.getReservationStatus(),
                        "totalAmount", reservation.getTotalAmount(),
                        "depositAmount", reservation.getDepositAmount(),
                        "createdAt", reservation.getCreatedAt()
                ))
                .toList());
    }
}
