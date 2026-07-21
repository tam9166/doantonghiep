package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import poly.edu.quanlynhahang.entity.Account;

class EmployeeResponsePrivacyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void employeeSummaryNeverContainsAccountCredentials() throws Exception {
        Account account = new Account();
        account.setUsername("waiter-a");
        account.setFullname("Waiter A");
        account.setPassword("bcrypt-hash");
        account.setTokenVersion(4L);
        account.setEnabled(true);
        account.setMustChangePassword(true);

        String json = objectMapper.writeValueAsString(EmployeeSummaryResponse.from(account));

        assertFalse(json.contains("password"));
        assertFalse(json.contains("tokenVersion"));
        assertFalse(json.contains("enabled"));
        assertFalse(json.contains("mustChangePassword"));
    }
}
