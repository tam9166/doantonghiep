package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import poly.edu.quanlynhahang.dto.CancellationRequestCreateRequest;
import poly.edu.quanlynhahang.dto.CancellationDecisionRequest;
import poly.edu.quanlynhahang.dto.RefundCompletionRequest;
import poly.edu.quanlynhahang.entity.CancellationRequestStatus;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationCancellationRequest;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.ReservationCancellationRequestRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

class ReservationCancellationServiceTest {
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ReservationCancellationRequestRepository requestRepository =
            mock(ReservationCancellationRequestRepository.class);
    private final PaymentTransactionRepository transactionRepository = mock(PaymentTransactionRepository.class);
    private final OrderRefundService orderRefundService = mock(OrderRefundService.class);
    private final RefundService refundService = mock(RefundService.class);
    private final TableLifecycleService tableLifecycleService = mock(TableLifecycleService.class);
    private ReservationCancellationService service;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = reservation(1L, "RES-A", "Nguyễn Văn A", "0912345678", "a@example.com");
        when(reservationRepository.findByReservationCodeAndCustomerPhone("RES-A", "0912345678"))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.findCancellationVerificationCandidates(
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.findLockedById(1L)).thenReturn(Optional.of(reservation));
        when(requestRepository.existsByReservationIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(requestRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            ReservationCancellationRequest value = invocation.getArgument(0);
            value.setId(10L);
            return value;
        });
        when(transactionRepository.findByAggregateTypeAndAggregateIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        service = new ReservationCancellationService(
                reservationRepository, requestRepository, transactionRepository,
                new ReservationCancellationPolicy(12, new BigDecimal("0.50")),
                mock(ReservationService.class), refundService, mock(ActivityLogService.class),
                tableLifecycleService, orderRefundService,
                "Moc Vi Restaurant", "137 Nguyen Thi Thap, Da Nang", "0347944028", "contact@mocvi.vn");
    }

    static java.util.stream.Stream<Arguments> validPairs() {
        return java.util.stream.Stream.of(
                Arguments.of("RES-A", "Nguyễn Văn A", null, null),
                Arguments.of("RES-A", null, "0912345678", null),
                Arguments.of("RES-A", null, null, "a@example.com"),
                Arguments.of(null, "Nguyễn Văn A", "0912345678", null),
                Arguments.of(null, "Nguyễn Văn A", null, "a@example.com"),
                Arguments.of(null, null, "0912345678", "a@example.com"),
                Arguments.of("RES-A", "Nguyễn Văn A", "0912345678", null),
                Arguments.of("RES-A", "Nguyễn Văn A", "0912345678", "a@example.com"));
    }

    @ParameterizedTest
    @MethodSource("validPairs")
    void acceptsEveryValidSameBookingCombination(String code, String name, String phone, String email) {
        var result = service.create(new CancellationRequestCreateRequest(code, name, phone, email, "Đổi kế hoạch"));
        assertEquals(CancellationRequestStatus.PENDING, result.status());
    }

    static java.util.stream.Stream<Arguments> singleOrZeroMatches() {
        return java.util.stream.Stream.of(
                Arguments.of("RES-A", "Sai", null, null),
                Arguments.of("SAI", "Nguyễn Văn A", null, null),
                Arguments.of("SAI", null, "0912345678", null),
                Arguments.of("SAI", null, null, "a@example.com"),
                Arguments.of("SAI", "Sai", "0900000000", "x@example.com"));
    }

    @ParameterizedTest
    @MethodSource("singleOrZeroMatches")
    void rejectsZeroOrOneMatchingField(String code, String name, String phone, String email) {
        assertThrows(ResponseStatusException.class, () -> service.create(
                new CancellationRequestCreateRequest(code, name, phone, email, null)));
    }

    @Test
    void rejectsFieldsThatBelongToDifferentBookings() {
        Reservation other = reservation(2L, "RES-B", "Trần Văn B", "0988888888", "b@example.com");
        when(reservationRepository.findCancellationVerificationCandidates(
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(reservation, other));
        assertThrows(ResponseStatusException.class, () -> service.create(
                new CancellationRequestCreateRequest(null, "Nguyễn Văn A", "0988888888", null, null)));
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = ReservationStatus.class,
            names = { "CANCELLED", "COMPLETED" })
    void rejectsClosedReservation(ReservationStatus status) {
        reservation.setReservationStatus(status);
        assertThrows(ResponseStatusException.class, () -> service.create(
                new CancellationRequestCreateRequest("RES-A", "Nguyễn Văn A", null, null, null)));
    }

    @Test
    void rejectsDuplicateActiveRequest() {
        when(requestRepository.existsByReservationIdAndStatusIn(anyLong(), any())).thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.create(
                new CancellationRequestCreateRequest("RES-A", "Nguyễn Văn A", null, null, null)));
    }

