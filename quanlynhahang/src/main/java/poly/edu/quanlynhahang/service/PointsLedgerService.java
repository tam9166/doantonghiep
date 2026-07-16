package poly.edu.quanlynhahang.service;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.PointsEventType;
import poly.edu.quanlynhahang.entity.PointsLedger;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.PointsLedgerRepository;

@Service
public class PointsLedgerService {

    private final AccountRepository accountRepository;
    private final PointsLedgerRepository ledgerRepository;

    public PointsLedgerService(AccountRepository accountRepository, PointsLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional
    public PointsLedger credit(String username,
                               PointsEventType eventType,
                               String eventKey,
                               int points,
                               String reason) {
        if (points <= 0) {
            throw new IllegalArgumentException("Points credit must be positive.");
        }
        PointsLedger existing = ledgerRepository.findByEventKey(eventKey).orElse(null);
        if (existing != null) {
            return existing;
        }

        Account account = accountRepository.findLockedByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND"));
        int balance = Math.addExact(account.getPoints() == null ? 0 : account.getPoints(), points);
        account.setPoints(balance);
        account.setMembershipTier(resolveTier(balance));
        accountRepository.save(account);

        PointsLedger ledger = new PointsLedger();
        ledger.setAccount(account);
        ledger.setEventType(eventType);
        ledger.setEventKey(eventKey);
        ledger.setDelta(points);
        ledger.setBalanceAfter(balance);
        ledger.setReason(reason);
        return ledgerRepository.save(ledger);
    }

    @Transactional
    public PointsLedger reverse(String username, String originalEventKey, String reversalEventKey, String reason) {
        PointsLedger original = ledgerRepository.findByEventKey(originalEventKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "POINTS_EVENT_NOT_FOUND"));
        PointsLedger existing = ledgerRepository.findByEventKey(reversalEventKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        if (!original.getAccount().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "POINTS_EVENT_OWNER_MISMATCH");
        }

        Account account = accountRepository.findLockedByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND"));
        int balance = Math.max(0, (account.getPoints() == null ? 0 : account.getPoints()) - original.getDelta());
        account.setPoints(balance);
        account.setMembershipTier(resolveTier(balance));
        accountRepository.save(account);

        PointsLedger reversal = new PointsLedger();
        reversal.setAccount(account);
        reversal.setEventType(PointsEventType.REVERSAL);
        reversal.setEventKey(reversalEventKey);
        reversal.setReferenceEventKey(originalEventKey);
        reversal.setDelta(-original.getDelta());
        reversal.setBalanceAfter(balance);
        reversal.setReason(reason);
        return ledgerRepository.save(reversal);
    }

    @Transactional
    public Optional<PointsLedger> reverseIfPresent(
            String username,
            String originalEventKey,
            String reversalEventKey,
            String reason) {
        Optional<PointsLedger> existingReversal = ledgerRepository.findByEventKey(reversalEventKey);
        if (existingReversal.isPresent()) {
            return existingReversal;
        }
        if (ledgerRepository.findByEventKey(originalEventKey).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(reverse(username, originalEventKey, reversalEventKey, reason));
    }

    private String resolveTier(int points) {
        if (points >= 2000) {
            return "Kim Cương";
        }
        if (points >= 1000) {
            return "Vàng";
        }
        if (points >= 500) {
            return "Bạc";
        }
        return "Đồng";
    }
}
