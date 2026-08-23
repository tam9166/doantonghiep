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
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.WaitlistStatus;
import poly.edu.quanlynhahang.repository.ReservationWaitlistRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

class ReservationWaitlistServiceTest {

    private final ReservationWaitlistRepository waitlistRepository = mock(ReservationWaitlistRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final TableAreaRepository areaRepository = mock(TableAreaRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final RestaurantBusinessHoursService businessHoursService = mock(RestaurantBusinessHoursService.class);
    private final ReservationWaitlistService service = new ReservationWaitlistService(
            waitlistRepository, reservationRepository, areaRepository,
            notificationService, activityLogService, businessHoursService);

    private ReservationWaitlistServiceTest() {
        when(businessHoursService.isServiceWindow(any(), any())).thenReturn(true);
        when(businessHoursService.getFormattedHours()).thenReturn("09:00 - 22:00");
    }

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

        when(waitlistRepository.findLockedById(20L)).thenReturn(Optional.of(entry));

        assertThrows(ResponseStatusException.class, () -> service.contact(20L, new WaitlistActionRequest()));
        verify(waitlistRepository).findLockedById(20L);
    }

    @Test
    void publicLookupRequiresMatchingPhoneNumber() {
        assertThrows(ResponseStatusException.class, () -> service.getPublic("WL20260704-001", " "));
        org.mockito.Mockito.verifyNoInteractions(waitlistRepository);
    }

    @Test
    void conversionRequiresAnExistingMatchingUnclaimedReservation() {
        ReservationWaitlist entry = waitingEntry(30L);
        when(waitlistRepository.findLockedById(30L)).thenReturn(Optional.of(entry));

        assertThrows(ResponseStatusException.class,
                () -> service.convert(30L, new WaitlistActionRequest()));

        WaitlistActionRequest action = new WaitlistActionRequest();
        action.setLinkedReservationCode("MV-20260823-ABCDEF12");
        Reservation reservation = new Reservation();
        reservation.setReservationCode(action.getLinkedReservationCode());
        reservation.setCustomerPhone(entry.getCustomerPhone());
        reservation.setReservationDate(entry.getReservationDate());
        reservation.setGuestCount(entry.getGuestCount());
        when(reservationRepository.findLockedByReservationCode(action.getLinkedReservationCode()))
                .thenReturn(Optional.of(reservation));
        when(waitlistRepository.findByLinkedReservationCode(action.getLinkedReservationCode()))
                .thenReturn(Optional.empty());
        when(waitlistRepository.save(entry)).thenReturn(entry);

        WaitlistResponse converted = service.convert(30L, action);

        assertEquals(WaitlistStatus.CONVERTED, converted.getStatus());
        assertEquals(action.getLinkedReservationCode(), converted.getLinkedReservationCode());
        verify(reservationRepository).findLockedByReservationCode(action.getLinkedReservationCode());
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

    private ReservationWaitlist waitingEntry(Long id) {
        ReservationWaitlist entry = new ReservationWaitlist();
        entry.setId(id);
        entry.setWaitlistCode("WL20260823-" + id);
        entry.setCustomerName("Nguyen Van A");
        entry.setCustomerPhone("0912345678");
        entry.setReservationDate(LocalDate.now().plusDays(1));
        entry.setPreferredStartTime(LocalTime.of(18, 0));
        entry.setPreferredEndTime(LocalTime.of(20, 0));
        entry.setGuestCount(4);
        entry.setStatus(WaitlistStatus.WAITING);
        return entry;
    }
}