    @Test
    void normalizesCaseWhitespaceAndVietnamPhonePrefix() {
        var result = service.create(new CancellationRequestCreateRequest(
                " res-a ", "  NGUYỄN   VĂN A ", "+84 912-345.678", " A@EXAMPLE.COM ", null));
        assertEquals(CancellationRequestStatus.PENDING, result.status());
    }

    @Test
    void previewsRefundUsingBackendPolicyWithoutCreatingRequest() {
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setPaidAmount(new BigDecimal("500000"));

        var preview = service.preview(new CancellationRequestCreateRequest(
                "RES-A", null, "0912345678", null, null));

        assertEquals(new BigDecimal("1000000"), preview.orderTotalAmount());
        assertEquals(new BigDecimal("500000"), preview.paidDepositAmount());
        assertEquals(new BigDecimal("0"), preview.penaltyAmount());
        assertEquals(new BigDecimal("1.00"), preview.refundRate());
        assertEquals(new BigDecimal("500000"), preview.expectedRefundAmount());
        assertEquals(true, preview.eligible());
        assertEquals("Có khoản hoàn dự kiến theo chính sách hủy hiện tại.", preview.message());
    }

    @Test
    void exactVerifiedReservationByCodeAndPhoneBypassesAmbiguousCandidateMatching() {
        when(reservationRepository.findCancellationVerificationCandidates(
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of(reservation, reservation(2L, "RES-B", "Nguyễn Văn A", "0912345678", "b@example.com")));

        var preview = service.preview(new CancellationRequestCreateRequest(
                "RES-A", "Nguyễn Văn A", "0912345678", null, null));

        assertEquals("RES-A", preview.reservationCode());
    }

    @Test
    void unpaidFutureBookingCanPreviewZeroRefundAndCreateCancellationRequest() {
        var preview = service.preview(new CancellationRequestCreateRequest(
                "RES-A", null, "0912345678", null, null));

        assertEquals(new BigDecimal("0"), preview.paidDepositAmount());
        assertEquals(new BigDecimal("0"), preview.expectedRefundAmount());
        assertEquals("Đặt bàn hợp lệ nhưng không có khoản tiền hoàn dự kiến.", preview.message());
        assertEquals(CancellationRequestStatus.PENDING, service.create(new CancellationRequestCreateRequest(
                "RES-A", null, "0912345678", null, "Khách đổi lịch")).status());
    }

    @Test
    void paidCancellationRequiresAndStoresRefundDestinationButUsesVerifiedBookingContact() {
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setPaidAmount(new BigDecimal("500000"));

        ResponseStatusException missing = assertThrows(ResponseStatusException.class, () -> service.create(
                new CancellationRequestCreateRequest("RES-A", null, "0912345678", null, "Đổi kế hoạch",
                        "EMAIL", null, null, null)));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, missing.getStatusCode());
        assertEquals("Vui lòng nhập ngân hàng để nhận tiền hoàn.", missing.getReason());

        var receipt = service.create(new CancellationRequestCreateRequest(
                "RES-A", null, "0912345678", null, "Đổi kế hoạch", "EMAIL",
                "  Ngân hàng Mộc Vị  ", "  0123456789 ", "  Nguyễn Văn A  "));

        assertEquals(CancellationRequestStatus.PENDING, receipt.status());
        org.mockito.ArgumentCaptor<ReservationCancellationRequest> request =
                org.mockito.ArgumentCaptor.forClass(ReservationCancellationRequest.class);
        verify(requestRepository).saveAndFlush(request.capture());
        assertEquals("EMAIL", request.getValue().getContactMethod());
        assertEquals("Ngân hàng Mộc Vị", request.getValue().getRefundBankName());
        assertEquals("0123456789", request.getValue().getRefundAccountNumber());
        assertEquals("Nguyễn Văn A", request.getValue().getRefundAccountHolder());
    }

    @Test
    void zeroRefundCancellationDoesNotRequireBankDetails() {
        var receipt = service.create(new CancellationRequestCreateRequest(
                "RES-A", null, "0912345678", null, "Đổi kế hoạch", "PHONE",
                null, null, null));

        assertEquals(CancellationRequestStatus.PENDING, receipt.status());
    }

