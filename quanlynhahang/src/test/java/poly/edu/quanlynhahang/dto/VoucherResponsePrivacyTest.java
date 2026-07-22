package poly.edu.quanlynhahang.dto;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Voucher;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoucherResponsePrivacyTest {

    @Test
    void voucherResponseExposesOnlyAssignedUsername() throws Exception {
        Account account = new Account();
        account.setUsername("customer-a");
        account.setPassword("bcrypt-hash");
        account.setTokenVersion(6L);
        account.setEmail("customer@example.test");
        Voucher voucher = new Voucher();
        voucher.setCode("WELCOME10");
        voucher.setDiscountPercent(10);
        voucher.setAccount(account);

        String json = new ObjectMapper().writeValueAsString(VoucherResponse.from(voucher));

        assertTrue(json.contains("customer-a"));
        assertFalse(json.contains("bcrypt-hash"));
        assertFalse(json.contains("tokenVersion"));
        assertFalse(json.contains("customer@example.test"));
        assertFalse(json.contains("account\":"));
    }
}
