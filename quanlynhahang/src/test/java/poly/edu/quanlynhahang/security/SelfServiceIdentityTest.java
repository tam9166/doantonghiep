package poly.edu.quanlynhahang.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import poly.edu.quanlynhahang.controller.ScheduleController;
import poly.edu.quanlynhahang.controller.ServiceZoneController;
import poly.edu.quanlynhahang.controller.TimekeepingController;
import poly.edu.quanlynhahang.dto.TimekeepingCheckRequest;
import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.ServiceZoneAssignmentRepository;
import poly.edu.quanlynhahang.repository.TimekeepingRepository;
import poly.edu.quanlynhahang.repository.WorkScheduleRepository;

class SelfServiceIdentityTest {

    private static final String ACTOR = "waiter-a";

    @Test
    void timekeepingStatusUsesAuthenticatedActor() {
        TimekeepingRepository repository = mock(TimekeepingRepository.class);
        TimekeepingController controller = new TimekeepingController();
        ReflectionTestUtils.setField(controller, "timekeepingRepository", repository);

        controller.getTodayStatus(authentication(ACTOR));

        verify(repository).findByAccountUsernameAndWorkDate(eq(ACTOR), any(LocalDate.class));
    }

    @Test
    void personalScheduleUsesAuthenticatedActor() {
        WorkScheduleRepository repository = mock(WorkScheduleRepository.class);
        ScheduleController controller = new ScheduleController();
        ReflectionTestUtils.setField(controller, "workScheduleRepository", repository);

        controller.getMySchedules(authentication(ACTOR), "2026-07-01", "2026-07-31");

        verify(repository).findByAccountUsernameAndWorkDateBetweenOrderByWorkDateAsc(
                eq(ACTOR), any(Date.class), any(Date.class));
    }

    @Test
    void personalServiceZoneUsesAuthenticatedActor() {
        ServiceZoneAssignmentRepository repository = mock(ServiceZoneAssignmentRepository.class);
        ServiceZoneController controller = new ServiceZoneController();
        ReflectionTestUtils.setField(controller, "zoneRepo", repository);

        controller.getMyZones(authentication(ACTOR), "2026-07-22");

        verify(repository).findByAccountUsernameAndWorkDate(eq(ACTOR), any(Date.class));
    }

    @Test
    void checkInUsesAuthenticatedActorInsteadOfClientIdentity() {
        TimekeepingRepository timekeepingRepository = mock(TimekeepingRepository.class);
        WorkScheduleRepository workScheduleRepository = mock(WorkScheduleRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        Account account = new Account();
        account.setUsername(ACTOR);
        when(accountRepository.findLockedByUsername(ACTOR)).thenReturn(java.util.Optional.of(account));
        when(workScheduleRepository.findByAccountUsernameAndWorkDate(eq(ACTOR), any(Date.class)))
                .thenReturn(Collections.emptyList());
        when(timekeepingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TimekeepingController controller = new TimekeepingController();
        ReflectionTestUtils.setField(controller, "timekeepingRepository", timekeepingRepository);
        ReflectionTestUtils.setField(controller, "workScheduleRepository", workScheduleRepository);
        ReflectionTestUtils.setField(controller, "accountRepository", accountRepository);

        controller.performCheck(authentication(ACTOR), new TimekeepingCheckRequest("IN"));

        verify(accountRepository).findLockedByUsername(ACTOR);
        verify(accountRepository, org.mockito.Mockito.never()).findById(ACTOR);
        verify(timekeepingRepository).findByAccountUsernameAndWorkDate(eq(ACTOR), any(LocalDate.class));
    }

    private Authentication authentication(String username) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(username);
        return authentication;
    }
}
