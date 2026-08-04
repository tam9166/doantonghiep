package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.ReservationStatus;

class ReservationStateMachineTest {

    private final ReservationStateMachine stateMachine = new ReservationStateMachine();

    @Test
    void allowsExpectedPaymentAndCheckInTransitions() {
        assertDoesNotThrow(() -> stateMachine.assertCanTransition(
                ReservationStatus.PENDING, ReservationStatus.DEPOSIT_PAID));
        assertDoesNotThrow(() -> stateMachine.assertCanTransition(
                ReservationStatus.DEPOSIT_PAID, ReservationStatus.CHECKED_IN));
    }

    @Test
    void blocksTerminalStatusFromReturningToActiveFlow() {
        assertThrows(ResponseStatusException.class, () -> stateMachine.assertCanTransition(
                ReservationStatus.CANCELLED, ReservationStatus.CONFIRMED));
    }
}
