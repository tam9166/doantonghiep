package poly.edu.quanlynhahang.service;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.ReservationStatus;

@Service
public class ReservationStateMachine {

    private static final Map<ReservationStatus, Set<ReservationStatus>> ALLOWED = new EnumMap<>(ReservationStatus.class);
    private static final Set<ReservationStatus> TERMINAL = EnumSet.of(
            ReservationStatus.REJECTED,
            ReservationStatus.CANCELLED,
            ReservationStatus.COMPLETED,
            ReservationStatus.EXPIRED,
            ReservationStatus.NO_SHOW);

    static {
        allow(ReservationStatus.WAITING_TABLE_ASSIGNMENT,
                ReservationStatus.CONFIRMED,
                ReservationStatus.DEPOSIT_REQUIRED,
                ReservationStatus.DEPOSIT_PAID,
                ReservationStatus.FULLY_PAID,
                ReservationStatus.REJECTED,
                ReservationStatus.CANCELLED,
                ReservationStatus.EXPIRED);
        allow(ReservationStatus.PENDING,
                ReservationStatus.WAITING_TABLE_ASSIGNMENT,
                ReservationStatus.CONFIRMED,
                ReservationStatus.DEPOSIT_REQUIRED,
                ReservationStatus.DEPOSIT_PAID,
                ReservationStatus.FULLY_PAID,
                ReservationStatus.REJECTED,
                ReservationStatus.CANCELLED,
                ReservationStatus.EXPIRED);
        allow(ReservationStatus.DEPOSIT_REQUIRED,
                ReservationStatus.CONFIRMED,        // P0: cho phép admin miễn cọc cho khách VIP
                ReservationStatus.DEPOSIT_PENDING,
                ReservationStatus.DEPOSIT_PAID,
                ReservationStatus.FULLY_PAID,
                ReservationStatus.CANCELLED,
                ReservationStatus.EXPIRED);
        allow(ReservationStatus.DEPOSIT_PENDING,
                ReservationStatus.DEPOSIT_PAID,
                ReservationStatus.FULLY_PAID,
                ReservationStatus.CANCELLED,
                ReservationStatus.EXPIRED);
        allow(ReservationStatus.DEPOSIT_PAID,
                ReservationStatus.FULLY_PAID,
                ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN,
                ReservationStatus.CANCELLED,
                ReservationStatus.NO_SHOW);
        allow(ReservationStatus.FULLY_PAID,
                ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN,
                ReservationStatus.CANCELLED,
                ReservationStatus.NO_SHOW);
        allow(ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN,
                ReservationStatus.CANCELLED,
                ReservationStatus.NO_SHOW);
        allow(ReservationStatus.CHECKED_IN,
                ReservationStatus.IN_SERVICE,
                ReservationStatus.CANCELLED);
        allow(ReservationStatus.IN_SERVICE,
                ReservationStatus.COMPLETED,
                ReservationStatus.CANCELLED);
    }

    public void assertCanTransition(ReservationStatus current, ReservationStatus next) {
        if (current == next) {
            return;
        }
        if (current == null || next == null || TERMINAL.contains(current)
                || !ALLOWED.getOrDefault(current, Set.of()).contains(next)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Không thể chuyển trạng thái đặt bàn từ " + current + " sang " + next);
        }
    }

    private static void allow(ReservationStatus from, ReservationStatus... to) {
        ALLOWED.put(from, EnumSet.copyOf(java.util.Arrays.asList(to)));
    }
}
