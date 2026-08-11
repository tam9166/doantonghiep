package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final OrderCheckoutService orderCheckoutService = mock(OrderCheckoutService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(
                reservationRepository,
                preorderItemRepository,
                mock(PaymentIntentRepository.class),
                mock(ReservationStatusHistoryRepository.class),
                mock(RestaurantTableRepository.class),
                mock(TableAreaRepository.class),
                mock(ProductRepository.class),
                mock(VoucherRepository.class),
                mock(ReservationVoucherUsageRepository.class),
                mock(NotificationService.class),
                mock(ActivityLogService.class),
                mock(ReservationRealtimeService.class),
                messagingTemplate,
                mock(DepositPolicyService.class),
                new ReservationStateMachine(),
                mock(PaymentCapabilityService.class),
                orderCheckoutService,
                new BigDecimal("0.50"), 15, 30);
    }

    @Test
    void rejectsLookupWithoutAnyIdentifier() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation(null, null, null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void rejectsMalformedEmailBeforeQueryingDatabase() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation(null, null, "not-an-email"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void searchesEmailCaseInsensitivelyAndDoesNotRevealWhetherItExists() {
        when(reservationRepository.findFirstByCustomerEmailIgnoreCaseOrderByCreatedAtDesc("guest@example.com"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation(null, null, "Guest@Example.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(reservationRepository).findFirstByCustomerEmailIgnoreCaseOrderByCreatedAtDesc("guest@example.com");
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

    private Reservation reservationForDepositPayment(BigDecimal depositAmount, BigDecimal totalAmount) {
        Reservation reservation = new Reservation();
        reservation.setReservationStatus(ReservationStatus.PENDING);
        reservation.setDepositAmount(depositAmount);
        reservation.setTotalAmount(totalAmount);
        return reservation;
    }
}
