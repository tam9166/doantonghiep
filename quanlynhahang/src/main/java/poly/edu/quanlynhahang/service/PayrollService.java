package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.quanlynhahang.dto.PayrollRowResponse;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Timekeeping;
import poly.edu.quanlynhahang.entity.WorkSchedule;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;

@Service
public class PayrollService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> PAYROLL_ROLES = Set.of(
            "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER");
    private final AccountRepository accountRepository;
    private final WorkScheduleRepository scheduleRepository;
    private final TimekeepingRepository timekeepingRepository;

    public PayrollService(AccountRepository accountRepository, WorkScheduleRepository scheduleRepository,
                          TimekeepingRepository timekeepingRepository) {
        this.accountRepository = accountRepository;
        this.scheduleRepository = scheduleRepository;
        this.timekeepingRepository = timekeepingRepository;
    }

    @Transactional(readOnly = true)
    public List<PayrollRowResponse> calculate(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        Date scheduleStart = Date.from(start.atStartOfDay(BUSINESS_ZONE).toInstant());
        Date scheduleEnd = Date.from(end.atStartOfDay(BUSINESS_ZONE).toInstant());
        List<WorkSchedule> schedules = scheduleRepository
                .findByWorkDateBetweenOrderByWorkDateAsc(scheduleStart, scheduleEnd);
        List<Timekeeping> attendance = timekeepingRepository
                .findByWorkDateBetweenOrderByWorkDateAsc(start, end);

        Map<String, Account> staff = accountRepository.findAllWithAuthorities().stream()
                .filter(account -> !Boolean.FALSE.equals(account.getEnabled()))
                .filter(account -> PAYROLL_ROLES.contains(primaryRole(account)))
                .collect(Collectors.toMap(Account::getUsername, Function.identity()));
        Map<String, Long> scheduled = schedules.stream().collect(Collectors.groupingBy(
                value -> value.getAccount().getUsername(), Collectors.counting()));
        Map<String, Long> worked = attendance.stream()
                .filter(value -> value.getCheckInTime() != null)
                .collect(Collectors.groupingBy(value -> value.getAccount().getUsername(), Collectors.counting()));

        Map<String, PayrollRowResponse> result = new LinkedHashMap<>();
        staff.values().stream().sorted(java.util.Comparator.comparing(Account::getFullname))
                .filter(account -> scheduled.containsKey(account.getUsername()) || worked.containsKey(account.getUsername()))
                .forEach(account -> {
                    long scheduledCount = scheduled.getOrDefault(account.getUsername(), 0L);
                    long workedCount = worked.getOrDefault(account.getUsername(), 0L);
                    BigDecimal rate = account.getShiftRate();
                    BigDecimal total = rate == null ? null : rate.multiply(BigDecimal.valueOf(workedCount));
                    result.put(account.getUsername(), new PayrollRowResponse(account.getUsername(), account.getFullname(),
                            primaryRole(account), scheduledCount, workedCount, rate, total, rate != null));
                });
        return List.copyOf(result.values());
    }

    private String primaryRole(Account account) {
        if (account.getAuthorities() == null) return "ROLE_USER";
        List<String> roles = account.getAuthorities().stream().map(Authority::getRole)
                .map(role -> role.getName().toUpperCase(Locale.ROOT))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role).toList();
        return List.of("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_KITCHEN", "ROLE_WAITER", "ROLE_CASHIER")
                .stream().filter(roles::contains).findFirst().orElse("ROLE_USER");
    }
}
