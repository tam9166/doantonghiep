package poly.edu.quanlynhahang.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.quanlynhahang.dto.CancellationDecisionRequest;
import poly.edu.quanlynhahang.dto.CancellationRequestCreateRequest;
import poly.edu.quanlynhahang.dto.CancellationPreviewResponse;
import poly.edu.quanlynhahang.dto.CancellationRequestReceipt;
import poly.edu.quanlynhahang.dto.CancellationRequestResponse;
import poly.edu.quanlynhahang.dto.RefundCompletionRequest;
import poly.edu.quanlynhahang.dto.ReservationActionRequest;
import poly.edu.quanlynhahang.entity.CancellationRequestStatus;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentDirection;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;
import poly.edu.quanlynhahang.entity.RefundTransaction;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationCancellationRequest;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.repository.PaymentTransactionRepository;
import poly.edu.quanlynhahang.repository.ReservationCancellationRequestRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;

@Service
public class ReservationCancellationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Set<CancellationRequestStatus> ACTIVE_REQUESTS = EnumSet.of(
            CancellationRequestStatus.PENDING, CancellationRequestStatus.REFUND_PENDING);
    private static final Set<ReservationStatus> CLOSED_RESERVATIONS = EnumSet.of(
            ReservationStatus.CANCELLED, ReservationStatus.COMPLETED, ReservationStatus.REJECTED,
            ReservationStatus.EXPIRED, ReservationStatus.NO_SHOW);

    private final ReservationRepository reservationRepository;
    private final ReservationCancellationRequestRepository requestRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationCancellationPolicy policy;
    private final ReservationService reservationService;
    private final RefundService refundService;
    private final ActivityLogService activityLogService;
    private final TableLifecycleService tableLifecycleService;
    private final OrderRefundService orderRefundService;
    private final String restaurantName;
    private final String restaurantAddress;
    private final String restaurantHotline;
    private final String restaurantEmail;

    public ReservationCancellationService(
            ReservationRepository reservationRepository,
            ReservationCancellationRequestRepository requestRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            ReservationCancellationPolicy policy,
            ReservationService reservationService,
            RefundService refundService,
            ActivityLogService activityLogService,
            TableLifecycleService tableLifecycleService,
            OrderRefundService orderRefundService,
            @Value("${restaurant.info.name:Moc Vi Restaurant}") String restaurantName,
            @Value("${restaurant.info.address:137 Nguyen Thi Thap, Da Nang}") String restaurantAddress,
            @Value("${restaurant.info.hotline:0347944028}") String restaurantHotline,
            @Value("${restaurant.info.email:contact@mocvi.vn}") String restaurantEmail) {
        this.reservationRepository = reservationRepository;
        this.requestRepository = requestRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.policy = policy;
        this.reservationService = reservationService;
        this.refundService = refundService;
        this.activityLogService = activityLogService;
        this.tableLifecycleService = tableLifecycleService;
        this.orderRefundService = orderRefundService;
        this.restaurantName = restaurantName;
        this.restaurantAddress = restaurantAddress;
        this.restaurantHotline = restaurantHotline;
        this.restaurantEmail = restaurantEmail;
    }

    @Transactional
    public CancellationRequestReceipt create(CancellationRequestCreateRequest input) {
        String code = normalizeCode(input.reservationCode());
        String name = normalizeName(input.customerName());
        String phone = normalizePhone(input.customerPhone());
        String email = normalizeEmail(input.customerEmail());
        int provided = countPresent(code, name, phone, email);
        if (provided < 2) throw insufficientVerification();
        Reservation verified = verifiedReservation(input);
        Reservation reservation = reservationRepository.findLockedById(verified.getId())
                .orElseThrow(this::reservationNotFound);
        ensureOpenAndFuture(reservation);
        if (requestRepository.existsByReservationIdAndStatusIn(reservation.getId(), ACTIVE_REQUESTS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Yêu cầu hủy đặt bàn đang được xử lý.");
        }

        Date requestedAt = new Date();
        ReservationCancellationPolicy.Calculation calculation = policy.calculate(
                reservation, requestedAt, actuallyPaidAmount(reservation));
        ReservationCancellationRequest request = new ReservationCancellationRequest();
        request.setRequestCode(nextRequestCode());
        request.setReservation(reservation);
        request.setReason(limit(input.reason(), 1000));
        String contactMethod = normalizedContactMethod(input.contactMethod());
        ensureVerifiedContactAvailable(reservation, contactMethod);
        request.setContactMethod(contactMethod);
        request.setMatchedFieldCount(matchCount(reservation, code, name, phone, email));
        request.setRequestedAt(requestedAt);
        applyCalculation(request, calculation);
        applyRefundDestination(request, input, calculation.refundAmount());
        try {
            ReservationCancellationRequest saved = requestRepository.saveAndFlush(request);
            activityLogService.log("CANCELLATION_REQUESTED", "Reservation", String.valueOf(reservation.getId()),
                    "Khách đã gửi yêu cầu hủy " + saved.getRequestCode());
            return new CancellationRequestReceipt(saved.getRequestCode(), saved.getStatus(),
                    "Yêu cầu hủy đã được ghi nhận và đang chờ nhà hàng xem xét.");
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Yêu cầu hủy đặt bàn đang được xử lý.");
        }
    }

    @Transactional(readOnly = true)
    public CancellationPreviewResponse preview(CancellationRequestCreateRequest input) {
        Reservation reservation = verifiedReservation(input);
        ensureOpenAndFuture(reservation);
        ReservationCancellationPolicy.Calculation calculation = policy.calculate(
                reservation, new Date(), actuallyPaidAmount(reservation));
        String message = calculation.refundAmount().signum() > 0
                ? "Có khoản hoàn dự kiến theo chính sách hủy hiện tại."
                : "Đặt bàn hợp lệ nhưng không có khoản tiền hoàn dự kiến.";
        String messageCode = calculation.refundAmount().signum() > 0 ? "REFUND_AVAILABLE" : "NO_REFUND";
        return new CancellationPreviewResponse(
                reservation.getReservationCode(), calculation.orderTotalAmount(), calculation.paidDepositAmount(),
                calculation.penaltyAmount(), calculation.refundRate(), calculation.refundAmount(),
                calculation.hoursBeforeReservation(), calculation.eligible(), calculation.policyApplied(), message,
                calculation.policyCode(), messageCode);
    }

    private Reservation verifiedReservation(CancellationRequestCreateRequest input) {
        String code = normalizeCode(input.reservationCode());
        String name = normalizeName(input.customerName());
        String phone = normalizePhone(input.customerPhone());
        String email = normalizeEmail(input.customerEmail());
        if (countPresent(code, name, phone, email) < 2) throw insufficientVerification();
        if (code != null && phone != null) {
            Reservation exactReservation = reservationRepository.findByReservationCodeAndCustomerPhone(code, phone)
                    .orElse(null);
            if (exactReservation != null) {
                if (name != null && !name.equals(normalizeName(exactReservation.getCustomerName()))) {
                    throw reservationNotFound();
                }
                if (email != null && !email.equals(normalizeEmail(exactReservation.getCustomerEmail()))) {
                    throw reservationNotFound();
                }
                return exactReservation;
            }
        }
        List<Reservation> matched = reservationRepository.findCancellationVerificationCandidates(
                        code, name, phone, email).stream()
                .filter(reservation -> matchCount(reservation, code, name, phone, email) >= 2)
                .toList();
        if (matched.size() != 1) throw reservationNotFound();
        return matched.getFirst();
    }

    private void ensureOpenAndFuture(Reservation reservation) {
        if (CLOSED_RESERVATIONS.contains(reservation.getReservationStatus())
                || !LocalDateTime.of(reservation.getReservationDate(), reservation.getArrivalTime())
                .isAfter(LocalDateTime.now(ReservationCancellationPolicy.BUSINESS_ZONE))) {
            throw cancellationTooLate();
        }
    }

    @Transactional(readOnly = true)
    public List<CancellationRequestResponse> list() {
        return requestRepository.findAllByOrderByRequestedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public CancellationRequestResponse approve(Long id, CancellationDecisionRequest decision) {
        ReservationCancellationRequest request = pendingRequest(id);
        Reservation reservation = reservationRepository.findLockedById(request.getReservation().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặt bàn"));
        if (CLOSED_RESERVATIONS.contains(reservation.getReservationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đặt bàn không còn có thể hủy");
        }

        ReservationCancellationPolicy.Calculation calculation = policy.calculate(
                reservation, request.getRequestedAt(), actuallyPaidAmount(reservation));
        applyCalculation(request, calculation);
        String actor = currentUsername();
        orderRefundService.cancelLinkedReservationPreorder(reservation.getKitchenOrderId(), actor);
        ReservationActionRequest action = new ReservationActionRequest();
        action.setNote(limit(decision.note(), 500));
        reservationService.cancelApproved(reservation.getId(), action);
        // A cancelled reservation is no longer an invoice receivable, even when a
        // policy retains part of the deposit. The refund ledger records that split.
        reservation.setRemainingAmount(BigDecimal.ZERO);

        RefundTransaction refund = refundService.requestReservationRefund(
                reservation,
                calculation.refundAmount(),
                calculation.paidDepositAmount().subtract(calculation.refundAmount()),
                RefundTransaction.RefundReason.CANCELLED_BY_CUSTOMER,
                request.getReason(),
                actor);
        request.setProcessedBy(actor);
        request.setProcessedAt(new Date());
        request.setProcessingNote(limit(decision.note(), 1000));
        if (refund == null) {
            request.setStatus(CancellationRequestStatus.APPROVED);
            reservation.setPaymentStatus(PaymentStatus.CANCELLED);
            tableLifecycleService.releaseReservationTables(reservation);
        } else {
            request.setRefundTransactionId(refund.getId());
            request.setStatus(CancellationRequestStatus.REFUND_PENDING);
            reservation.setPaymentStatus(PaymentStatus.REFUND_PENDING);
        }
        reservationRepository.save(reservation);
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public CancellationRequestResponse reject(Long id, CancellationDecisionRequest decision) {
        ReservationCancellationRequest request = pendingRequest(id);
        request.setStatus(CancellationRequestStatus.REJECTED);
        request.setProcessedBy(currentUsername());
        request.setProcessedAt(new Date());
        request.setProcessingNote(limit(decision.note(), 1000));
        activityLogService.log("CANCELLATION_REJECTED", "ReservationCancellationRequest", String.valueOf(id),
                request.getProcessingNote());
        return toResponse(requestRepository.save(request));
    }

    @Transactional
    public CancellationRequestResponse completeRefund(Long id, RefundCompletionRequest completion) {
        ReservationCancellationRequest request = requestRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hủy"));
        if (CancellationRequestStatus.REFUNDED.equals(request.getStatus())) return toResponse(request);
        if (!CancellationRequestStatus.REFUND_PENDING.equals(request.getStatus())
                || request.getRefundTransactionId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu không chờ hoàn tiền");
        }
        refundService.confirmCompleted(request.getRefundTransactionId(), completion.providerReference(),
                limit(completion.note(), 1000), currentUsername());
        request.setStatus(CancellationRequestStatus.REFUNDED);
        request.setProcessedAt(new Date());
        request.setProcessingNote(limit(completion.note(), 1000));
        Reservation reservation = request.getReservation();
        reservation.setPaymentStatus(request.getExpectedRefundAmount()
                        .compareTo(request.getPaidDepositAmount()) < 0
                ? PaymentStatus.PARTIALLY_REFUNDED : PaymentStatus.REFUNDED);
        reservation.setRemainingAmount(BigDecimal.ZERO);
        reservationRepository.save(reservation);
        tableLifecycleService.releaseReservationTables(reservation);
        return toResponse(requestRepository.save(request));
    }

    private ReservationCancellationRequest pendingRequest(Long id) {
        ReservationCancellationRequest request = requestRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hủy"));
        if (!CancellationRequestStatus.PENDING.equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu đã được xử lý");
        }
        return request;
    }

    private BigDecimal actuallyPaidAmount(Reservation reservation) {
        List<PaymentTransaction> ledger = paymentTransactionRepository
                .findByAggregateTypeAndAggregateIdAndStatus(
                        "RESERVATION", reservation.getId(), PaymentTransactionStatus.SUCCESS);
        BigDecimal netPaid = ledger.stream().reduce(BigDecimal.ZERO,
                (total, transaction) -> PaymentDirection.REFUND.equals(transaction.getDirection())
                        ? total.subtract(transaction.getAmount()) : total.add(transaction.getAmount()),
                BigDecimal::add).max(BigDecimal.ZERO);
        if (ledger.isEmpty() && DepositStatus.PAID.equals(reservation.getDepositStatus())) {
            netPaid = reservation.getPaidAmount() == null ? BigDecimal.ZERO : reservation.getPaidAmount();
        }
        return netPaid.max(BigDecimal.ZERO);
    }

    private void applyCalculation(ReservationCancellationRequest request,
                                  ReservationCancellationPolicy.Calculation calculation) {
        request.setHoursBeforeReservation(calculation.hoursBeforeReservation());
        request.setRefundRate(calculation.refundRate());
        request.setPaidDepositAmount(calculation.paidDepositAmount());
        request.setExpectedRefundAmount(calculation.refundAmount());
    }

    private int matchCount(Reservation reservation, String code, String name, String phone, String email) {
        int matches = 0;
        if (code != null && code.equals(normalizeCode(reservation.getReservationCode()))) matches++;
        if (name != null && name.equals(normalizeName(reservation.getCustomerName()))) matches++;
        if (phone != null && phone.equals(normalizePhone(reservation.getCustomerPhone()))) matches++;
        if (email != null && email.equals(normalizeEmail(reservation.getCustomerEmail()))) matches++;
        return matches;
    }

    private int countPresent(String... values) {
        int count = 0;
        for (String value : values) if (value != null) count++;
        return count;
    }

    private String normalizeCode(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String value) {
        String normalized = trim(value);
        if (normalized == null) return null;
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC)
                .replaceAll("[\\s.()-]", "");
        if (normalized.startsWith("+84")) normalized = "0" + normalized.substring(3);
        else if (normalized.startsWith("84") && normalized.length() == 11) normalized = "0" + normalized.substring(2);
        return normalized;
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int max) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.substring(0, Math.min(max, normalized.length()));
    }

    private ResponseStatusException insufficientVerification() {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Vui lòng nhập ít nhất 2 trong 4 thông tin xác minh.");
    }

    private ResponseStatusException reservationNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Không tìm thấy thông tin đặt bàn phù hợp.");
    }

    private ResponseStatusException cancellationTooLate() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Đặt bàn đã quá thời gian hỗ trợ hủy trực tuyến.\n"
                        + "Vui lòng liên hệ nhà hàng:\n"
                        + "Nhà hàng: " + restaurantName + "\n"
                        + "Địa chỉ: " + restaurantAddress + "\n"
                        + "Số điện thoại: " + restaurantHotline + "\n"
                        + "Email: " + restaurantEmail);
    }

    private String nextRequestCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder code = new StringBuilder("CR-");
            for (int i = 0; i < 12; i++) {
                code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!requestRepository.existsByRequestCode(candidate)) return candidate;
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể tạo mã yêu cầu lúc này. Vui lòng thử lại.");
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "SYSTEM" : authentication.getName();
    }

    private CancellationRequestResponse toResponse(ReservationCancellationRequest request) {
        Reservation reservation = request.getReservation();
        return new CancellationRequestResponse(
                request.getId(), request.getRequestCode(), reservation.getId(), reservation.getReservationCode(),
                reservation.getCustomerName(), reservation.getCustomerPhone(), reservation.getCustomerEmail(),
                reservation.getReservationDate(), reservation.getArrivalTime(), reservation.getGuestCount(),
                reservation.getDepositAmount(), request.getPaidDepositAmount(), request.getRequestedAt(),
                request.getHoursBeforeReservation(), request.getRefundRate(), request.getExpectedRefundAmount(),
                request.getReason(), request.getContactMethod(), request.getRefundBankName(),
                request.getRefundAccountNumber(), request.getRefundAccountHolder(), request.getStatus(),
                request.getRefundTransactionId(), request.getProcessedBy(),
                request.getProcessedAt(), request.getProcessingNote());
    }

    private void applyRefundDestination(ReservationCancellationRequest request,
                                        CancellationRequestCreateRequest input,
                                        BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.signum() <= 0) return;
        String bank = limit(input.refundBankName(), 120);
        String account = limit(input.refundAccountNumber(), 40);
        String holder = limit(input.refundAccountHolder(), 150);
        if (bank == null) throw missingRefundField("ngân hàng");
        if (account == null) throw missingRefundField("số tài khoản");
        if (holder == null) throw missingRefundField("chủ tài khoản");
        request.setRefundBankName(bank);
        request.setRefundAccountNumber(account);
        request.setRefundAccountHolder(holder);
    }

    private String normalizedContactMethod(String value) {
        String normalized = trim(value);
        if (normalized == null) return "PHONE";
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!"PHONE".equals(normalized) && !"EMAIL".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Phương thức liên lạc không được hỗ trợ.");
        }
        return normalized;
    }

    private void ensureVerifiedContactAvailable(Reservation reservation, String contactMethod) {
        String contact = "EMAIL".equals(contactMethod)
                ? normalizeEmail(reservation.getCustomerEmail())
                : normalizePhone(reservation.getCustomerPhone());
        if (contact == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Đặt bàn chưa có " + ("EMAIL".equals(contactMethod) ? "email" : "số điện thoại")
                            + " đã xác minh để liên hệ.");
        }
    }

    private ResponseStatusException missingRefundField(String label) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Vui lòng nhập " + label + " để nhận tiền hoàn.");
    }
}
