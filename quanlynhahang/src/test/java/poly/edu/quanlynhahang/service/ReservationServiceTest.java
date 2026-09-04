package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReservationPreorderItemRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.ReservationStatusHistoryRepository;
import poly.edu.quanlynhahang.repository.ReservationContactLogRepository;
import poly.edu.quanlynhahang.dto.ReservationContactUpdateRequest;
import poly.edu.quanlynhahang.entity.ContactStatus;
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
    private final ReservationContactLogRepository contactLogRepository = mock(ReservationContactLogRepository.class);
    private final TableAreaRepository areaRepository = mock(TableAreaRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final ActivityLogService activityLogService = mock(ActivityLogService.class);
    private final ReservationRealtimeService realtimeService = mock(ReservationRealtimeService.class);
    private final PaymentCapabilityService paymentCapabilityService = mock(PaymentCapabilityService.class);
    private final RestaurantCapacityService capacityService = mock(RestaurantCapacityService.class);
    private final RestaurantSettingsService settingsService = mock(RestaurantSettingsService.class);
    private final TableLifecycleService tableLifecycleService = mock(TableLifecycleService.class);
    private final SqlServerApplicationLockService applicationLockService = mock(SqlServerApplicationLockService.class);
    private final TableAreaReadinessService areaReadinessService = mock(TableAreaReadinessService.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final MenuAvailabilityService menuAvailabilityService = mock(MenuAvailabilityService.class);
    private ReservationService service;

    @BeforeEach
    void setUp() {
        // Mock business hours for tests
        when(businessHoursService.isOpen(any())).thenReturn(true);
        when(businessHoursService.acceptsOrders(any())).thenReturn(true);
        when(businessHoursService.acceptsReservationArrival(any())).thenReturn(true);
        when(businessHoursService.getOpeningTime()).thenReturn(LocalTime.of(9, 0));
        when(businessHoursService.getClosingTime()).thenReturn(LocalTime.of(22, 0));
        when(businessHoursService.getLastOrderTime()).thenReturn(LocalTime.of(21, 30));
        when(businessHoursService.getFormattedHours()).thenReturn("09:00 - 22:00");
        when(areaReadinessService.evaluate(any())).thenReturn(
                new TableAreaReadinessService.Readiness(true, "Sẵn sàng nhận đặt bàn", 2, 8));
        
        service = new ReservationService(
                reservationRepository,
                preorderItemRepository,
                mock(PaymentIntentRepository.class),
                mock(ReservationStatusHistoryRepository.class),
                tableRepository,
                areaRepository,
                productRepository,
                mock(VoucherRepository.class),
                mock(ReservationVoucherUsageRepository.class),
                notificationService,
                activityLogService,
                realtimeService,
                messagingTemplate,
                depositPolicyService,
                new ReservationStateMachine(),
                paymentCapabilityService,
                orderCheckoutService,
                mock(AutoTableAssignmentService.class),
                capacityService,
                settingsService,
                businessHoursService,
                contactLogRepository,
                tableLifecycleService,
                applicationLockService,
                areaReadinessService,
                menuAvailabilityService,
                mock(PlatformTransactionManager.class),
                new BigDecimal("0.50"), 15, 20);
    }

    @Test
    void everyContactAttemptAppendsAHistoryLog() {
        Reservation reservation = new Reservation();
        reservation.setId(9L);
        when(reservationRepository.findById(9L)).thenReturn(java.util.Optional.of(reservation));
        when(reservationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateContactStatus(9L,
                new ReservationContactUpdateRequest(ContactStatus.UNREACHABLE, "Không bắt máy"));
        service.updateContactStatus(9L,
                new ReservationContactUpdateRequest(ContactStatus.CONFIRMED_BY_CUSTOMER, "Đã xác nhận lại"));

        org.mockito.Mockito.verify(contactLogRepository, org.mockito.Mockito.times(2)).save(any());
        assertEquals(ContactStatus.CONFIRMED_BY_CUSTOMER, reservation.getContactStatus());
    }

    @Test
    void rejectsLookupWithoutBookingCode() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.lookupPublicReservation(null, "0901234567", null));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }

    @Test
    void generatedReservationCodesAreUniqueAndDatabaseChecked() {
        when(reservationRepository.findByReservationCode(any())).thenReturn(Optional.empty());
        LocalDate reservationDate = LocalDate.of(2026, 8, 20);
        Set<String> codes = new HashSet<>();

        for (int index = 0; index < 100; index++) {
            String code = ReflectionTestUtils.invokeMethod(service, "generateReservationCode", reservationDate);
            codes.add(code);
        }

        assertEquals(100, codes.size());
        codes.forEach(code -> org.junit.jupiter.api.Assertions.assertTrue(
                code.matches("MV-20260820-[0-9A-F]{8}")));
        verify(reservationRepository, org.mockito.Mockito.times(100)).findByReservationCode(any());
    }

    @Test
    void availabilityDetectsAReservationFromThePreviousDateThatRunsPastMidnight() {
        LocalDate requestedDate = LocalDate.now().plusDays(30);
        RestaurantTable table = new RestaurantTable();
        table.setId(7);
        table.setName("Bàn đêm");
        table.setAreaId(3);
        table.setActive(true);
        table.setIsOccupied(0);
        table.setCapacity(4);
        poly.edu.quanlynhahang.entity.TableArea area = new poly.edu.quanlynhahang.entity.TableArea();
        area.setId(3);
        area.setNameVi("Sảnh chính");
        area.setStatus("ACTIVE");

        Reservation overnight = new Reservation();
        overnight.setId(70L);
        overnight.setReservationDate(requestedDate.minusDays(1));
        overnight.setArrivalTime(LocalTime.of(23, 30));
        overnight.setExpectedDurationMinutes(120);
        overnight.setReservationStatus(ReservationStatus.CONFIRMED);

        when(businessHoursService.getClosingTime()).thenReturn(LocalTime.of(6, 0));
        when(tableRepository.findOperationalTables()).thenReturn(List.of(table));
        when(areaRepository.findById(3)).thenReturn(Optional.of(area));
        when(reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                org.mockito.ArgumentMatchers.eq(requestedDate), org.mockito.ArgumentMatchers.eq(7), any())).thenReturn(List.of());
        when(reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                org.mockito.ArgumentMatchers.eq(requestedDate.minusDays(1)), org.mockito.ArgumentMatchers.eq(7), any())).thenReturn(List.of(overnight));

        var result = service.findAvailableTables(
                requestedDate.toString(), "00:30", 60, 2, null, false);

        assertEquals(1, result.size());
        assertEquals("RESERVED", result.getFirst().getAvailabilityStatus());
        assertTrue(result.getFirst().getFitScore() >= 0);
    }

    @Test
    void lateDiningConfirmationAllowsAvailabilityCheckForTheSameRequestedTime() {
        LocalDate date = LocalDate.now().plusDays(30);
        when(tableRepository.findOperationalTables()).thenReturn(List.of());

        when(businessHoursService.requiresLateDiningConfirmation(LocalTime.of(21, 41), 120)).thenReturn(true);
        ResponseStatusException blocked = assertThrows(ResponseStatusException.class,
                () -> service.findAvailableTables(date.toString(), "21:41", 120, 2, null, false));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, blocked.getStatusCode());
        assertTrue(blocked.getReason().contains("xác nhận dùng bữa"));
        assertTrue(service.findAvailableTables(date.toString(), "21:41", 120, 2, null, true).isEmpty());
    }

    @Test
    void quoteUsesLateDiningConfirmationFromTheRequest() {
        LocalDate date = LocalDate.now().plusDays(30);
        when(businessHoursService.requiresLateDiningConfirmation(LocalTime.of(20, 41), 120)).thenReturn(true);
        var request = new poly.edu.quanlynhahang.dto.ReservationQuoteRequest();
        request.setReservationDate(date.toString());
        request.setArrivalTime("20:41");
        request.setDurationMinutes(120);
        request.setGuestCount(2);

        request.setLateDiningConfirmed(false);
        ResponseStatusException blocked = assertThrows(ResponseStatusException.class, () -> service.quote(request));
        assertTrue(blocked.getReason().contains("xác nhận dùng bữa"));

        request.setLateDiningConfirmed(true);
        ResponseStatusException afterConfirmation = assertThrows(ResponseStatusException.class, () -> service.quote(request));
        assertEquals("Vui lòng chọn khu vực", afterConfirmation.getReason());
    }

    @Test
    void availabilityRejectsAreaThatIsActiveButNotBookingReady() {
        LocalDate requestedDate = LocalDate.now().plusDays(30);
        poly.edu.quanlynhahang.entity.TableArea area = new poly.edu.quanlynhahang.entity.TableArea();
        area.setId(12);
        area.setNameVi("Khu thiếu bàn");
        area.setStatus("ACTIVE");
        when(areaRepository.findById(12)).thenReturn(Optional.of(area));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Cần ít nhất 2 bàn đang hoạt động"))
                .when(areaReadinessService).requireBookingReady(area);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.findAvailableTables(requestedDate.toString(), "18:00", 120, 2, 12, false));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(tableRepository, never()).findOperationalTables();
    }

    @Test
    void eventBookingRetryWithSameKeyReturnsOriginalAndRejectsChangedPayload() {
        var request = new poly.edu.quanlynhahang.dto.EventBookingRequest(
                "Nguyễn An", "0901234567", "an@example.test", 2,
                poly.edu.quanlynhahang.entity.EventType.WEDDING,
                LocalDate.now().plusDays(2).toString(), "18:00", 4, 80,
                true, false, "Tiệc tối", false, List.of(), false);
        Reservation existing = new Reservation();
        existing.setId(91L);
        existing.setReservationCode("MV-20260825-ABCDEF12");
        existing.setRequestFingerprint(ReflectionTestUtils.invokeMethod(service, "eventFingerprint", request));
        existing.setReservationStatus(ReservationStatus.WAITING_TABLE_ASSIGNMENT);
        when(reservationRepository.findByIdempotencyKey("event-key-91")).thenReturn(Optional.of(existing));

        var retry = service.createEventBooking(request, "event-key-91");

        assertEquals("MV-20260825-ABCDEF12", retry.getReservationCode());
        verify(reservationRepository, never()).save(any(Reservation.class));

        var changed = new poly.edu.quanlynhahang.dto.EventBookingRequest(
                request.customerName(), request.customerPhone(), request.customerEmail(), request.areaId(),
                request.eventType(), request.reservationDate(), request.arrivalTime(), request.durationHours(),
                81, request.decorationRequired(), request.mcRequired(), request.eventNote(),
                request.preorderEnabled(), request.preorderItems(), request.lateDiningConfirmed());
        ResponseStatusException conflict = assertThrows(ResponseStatusException.class,
                () -> service.createEventBooking(changed, "event-key-91"));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
    }

    @Test
    void eventBookingCreatesDepositCapabilityExpiryAndRealtimeNotification() {
        poly.edu.quanlynhahang.entity.TableArea area = new poly.edu.quanlynhahang.entity.TableArea();
        area.setId(2);
        area.setNameVi("Sảnh cưới");
        area.setAreaType(poly.edu.quanlynhahang.entity.AreaType.EVENT_HALL);
        area.setStatus("ACTIVE");
        area.setMinGuestCount(20);
        area.setMaxGuestCount(200);
        area.setMinBookingHours(2);
        area.setHourlyRate(new BigDecimal("1000000"));
        area.setPackagePrice(BigDecimal.ZERO);
        poly.edu.quanlynhahang.dto.DepositPolicyResponse policy =
                new poly.edu.quanlynhahang.dto.DepositPolicyResponse();
        policy.setPolicyCode("EVENT-50");
        policy.setExplanation("Cọc sự kiện");
        when(areaRepository.findById(2)).thenReturn(Optional.of(area));
        when(reservationRepository.findByReservationCode(any())).thenReturn(Optional.empty());
        when(depositPolicyService.calculate(any(), any(Integer.class), any(), any(), any(),
                org.mockito.ArgumentMatchers.isNull(), any()))
                .thenReturn(new DepositPolicyService.DepositCalculation(
                        new BigDecimal("2000000"), new BigDecimal("0.50"), policy));
        when(paymentCapabilityService.issue(any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn("event-capability");
        when(reservationRepository.save(any())).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(92L);
            return saved;
        });
        var request = new poly.edu.quanlynhahang.dto.EventBookingRequest(
                "Nguyễn An", "0901234567", "an@example.test", 2,
                poly.edu.quanlynhahang.entity.EventType.WEDDING,
                LocalDate.now().plusDays(2).toString(), "18:00", 4, 80,
                true, false, "Tiệc tối", false, List.of(), false);

        var response = service.createEventBooking(request, "event-key-92");

        assertEquals("event-capability", response.getPaymentCapabilityToken());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getDepositAmount());
        org.mockito.ArgumentCaptor<Reservation> saved = org.mockito.ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertNotNull(saved.getValue().getDepositExpiresAt());
        verify(notificationService).createNotification(
                org.mockito.ArgumentMatchers.eq("EVENT_BOOKING_NEW"), any(), any(), any(), any(), any(), any());
        verify(realtimeService).publish(
                org.mockito.ArgumentMatchers.eq("EVENT_BOOKING_CREATED"), any(), any(), any(), any(), any());
    }

    @Test
    void eventBookingRejectsUnavailablePreorderDishBeforeSavingReservation() {
        poly.edu.quanlynhahang.entity.TableArea area = new poly.edu.quanlynhahang.entity.TableArea();
        area.setId(2);
        area.setNameVi("Sảnh cưới");
        area.setAreaType(poly.edu.quanlynhahang.entity.AreaType.EVENT_HALL);
        area.setStatus("ACTIVE");
        area.setMinGuestCount(20);
        area.setMaxGuestCount(200);
        area.setMinBookingHours(2);
        area.setHourlyRate(new BigDecimal("1000000"));
        area.setPackagePrice(BigDecimal.ZERO);
        Product product = new Product();
        product.setId(77);
        product.setNameVi("Gỏi sen");
        product.setStatus(true);
        product.setAvailable(true);
        poly.edu.quanlynhahang.dto.PreorderItemRequest preorder = new poly.edu.quanlynhahang.dto.PreorderItemRequest();
        preorder.setProductId(77);
        preorder.setQuantity(2);
        when(areaRepository.findById(2)).thenReturn(Optional.of(area));
        when(productRepository.findById(77)).thenReturn(Optional.of(product));
        when(menuAvailabilityService.availableQuantity(product)).thenReturn(1);
        var request = new poly.edu.quanlynhahang.dto.EventBookingRequest(
                "Nguyễn An", "0901234567", "an@example.test", 2,
                poly.edu.quanlynhahang.entity.EventType.WEDDING,
                LocalDate.now().plusDays(2).toString(), "18:00", 4, 80,
                true, false, "Tiệc tối", true, List.of(preorder), false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.createEventBooking(request, "event-key-unavailable"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(reservationRepository, never()).save(any(Reservation.class));
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
                () -> service.lookupPublicReservation("MV-TEST-123", "0987654321", null));

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

        when(reservationRepository.findExpiryCandidateIds(any(), any(), any(), any()))
                .thenReturn(List.of());

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
        reservation.setReservationStatus(ReservationStatus.WAITING_TABLE_ASSIGNMENT);
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
    void confirmingPaidDepositPreservesDepositedStatus() {
        RestaurantTable table = new RestaurantTable();
        table.setId(10);
        table.setName("B10");
        Reservation reservation = new Reservation();
        reservation.setId(25L);
        reservation.setReservationCode("MV-TEST-0025");
        reservation.setTable(table);
        reservation.setReservationDate(LocalDate.now().plusDays(1));
        reservation.setArrivalTime(LocalTime.of(19, 0));
        reservation.setExpectedDurationMinutes(120);
        reservation.setGuestCount(2);
        reservation.setDepositAmount(new BigDecimal("50000"));
        reservation.setPaidAmount(new BigDecimal("50000"));
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setReservationStatus(ReservationStatus.DEPOSIT_PAID);

        when(reservationRepository.findById(25L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(tableRepository.findLockedByIdIn(List.of(10))).thenReturn(List.of(table));
        when(reservationRepository.save(org.mockito.ArgumentMatchers.any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.confirm(25L, null);

        assertEquals(ReservationStatus.DEPOSIT_PAID, result.getReservationStatus());
        assertEquals(DepositStatus.PAID, reservation.getDepositStatus());
    }

    @Test
    void adminStatusCountsUseRepositoryAggregateInsteadOfCurrentPageRows() {
        ReservationRepository.AdminStatusCount pending = mock(ReservationRepository.AdminStatusCount.class);
        when(pending.getStatus()).thenReturn(ReservationStatus.PENDING);
        when(pending.getTotal()).thenReturn(3L);
        ReservationRepository.AdminStatusCount deposited = mock(ReservationRepository.AdminStatusCount.class);
        when(deposited.getStatus()).thenReturn(ReservationStatus.DEPOSIT_PAID);
        when(deposited.getTotal()).thenReturn(5L);
        ReservationRepository.AdminStatusCount cancelled = mock(ReservationRepository.AdminStatusCount.class);
        when(cancelled.getStatus()).thenReturn(ReservationStatus.CANCELLED);
        when(cancelled.getTotal()).thenReturn(6L);
        when(reservationRepository.countAdminByStatus()).thenReturn(List.of(pending, deposited, cancelled));

        Map<ReservationStatus, Long> counts = service.getAdminReservationStatusCounts();

        assertEquals(3L, counts.get(ReservationStatus.PENDING));
        assertEquals(5L, counts.get(ReservationStatus.DEPOSIT_PAID));
        assertEquals(6L, counts.get(ReservationStatus.CANCELLED));
        assertEquals(0L, counts.get(ReservationStatus.CHECKED_IN));
        verify(reservationRepository).countAdminByStatus();
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
        reservation.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        stubExpiryCandidates(reservation);
        when(depositPolicyService.calculateNoShowForfeiture(reservation)).thenReturn(BigDecimal.valueOf(500_000));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        service.expireStaleReservations();

        assertEquals(ReservationStatus.NO_SHOW, reservation.getReservationStatus());
        assertEquals(DepositStatus.FORFEITED, reservation.getDepositStatus());
        assertEquals(BigDecimal.valueOf(500_000), reservation.getPaidAmount());
        assertEquals(PaymentStatus.PARTIALLY_PAID, reservation.getPaymentStatus());
        verify(tableLifecycleService).releaseReservationTables(reservation);
        verify(depositPolicyService).calculateNoShowForfeiture(reservation);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void doesNotExpireFuturePendingReservationFromItsPaymentDeadline() {
        Reservation pendingReservation = new Reservation();
        pendingReservation.setReservationStatus(ReservationStatus.PENDING);
        pendingReservation.setDepositAmount(BigDecimal.TEN);
        pendingReservation.setCreatedAt(java.util.Date.from(java.time.Instant.now().minusSeconds(25 * 3600))); // 25 hours ago
        pendingReservation.setReservationDate(LocalDate.now().plusDays(1));
        pendingReservation.setArrivalTime(LocalTime.NOON);
        
        stubExpiryCandidates(pendingReservation);
                
        service.expireStaleReservations();
        
        assertEquals(ReservationStatus.PENDING, pendingReservation.getReservationStatus());
    }

    @Test
    void appliesNoShowOnlyStrictlyAfterTwentyMinuteArrivalGracePeriod() {
        Reservation reservation = new Reservation();
        reservation.setId(62L);
        reservation.setReservationCode("MV-TEST-GRACE");
        reservation.setReservationStatus(ReservationStatus.CONFIRMED);
        reservation.setDepositStatus(DepositStatus.NOT_REQUIRED);
        reservation.setReservationDate(LocalDate.of(2026, 9, 4));
        reservation.setArrivalTime(LocalTime.of(19, 5));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        service.processSingleExpiry(reservation, LocalDateTime.of(2026, 9, 4, 19, 25));

        assertEquals(ReservationStatus.CONFIRMED, reservation.getReservationStatus());
        verify(reservationRepository, never()).save(reservation);

        service.processSingleExpiry(reservation, LocalDateTime.of(2026, 9, 4, 19, 25, 1));

        assertEquals(ReservationStatus.NO_SHOW, reservation.getReservationStatus());
        verify(tableLifecycleService).releaseReservationTables(reservation);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void expiryJobContinuesWhenOneCandidateFailsBusinessTransition() {
        Reservation failingNoShow = noShowReservation(DepositStatus.PAID);
        failingNoShow.setId(61L);
        failingNoShow.setReservationCode("MV-TEST-FAIL");
        failingNoShow.setDepositAmount(BigDecimal.valueOf(500_000));
        failingNoShow.setPaidAmount(BigDecimal.valueOf(500_000));

        stubExpiryCandidates(failingNoShow);
        when(depositPolicyService.calculateNoShowForfeiture(failingNoShow)).thenReturn(BigDecimal.valueOf(500_000));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Bàn còn hóa đơn chưa thanh toán"))
                .when(tableLifecycleService).releaseReservationTables(failingNoShow);

        service.expireStaleReservations();

        verify(reservationRepository, never()).save(any(Reservation.class));
    }
    
    @Test
    void doesNotMarkInServiceReservationAsNoShow() {
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
        when(reservationRepository.findExpiryCandidateIds(any(), any(), any(), any()))
                .thenReturn(List.of());
            
        service.expireStaleReservations();
        
        assertEquals(ReservationStatus.IN_SERVICE, reservation.getReservationStatus());
        verify(reservationRepository, never()).save(reservation);
    }

    private Reservation reservationForDepositPayment(BigDecimal depositAmount, BigDecimal totalAmount) {
        Reservation reservation = new Reservation();
        reservation.setReservationStatus(ReservationStatus.WAITING_TABLE_ASSIGNMENT);
        reservation.setDepositAmount(depositAmount);
        reservation.setTotalAmount(totalAmount);
        return reservation;
    }

    private void stubExpiryCandidates(Reservation... reservations) {
        List<Long> ids = java.util.stream.IntStream.range(0, reservations.length)
                .mapToObj(index -> {
                    long id = reservations[index].getId() == null ? index + 1L : reservations[index].getId();
                    reservations[index].setId(id);
                    return id;
                })
                .toList();
        when(reservationRepository.findExpiryCandidateIds(any(), any(), any(), any()))
                .thenReturn(ids);
        for (int index = 0; index < reservations.length; index++) {
            when(reservationRepository.findById(ids.get(index))).thenReturn(Optional.of(reservations[index]));
        }
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
        LocalDateTime overdueArrival = LocalDateTime.now().minusMinutes(21);
        reservation.setReservationDate(overdueArrival.toLocalDate());
        reservation.setArrivalTime(overdueArrival.toLocalTime());
        reservation.setTable(table);
        return reservation;
    }
}
