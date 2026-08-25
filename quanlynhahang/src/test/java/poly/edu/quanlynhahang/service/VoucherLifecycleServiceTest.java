package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.VoucherRepository;

class VoucherLifecycleServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-25T10:00:00Z");
    private final VoucherLifecycleService service = new VoucherLifecycleService(
            mock(VoucherRepository.class), Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh")));

    @Test
    void supportsUnlimitedTimeAndUsage() {
        Voucher voucher = activeVoucher();

        service.validateForUse(voucher, null);
        service.redeemLocked(voucher, null);
        service.redeemLocked(voucher, null);

        assertEquals(2, voucher.getUsedCount());
        assertEquals("ACTIVE", VoucherLifecycleService.statusOf(voucher, Date.from(NOW)));
    }

    @Test
    void rejectsPausedNotStartedExpiredAndExhaustedVouchers() {
        Voucher paused = activeVoucher();
        paused.setActive(false);
        assertRejected(paused, "PAUSED");

        Voucher future = activeVoucher();
        future.setStartAt(Date.from(NOW.plusSeconds(60)));
        assertRejected(future, "NOT_STARTED");

        Voucher expired = activeVoucher();
        expired.setEndAt(Date.from(NOW.minusSeconds(60)));
        assertRejected(expired, "EXPIRED");

        Voucher exhausted = activeVoucher();
        exhausted.setUsageLimit(1);
        exhausted.setUsedCount(1);
        assertRejected(exhausted, "EXHAUSTED");
    }

    @Test
    void lastUsageCannotIncrementPastLimit() {
        Voucher voucher = activeVoucher();
        voucher.setUsageLimit(1);

        service.redeemLocked(voucher, null);

        assertEquals(1, voucher.getUsedCount());
        assertThrows(ResponseStatusException.class, () -> service.redeemLocked(voucher, null));
        assertEquals(1, voucher.getUsedCount());
    }

    private void assertRejected(Voucher voucher, String status) {
        assertEquals(status, VoucherLifecycleService.statusOf(voucher, Date.from(NOW)));
        assertThrows(ResponseStatusException.class, () -> service.validateForUse(voucher, null));
    }

    private Voucher activeVoucher() {
        Voucher voucher = new Voucher();
        voucher.setCode("TEST");
        voucher.setDiscountPercent(10);
        voucher.setActive(true);
        voucher.setUsedCount(0);
        return voucher;
    }
}
