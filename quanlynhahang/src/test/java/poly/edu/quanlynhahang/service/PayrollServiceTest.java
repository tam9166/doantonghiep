package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.dto.PayrollRowResponse;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;

class PayrollServiceTest {
    @Test
    void calculatesFromPersistedRateAndFlagsMissingRate() {
        AccountRepository accounts = mock(AccountRepository.class);
        WorkScheduleRepository schedules = mock(WorkScheduleRepository.class);
        TimekeepingRepository attendance = mock(TimekeepingRepository.class);
        Account paid = account("waiter", "Phục vụ", "ROLE_WAITER", new BigDecimal("250000"));
        Account missing = account("cashier", "Thu ngân", "ROLE_CASHIER", null);
        when(accounts.findAllWithAuthorities()).thenReturn(List.of(paid, missing));
        when(schedules.findByWorkDateBetweenOrderByWorkDateAsc(any(), any()))
                .thenReturn(List.of(schedule(paid), schedule(paid), schedule(missing)));
        when(attendance.findByWorkDateBetweenOrderByWorkDateAsc(any(), any()))
                .thenReturn(List.of(attendance(paid), attendance(paid), attendance(missing)));

        List<PayrollRowResponse> result = new PayrollService(accounts, schedules, attendance)
                .calculate(YearMonth.of(2026, 8));

        PayrollRowResponse waiter = result.stream().filter(row -> row.username().equals("waiter")).findFirst().orElseThrow();
        PayrollRowResponse cashier = result.stream().filter(row -> row.username().equals("cashier")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("500000"), waiter.totalSalary());
        assertEquals(2, waiter.scheduledShifts());
        assertFalse(cashier.rateConfigured());
        assertEquals(null, cashier.totalSalary());
    }

    private Account account(String username, String fullname, String roleName, BigDecimal rate) {
        Account account = new Account();
        account.setUsername(username);
        account.setFullname(fullname);
        account.setEnabled(true);
        account.setShiftRate(rate);
        Role role = new Role();
        role.setName(roleName);
        Authority authority = new Authority();
        authority.setRole(role);
        authority.setAccount(account);
        account.setAuthorities(List.of(authority));
        return account;
    }

    private WorkSchedule schedule(Account account) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setAccount(account);
        schedule.setWorkDate(Date.from(LocalDate.of(2026, 8, 2).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()));
        return schedule;
    }

    private Timekeeping attendance(Account account) {
        Timekeeping value = new Timekeeping();
        value.setAccount(account);
        value.setWorkDate(LocalDate.of(2026, 8, 2));
        value.setCheckInTime(LocalTime.of(6, 0));
        return value;
    }
}
