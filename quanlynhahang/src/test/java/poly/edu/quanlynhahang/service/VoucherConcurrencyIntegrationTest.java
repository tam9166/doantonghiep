package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.VoucherRepository;

@SpringBootTest
class VoucherConcurrencyIntegrationTest {
    @Autowired VoucherRepository repository;
    @Autowired VoucherLifecycleService lifecycleService;
    private Long voucherId;

    @AfterEach
    void cleanup() {
        if (voucherId != null) repository.deleteById(voucherId);
    }

    @Test
    @Timeout(30)
    void concurrentLastRedemptionAllowsExactlyOneRequest() throws Exception {
        Voucher voucher = new Voucher();
        voucher.setCode("LAST-" + UUID.randomUUID().toString().substring(0, 8));
        voucher.setDiscountPercent(10);
        voucher.setCreateDate(new Date());
        voucher.setActive(true);
        voucher.setUsageLimit(1);
        voucher.setUsedCount(0);
        voucher.setIsUsed(false);
        voucher = repository.saveAndFlush(voucher);
        voucherId = voucher.getId();
        String code = voucher.getCode();

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> redeem(start, code));
            Future<Boolean> second = executor.submit(() -> redeem(start, code));
            start.countDown();
            List<Boolean> outcomes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));

            assertEquals(1, outcomes.stream().filter(Boolean::booleanValue).count(), outcomes::toString);
            assertEquals(1, repository.findById(voucherId).orElseThrow().getUsedCount());
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean redeem(CountDownLatch start, String code) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        try {
            lifecycleService.redeem(code, null);
            return true;
        } catch (RuntimeException expectedConflict) {
            return false;
        }
    }
}