    @Test
    void emailContactRequiresAnEmailOnTheVerifiedBooking() {
        reservation.setCustomerEmail(null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.create(
                new CancellationRequestCreateRequest("RES-A", null, "0912345678", null, "Đổi kế hoạch",
                        "EMAIL", null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("Đặt bàn chưa có email đã xác minh để liên hệ.", error.getReason());
    }

    @Test
    void rejectsMissingReservationWithSpecificNotFoundMessage() {
        when(reservationRepository.findCancellationVerificationCandidates(
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.preview(
                new CancellationRequestCreateRequest("RES-Z", "Nguyễn Văn A", null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Không tìm thấy thông tin đặt bàn phù hợp.", ex.getReason());
    }

    @Test
    void rejectsTooLateCancellationWithRestaurantContactInfo() {
        reservation.setReservationDate(LocalDateTime.now(ReservationCancellationPolicy.BUSINESS_ZONE)
                .minusDays(1).toLocalDate());
        reservation.setArrivalTime(LocalDateTime.now(ReservationCancellationPolicy.BUSINESS_ZONE)
                .minusHours(1).toLocalTime());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.preview(
                new CancellationRequestCreateRequest("RES-A", null, "0912345678", null, null)));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(true, ex.getReason().contains("Đặt bàn đã quá thời gian hỗ trợ hủy trực tuyến."));
        assertEquals(true, ex.getReason().contains("Địa chỉ: 137 Nguyen Thi Thap, Da Nang"));
        assertEquals(true, ex.getReason().contains("Số điện thoại: 0347944028"));
        assertEquals(true, ex.getReason().contains("Email: contact@mocvi.vn"));
    }

    @Test
    void twelveToTwentyFourHoursUsesHalfOrderPenaltyWithoutExceedingPaidAmount() {
        LocalDateTime bookingAt = LocalDateTime.now(ReservationCancellationPolicy.BUSINESS_ZONE).plusHours(18);
        reservation.setReservationDate(bookingAt.toLocalDate());
        reservation.setArrivalTime(bookingAt.toLocalTime());
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setPaidAmount(new BigDecimal("1000000"));

        var preview = service.preview(new CancellationRequestCreateRequest(
                "RES-A", null, "0912345678", null, null));

        assertEquals(new BigDecimal("500000"), preview.penaltyAmount());
        assertEquals(new BigDecimal("500000"), preview.expectedRefundAmount());
    }

    @Test
    void approvingCancellationCancelsLinkedPreorderWithoutPrematureTableRelease() {
        reservation.setKitchenOrderId(91);
        ReservationCancellationRequest request = new ReservationCancellationRequest();
        request.setId(10L);
        request.setReservation(reservation);
        request.setStatus(CancellationRequestStatus.PENDING);
        request.setRequestedAt(new java.util.Date());
        when(requestRepository.findLockedById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(10L, new CancellationDecisionRequest("Khách đổi kế hoạch"));

        verify(orderRefundService).cancelLinkedReservationPreorder(91, "SYSTEM");
    }

    @Test
    void completedCancellationRefundClearsBookingOutstandingBalance() {
        reservation.setRemainingAmount(new BigDecimal("114500"));
        ReservationCancellationRequest request = new ReservationCancellationRequest();
        request.setId(10L);
        request.setReservation(reservation);
        request.setStatus(CancellationRequestStatus.REFUND_PENDING);
        request.setRefundTransactionId(20L);
        request.setPaidDepositAmount(new BigDecimal("114500"));
        request.setExpectedRefundAmount(new BigDecimal("114500"));
        when(requestRepository.findLockedById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.completeRefund(10L, new RefundCompletionRequest(null, "Đã chuyển khoản hoàn tiền."));

        assertEquals(BigDecimal.ZERO, reservation.getRemainingAmount());
        assertEquals(poly.edu.quanlynhahang.entity.PaymentStatus.REFUNDED, reservation.getPaymentStatus());
        verify(refundService).confirmCompleted(20L, null, "Đã chuyển khoản hoàn tiền.", "SYSTEM");
        verify(tableLifecycleService).releaseReservationTables(reservation);
    }

    private Reservation reservation(Long id, String code, String name, String phone, String email) {
        Reservation value = new Reservation();
        value.setId(id);
        value.setReservationCode(code);
        value.setCustomerName(name);
        value.setCustomerPhone(phone);
        value.setCustomerEmail(email);
        LocalDateTime future = LocalDateTime.now(ReservationCancellationPolicy.BUSINESS_ZONE).plusDays(2);
        value.setReservationDate(future.toLocalDate());
        value.setArrivalTime(future.toLocalTime());
        value.setReservationStatus(ReservationStatus.CONFIRMED);
        value.setDepositStatus(DepositStatus.PENDING);
        value.setDepositAmount(new BigDecimal("500000"));
        value.setPaidAmount(BigDecimal.ZERO);
        value.setTotalAmount(new BigDecimal("1000000"));
        return value;
    }
}
