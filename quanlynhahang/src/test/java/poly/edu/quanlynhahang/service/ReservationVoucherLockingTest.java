package poly.edu.quanlynhahang.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReservationVoucherLockingTest {
    @Test
    void reservationCreationLocksVoucherBeforeConsumingItsSingleUse() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/poly/edu/quanlynhahang/service/ReservationService.java"));

        assertThat(source).contains(
                "markAsUsed\n                ? voucherRepository.findLockedByCode(code)\n                : voucherRepository.findByCode(code)");
        assertThat(source).contains("voucher.setIsUsed(true)");
    }
}
