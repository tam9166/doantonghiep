package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import poly.edu.quanlynhahang.controller.TimekeepingController;
import poly.edu.quanlynhahang.dto.TimekeepingCheckRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.repository.AccountRepository;

@SpringBootTest
class AttendanceConcurrencyIntegrationTest {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired TimekeepingController controller;
    @Autowired AccountRepository accountRepository;
    @Autowired JdbcTemplate jdbc;

    private String username;

    @AfterEach
    void cleanup() {
        if (username == null) return;
        jdbc.update("DELETE FROM timekeeping WHERE username = ?", username);
        accountRepository.deleteById(username);
        username = null;
    }

    @Test
    @Timeout(30)
    void concurrentDoubleCheckInPersistsExactlyOneAttendanceRow() throws Exception {
        username = "reg_attendance_" + UUID.randomUUID().toString().substring(0, 8);
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("not-used-by-test");
        account.setFullname(username);
        account.setEmail(username + "@example.test");
        accountRepository.saveAndFlush(account);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> checkInAfter(start));
            Future<Integer> second = executor.submit(() -> checkInAfter(start));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            statuses.add(first.get(20, TimeUnit.SECONDS));
            statuses.add(second.get(20, TimeUnit.SECONDS));

            assertEquals(1, statuses.stream().filter(status -> status == 200).count(), statuses::toString);
            assertEquals(1, statuses.stream().filter(status -> status == 400).count(), statuses::toString);
            assertEquals(1, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM timekeeping
                    WHERE username = ? AND work_date = ? AND check_in_time IS NOT NULL
                    """, Integer.class, username, LocalDate.now(VIETNAM_ZONE)));
        } finally {
            executor.shutdownNow();
        }
    }

    private int checkInAfter(CountDownLatch start) throws Exception {
        start.await(5, TimeUnit.SECONDS);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                username, "", List.of(new SimpleGrantedAuthority("ROLE_WAITER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            ResponseEntity<?> response = controller.performCheck(
                    authentication, new TimekeepingCheckRequest("IN"));
            return response.getStatusCode().value();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
