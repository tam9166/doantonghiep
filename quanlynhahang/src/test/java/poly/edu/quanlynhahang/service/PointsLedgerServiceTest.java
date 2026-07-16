package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.entity.PointsLedger;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.PointsLedgerRepository;

class PointsLedgerServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final PointsLedgerRepository ledgerRepository = mock(PointsLedgerRepository.class);
    private final PointsLedgerService service = new PointsLedgerService(accountRepository, ledgerRepository);

    @Test
    void creditsAccountAndPersistsLedgerAtomically() {
        Account account = account("customer", 498);
        when(ledgerRepository.findByEventKey("ORDER_COMPLETED:10")).thenReturn(Optional.empty());
        when(accountRepository.findLockedByUsername("customer")).thenReturn(Optional.of(account));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PointsLedger ledger = service.credit(
                "customer", PointsEventType.ORDER_COMPLETED, "ORDER_COMPLETED:10", 5, "Order #10");

        assertEquals(503, account.getPoints());
        assertEquals("Bạc", account.getMembershipTier());
        assertEquals(5, ledger.getDelta());
        assertEquals(503, ledger.getBalanceAfter());
        verify(accountRepository).save(account);
        verify(ledgerRepository).save(ledger);
    }

    @Test
    void duplicateEventReturnsExistingLedgerWithoutAddingPointsAgain() {
        PointsLedger existing = new PointsLedger();
        existing.setEventKey("REVIEW:20");
        when(ledgerRepository.findByEventKey("REVIEW:20")).thenReturn(Optional.of(existing));

        PointsLedger result = service.credit("customer", PointsEventType.REVIEW, "REVIEW:20", 2, "Review");

        assertSame(existing, result);
        verify(accountRepository, never()).findLockedByUsername(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void reversalIsIdempotentAndCannotMakeBalanceNegative() {
        Account account = account("customer", 2);
        PointsLedger original = new PointsLedger();
        original.setAccount(account);
        original.setEventKey("ORDER_COMPLETED:10");
        original.setDelta(5);
        when(ledgerRepository.findByEventKey("ORDER_COMPLETED:10")).thenReturn(Optional.of(original));
        when(ledgerRepository.findByEventKey("REVERSAL:ORDER:10")).thenReturn(Optional.empty());
        when(accountRepository.findLockedByUsername("customer")).thenReturn(Optional.of(account));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PointsLedger reversal = service.reverse(
                "customer", "ORDER_COMPLETED:10", "REVERSAL:ORDER:10", "Refund");

        assertEquals(0, account.getPoints());
        assertEquals(-5, reversal.getDelta());
        assertEquals(0, reversal.getBalanceAfter());
    }

    @Test
    void cancellationWithoutAwardDoesNotFailOrCreateLedger() {
        when(ledgerRepository.findByEventKey("ORDER_CANCELLED:11")).thenReturn(Optional.empty());
        when(ledgerRepository.findByEventKey("ORDER_COMPLETED:11")).thenReturn(Optional.empty());

        Optional<PointsLedger> reversal = service.reverseIfPresent(
                "customer", "ORDER_COMPLETED:11", "ORDER_CANCELLED:11", "Cancelled");

        assertTrue(reversal.isEmpty());
        verify(accountRepository, never()).findLockedByUsername(any());
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void duplicateCancellationReturnsExistingReversal() {
        PointsLedger existing = new PointsLedger();
        existing.setEventKey("ORDER_CANCELLED:12");
        when(ledgerRepository.findByEventKey("ORDER_CANCELLED:12")).thenReturn(Optional.of(existing));

        Optional<PointsLedger> reversal = service.reverseIfPresent(
                "customer", "ORDER_COMPLETED:12", "ORDER_CANCELLED:12", "Cancelled");

        assertSame(existing, reversal.orElseThrow());
        verify(accountRepository, never()).findLockedByUsername(any());
        verify(ledgerRepository, never()).save(any());
    }

    private Account account(String username, int points) {
        Account account = new Account();
        account.setUsername(username);
        account.setPoints(points);
        return account;
    }
}
