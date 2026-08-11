package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.WaitlistActionRequest;
import poly.edu.quanlynhahang.dto.WaitlistRequest;
import poly.edu.quanlynhahang.dto.WaitlistResponse;
import poly.edu.quanlynhahang.entity.ReservationWaitlist;
import poly.edu.quanlynhahang.entity.WaitlistStatus;
import poly.edu.quanlynhahang.repository.ReservationWaitlistRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

class ReservationWaitlistServiceTest {

    private final ReservationWaitlistRepository waitlistRepository = mock(ReservationWaitlistRepository.class);
    private final TableAreaRepository areaRepository = mock(TableAreaRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final ReservationWaitlistService service = new ReservationWaitlistService(
            waitlistRepository, areaRepository, notificationService, activityLogService);

    @Test
    void createsWaitingEntryWithNormalizedPhone() {
        WaitlistRequest request = new WaitlistRequest();
        request.setCustomerName("Nguyen Van A");
        request.setCustomerPhone(" 0912 345 678 ");
        request.setReservationDate(LocalDate.now().plusDays(1).toString());
        request.setPreferredStartTime("18:00");
        request.setPreferredEndTime("20:00");
        request.setGuestCount(4);

        when(waitlistRepository.countByReservationDate(any())).thenReturn(0L);
        when(waitlistRepository.save(any())).thenAnswer(invocation -> {
            ReservationWaitlist entry = invocation.getArgument(0);
            entry.setId(10L);
            return entry;
        });

        WaitlistResponse response = service.create(request);

        assertEquals(WaitlistStatus.WAITING, response.getStatus());
        assertEquals("0912****78", response.getCustomerPhone());
        verify(notificationService).createNotification(eq("WAITLIST_NEW"), any(), any(), eq("ROLE_MANAGER"), any(), any(), eq("10"));
    }

    @Test
    void blocksTerminalEntryFromContactUpdate() {
        ReservationWaitlist entry = new ReservationWaitlist();
        entry.setId(20L);
        entry.setWaitlistCode("WL20260704-001");
        entry.setCustomerName("Nguyen Van A");
        entry.setCustomerPhone("0912345678");
        entry.setReservationDate(LocalDate.now().plusDays(1));
        entry.setPreferredStartTime(LocalTime.of(18, 0));
        entry.setPreferredEndTime(LocalTime.of(20, 0));
        entry.setGuestCount(4);
        entry.setStatus(WaitlistStatus.CANCELLED);

        when(waitlistRepository.findById(20L)).thenReturn(Optional.of(entry));

        assertThrows(ResponseStatusException.class, () -> service.contact(20L, new WaitlistActionRequest()));
    }

    @Test
    void publicLookupRequiresMatchingPhoneNumber() {
        assertThrows(ResponseStatusException.class, () -> service.getPublic("WL20260704-001", " "));
        org.mockito.Mockito.verifyNoInteractions(waitlistRepository);
    }

    @Test
    void storesGroupTooLargeReasonWhenEventHallHasNoCapacity() {
        WaitlistRequest request = new WaitlistRequest();
        request.setCustomerName("Nguyen Van A");
        request.setCustomerPhone("0912345678");
        request.setReservationDate(LocalDate.now().plusDays(1).toString());
        request.setPreferredStartTime("18:00");
        request.setPreferredEndTime("20:00");
        request.setGuestCount(25);
        request.setOverflowReason("GROUP_TOO_LARGE");
        AtomicReference<ReservationWaitlist> savedEntry = new AtomicReference<>();

        when(waitlistRepository.countByReservationDate(any())).thenReturn(0L);
        when(waitlistRepository.save(any())).thenAnswer(invocation -> {
            ReservationWaitlist entry = invocation.getArgument(0);
            entry.setId(11L);
            savedEntry.set(entry);
            return entry;
        });

        service.create(request);

        assertEquals("GROUP_TOO_LARGE", savedEntry.get().getOverflowReason());
    }
}
