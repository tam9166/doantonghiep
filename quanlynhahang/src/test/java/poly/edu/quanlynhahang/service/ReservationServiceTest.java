package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReservationPreorderItemRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.ReservationStatusHistoryRepository;
import poly.edu.quanlynhahang.repository.ReservationVoucherUsageRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

class ReservationServiceTest {
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ReservationPreorderItemRepository preorderItemRepository = mock(ReservationPreorderItemRepository.class);
    private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);
    private final OrderCheckoutService orderCheckoutService = mock(OrderCheckoutService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final DepositPolicyService depositPolicyService = mock(DepositPolicyService.class);
    private final RestaurantBusinessHoursService businessHoursService = mock(RestaurantBusinessHoursService.class);
    private ReservationService service;

    @BeforeEach
    void setUp() {
        // Mock business hours for tests
        when(businessHoursService.isOpen(any())).thenReturn(true);
        when(businessHoursService.acceptsOrders(any())).thenReturn(true);
        when(businessHoursService.getOpeningTime()).thenReturn(LocalTime.of(9, 0));
        when(businessHoursService.getClosingTime()).thenReturn(LocalTime.of(22, 0));
        when(businessHoursService.getLastOrderTime()).thenReturn(LocalTime.of(21, 30));
        when(businessHoursService.getFormattedHours()).thenReturn("09:00 - 22:00");
        
        service = new ReservationService(
                reservationRepository,
                preorderItemRepository,
                mock(PaymentIntentRepository.class),
                mock(ReservationStatusHistoryRepository.class),
                tableRepository,
                mock(TableAreaRepository.class),
                mock(ProductRepository.class),
                mock(VoucherRepository.class),
                mock(ReservationVoucherUsageRepository.class),
                mock(NotificationService.class),
                mock(ActivityLogService.class),
                mock(ReservationRealtimeService.class),
                messagingTemplate,
                depositPolicyService,
                new ReservationStateMachine(),
                mock(PaymentCapabilityService.class),
                orderCheckoutService,
                mock(AutoTableAssignmentService.class),
                mock(RestaurantCapacityService.class),
                mock(RestaurantSettingsService.class),
                businessHoursService,
                new BigDecimal("0.50"), 15, 15);
    }

    @Test
    void rejectsLookupWithoutBookingCode() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation(null, "0901234567", null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }
    
    @Test
    void rejectsLookupWithoutPhone() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation("MV-TEST-123", null, null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }
    
    @Test
    void rejectsLookupWithInvalidPhoneFormat() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation("MV-TEST-123", "invalid-phone", null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }
    
    @Test
    void requiresBothCodeAndPhoneToFindReservation() {
        Reservation reservation = new Reservation();
        reservation.setReservationCode("MV-TEST-123");
        reservation.setCustomerPhone("+84901234567");
        
        when(reservationRepository.findByReservationCode("MV-TEST-123"))
                .thenReturn(Optional.of(reservation));

        // Should fail because phone doesn't match
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation("MV-TEST-123", "0901234567", null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void expiryJobLeavesRecentAndFutureReservationsUntouched() {
        Reservation pendingPayment = new Reservation();
        pendingPayment.setReservationStatus(ReservationStatus.PENDING);
        pendingPayment.setDepositAmount(BigDecimal.TEN);
        pendingPayment.setCreatedAt(new Date());

        Reservation futureConfirmed = new Reservation();
        futureConfirmed.setReservationStatus(ReservationStatus.CONFIRMED);
        futureConfirmed.setReservationDate(LocalDate.now().plusDays(1));
        futureConfirmed.setArrivalTime(LocalTime.NOON);

        when(reservationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingPayment, futureConfirmed));

        service.expireStaleReservations();

        verify(reservationRepository, never()).save(org.mockito.ArgumentMatchers.any(Reservation.class));
    }

    @Test
    void confirmingPreorderCreatesExactlyOneKitchenOrder() {
        RestaurantTable table = new RestaurantTable();
        table.setId(9);
        table.setName("B09");
        Reservation reservation = new Reservation();
        reservation.setId(24L);
        reservation.setReservationCode("MV-TEST-0024");
        reservation.setTable(table);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setArrivalTime(LocalTime.of(18, 0));
        reservation.setExpectedDurationMinutes(120);
        reservation.setGuestCount(2);
        reservation.setDepositAmount(BigDecimal.ZERO);
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation.setPreorderEnabled(true);
        ReservationPreorderItem preorder = new ReservationPreorderItem();
        preorder.setProductId(5);
        preorder.setQuantity(2);

        when(reservationRepository.findById(24L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(tableRepository.findLockedByIdIn(List.of(9))).thenReturn(List.of(table));
        when(preorderItemRepository.findByReservationIdOrderByIdAsc(24L)).thenReturn(List.of(preorder));
        when(orderCheckoutService.dispatchReservationPreorder(reservation, List.of(preorder))).thenReturn(91);
        when(reservationRepository.save(org.mockito.ArgumentMatchers.any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.confirm(24L, null);

        assertEquals(91, result.getKitchenOrderId());
        verify(orderCheckoutService).dispatchReservationPreorder(reservation, List.of(preorder));
        verify(messagingTemplate).convertAndSend("/topic/kitchen", "NEW_ORDER");
    }

    @Test
    void returnsOnlyReservationsCreatedByTheAuthenticatedUser() {
        Reservation reservation = new Reservation();
        reservation.setId(7L);
        reservation.setReservationCode("MV-TEST-0007");
        when(reservationRepository.findByCreatedByOrderByCreatedAtDesc("customer"))
                .thenReturn(List.of(reservation));
        when(preorderItemRepository.findByReservationIdOrderByIdAsc(7L)).thenReturn(List.of());

        var result = service.getReservationsForUser("customer");

        assertEquals(1, result.size());
        assertEquals("MV-TEST-0007", result.getFirst().getReservationCode());
        verify(reservationRepository).findByCreatedByOrderByCreatedAtDesc("customer");
    }

    @Test
    void markingDepositPaidRecordsPartialPaymentAndRemainingBalance() {
        Reservation reservation = reservationForDepositPayment(BigDecimal.valueOf(500_000), BigDecimal.valueOf(1_000_000));
        when(reservationRepository.findById(31L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        service.markDepositPaid(31L, null);

        assertEquals(DepositStatus.PAID, reservation.getDepositStatus());
        assertEquals(BigDecimal.valueOf(500_000), reservation.getPaidAmount());
        assertEquals(BigDecimal.valueOf(500_000), reservation.getRemainingAmount());
        assertEquals(PaymentStatus.PARTIALLY_PAID, reservation.getPaymentStatus());
    }

    @Test
    void markingDepositPaidMarksReservationFullyPaidWhenDepositCoversTotal() {
        Reservation reservation = reservationForDepositPayment(BigDecimal.valueOf(1_000_000), BigDecimal.valueOf(1_000_000));
        when(reservationRepository.findById(32L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        service.markDepositPaid(32L, null);

        assertEquals(BigDecimal.valueOf(1_000_000), reservation.getPaidAmount());
        assertEquals(BigDecimal.ZERO, reservation.getRemainingAmount());
        assertEquals(PaymentStatus.PAID, reservation.getPaymentStatus());
    }

    @Test
    void noShowWithPaidDepositForfeitsThePolicyDepositAndReleasesTheTable() {
        Reservation reservation = noShowReservation(DepositStatus.PAID);
        reservation.setDepositAmount(BigDecimal.valueOf(500_000));
        reservation.setPaidAmount(BigDecimal.valueOf(500_000));
        when(reservationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(reservation));
        when(depositPolicyService.calculateNoShowForfeiture(reservation)).thenReturn(BigDecimal.valueOf(500_000));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        service.expireStaleReservations();

        assertEquals(ReservationStatus.NO_SHOW, reservation.getReservationStatus());
        assertEquals(DepositStatus.FORFEITED, reservation.getDepositStatus());
        assertEquals(0, reservation.getTable().getIsOccupied());
        verify(depositPolicyService).calculateNoShowForfeiture(reservation);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void expiresStaleReservationsWithDepositExpiry() {
        Reservation pendingReservation = new Reservation();
        pendingReservation.setReservationStatus(ReservationStatus.PENDING);
        pendingReservation.setDepositAmount(BigDecimal.TEN);
        pendingReservation.setCreatedAt(java.util.Date.from(java.time.Instant.now().minusSeconds(25 * 3600))); // 25 hours ago
        
        when(reservationRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(pendingReservation));
                
        service.expireStaleReservations();
        
        // Should be expired due to deposit expiry
        assertEquals(ReservationStatus.EXPIRED, pendingReservation.getReservationStatus());
    }
    
    @Test
    void expiresNoShowReservations() {
        RestaurantTable table = new RestaurantTable();
        table.setId(44);
        table.setIsOccupied(2);
        table.setReservedTime("MV-TEST-NOSHOW");
        
        Reservation reservation = new Reservation();
        reservation.setReservationStatus(ReservationStatus.IN_SERVICE);
        reservation.setReservationDate(LocalDate.now().minusDays(1));
        reservation.setArrivalTime(LocalTime.of(19, 0));
        reservation.setTable(table);
        reservation.setDepositStatus(DepositStatus.FORFEITED);
        reservation.setDepositAmount(BigDecimal.valueOf(500_000));
        reservation.setPaidAmount(BigDecimal.valueOf(500_000));
        
        when(depositPolicyService.calculateNoShowForfeiture(reservation))
            .thenReturn(BigDecimal.valueOf(500_000));
        when(reservationRepository.findAllByOrderByCreatedAtDesc())
            .thenReturn(List.of(reservation));
            
        service.expireStaleReservations();
        
        assertEquals(ReservationStatus.NO_SHOW, reservation.getReservationStatus());
        assertEquals(ReservationStatus.NO_SHOW, reservation.getReservationStatus());
        verify(reservationRepository).save(reservation);
    }

    private Reservation reservationForDepositPayment(BigDecimal depositAmount, BigDecimal totalAmount) {
        Reservation reservation = new Reservation();
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation.setDepositAmount(depositAmount);
        reservation.setTotalAmount(totalAmount);
        return reservation;
    }

    private Reservation noShowReservation(DepositStatus depositStatus) {
        RestaurantTable table = new RestaurantTable();
        table.setId(44);
        table.setIsOccupied(2);
        table.setReservedTime("MV-TEST-NOSHOW");
        Reservation reservation = new Reservation();
        reservation.setId(44L);
        reservation.setReservationCode("MV-TEST-NOSHOW");
        reservation.setReservationStatus(ReservationStatus.DEPOSIT_PAID);
        reservation.setDepositStatus(depositStatus);
        LocalDateTime overdueArrival = LocalDateTime.now().minusMinutes(16);
        reservation.setReservationDate(overdueArrival.toLocalDate());
        reservation.setArrivalTime(overdueArrival.toLocalTime());
        reservation.setTable(table);
        return reservation;
    }
}
