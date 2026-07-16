package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.AvailableTableResponse;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.dto.PreorderItemRequest;
import poly.edu.quanlynhahang.dto.PreorderItemResponse;
import poly.edu.quanlynhahang.dto.ReservationActionRequest;
import poly.edu.quanlynhahang.dto.ReservationQuoteRequest;
import poly.edu.quanlynhahang.dto.ReservationQuoteResponse;
import poly.edu.quanlynhahang.dto.ReservationRequest;
import poly.edu.quanlynhahang.dto.ReservationResponse;
import poly.edu.quanlynhahang.dto.TableSuggestionRequest;
import poly.edu.quanlynhahang.dto.TableSuggestionResponse;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.ReservationStatusHistory;
import poly.edu.quanlynhahang.entity.ReservationVoucherUsage;
import poly.edu.quanlynhahang.entity.RestaurantTable;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.entity.Product;
import poly.edu.quanlynhahang.entity.Voucher;
import poly.edu.quanlynhahang.repository.PaymentIntentRepository;
import poly.edu.quanlynhahang.repository.ProductRepository;
import poly.edu.quanlynhahang.repository.ReservationPreorderItemRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.ReservationStatusHistoryRepository;
import poly.edu.quanlynhahang.repository.ReservationVoucherUsageRepository;
import poly.edu.quanlynhahang.repository.RestaurantTableRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;
import poly.edu.quanlynhahang.repository.VoucherRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class ReservationService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_TEXT_PATTERN = Pattern.compile("(?i)<\\s*script|javascript:|onerror\\s*=|onload\\s*=");
    private static final int DEFAULT_DURATION_MINUTES = 120;
    private static final int CLEANUP_MINUTES = 15;
    private static final int MIN_ADVANCE_MINUTES = 30;
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(22, 0);
    private static final EnumSet<ReservationStatus> BLOCKING_STATUSES = EnumSet.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED,
            ReservationStatus.DEPOSIT_REQUIRED,
            ReservationStatus.DEPOSIT_PENDING,
            ReservationStatus.DEPOSIT_PAID,
            ReservationStatus.FULLY_PAID,
            ReservationStatus.CHECKED_IN,
            ReservationStatus.IN_SERVICE
    );

    private final ReservationRepository reservationRepository;
    private final ReservationPreorderItemRepository preorderItemRepository;
    private final PaymentIntentRepository paymentIntentRepository;
    private final ReservationStatusHistoryRepository historyRepository;
    private final RestaurantTableRepository tableRepository;
    private final TableAreaRepository areaRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final ReservationVoucherUsageRepository voucherUsageRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final ReservationRealtimeService realtimeService;
    private final DepositPolicyService depositPolicyService;
    private final ReservationStateMachine stateMachine;
    private final PaymentCapabilityService paymentCapabilityService;
    private final BigDecimal depositRate;
    private final long depositExpiryMinutes;
    private final long noShowGraceMinutes;

    public ReservationService(ReservationRepository reservationRepository,
                              ReservationPreorderItemRepository preorderItemRepository,
                              PaymentIntentRepository paymentIntentRepository,
                              ReservationStatusHistoryRepository historyRepository,
                              RestaurantTableRepository tableRepository,
                              TableAreaRepository areaRepository,
                              ProductRepository productRepository,
                              VoucherRepository voucherRepository,
                              ReservationVoucherUsageRepository voucherUsageRepository,
                              NotificationService notificationService,
                              ActivityLogService activityLogService,
                              ReservationRealtimeService realtimeService,
                              DepositPolicyService depositPolicyService,
                              ReservationStateMachine stateMachine,
                              PaymentCapabilityService paymentCapabilityService,
                              @Value("${restaurant.reservation.deposit-rate:0.50}") BigDecimal depositRate,
                              @Value("${restaurant.reservation.deposit-expiry-minutes:15}") long depositExpiryMinutes,
                              @Value("${restaurant.reservation.no-show-grace-minutes:30}") long noShowGraceMinutes) {
        this.reservationRepository = reservationRepository;
        this.preorderItemRepository = preorderItemRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.historyRepository = historyRepository;
        this.tableRepository = tableRepository;
        this.areaRepository = areaRepository;
        this.productRepository = productRepository;
        this.voucherRepository = voucherRepository;
        this.voucherUsageRepository = voucherUsageRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.realtimeService = realtimeService;
        this.depositPolicyService = depositPolicyService;
        this.stateMachine = stateMachine;
        this.paymentCapabilityService = paymentCapabilityService;
        this.depositRate = depositRate;
        this.depositExpiryMinutes = depositExpiryMinutes;
        this.noShowGraceMinutes = noShowGraceMinutes;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        return createReservation(request, null);
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, String idempotencyKey) {
        NormalizedReservation normalized = normalizeAndValidate(request);
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String requestFingerprint = fingerprint(request, normalized);
        if (normalizedIdempotencyKey != null) {
            Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) {
                Reservation reservation = existing.get();
                if (!requestFingerprint.equals(reservation.getRequestFingerprint())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key đã được dùng cho yêu cầu khác");
                }
                return toResponse(reservation, false);
            }
        }

        RestaurantTable table = tableRepository.findLockedById(normalized.tableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"));

        validateTableForGuests(table, normalized.guestCount());
        if (hasConflict(table.getId(), normalized.date(), normalized.time(), normalized.durationMinutes(), null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn đã có lượt đặt trùng khung giờ");
        }

        Reservation reservation = new Reservation();
        reservation.setReservationCode(generateReservationCode(normalized.date()));
        reservation.setIdempotencyKey(normalizedIdempotencyKey);
        reservation.setRequestFingerprint(requestFingerprint);
        reservation.setCustomerName(normalized.customerName());
        reservation.setCustomerPhone(normalized.customerPhone());
        reservation.setCustomerEmail(normalized.customerEmail());
        reservation.setContactNote(trimToNull(request.getContactNote()));
        reservation.setReservationDate(normalized.date());
        reservation.setArrivalTime(normalized.time());
        reservation.setExpectedDurationMinutes(normalized.durationMinutes());
        reservation.setGuestCount(normalized.guestCount());
        reservation.setOccasion(trimToNull(request.getOccasion()));
        reservation.setSpecialRequest(limit(trimToNull(request.getSpecialRequest()), 500));
        reservation.setSeatingPreference(trimToNull(request.getSeatingPreference()));
        reservation.setTable(table);

        TableArea area = resolveArea(request.getAreaId(), table);
        reservation.setArea(area);

        Price tablePrice = calculatePrice(table, area);
        List<ReservationPreorderItem> preorderItems = buildPreorderItems(request.getPreorderItems());
        BigDecimal foodAmount = preorderItems.stream()
                .map(ReservationPreorderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        PaymentOption paymentOption = request.getPaymentOption() == null ? PaymentOption.DEPOSIT_50 : request.getPaymentOption();
        BigDecimal originalTotalAmount = tablePrice.total().add(foodAmount).setScale(0, RoundingMode.HALF_UP);
        VoucherApplication voucherApplication = applyVoucher(request.getVoucherCode(), originalTotalAmount, true);
        BigDecimal totalAmount = voucherApplication.totalAfterDiscount();
        DepositPolicyService.DepositCalculation deposit = depositPolicyService.calculate(
                totalAmount, normalized.guestCount(), normalized.date(), normalized.time(), area == null ? null : area.getId(), table, depositRate);
        BigDecimal payableNow = calculatePayableNow(totalAmount, paymentOption, deposit.amount());

        reservation.setPreorderEnabled(Boolean.TRUE.equals(request.getPreorderEnabled()) && !preorderItems.isEmpty());
        reservation.setPaymentOption(paymentOption);
        reservation.setTableAmount(tablePrice.total());
        reservation.setFoodAmount(foodAmount);
        reservation.setTotalAmount(totalAmount);
        reservation.setDepositRate(deposit.rate());
        reservation.setDepositPolicyCode(deposit.policy().getPolicyCode());
        reservation.setDepositPolicySnapshot(deposit.policy().getExplanation());
        reservation.setDepositAmount(payableNow);
        reservation.setPaidAmount(BigDecimal.ZERO);
        reservation.setRemainingAmount(totalAmount);
        reservation.setPaymentStatus(PaymentStatus.UNPAID);
        reservation.setDepositStatus(payableNow.signum() > 0 ? DepositStatus.PENDING : DepositStatus.NOT_REQUIRED);
        reservation.setReservationStatus(ReservationStatus.PENDING);

        String paymentCapabilityToken = null;
        if (payableNow.signum() > 0 && !PaymentOption.PAY_AT_RESTAURANT.equals(paymentOption)) {
            paymentCapabilityToken = paymentCapabilityService.issue(reservation, currentUsernameOrNull());
        } else {
            reservation.setCreatedBy(currentUsernameOrNull());
        }

        Reservation saved = reservationRepository.save(reservation);
        saveVoucherUsage(saved, voucherApplication, originalTotalAmount);
        for (ReservationPreorderItem item : preorderItems) {
            item.setReservation(saved);
            preorderItemRepository.save(item);
        }
        addHistory(saved, null, ReservationStatus.PENDING, "Khách gửi yêu cầu đặt bàn");
        notificationService.createNotification(
                "RESERVATION_NEW",
                "Yêu cầu đặt bàn mới",
                saved.getReservationCode() + " - " + saved.getCustomerName() + " (" + saved.getGuestCount() + " khách)",
                "ROLE_MANAGER",
                "info",
                "reservation",
                String.valueOf(saved.getId()));
        activityLogService.log("CREATE", "Reservation", String.valueOf(saved.getId()), "Tạo yêu cầu đặt bàn " + saved.getReservationCode());
        ReservationResponse response = toResponse(saved, false);
        realtimeService.publish("RESERVATION_CREATED", saved.getReservationCode(), null, ReservationStatus.PENDING,
                "Khách gửi yêu cầu đặt bàn mới", response);
        response.setPaymentCapabilityToken(paymentCapabilityToken);
        return response;
    }

    @Transactional(readOnly = true)
    public ReservationResponse getPublicReservation(String code, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng nhập số điện thoại để tra cứu đặt bàn");
        }
        Reservation reservation = reservationRepository.findByReservationCodeAndCustomerPhone(code, normalizePhone(phone))
                .orElseThrow(() -> notFound());
        return toResponse(reservation, false);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAdminReservations() {
        return reservationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toResponse(r, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getAdminReservation(Long id) {
        return toResponse(findReservation(id), true);
    }

    @Transactional(readOnly = true)
    public List<AvailableTableResponse> findAvailableTables(String date, String time, Integer durationMinutes,
                                                            Integer guestCount, Integer areaId) {
        LocalDate reservationDate = parseDate(date);
        LocalTime arrivalTime = parseTime(time);
        int duration = durationMinutes == null || durationMinutes < 30 ? DEFAULT_DURATION_MINUTES : durationMinutes;
        int guests = guestCount == null || guestCount < 1 ? 1 : guestCount;

        return tableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()) || t.getActive() == null)
                .filter(t -> areaId == null || (t.getAreaId() != null && t.getAreaId().equals(areaId)))
                .map(t -> toAvailableTable(t, reservationDate, arrivalTime, duration, guests))
                .sorted(Comparator.comparing((AvailableTableResponse t) -> availabilityRank(t.getAvailabilityStatus()))
                        .thenComparing(AvailableTableResponse::getFitScore))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationQuoteResponse quote(ReservationQuoteRequest request) {
        if (request == null || request.getTableId() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng chọn bàn trước khi báo giá");
        }
        RestaurantTable table = tableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn"));
        TableArea area = resolveArea(request.getAreaId(), table);
        Price tablePrice = calculatePrice(table, area);
        List<ReservationPreorderItem> items = buildPreorderItems(request.getPreorderItems());
        BigDecimal foodAmount = items.stream()
                .map(ReservationPreorderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal originalTotal = tablePrice.total().add(foodAmount).setScale(0, RoundingMode.HALF_UP);
        VoucherApplication voucherApplication = applyVoucher(request.getVoucherCode(), originalTotal, false);
        BigDecimal total = voucherApplication.totalAfterDiscount();
        PaymentOption option = request.getPaymentOption() == null ? PaymentOption.DEPOSIT_50 : request.getPaymentOption();
        LocalDate quoteDate = request.getReservationDate() == null || request.getReservationDate().isBlank()
                ? LocalDate.now()
                : parseDate(request.getReservationDate());
        LocalTime quoteTime = request.getArrivalTime() == null || request.getArrivalTime().isBlank()
                ? LocalTime.now()
                : parseTime(request.getArrivalTime());
        DepositPolicyService.DepositCalculation deposit = depositPolicyService.calculate(
                total, request.getGuestCount() == null ? 1 : request.getGuestCount(), quoteDate,
                quoteTime, area == null ? null : area.getId(), table, depositRate);
        BigDecimal payableNow = calculatePayableNow(total, option, deposit.amount());

        ReservationQuoteResponse response = new ReservationQuoteResponse();
        response.setTableAmount(tablePrice.total());
        response.setFoodAmount(foodAmount);
        response.setOriginalTotalAmount(originalTotal);
        response.setVoucherCode(voucherApplication.voucherCode());
        response.setDiscountAmount(voucherApplication.discountAmount());
        response.setTotalAmount(total);
        response.setPaymentOption(option);
        response.setPaymentRate(PaymentOption.DEPOSIT_50.equals(option) ? deposit.rate()
                : (PaymentOption.FULL.equals(option) ? BigDecimal.ONE : BigDecimal.ZERO));
        response.setPayableNow(payableNow);
        response.setRemainingAmount(total.subtract(payableNow));
        response.setDepositPolicy(deposit.policy());
        response.setPreorderItems(items.stream().map(this::toPreorderResponse).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public List<TableSuggestionResponse> suggestTables(TableSuggestionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu gợi ý bàn không hợp lệ");
        }
        LocalDate date = parseDate(request.getReservationDate());
        LocalTime time = parseTime(request.getArrivalTime());
        int duration = request.getDurationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.getDurationMinutes();
        int guests = request.getGuestCount() == null ? 1 : Math.max(1, request.getGuestCount());
        String preference = trimToNull(request.getSeatingPreference());

        List<TableSuggestionResponse> suggestions = tableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()) || t.getActive() == null)
                .filter(t -> request.getAreaId() == null || request.getAreaId().equals(t.getAreaId()))
                .map(t -> toSuggestion(t, date, time, duration, guests, preference, request.getAreaId()))
                .filter(s -> "AVAILABLE".equals(s.getAvailabilityStatus()))
                .sorted(Comparator.comparing(TableSuggestionResponse::getScore).reversed()
                        .thenComparing(TableSuggestionResponse::getTableId))
                .limit(8)
                .toList();
        if (!suggestions.isEmpty()) {
            suggestions.get(0).setBest(true);
        }
        return suggestions;
    }

    @Transactional
    public ReservationResponse confirm(Long id, ReservationActionRequest request) {
        Reservation reservation = findReservation(id);
        Integer newTableId = request != null ? request.getTableId() : null;
        if (newTableId != null && !newTableId.equals(reservation.getTable().getId())) {
            RestaurantTable newTable = tableRepository.findLockedById(newTableId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn mới"));
            validateTableForGuests(newTable, reservation.getGuestCount());
            reservation.setTable(newTable);
            reservation.setArea(resolveArea(request.getAreaId(), newTable));
            Price price = calculatePrice(newTable, reservation.getArea());
            BigDecimal foodAmount = reservation.getFoodAmount() == null ? BigDecimal.ZERO : reservation.getFoodAmount();
            BigDecimal totalAmount = price.total().add(foodAmount).setScale(0, RoundingMode.HALF_UP);
            BigDecimal depositAmount = reservation.getDepositAmount() == null ? BigDecimal.ZERO : reservation.getDepositAmount();
            BigDecimal payableNow = calculatePayableNow(totalAmount, reservation.getPaymentOption(), depositAmount);
            reservation.setTableAmount(price.total());
            reservation.setTotalAmount(totalAmount);
            reservation.setDepositAmount(payableNow);
            reservation.setRemainingAmount(totalAmount.subtract(payableNow));
        }

        if (hasConflict(reservation.getTable().getId(), reservation.getReservationDate(), reservation.getArrivalTime(),
                reservation.getExpectedDurationMinutes(), reservation.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn không còn khả dụng trong khung giờ này");
        }

        ReservationStatus old = reservation.getReservationStatus();
        ReservationStatus nextStatus = reservation.getDepositAmount() != null && reservation.getDepositAmount().signum() > 0
                ? ReservationStatus.DEPOSIT_REQUIRED
                : ReservationStatus.CONFIRMED;
        stateMachine.assertCanTransition(old, nextStatus);
        reservation.setReservationStatus(nextStatus);
        reservation.setDepositStatus(DepositStatus.PENDING);
        reservation.setConfirmedBy(currentUsername());
        reservation.setConfirmedAt(new Date());
        reservation.setManagerNote(trimToNull(request != null ? request.getNote() : null));
        reservation.setUpdatedAt(new Date());
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, nextStatus, "Quản lý xác nhận đặt bàn");
        notifyReservation(saved, "RESERVATION_CONFIRMED", "Đặt bàn đã được xác nhận", "Yêu cầu " + saved.getReservationCode() + " đã được xác nhận.");
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_CONFIRMED", saved.getReservationCode(), old, nextStatus,
                "Đặt bàn đã được xác nhận", response);
        return response;
    }

    @Transactional
    public ReservationResponse reject(Long id, ReservationActionRequest request) {
        String reason = trimToNull(request != null ? request.getReason() : null);
        if (reason == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng nhập lý do từ chối");
        }
        Reservation reservation = findReservation(id);
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, ReservationStatus.REJECTED);
        reservation.setReservationStatus(ReservationStatus.REJECTED);
        reservation.setRejectedReason(reason);
        reservation.setUpdatedAt(new Date());
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, ReservationStatus.REJECTED, reason);
        notifyReservation(saved, "RESERVATION_REJECTED", "Đặt bàn bị từ chối", reason);
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_REJECTED", saved.getReservationCode(), old, ReservationStatus.REJECTED, reason, response);
        return response;
    }

    @Transactional
    public ReservationResponse cancel(Long id, ReservationActionRequest request) {
        Reservation reservation = findReservation(id);
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, ReservationStatus.CANCELLED);
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservation.setManagerNote(trimToNull(request != null ? request.getNote() : null));
        reservation.setUpdatedAt(new Date());
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, ReservationStatus.CANCELLED, saved.getManagerNote());
        notifyReservation(saved, "RESERVATION_CANCELLED", "Đặt bàn đã bị hủy", saved.getManagerNote());
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_CANCELLED", saved.getReservationCode(), old, ReservationStatus.CANCELLED,
                "Đặt bàn đã bị hủy", response);
        return response;
    }

    @Transactional
    public ReservationResponse markDepositPaid(Long id, ReservationActionRequest request) {
        Reservation reservation = findReservation(id);
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, ReservationStatus.DEPOSIT_PAID);
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setReservationStatus(ReservationStatus.DEPOSIT_PAID);
        reservation.setManagerNote(trimToNull(request != null ? request.getNote() : null));
        reservation.setUpdatedAt(new Date());
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, ReservationStatus.DEPOSIT_PAID, "Đã nhận tiền đặt cọc");
        notifyReservation(saved, "RESERVATION_DEPOSIT_PAID", "Đã nhận tiền đặt cọc", saved.getReservationCode());
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_DEPOSIT_PAID", saved.getReservationCode(), old, ReservationStatus.DEPOSIT_PAID,
                "Đã nhận tiền đặt cọc", response);
        return response;
    }

    @Transactional
    public ReservationResponse checkIn(Long id, ReservationActionRequest request) {
        Reservation reservation = findReservation(id);
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, ReservationStatus.CHECKED_IN);
        reservation.setReservationStatus(ReservationStatus.CHECKED_IN);
        reservation.setUpdatedAt(new Date());
        RestaurantTable table = reservation.getTable();
        table.setIsOccupied(2);
        table.setReservedTime("Khách đã đến: " + reservation.getReservationCode());
        tableRepository.save(table);
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, ReservationStatus.CHECKED_IN, trimToNull(request != null ? request.getNote() : null));
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_CHECKED_IN", saved.getReservationCode(), old, ReservationStatus.CHECKED_IN,
                "Khách đã đến nhà hàng", response);
        return response;
    }

    @Scheduled(
            initialDelayString = "${restaurant.reservation.expiry-initial-delay-ms:60000}",
            fixedDelayString = "${restaurant.reservation.expiry-scan-ms:60000}")
    @Transactional
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();
        Date depositDeadline = new Date(System.currentTimeMillis() - depositExpiryMinutes * 60_000L);
        for (Reservation reservation : reservationRepository.findAllByOrderByCreatedAtDesc()) {
            ReservationStatus status = reservation.getReservationStatus();
            if (isDepositExpired(status, reservation, depositDeadline)) {
                transitionSystem(reservation, ReservationStatus.EXPIRED, "Quá hạn thanh toán đặt cọc");
                continue;
            }
            if (isNoShow(status, reservation, now)) {
                transitionSystem(reservation, ReservationStatus.NO_SHOW, "Khách không đến sau thời gian giữ bàn");
            }
        }
    }

    private boolean isDepositExpired(ReservationStatus status, Reservation reservation, Date deadline) {
        if (!EnumSet.of(ReservationStatus.PENDING, ReservationStatus.DEPOSIT_REQUIRED, ReservationStatus.DEPOSIT_PENDING).contains(status)) {
            return false;
        }
        if (reservation.getDepositAmount() == null || reservation.getDepositAmount().signum() <= 0) {
            return false;
        }
        return reservation.getCreatedAt() != null && reservation.getCreatedAt().before(deadline);
    }

    private boolean isNoShow(ReservationStatus status, Reservation reservation, LocalDateTime now) {
        if (!EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.DEPOSIT_PAID, ReservationStatus.FULLY_PAID).contains(status)) {
            return false;
        }
        if (reservation.getReservationDate() == null || reservation.getArrivalTime() == null) {
            return false;
        }
        LocalDateTime noShowAt = LocalDateTime.of(reservation.getReservationDate(), reservation.getArrivalTime())
                .plusMinutes(noShowGraceMinutes);
        return now.isAfter(noShowAt);
    }

    private void transitionSystem(Reservation reservation, ReservationStatus nextStatus, String note) {
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, nextStatus);
        reservation.setReservationStatus(nextStatus);
        reservation.setUpdatedAt(new Date());
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, nextStatus, note);
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_" + nextStatus.name(), saved.getReservationCode(), old, nextStatus, note, response);
        activityLogService.log("UPDATE", "Reservation", String.valueOf(saved.getId()), note + " " + saved.getReservationCode());
    }

    private NormalizedReservation normalizeAndValidate(ReservationRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đặt bàn không hợp lệ");
        String name = trimToNull(request.getCustomerName());
        String phone = normalizePhone(request.getCustomerPhone());
        String email = trimToNull(request.getCustomerEmail());
        LocalDate date = parseDate(request.getReservationDate());
        LocalTime time = parseTime(request.getArrivalTime());
        int duration = request.getExpectedDurationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.getExpectedDurationMinutes();
        Integer tableId = request.getTableId();
        Integer guests = request.getGuestCount();

        validateSafeText(name, "Họ tên");
        validateSafeText(email, "Email");
        validateSafeText(request.getContactNote(), "Ghi chú liên hệ");
        validateSafeText(request.getOccasion(), "Dịp sử dụng");
        validateSafeText(request.getSeatingPreference(), "Sở thích vị trí");
        validateSafeText(request.getSpecialRequest(), "Yêu cầu đặc biệt");

        if (name == null) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Họ tên không được để trống");
        if (name.length() < 2) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Họ tên phải có ít nhất 2 ký tự");
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số điện thoại Việt Nam không hợp lệ");
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Email không hợp lệ");
        }
        if (duration < 30 || duration > 360) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Thời lượng sử dụng bàn phải từ 30 đến 360 phút");
        }
        if (guests == null || guests < 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số lượng khách không hợp lệ");
        }
        if (tableId == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng chọn bàn");
        }
        validateReservationTime(date, time, duration);
        return new NormalizedReservation(name, phone, email, date, time, duration, guests, tableId);
    }

    private void validateReservationTime(LocalDate date, LocalTime time, int duration) {
        LocalDateTime arrival = LocalDateTime.of(date, time);
        if (arrival.isBefore(LocalDateTime.now().plusMinutes(MIN_ADVANCE_MINUTES))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cần đặt bàn trước ít nhất 30 phút");
        }
        if (time.isBefore(OPEN_TIME) || time.plusMinutes(duration).isAfter(CLOSE_TIME)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Thời gian đặt bàn nằm ngoài giờ hoạt động 09:00-22:00");
        }
    }

    private boolean hasConflict(Integer tableId, LocalDate date, LocalTime start, int duration, Long currentReservationId) {
        LocalTime end = start.plusMinutes(duration + CLEANUP_MINUTES);
        List<Reservation> reservations = reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                date, tableId, BLOCKING_STATUSES);
        return reservations.stream()
                .filter(r -> currentReservationId == null || !r.getId().equals(currentReservationId))
                .anyMatch(r -> {
                    LocalTime otherStart = r.getArrivalTime();
                    LocalTime otherEnd = otherStart.plusMinutes(r.getExpectedDurationMinutes() + CLEANUP_MINUTES);
                    return start.isBefore(otherEnd) && end.isAfter(otherStart);
                });
    }

    private void validateTableForGuests(RestaurantTable table, int guestCount) {
        if (Boolean.FALSE.equals(table.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn đang tạm ngưng");
        }
        if (table.getIsOccupied() != null && table.getIsOccupied() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn không ở trạng thái trống");
        }
        int max = table.getMaxCapacity() != null ? table.getMaxCapacity() :
                (table.getCapacity() != null ? table.getCapacity() : 0);
        if (max < guestCount) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sức chứa bàn nhỏ hơn số lượng khách");
        }
    }

    private Price calculatePrice(RestaurantTable table, TableArea area) {
        BigDecimal tablePrice = table.getReservationPrice() != null ? table.getReservationPrice() : BigDecimal.ZERO;
        BigDecimal areaPrice = area != null && area.getBasePrice() != null ? area.getBasePrice() : BigDecimal.ZERO;
        BigDecimal fallback = BigDecimal.valueOf(Math.max(1, table.getMaxCapacity() != null ? table.getMaxCapacity() : 4) * 100000L);
        BigDecimal total = tablePrice.signum() > 0 ? tablePrice : (areaPrice.signum() > 0 ? areaPrice : fallback);
        total = total.setScale(0, RoundingMode.HALF_UP);
        BigDecimal deposit = total.multiply(depositRate).setScale(0, RoundingMode.HALF_UP);
        return new Price(total, deposit, total.subtract(deposit));
    }

    private BigDecimal calculatePayableNow(BigDecimal totalAmount, PaymentOption option, BigDecimal depositAmount) {
        if (PaymentOption.PAY_AT_RESTAURANT.equals(option)) {
            return BigDecimal.ZERO;
        }
        if (PaymentOption.FULL.equals(option)) {
            return totalAmount.setScale(0, RoundingMode.HALF_UP);
        }
        return depositAmount == null
                ? totalAmount.multiply(depositRate).setScale(0, RoundingMode.HALF_UP)
                : depositAmount.setScale(0, RoundingMode.HALF_UP);
    }

    private VoucherApplication applyVoucher(String rawCode, BigDecimal originalTotal, boolean markAsUsed) {
        String code = trimToNull(rawCode);
        if (code == null) {
            return VoucherApplication.none(originalTotal);
        }
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã giảm giá không tồn tại"));
        if (Boolean.TRUE.equals(voucher.getIsUsed())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã giảm giá đã được sử dụng");
        }
        if (voucher.getDiscountPercent() == null || voucher.getDiscountPercent() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã giảm giá không hợp lệ");
        }
        if (voucher.getAccount() != null) {
            String currentUsername = currentUsernameOrNull();
            if (currentUsername == null || !voucher.getAccount().getUsername().equals(currentUsername)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Mã giảm giá này không dành cho bạn");
            }
        }
        BigDecimal discount = originalTotal
                .multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .min(originalTotal)
                .max(BigDecimal.ZERO);
        BigDecimal totalAfterDiscount = originalTotal.subtract(discount).setScale(0, RoundingMode.HALF_UP);
        if (markAsUsed) {
            voucher.setIsUsed(true);
            voucherRepository.save(voucher);
        }
        return new VoucherApplication(voucher, voucher.getCode(), discount, totalAfterDiscount);
    }

    private void saveVoucherUsage(Reservation reservation, VoucherApplication voucherApplication, BigDecimal originalTotal) {
        if (voucherApplication == null || voucherApplication.voucher() == null || voucherApplication.discountAmount().signum() <= 0) {
            return;
        }
        ReservationVoucherUsage usage = new ReservationVoucherUsage();
        usage.setReservationId(reservation.getId());
        usage.setVoucherId(voucherApplication.voucher().getId());
        usage.setVoucherCode(voucherApplication.voucherCode());
        usage.setDiscountScope("RESERVATION_TOTAL");
        usage.setDiscountAmount(voucherApplication.discountAmount());
        usage.setSnapshotJson("{\"originalTotal\":" + originalTotal + ",\"discountPercent\":"
                + voucherApplication.voucher().getDiscountPercent() + ",\"totalAfterDiscount\":"
                + voucherApplication.totalAfterDiscount() + "}");
        usage.setCreatedAt(new Date());
        voucherUsageRepository.save(usage);
    }

    private String currentUsernameOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return authentication.getName();
    }

    private List<ReservationPreorderItem> buildPreorderItems(List<PreorderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .filter(item -> item != null && item.getProductId() != null && item.getQuantity() != null && item.getQuantity() > 0)
                .map(this::buildPreorderItem)
                .toList();
    }

    private ReservationPreorderItem buildPreorderItem(PreorderItemRequest request) {
        if (request.getQuantity() > 50) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số lượng món đặt trước không hợp lệ");
        }
        validateSafeText(request.getNote(), "Ghi chú món");
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));
        if (Boolean.FALSE.equals(product.getStatus()) || Boolean.FALSE.equals(product.getAvailable())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Món ăn không còn khả dụng để đặt trước");
        }
        BigDecimal unitPrice = BigDecimal.valueOf(product.getPrice() == null ? 0 : product.getPrice()).setScale(0, RoundingMode.HALF_UP);
        ReservationPreorderItem item = new ReservationPreorderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getImage());
        item.setCategoryName(product.getCategory() == null ? null : product.getCategory().getName());
        item.setUnitPrice(unitPrice);
        item.setQuantity(request.getQuantity());
        item.setNote(limit(trimToNull(request.getNote()), 300));
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())).setScale(0, RoundingMode.HALF_UP));
        return item;
    }

    private PreorderItemResponse toPreorderResponse(ReservationPreorderItem item) {
        PreorderItemResponse response = new PreorderItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProductId());
        response.setProductName(item.getProductName());
        response.setProductImage(item.getProductImage());
        response.setCategoryName(item.getCategoryName());
        response.setUnitPrice(item.getUnitPrice());
        response.setQuantity(item.getQuantity());
        response.setNote(item.getNote());
        response.setLineTotal(item.getLineTotal());
        response.setStatus(item.getStatus() == null ? null : item.getStatus().name());
        return response;
    }

    private PaymentQrResponse toPaymentResponse(PaymentIntent payment) {
        PaymentQrResponse response = new PaymentQrResponse();
        response.setPaymentCode(payment.getPaymentCode());
        response.setAmount(payment.getAmount());
        response.setPaymentOption(payment.getPaymentOption());
        response.setStatus(payment.getStatus());
        response.setBankCode(payment.getBankCode());
        response.setAccountNumber(payment.getAccountNumber());
        response.setAccountHolder(payment.getAccountHolder());
        response.setTransferContent(payment.getTransferContent());
        response.setQrUrl(payment.getQrUrl());
        response.setExpiresAt(payment.getExpiresAt());
        return response;
    }

    private ReservationResponse toResponse(Reservation reservation, boolean includeInternal) {
        ReservationResponse response = new ReservationResponse();
        response.setId(reservation.getId());
        response.setReservationCode(reservation.getReservationCode());
        response.setCustomerName(reservation.getCustomerName());
        response.setCustomerPhone(includeInternal
                ? reservation.getCustomerPhone()
                : maskPhone(reservation.getCustomerPhone()));
        response.setCustomerEmail(includeInternal
                ? reservation.getCustomerEmail()
                : maskEmail(reservation.getCustomerEmail()));
        response.setReservationDate(reservation.getReservationDate() == null ? null : reservation.getReservationDate().toString());
        response.setArrivalTime(reservation.getArrivalTime() == null ? null : reservation.getArrivalTime().toString());
        response.setExpectedDurationMinutes(reservation.getExpectedDurationMinutes());
        response.setGuestCount(reservation.getGuestCount());
        response.setOccasion(reservation.getOccasion());
        response.setSpecialRequest(reservation.getSpecialRequest());
        response.setSeatingPreference(reservation.getSeatingPreference());
        response.setPreorderEnabled(reservation.getPreorderEnabled());
        response.setReservationStatus(reservation.getReservationStatus());
        response.setOriginalTotalAmount(reservation.getTotalAmount());
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setTotalAmount(reservation.getTotalAmount());
        response.setTableAmount(reservation.getTableAmount());
        response.setFoodAmount(reservation.getFoodAmount());
        response.setDepositRate(reservation.getDepositRate());
        response.setDepositAmount(reservation.getDepositAmount());
        response.setPaidAmount(reservation.getPaidAmount());
        response.setRemainingAmount(reservation.getRemainingAmount());
        response.setDepositStatus(reservation.getDepositStatus());
        response.setPaymentOption(reservation.getPaymentOption());
        response.setPaymentStatus(reservation.getPaymentStatus());
        response.setRejectedReason(reservation.getRejectedReason());
        response.setConfirmedAt(reservation.getConfirmedAt());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setPreorderItems(preorderItemRepository.findByReservationIdOrderByIdAsc(reservation.getId()).stream()
                .map(this::toPreorderResponse)
                .toList());
        if (includeInternal) {
            response.setPayments(paymentIntentRepository.findByReservationIdOrderByCreatedAtDesc(reservation.getId()).stream()
                    .map(this::toPaymentResponse)
                    .toList());
        }
        voucherUsageRepository.findByReservationIdOrderByCreatedAtDesc(reservation.getId()).stream().findFirst()
                .ifPresent(usage -> {
                    response.setVoucherCode(usage.getVoucherCode());
                    response.setDiscountAmount(usage.getDiscountAmount());
                    response.setOriginalTotalAmount(reservation.getTotalAmount().add(usage.getDiscountAmount()));
                });
        if (reservation.getArea() != null) {
            response.setAreaId(reservation.getArea().getId());
            response.setAreaName(reservation.getArea().getNameVi());
        }
        if (reservation.getTable() != null) {
            response.setTableId(reservation.getTable().getId());
            response.setTableName(reservation.getTable().getName());
            response.setTableFloor(reservation.getTable().getFloor());
        }
        if (includeInternal) {
            response.setManagerNote(reservation.getManagerNote());
            response.setHistory(historyRepository.findByReservationIdOrderByChangedAtAsc(reservation.getId()).stream()
                    .map(h -> h.getChangedAt() + " - " + h.getNewStatus() + " - " + (h.getNote() == null ? "" : h.getNote()))
                    .toList());
        }
        return response;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }

    private AvailableTableResponse toAvailableTable(RestaurantTable table, LocalDate date, LocalTime time, int duration, int guests) {
        TableArea area = table.getAreaId() == null ? null : areaRepository.findById(table.getAreaId()).orElse(null);
        boolean conflict = hasConflict(table.getId(), date, time, duration, null);
        boolean unavailableByState = Boolean.FALSE.equals(table.getActive())
                || (table.getIsOccupied() != null && table.getIsOccupied() != 0);
        int max = table.getMaxCapacity() != null ? table.getMaxCapacity() : (table.getCapacity() == null ? 0 : table.getCapacity());
        AvailableTableResponse response = new AvailableTableResponse();
        response.setId(table.getId());
        response.setName(table.getName());
        response.setFloor(table.getFloor());
        response.setCapacity(table.getCapacity());
        response.setMinCapacity(table.getMinCapacity());
        response.setMaxCapacity(max);
        response.setReservationPrice(calculatePrice(table, area).total());
        response.setAreaId(table.getAreaId());
        response.setAreaName(area != null ? area.getNameVi() : table.getFloor());
        response.setViewType(table.getViewType());
        response.setHasView(table.getHasView());
        response.setWindowSeat(table.getWindowSeat());
        response.setPrivateRoom(table.getPrivateRoom());
        response.setChildFriendly(table.getChildFriendly());
        response.setPositionDescription(table.getPositionDescription());
        response.setImageUrl(table.getImageUrl());
        response.setAvailabilityStatus(conflict ? "RESERVED" : (unavailableByState ? "UNAVAILABLE" : "AVAILABLE"));
        response.setFitScore(Math.abs(max - guests));
        if (max < guests) {
            response.setAvailabilityStatus("TOO_SMALL");
            response.setWarning("Bàn nhỏ hơn số lượng khách");
        } else if (max - guests >= 4) {
            response.setWarning("Bàn lớn hơn nhu cầu, vui lòng cân nhắc");
        }
        return response;
    }

    private TableSuggestionResponse toSuggestion(RestaurantTable table, LocalDate date, LocalTime time, int duration,
                                                 int guests, String preference, Integer requestedAreaId) {
        TableArea area = table.getAreaId() == null ? null : areaRepository.findById(table.getAreaId()).orElse(null);
        int max = table.getMaxCapacity() != null ? table.getMaxCapacity() : (table.getCapacity() == null ? 0 : table.getCapacity());
        boolean conflict = hasConflict(table.getId(), date, time, duration, null);
        boolean unavailableByState = Boolean.FALSE.equals(table.getActive())
                || (table.getIsOccupied() != null && table.getIsOccupied() != 0);
        List<String> reasons = new java.util.ArrayList<>();
        int score = 0;

        if (!conflict && !unavailableByState && max >= guests) {
            int waste = max - guests;
            int capacityScore = Math.max(0, 40 - waste * 6);
            score += capacityScore;
            reasons.add(waste == 0 ? "Sức chứa vừa đủ" : "Sức chứa phù hợp, dư " + waste + " chỗ");
        }
        if (requestedAreaId != null && requestedAreaId.equals(table.getAreaId())) {
            score += 20;
            reasons.add("Đúng khu vực khách chọn");
        }
        if (preference != null) {
            String p = preference.toLowerCase(Locale.ROOT);
            if ((p.contains("cửa sổ") || p.contains("window")) && Boolean.TRUE.equals(table.getWindowSeat())) {
                score += 15;
                reasons.add("Phù hợp sở thích gần cửa sổ");
            }
            if ((p.contains("riêng") || p.contains("vip") || p.contains("private")) && Boolean.TRUE.equals(table.getPrivateRoom())) {
                score += 15;
                reasons.add("Phù hợp nhu cầu phòng riêng/VIP");
            }
            if ((p.contains("view") || p.contains("cảnh")) && Boolean.TRUE.equals(table.getHasView())) {
                score += 10;
                reasons.add("Có view phù hợp");
            }
        }
        if (Boolean.TRUE.equals(table.getChildFriendly())) {
            score += 5;
            reasons.add("Phù hợp gia đình/trẻ em");
        }

        TableSuggestionResponse response = new TableSuggestionResponse();
        response.setTableId(table.getId());
        response.setTableName(table.getName());
        response.setAreaId(table.getAreaId());
        response.setAreaName(area != null ? area.getNameVi() : table.getFloor());
        response.setCapacity(table.getCapacity());
        response.setMaxCapacity(max);
        response.setReservationPrice(calculatePrice(table, area).total());
        response.setScore(score);
        response.setReasons(reasons);
        response.setAvailabilityStatus(conflict ? "RESERVED" : (unavailableByState || max < guests ? "UNAVAILABLE" : "AVAILABLE"));
        return response;
    }

    private void addHistory(Reservation reservation, ReservationStatus oldStatus, ReservationStatus newStatus, String note) {
        ReservationStatusHistory history = new ReservationStatusHistory();
        history.setReservation(reservation);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(currentUsername());
        history.setNote(note);
        historyRepository.save(history);
    }
    private void notifyReservation(Reservation reservation, String type, String title, String message) {
        notificationService.createNotification(type, title, message, "ROLE_MANAGER", "info", "reservation", String.valueOf(reservation.getId()));
        activityLogService.log("UPDATE", "Reservation", String.valueOf(reservation.getId()), title + " " + reservation.getReservationCode());
    }

    private TableArea resolveArea(Integer areaId, RestaurantTable table) {
        Integer resolvedId = areaId != null ? areaId : table.getAreaId();
        if (resolvedId == null) return null;
        return areaRepository.findById(resolvedId).orElse(null);
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> notFound());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặt bàn");
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(trimToNull(value));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày đặt bàn không hợp lệ");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(trimToNull(value));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Giờ đến không hợp lệ");
        }
    }

    private String generateReservationCode(LocalDate date) {
        String prefix = "MV-" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long sequence = reservationRepository.countByReservationDate(date) + 1;
        String code;
        do {
            code = prefix + String.format(Locale.ROOT, "%04d", sequence++);
        } while (reservationRepository.findByReservationCode(code).isPresent());
        return code;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        String normalized = trimToNull(idempotencyKey);
        if (normalized == null) return null;
        if (normalized.length() > 80) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency key không hợp lệ");
        }
        return normalized;
    }

    private String fingerprint(ReservationRequest request, NormalizedReservation normalized) {
        String preorder = request.getPreorderItems() == null ? "" : request.getPreorderItems().stream()
                .map(item -> item.getProductId() + ":" + item.getQuantity() + ":" + trimToNull(item.getNote()))
                .sorted()
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
        String source = String.join("|",
                normalized.customerName(),
                normalized.customerPhone(),
                String.valueOf(normalized.customerEmail()),
                normalized.date().toString(),
                normalized.time().toString(),
                String.valueOf(normalized.durationMinutes()),
                String.valueOf(normalized.guestCount()),
                String.valueOf(normalized.tableId()),
                String.valueOf(request.getAreaId()),
                String.valueOf(request.getPaymentOption()),
                String.valueOf(trimToNull(request.getVoucherCode())),
                String.valueOf(Boolean.TRUE.equals(request.getPreorderEnabled())),
                preorder);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo fingerprint đặt bàn", e);
        }
    }

    private String normalizePhone(String phone) {
        String normalized = trimToNull(phone);
        if (normalized == null) return null;
        normalized = normalized.replaceAll("[\\s.\\-]", "");
        if (normalized.startsWith("+84")) {
            normalized = "0" + normalized.substring(3);
        } else if (normalized.startsWith("84") && normalized.length() == 11) {
            normalized = "0" + normalized.substring(2);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    private void validateSafeText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed != null && UNSAFE_TEXT_PATTERN.matcher(trimmed).find()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, fieldName + " chứa nội dung không an toàn");
        }
    }

    private int availabilityRank(String status) {
        if ("AVAILABLE".equals(status)) return 0;
        if ("TOO_SMALL".equals(status)) return 1;
        if ("RESERVED".equals(status)) return 2;
        return 3;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "GUEST";
    }

    private record NormalizedReservation(String customerName, String customerPhone, String customerEmail,
                                         LocalDate date, LocalTime time, int durationMinutes,
                                         int guestCount, Integer tableId) {
    }

    private record Price(BigDecimal total, BigDecimal deposit, BigDecimal remaining) {
    }

    private record VoucherApplication(Voucher voucher, String voucherCode, BigDecimal discountAmount,
                                      BigDecimal totalAfterDiscount) {
        static VoucherApplication none(BigDecimal total) {
            return new VoucherApplication(null, null, BigDecimal.ZERO, total);
        }
    }
}
