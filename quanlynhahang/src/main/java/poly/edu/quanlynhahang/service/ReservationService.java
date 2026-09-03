package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import poly.edu.quanlynhahang.dto.AvailableTableResponse;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.dto.PreorderItemRequest;
import poly.edu.quanlynhahang.dto.PreorderItemResponse;
import poly.edu.quanlynhahang.dto.ReservationActionRequest;
import poly.edu.quanlynhahang.dto.ReservationContactUpdateRequest;
import poly.edu.quanlynhahang.dto.ReservationContactLogResponse;
import poly.edu.quanlynhahang.dto.ReservationQuoteRequest;
import poly.edu.quanlynhahang.dto.ReservationQuoteResponse;
import poly.edu.quanlynhahang.dto.ReservationRequest;
import poly.edu.quanlynhahang.dto.ReservationResponse;
import poly.edu.quanlynhahang.dto.EventBookingRequest;
import poly.edu.quanlynhahang.dto.ReservationTableResponse;
import poly.edu.quanlynhahang.dto.PublicReservationResponse;
import poly.edu.quanlynhahang.dto.TableCombinationResponse;
import poly.edu.quanlynhahang.dto.TableSuggestionRequest;
import poly.edu.quanlynhahang.dto.TableSuggestionResponse;
import poly.edu.quanlynhahang.dto.AdminTableAssignmentOptions;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.entity.PaymentIntent;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.ReservationPreorderItem;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.ReservationStatusHistory;
import poly.edu.quanlynhahang.entity.ReservationTableAssignment;
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
import poly.edu.quanlynhahang.repository.ReservationContactLogRepository;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReservationService {
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_TEXT_PATTERN = Pattern.compile("(?i)<\\s*script|javascript:|onerror\\s*=|onload\\s*=");
    private static final int DEFAULT_DURATION_MINUTES = 120;
    // NOTE: Thời gian dọn bàn được cộng vào cuối mỗi lượt để tránh xếp hai lượt quá sát nhau.
    private static final int CLEANUP_MINUTES = 15;
    // NOTE: Quy tắc đặt trước được kiểm tra tại backend, không phụ thuộc dữ liệu hợp lệ từ giao diện.
    private static final int MIN_ADVANCE_MINUTES = 30;
    // NOTE: Giới hạn số bàn ghép giúp phương án vận hành thực tế và tránh tổ hợp quá phức tạp.
    private static final int MAX_COMBINED_TABLES = 4;
    // NOTE: Chỉ các trạng thái còn chiếm dụng khung giờ mới tham gia kiểm tra trùng bàn.
    private static final EnumSet<ReservationStatus> BLOCKING_STATUSES = EnumSet.of(
            ReservationStatus.PENDING,
            ReservationStatus.WAITING_TABLE_ASSIGNMENT,
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
    private final ReservationContactLogRepository contactLogRepository;
    private final RestaurantTableRepository tableRepository;
    private final TableAreaRepository areaRepository;
    private final ProductRepository productRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherLifecycleService voucherLifecycleService;
    private final ReservationVoucherUsageRepository voucherUsageRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final ReservationRealtimeService realtimeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final DepositPolicyService depositPolicyService;
    private final ReservationStateMachine stateMachine;
    private final PaymentCapabilityService paymentCapabilityService;
    private final OrderCheckoutService orderCheckoutService;
    private final AutoTableAssignmentService autoTableAssignmentService;
    private final RestaurantCapacityService restaurantCapacityService;
    private final RestaurantSettingsService restaurantSettingsService;
    private final RestaurantBusinessHoursService businessHoursService;
    private final TableLifecycleService tableLifecycleService;
    private final SqlServerApplicationLockService applicationLockService;
    private final TableAreaReadinessService areaReadinessService;
    private final TransactionTemplate expiryTransactionTemplate;
    private final BigDecimal depositRate;
    private final long depositExpiryMinutes;
    private final long noShowGraceMinutes;
    private final TableCombinationPlanner tableCombinationPlanner = new TableCombinationPlanner();

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
                              SimpMessagingTemplate messagingTemplate,
                              DepositPolicyService depositPolicyService,
                              ReservationStateMachine stateMachine,
                              PaymentCapabilityService paymentCapabilityService,
                              OrderCheckoutService orderCheckoutService,
                              AutoTableAssignmentService autoTableAssignmentService,
                              RestaurantCapacityService restaurantCapacityService,
                              RestaurantSettingsService restaurantSettingsService,
                              RestaurantBusinessHoursService businessHoursService,
                              ReservationContactLogRepository contactLogRepository,
                              TableLifecycleService tableLifecycleService,
                              SqlServerApplicationLockService applicationLockService,
                              TableAreaReadinessService areaReadinessService,
                              PlatformTransactionManager transactionManager,
                              @Value("${restaurant.reservation.deposit-rate:0.50}") BigDecimal depositRate,
                              @Value("${restaurant.reservation.deposit-expiry-minutes:15}") long depositExpiryMinutes,
                              @Value("${restaurant.reservation.no-show-grace-minutes:15}") long noShowGraceMinutes) {
        this.reservationRepository = reservationRepository;
        this.preorderItemRepository = preorderItemRepository;
        this.paymentIntentRepository = paymentIntentRepository;
        this.historyRepository = historyRepository;
        this.tableRepository = tableRepository;
        this.areaRepository = areaRepository;
        this.productRepository = productRepository;
        this.voucherRepository = voucherRepository;
        this.voucherLifecycleService = new VoucherLifecycleService(voucherRepository);
        this.voucherUsageRepository = voucherUsageRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.realtimeService = realtimeService;
        this.messagingTemplate = messagingTemplate;
        this.depositPolicyService = depositPolicyService;
        this.stateMachine = stateMachine;
        this.paymentCapabilityService = paymentCapabilityService;
        this.orderCheckoutService = orderCheckoutService;
        this.autoTableAssignmentService = autoTableAssignmentService;
        this.restaurantCapacityService = restaurantCapacityService;
        this.restaurantSettingsService = restaurantSettingsService;
        this.businessHoursService = businessHoursService;
        this.contactLogRepository = contactLogRepository;
        this.tableLifecycleService = tableLifecycleService;
        this.applicationLockService = applicationLockService;
        this.areaReadinessService = areaReadinessService;
        this.expiryTransactionTemplate = new TransactionTemplate(transactionManager);
        this.expiryTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.depositRate = depositRate;
        this.depositExpiryMinutes = depositExpiryMinutes;
        this.noShowGraceMinutes = noShowGraceMinutes;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        return createReservation(request, null);
    }

    @Transactional
    public ReservationResponse createEventBooking(EventBookingRequest request) {
        return createEventBooking(request, null);
    }

    @Transactional
    public ReservationResponse createEventBooking(EventBookingRequest request, String idempotencyKey) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String requestFingerprint = eventFingerprint(request);
        if (normalizedIdempotencyKey != null) {
            requireIdempotencyLock("event:" + normalizedIdempotencyKey);
            Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) {
                if (!requestFingerprint.equals(existing.get().getRequestFingerprint())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Idempotency key đã được dùng cho yêu cầu khác");
                }
                return toResponse(existing.get(), false);
            }
        }
        TableArea area = areaRepository.findById(request.areaId()).orElseThrow(this::notFound);
        if (area.getAreaType() != AreaType.EVENT_HALL) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Khu vực không phải sảnh sự kiện");
        if (!"ACTIVE".equals(area.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Sảnh sự kiện đang tạm ngưng");
        if (area.getSuitableEventTypes() != null && !area.getSuitableEventTypes().isEmpty()
                && !area.getSuitableEventTypes().contains(request.eventType().name())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Sảnh này không phù hợp với loại sự kiện đã chọn");
        }
        if (request.guestCount() < area.getMinGuestCount() || request.guestCount() > area.getMaxGuestCount()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số khách không nằm trong sức chứa sảnh");
        if (request.durationHours() < area.getMinBookingHours()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Thời lượng thuê chưa đạt mức tối thiểu");
        LocalDate date = parseDate(request.reservationDate()); LocalTime time = parseTime(request.arrivalTime());
        validateReservationTime(date, time, request.durationHours() * 60, false);
        restaurantCapacityService.requireCapacity(date, time, request.durationHours() * 60, request.guestCount());
        List<ReservationPreorderItem> preorderItems = buildPreorderItems(request.preorderItems());
        BigDecimal foodAmount = preorderItems.stream().map(ReservationPreorderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = area.getHourlyRate().multiply(BigDecimal.valueOf(request.durationHours())).add(area.getPackagePrice()).add(foodAmount).setScale(0, RoundingMode.HALF_UP);
        DepositPolicyService.DepositCalculation deposit = depositPolicyService.calculate(total, request.guestCount(), date, time, area.getId(), null, new BigDecimal("0.70"));
        Reservation reservation = new Reservation();
        reservation.setIdempotencyKey(normalizedIdempotencyKey);
        reservation.setRequestFingerprint(requestFingerprint);
        reservation.setReservationCode(generateReservationCode(date)); reservation.setCustomerName(request.customerName().trim()); reservation.setCustomerPhone(normalizePhone(request.customerPhone())); reservation.setCustomerEmail(trimToNull(request.customerEmail()));
        reservation.setReservationDate(date); reservation.setArrivalTime(time); reservation.setExpectedDurationMinutes(request.durationHours() * 60); reservation.setGuestCount(request.guestCount()); reservation.setArea(area);
        reservation.setEventType(request.eventType()); reservation.setEventDecorationRequired(Boolean.TRUE.equals(request.decorationRequired())); reservation.setEventMcRequired(Boolean.TRUE.equals(request.mcRequired())); reservation.setEventNote(trimToNull(request.eventNote()));
        reservation.setPreorderEnabled(Boolean.TRUE.equals(request.preorderEnabled()) && !preorderItems.isEmpty());
        reservation.setPaymentOption(PaymentOption.DEPOSIT_50);
        reservation.setTotalAmount(total);
        reservation.setTableAmount(total.subtract(foodAmount));
        reservation.setFoodAmount(foodAmount);
        reservation.setDepositAmount(deposit.amount());
        reservation.setDepositRate(deposit.rate());
        reservation.setDepositPolicyCode(deposit.policy().getPolicyCode());
        reservation.setDepositPolicySnapshot(deposit.policy().getExplanation());
        reservation.setPaidAmount(BigDecimal.ZERO);
        reservation.setRemainingAmount(total);
        reservation.setPaymentStatus(PaymentStatus.UNPAID);
        reservation.setDepositStatus(deposit.amount().signum() > 0
                ? DepositStatus.PENDING : DepositStatus.NOT_REQUIRED);
        reservation.setReservationStatus(ReservationStatus.WAITING_TABLE_ASSIGNMENT);
        String createdBy = currentUsernameOrNull();
        reservation.setCreatedBy(createdBy);
        long expiryMinutes = depositExpiryMinutes > 0 ? depositExpiryMinutes : 1440;
        reservation.setDepositExpiresAt(new Date(System.currentTimeMillis() + expiryMinutes * 60_000L));
        String paymentCapabilityToken = deposit.amount().signum() > 0
                ? paymentCapabilityService.issue(reservation, createdBy) : null;

        Reservation saved = reservationRepository.save(reservation);
        for (ReservationPreorderItem item : preorderItems) {
            item.setReservation(saved);
            preorderItemRepository.save(item);
        }
        addHistory(saved, null, ReservationStatus.WAITING_TABLE_ASSIGNMENT,
                "Khách gửi yêu cầu đặt sảnh sự kiện - chờ nhà hàng bố trí");
        notificationService.createNotification(
                "EVENT_BOOKING_NEW", "Yêu cầu đặt sự kiện mới",
                saved.getReservationCode() + " - " + saved.getCustomerName()
                        + " (" + saved.getGuestCount() + " khách)",
                "ROLE_MANAGER", "info", "reservation", String.valueOf(saved.getId()));
        activityLogService.log("CREATE_EVENT", "Reservation", String.valueOf(saved.getId()),
                "Tạo yêu cầu sự kiện " + saved.getReservationCode());
        ReservationResponse response = toResponse(saved, false);
        realtimeService.publish("EVENT_BOOKING_CREATED", saved.getReservationCode(), null,
                saved.getReservationStatus(), "Khách gửi yêu cầu đặt sự kiện", response);
        response.setPaymentCapabilityToken(paymentCapabilityToken);
        return response;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, String idempotencyKey) {
        // NOTE: Luồng tạo chuẩn hóa đầu vào, chống gửi lặp tùy chọn, kiểm tra sức chứa và gán bàn
        // trước khi lưu; sự kiện realtime chỉ được phát sau khi bản ghi đã lưu thành công.
        NormalizedReservation normalized = normalizeAndValidate(request);
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        String requestFingerprint = fingerprint(request, normalized);
        if (normalizedIdempotencyKey != null) {
            requireIdempotencyLock("reservation:" + normalizedIdempotencyKey);
            Optional<Reservation> existing = reservationRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) {
                Reservation reservation = existing.get();
                if (!requestFingerprint.equals(reservation.getRequestFingerprint())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key đã được dùng cho yêu cầu khác");
                }
                return toResponse(reservation, false);
            }
        }

        restaurantCapacityService.requireCapacity(normalized.date(), normalized.time(),
                normalized.durationMinutes(), normalized.guestCount());
        if (request.getAreaId() != null) {
            TableArea requestedArea = areaRepository.findById(request.getAreaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khu vực"));
            areaReadinessService.requireBookingReady(requestedArea);
        }
        boolean largeParty = normalized.guestCount() >= restaurantSettingsService.largePartyThreshold();
        RestaurantTable table = largeParty ? null : autoTableAssignmentService.assign(
                request.getAreaId(), normalized.guestCount(), normalized.date(), normalized.time(), normalized.durationMinutes());
        List<RestaurantTable> assignedTables = table == null ? List.of() : List.of(table);

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
        if (table != null) setTableAssignments(reservation, assignedTables, table.getId());

        TableArea area = resolveArea(request.getAreaId(), table);
        if (area == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng chọn khu vực");
        }
        areaReadinessService.requireBookingReady(area);
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
                totalAmount, normalized.guestCount(), normalized.date(), normalized.time(), area.getId(), table, depositRate);
        BigDecimal payableNow = calculatePayableNow(totalAmount, paymentOption, deposit.amount());

        validateSafeText(request.getOrderNote(), "Ghi chú cho nhà bếp");
        reservation.setPreorderEnabled(Boolean.TRUE.equals(request.getPreorderEnabled()) && !preorderItems.isEmpty());
        reservation.setOrderNote(limit(trimToNull(request.getOrderNote()), 500));
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
        reservation.setReservationStatus(largeParty
                ? ReservationStatus.WAITING_TABLE_ASSIGNMENT : ReservationStatus.PENDING);
        reservation.setCreatedBy(currentUsernameOrNull());

        // P0-05: Set explicit expiry time for waiting/deposit-required reservations
        long expiryMinutes = depositExpiryMinutes > 0 ? depositExpiryMinutes : 1440;
        reservation.setDepositExpiresAt(new Date(System.currentTimeMillis() + expiryMinutes * 60_000L));

        String paymentCapabilityToken = null;
        if (payableNow.signum() > 0 && !PaymentOption.PAY_AT_RESTAURANT.equals(paymentOption)) {
            paymentCapabilityToken = paymentCapabilityService.issue(reservation, currentUsernameOrNull());
        }

        Reservation saved = reservationRepository.save(reservation);
        saveVoucherUsage(saved, voucherApplication, originalTotalAmount);
        for (ReservationPreorderItem item : preorderItems) {
            item.setReservation(saved);
            preorderItemRepository.save(item);
        }
        addHistory(saved, null, saved.getReservationStatus(), largeParty
                ? "Đoàn đông chờ nhà hàng bố trí bàn" : "Hệ thống tự động bố trí bàn phù hợp");
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
        realtimeService.publish("RESERVATION_CREATED", saved.getReservationCode(), null, saved.getReservationStatus(),
                "Khách gửi yêu cầu đặt bàn mới", response);
        response.setPaymentCapabilityToken(paymentCapabilityToken);
        return response;
    }

    @Transactional(readOnly = true)
    public PublicReservationResponse getPublicReservation(String code, String phone) {
        return lookupPublicReservation(code, phone);
    }

    /**
     * Tra cứu booking công khai: BẮT BUỘC có bookingCode + phone.
     * Phone xác thực phải khớp chính xác với database.
     * Không cho phép tra cứu bằng code đơn thuần hay phone/email đơn thuần.
     */
    @Transactional(readOnly = true)
    public PublicReservationResponse lookupPublicReservation(String code, String phone) {
        // Yêu cầu tối thiểu: bookingCode + phone hợp lệ
        String reservationCode = trimToNull(code);
        String customerPhone = normalizePhone(phone);

        if (reservationCode == null || reservationCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng nhập mã đặt bàn.");
        }
        if (customerPhone == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng nhập số điện thoại.");
        }
        if (!PHONE_PATTERN.matcher(customerPhone).matches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Số điện thoại không hợp lệ.");
        }

        // Tìm booking theo code
        Reservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy đặt bàn với mã này."));

        // Xác thực phone khớp chính xác
        if (!customerPhone.equals(normalizePhone(reservation.getCustomerPhone()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Mã đặt bàn hoặc số điện thoại không đúng.");
        }

        return PublicReservationResponse.from(toResponse(reservation, false));
    }

    /**
     * Compatibility overload for internal callers. Email is deliberately ignored:
     * public lookup requires the reservation code and matching phone only.
     */
    @Deprecated(forRemoval = false)
    public PublicReservationResponse lookupPublicReservation(String code, String phone, String ignoredEmail) {
        return lookupPublicReservation(code, phone);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAdminReservations() {
        return reservationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(r -> toResponse(r, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsForUser(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để xem lịch sử đặt bàn");
        }
        return reservationRepository.findByCreatedByOrderByCreatedAtDesc(username).stream()
                .map(reservation -> toResponse(reservation, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse getAdminReservation(Long id) {
        return toResponse(findReservation(id), true);
    }

    @Transactional(readOnly = true)
    public List<AvailableTableResponse> findAvailableTables(String date, String time, Integer durationMinutes,
                                                            Integer guestCount, Integer areaId,
                                                            Boolean lateDiningConfirmed) {
        // NOTE: Danh sách khả dụng được tính từ trạng thái vận hành, khu vực, sức chứa và lịch đặt thực tế.
        LocalDate reservationDate = parseDate(date);
        LocalTime arrivalTime = parseTime(time);
        int duration = durationMinutes == null || durationMinutes < 30 ? DEFAULT_DURATION_MINUTES : durationMinutes;
        int guests = guestCount == null || guestCount < 1 ? 1 : guestCount;
        validateReservationTime(reservationDate, arrivalTime, duration, Boolean.TRUE.equals(lateDiningConfirmed));
        if (areaId != null) {
            TableArea area = areaRepository.findById(areaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khu vực"));
            areaReadinessService.requireBookingReady(area);
        }

        return tableRepository.findOperationalTables().stream()
                .filter(t -> areaId == null || (t.getAreaId() != null && t.getAreaId().equals(areaId)))
                .filter(this::isTableInBookingReadyArea)
                .map(t -> toAvailableTable(t, reservationDate, arrivalTime, duration, guests))
                .sorted(Comparator.comparing((AvailableTableResponse t) -> availabilityRank(t.getAvailabilityStatus()))
                        .thenComparing(AvailableTableResponse::getFitScore))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReservationQuoteResponse quote(ReservationQuoteRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Dữ liệu báo giá không hợp lệ");
        }
        LocalDate quoteDate = parseDate(request.getReservationDate());
        LocalTime quoteTime = parseTime(request.getArrivalTime());
        int guests = request.getGuestCount() == null ? 1 : request.getGuestCount();
        int duration = request.getDurationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.getDurationMinutes();
        validateReservationTime(quoteDate, quoteTime, duration, false);
        restaurantCapacityService.requireCapacity(quoteDate, quoteTime, duration, guests);
        if (request.getAreaId() != null) {
            TableArea requestedArea = areaRepository.findById(request.getAreaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khu vực"));
            areaReadinessService.requireBookingReady(requestedArea);
        }
        boolean largeParty = guests >= restaurantSettingsService.largePartyThreshold();
        RestaurantTable table = largeParty ? null : autoTableAssignmentService.assign(
                request.getAreaId(), guests, quoteDate, quoteTime, duration);
        TableArea area = resolveArea(request.getAreaId(), table);
        if (area == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng chọn khu vực");
        }
        areaReadinessService.requireBookingReady(area);
        Price tablePrice = calculatePrice(table, area);
        List<ReservationPreorderItem> items = buildPreorderItems(request.getPreorderItems());
        BigDecimal foodAmount = items.stream()
                .map(ReservationPreorderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal originalTotal = tablePrice.total().add(foodAmount).setScale(0, RoundingMode.HALF_UP);
        VoucherApplication voucherApplication = applyVoucher(request.getVoucherCode(), originalTotal, false);
        BigDecimal total = voucherApplication.totalAfterDiscount();
        PaymentOption option = request.getPaymentOption() == null ? PaymentOption.DEPOSIT_50 : request.getPaymentOption();
        DepositPolicyService.DepositCalculation deposit = depositPolicyService.calculate(
                total, guests, quoteDate,
                quoteTime, area == null ? null : area.getId(), table, depositRate);
        BigDecimal payableNow = calculatePayableNow(total, option, deposit.amount());

        ReservationQuoteResponse response = new ReservationQuoteResponse();
        response.setProposedTableId(table == null ? null : table.getId());
        response.setProposedTableName(table == null ? null : table.getName());
        response.setRequiresManualAssignment(largeParty);
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

        List<TableSuggestionResponse> suggestions = tableRepository.findOperationalTables().stream()
                .filter(t -> request.getAreaId() == null || request.getAreaId().equals(t.getAreaId()))
                .filter(this::isTableInBookingReadyArea)
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

    @Transactional(readOnly = true)
    public TableCombinationResponse suggestTableCombo(TableSuggestionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu gợi ý ghép bàn không hợp lệ");
        }
        LocalDate date = parseDate(request.getReservationDate());
        LocalTime time = parseTime(request.getArrivalTime());
        int duration = request.getDurationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.getDurationMinutes();
        int guests = request.getGuestCount() == null ? 1 : Math.max(1, request.getGuestCount());
        List<RestaurantTable> available = tableRepository.findOperationalTables().stream()
                .filter(t -> t.getIsOccupied() == null || t.getIsOccupied() == 0)
                .filter(t -> request.getAreaId() == null || request.getAreaId().equals(t.getAreaId()))
                .filter(this::isTableInBookingReadyArea)
                .filter(t -> !hasConflict(t.getId(), date, time, duration, null))
                .toList();

        TableCombinationResponse response = new TableCombinationResponse();
        response.setTotalReservationPrice(BigDecimal.ZERO);
        if (available.stream().anyMatch(table -> maxCapacity(table) >= guests)) {
            response.setAvailable(true);
            response.setCombinationRequired(false);
            response.setTables(List.of());
            response.setTotalCapacity(0);
            response.setReasons(List.of("Đã có bàn đơn phù hợp, không cần ghép bàn."));
            return response;
        }

        Optional<List<RestaurantTable>> combination = tableCombinationPlanner.findBestCombination(available, guests);
        if (combination.isEmpty()) {
            response.setAvailable(false);
            response.setCombinationRequired(false);
            response.setTables(List.of());
            response.setTotalCapacity(0);
            response.setReasons(List.of("Không có tổ hợp tối đa 4 bàn đủ sức chứa trong khung giờ này."));
            return response;
        }
        List<RestaurantTable> selected = combination.get();
        int totalCapacity = selected.stream().mapToInt(this::maxCapacity).sum();
        response.setAvailable(true);
        response.setCombinationRequired(true);
        response.setTables(selected.stream().map(table -> toReservationTableResponse(table, table.equals(selected.getFirst()))).toList());
        response.setTotalCapacity(totalCapacity);
        response.setReasons(List.of("Ghép " + selected.size() + " bàn còn trống gần nhau.",
                "Tổng sức chứa " + totalCapacity + " chỗ cho nhóm " + guests + " khách."));
        return response;
    }

    @Transactional(readOnly = true)
    public AdminTableAssignmentOptions getAssignmentOptions(Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        Integer areaId = reservation.getArea() == null ? null : reservation.getArea().getId();
        List<RestaurantTable> available = tableRepository.findOperationalTables().stream()
                .filter(t -> t.getIsOccupied() == null || t.getIsOccupied() == 0)
                .filter(t -> areaId == null || areaId.equals(t.getAreaId()))
                .filter(this::isTableInBookingReadyArea)
                .filter(t -> !hasConflict(t.getId(), reservation.getReservationDate(), reservation.getArrivalTime(),
                        reservation.getExpectedDurationMinutes(), reservation.getId()))
                .sorted(Comparator.comparingInt(this::maxCapacity)
                        .thenComparing(t -> t.getDisplayOrder() == null ? Integer.MAX_VALUE : t.getDisplayOrder())
                        .thenComparing(RestaurantTable::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        List<List<Integer>> options = new ArrayList<>();
        available.stream().filter(t -> maxCapacity(t) >= reservation.getGuestCount()).findFirst()
                .ifPresent(t -> options.add(List.of(t.getId())));
        addCombinationOption(options, available, reservation.getGuestCount());
        if (!options.isEmpty() && options.getFirst().size() > 1) {
            for (Integer excluded : List.copyOf(options.getFirst())) {
                addCombinationOption(options, available.stream().filter(t -> !t.getId().equals(excluded)).toList(), reservation.getGuestCount());
                if (options.size() >= 3) break;
            }
        }
        return new AdminTableAssignmentOptions(reservationId, reservation.getGuestCount(), areaId,
                available.stream().map(t -> toReservationTableResponse(t, false)).toList(), options);
    }

    private void addCombinationOption(List<List<Integer>> options, List<RestaurantTable> candidates, int guests) {
        tableCombinationPlanner.findBestCombination(candidates, guests).ifPresent(tables -> {
            List<Integer> ids = tables.stream().map(RestaurantTable::getId).sorted().toList();
            if (!options.contains(ids)) options.add(ids);
        });
    }

    @Transactional
    public ReservationResponse confirm(Long id, ReservationActionRequest request) {
        // NOTE: Khi xác nhận, bàn được khóa và kiểm tra lại để tránh dữ liệu khả dụng cũ giữa hai yêu cầu đồng thời.
        // Trạng thái kế tiếp phụ thuộc nghĩa vụ cọc; realtime được phát sau khi trạng thái đã được lưu.
        Reservation reservation = findReservation(id);
        List<Integer> requestedTableIds = requestedTableIds(request, reservation);
        if (requestedTableIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Vui lòng chọn bàn hoặc phương án ghép bàn trước khi xác nhận");
        }
        Integer primaryTableId = request != null && request.getTableId() != null ? request.getTableId() : requestedTableIds.getFirst();
        boolean changingTables = !assignedTableIds(reservation).equals(new LinkedHashSet<>(requestedTableIds));
        if (changingTables) {
            List<RestaurantTable> assignedTables = lockAndValidateTables(requestedTableIds, reservation.getGuestCount(),
                    reservation.getReservationDate(), reservation.getArrivalTime(), reservation.getExpectedDurationMinutes(),
                    reservation.getId(), request == null ? null : request.getAreaId());
            RestaurantTable newTable = assignedTables.stream().filter(table -> table.getId().equals(primaryTableId)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bàn chính"));
            reservation.setTable(newTable);
            setTableAssignments(reservation, assignedTables, primaryTableId);
            TableArea area = resolveArea(request == null ? null : request.getAreaId(), newTable);
            areaReadinessService.requireBookingReady(area);
            reservation.setArea(area);
            Price price = calculatePrice(newTable, reservation.getArea());
            BigDecimal foodAmount = reservation.getFoodAmount() == null ? BigDecimal.ZERO : reservation.getFoodAmount();
            BigDecimal totalAmount = price.total().add(foodAmount).setScale(0, RoundingMode.HALF_UP);
            BigDecimal depositAmount = reservation.getDepositAmount() == null ? BigDecimal.ZERO : reservation.getDepositAmount();
            BigDecimal payableNow = calculatePayableNow(totalAmount, reservation.getPaymentOption(), depositAmount);
            reservation.setTableAmount(price.total());
            reservation.setTotalAmount(totalAmount);
            reservation.setDepositAmount(payableNow);
            reservation.setRemainingAmount(totalAmount.subtract(payableNow));
        } else {
            lockAndValidateTables(requestedTableIds, reservation.getGuestCount(), reservation.getReservationDate(),
                    reservation.getArrivalTime(), reservation.getExpectedDurationMinutes(), reservation.getId(),
                    request == null ? null : request.getAreaId());
        }

        ReservationStatus old = reservation.getReservationStatus();
        boolean depositRequired = reservation.getDepositAmount() != null
                && reservation.getDepositAmount().signum() > 0;
        boolean depositPaid = DepositStatus.PAID.equals(reservation.getDepositStatus())
                || (depositRequired && reservation.getPaidAmount() != null
                    && reservation.getPaidAmount().compareTo(reservation.getDepositAmount()) >= 0);
        ReservationStatus nextStatus = depositRequired && !depositPaid
                ? ReservationStatus.DEPOSIT_REQUIRED
                : ReservationStatus.CONFIRMED;
        stateMachine.assertCanTransition(old, nextStatus);
        reservation.setReservationStatus(nextStatus);
        if (depositRequired && !depositPaid) {
            reservation.setDepositStatus(DepositStatus.PENDING);
        } else if (!depositRequired) {
            reservation.setDepositStatus(DepositStatus.NOT_REQUIRED);
        }
        reservation.setConfirmedBy(currentUsername());
        reservation.setConfirmedAt(new Date());
        reservation.setManagerNote(trimToNull(request != null ? request.getNote() : null));
        reservation.setUpdatedAt(new Date());
        Reservation saved = reservationRepository.save(reservation);
        dispatchPreorderIfNeeded(saved);
        addHistory(saved, old, nextStatus, "Quản lý xác nhận đặt bàn");
        notifyReservation(saved, "RESERVATION_CONFIRMED", "Đặt bàn đã được xác nhận", "Yêu cầu " + saved.getReservationCode() + " đã được xác nhận.");
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_CONFIRMED", saved.getReservationCode(), old, nextStatus,
                "Đặt bàn đã được xác nhận", response);
        return response;
    }

    private void dispatchPreorderIfNeeded(Reservation reservation) {
        if (!Boolean.TRUE.equals(reservation.getPreorderEnabled()) || reservation.getKitchenOrderId() != null) {
            return;
        }
        List<ReservationPreorderItem> preorderItems = preorderItemRepository.findByReservationIdOrderByIdAsc(reservation.getId());
        if (preorderItems.isEmpty()) {
            return;
        }
        reservation.setKitchenOrderId(orderCheckoutService.dispatchReservationPreorder(reservation, preorderItems));
        reservationRepository.save(reservation);
        messagingTemplate.convertAndSend("/topic/kitchen", "NEW_ORDER");
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
        return cancelInternal(id, request, false);
    }

    @Transactional
    public ReservationResponse cancelApproved(Long id, ReservationActionRequest request) {
        return cancelInternal(id, request, true);
    }

    private ReservationResponse cancelInternal(Long id, ReservationActionRequest request, boolean approvedWorkflow) {
        Reservation reservation = findReservation(id);
        if (!approvedWorkflow && reservation.getPaidAmount() != null
                && reservation.getPaidAmount().signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đặt bàn đã thanh toán phải được xử lý qua quy trình yêu cầu hủy/hoàn cọc");
        }
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, ReservationStatus.CANCELLED);
        reservation.setReservationStatus(ReservationStatus.CANCELLED);
        reservation.setManagerNote(trimToNull(request != null ? request.getNote() : null));
        reservation.setUpdatedAt(new Date());
        if (!approvedWorkflow) {
            tableLifecycleService.releaseReservationTables(reservation);
        }
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
        BigDecimal paidAmount = reservation.getDepositAmount() == null ? BigDecimal.ZERO : reservation.getDepositAmount();
        BigDecimal totalAmount = reservation.getTotalAmount() == null ? BigDecimal.ZERO : reservation.getTotalAmount();
        reservation.setDepositStatus(DepositStatus.PAID);
        reservation.setReservationStatus(ReservationStatus.DEPOSIT_PAID);
        reservation.setPaidAmount(paidAmount);
        reservation.setRemainingAmount(totalAmount.subtract(paidAmount).max(BigDecimal.ZERO));
        reservation.setPaymentStatus(paidAmount.compareTo(totalAmount) >= 0
                ? PaymentStatus.PAID
                : PaymentStatus.PARTIALLY_PAID);
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
    public ReservationResponse updateContactStatus(Long id, ReservationContactUpdateRequest request) {
        Reservation reservation = findReservation(id);
        Date contactedAt = new Date();
        String contactedBy = currentUsername();
        reservation.setContactStatus(request.status());
        reservation.setContactCallNote(limit(trimToNull(request.note()), 1000));
        reservation.setContactCalledAt(contactedAt);
        reservation.setContactCalledBy(contactedBy);
        poly.edu.quanlynhahang.entity.ReservationContactLog contactLog =
                new poly.edu.quanlynhahang.entity.ReservationContactLog();
        contactLog.setReservation(reservation);
        contactLog.setStaffUsername(contactedBy);
        contactLog.setContactType("PHONE");
        contactLog.setResult(request.status());
        contactLog.setContactedAt(contactedAt);
        contactLog.setNote(limit(trimToNull(request.note()), 1000));
        contactLogRepository.save(contactLog);
        Reservation saved = reservationRepository.save(reservation);
        activityLogService.log("CONTACT_STATUS", "Reservation", String.valueOf(id),
                "Cập nhật trạng thái liên hệ " + request.status());
        return toResponse(saved, true);
    }

    @Transactional(readOnly = true)
    public List<ReservationContactLogResponse> getContactLogs(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặt bàn");
        }
        return contactLogRepository.findByReservationIdOrderByContactedAtDesc(id).stream()
                .map(logEntry -> new ReservationContactLogResponse(
                        logEntry.getId(), logEntry.getStaffUsername(), logEntry.getContactType(),
                        logEntry.getResult(), logEntry.getContactedAt(), logEntry.getNote()))
                .toList();
    }

    @Transactional
    public ReservationResponse checkIn(Long id, ReservationActionRequest request) {
        Reservation reservation = findReservation(id);
        ReservationStatus old = reservation.getReservationStatus();
        stateMachine.assertCanTransition(old, ReservationStatus.CHECKED_IN);
        reservation.setReservationStatus(ReservationStatus.CHECKED_IN);
        reservation.setUpdatedAt(new Date());
        for (RestaurantTable table : assignedTables(reservation)) {
            table.setIsOccupied(2);
            table.setReservedTime("Khách đã đến: " + reservation.getReservationCode());
            tableRepository.save(table);
        }
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
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now();
        long expiryMinutes = depositExpiryMinutes > 0 ? depositExpiryMinutes : 1440;
        Date depositDeadline = new Date(System.currentTimeMillis() - expiryMinutes * 60_000L);
        LocalDateTime noShowThreshold = now.minusMinutes(noShowGraceMinutes);
        List<Long> candidateIds = reservationRepository.findExpiryCandidateIds(
                new Date(),
                depositDeadline,
                noShowThreshold.toLocalDate(),
                noShowThreshold.toLocalTime(),
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.WAITING_TABLE_ASSIGNMENT,
                        ReservationStatus.DEPOSIT_REQUIRED, ReservationStatus.DEPOSIT_PENDING),
                EnumSet.of(ReservationStatus.PENDING, ReservationStatus.DEPOSIT_REQUIRED,
                        ReservationStatus.DEPOSIT_PENDING),
                EnumSet.of(ReservationStatus.CONFIRMED, ReservationStatus.DEPOSIT_PAID,
                        ReservationStatus.FULLY_PAID),
                PageRequest.of(0, 200));
        int total = 0, success = 0, fail = 0;

        for (Long reservationId : candidateIds) {
            try {
                expiryTransactionTemplate.executeWithoutResult(status -> {
                    Reservation reservation = reservationRepository.findById(reservationId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đặt bàn"));
                    processSingleExpiry(reservation, now, depositDeadline);
                });
                success++;
            } catch (Exception e) {
                fail++;
                log.error("Expire failed for reservation id {}: {}", reservationId, e.getMessage());
            }
            total++;
        }

        if (fail > 0) {
            log.warn("Expire scan: {}/{} succeeded, {} failed", success, total, fail);
        }
    }

    @Transactional
    public void processSingleExpiry(Reservation reservation, LocalDateTime now, Date depositDeadline) {
        ReservationStatus status = reservation.getReservationStatus();

        // P0-05: Check explicit expiry first
        if (reservation.getDepositExpiresAt() != null && reservation.getDepositExpiresAt().before(new Date())) {
            if (isWaitingStatus(status)) {
                transitionSystem(reservation, ReservationStatus.EXPIRED, "Quá hạn chờ bố trí bàn");
                return;
            }
        }

        // Legacy check using created_at
        if (isDepositExpired(status, reservation, depositDeadline)) {
            transitionSystem(reservation, ReservationStatus.EXPIRED, "Quá hạn thanh toán đặt cọc");
            return;
        }
        if (isNoShow(status, reservation, now)) {
            transitionSystem(reservation, ReservationStatus.NO_SHOW, "Khách không đến sau thời gian giữ bàn");
        }
    }

    /** P0-05: Check if status is a waiting/pending status that should expire */
    private boolean isWaitingStatus(ReservationStatus status) {
        return EnumSet.of(
            ReservationStatus.PENDING,
            ReservationStatus.WAITING_TABLE_ASSIGNMENT,
            ReservationStatus.DEPOSIT_REQUIRED,
            ReservationStatus.DEPOSIT_PENDING
        ).contains(status);
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
        if (nextStatus == ReservationStatus.NO_SHOW) {
            if (reservation.getDepositStatus() == DepositStatus.PAID) {
                BigDecimal forfeitedAmount = depositPolicyService.calculateNoShowForfeiture(reservation);
                reservation.setDepositStatus(DepositStatus.FORFEITED);
                note = note + ". Tiền cọc bị giữ lại: " + forfeitedAmount.toPlainString() + " VND";
            }
            tableLifecycleService.releaseReservationTables(reservation);
        }
        Reservation saved = reservationRepository.save(reservation);
        addHistory(saved, old, nextStatus, note);
        ReservationResponse response = toResponse(saved, true);
        realtimeService.publish("RESERVATION_" + nextStatus.name(), saved.getReservationCode(), old, nextStatus, note, response);
        activityLogService.log("UPDATE", "Reservation", String.valueOf(saved.getId()), note + " " + saved.getReservationCode());
    }

    private NormalizedReservation normalizeAndValidate(ReservationRequest request) {
        // NOTE: Backend là nguồn tin cậy cuối cùng: chuẩn hóa và xác thực lại toàn bộ dữ liệu khách gửi lên.
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đặt bàn không hợp lệ");
        String name = trimToNull(request.getCustomerName());
        String phone = normalizePhone(request.getCustomerPhone());
        String email = trimToNull(request.getCustomerEmail());
        LocalDate date = parseDate(request.getReservationDate());
        LocalTime time = parseTime(request.getArrivalTime());
        int duration = request.getExpectedDurationMinutes() == null ? DEFAULT_DURATION_MINUTES : request.getExpectedDurationMinutes();
        Integer tableId = request.getTableId();
        List<Integer> tableIds = request.getTableIds() == null || request.getTableIds().isEmpty()
                ? (tableId == null ? List.of() : List.of(tableId))
                : new ArrayList<>(request.getTableIds());
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
        if (tableId != null && !tableIds.contains(tableId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Bàn chính phải nằm trong danh sách bàn ghép");
        }
        if (tableIds.size() > MAX_COMBINED_TABLES || new LinkedHashSet<>(tableIds).size() != tableIds.size()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh sách bàn ghép không hợp lệ");
        }
        validateReservationTime(date, time, duration, Boolean.TRUE.equals(request.getLateDiningConfirmed()));
        return new NormalizedReservation(name, phone, email, date, time, duration, guests, tableId, List.copyOf(tableIds));
    }

    private void validateReservationTime(LocalDate date, LocalTime time, int duration, boolean lateDiningConfirmed) {
        // NOTE: Kiểm tra thời điểm quá khứ, thời gian đặt trước và giờ hoạt động trước khi giữ chỗ.
        LocalDateTime arrival = LocalDateTime.of(date, time);
        // P0: Check quá khứ tuyệt đối — không thể đặt bàn vào thời gian đã qua
        if (arrival.isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không thể đặt bàn vào thời gian đã qua. Vui lòng chọn thời gian trong tương lai.");
        }
        if (arrival.isBefore(LocalDateTime.now().plusMinutes(MIN_ADVANCE_MINUTES))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cần đặt bàn trước ít nhất 30 phút");
        }
        if (!businessHoursService.isOpen(time)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Thời gian đặt bàn nằm ngoài giờ hoạt động " + businessHoursService.getFormattedHours());
        }
        if (time.plusMinutes(duration).isAfter(businessHoursService.getClosingTime()) && !lateDiningConfirmed) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng xác nhận dùng bữa sau giờ phục vụ trước khi đặt bàn");
        }
    }

    private boolean hasConflict(Integer tableId, LocalDate date, LocalTime start, int duration, Long currentReservationId) {
        // NOTE: Hai khoảng thời gian trùng khi requestedStart < otherEnd và requestedEnd > otherStart;
        // cả hai khoảng đều cộng thời gian dọn bàn để bảo vệ khoảng chuyển ca phục vụ.
        LocalDateTime requestedStart = LocalDateTime.of(date, start);
        LocalDateTime requestedEnd = requestedStart.plusMinutes(duration + CLEANUP_MINUTES);
        List<Reservation> reservations = new ArrayList<>(
                reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                        date, tableId, BLOCKING_STATUSES));
        reservations.addAll(reservationRepository.findLockedByReservationDateAndTableIdAndReservationStatusIn(
                date.minusDays(1), tableId, BLOCKING_STATUSES));
        return reservations.stream()
                .filter(r -> currentReservationId == null || !r.getId().equals(currentReservationId))
                .anyMatch(r -> {
                    LocalDateTime otherStart = LocalDateTime.of(r.getReservationDate(), r.getArrivalTime());
                    LocalDateTime otherEnd = otherStart.plusMinutes(r.getExpectedDurationMinutes() + CLEANUP_MINUTES);
                    return requestedStart.isBefore(otherEnd) && requestedEnd.isAfter(otherStart);
                });
    }

    private void validateTableAvailable(RestaurantTable table) {
        if (Boolean.FALSE.equals(table.getActive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn đang tạm ngưng");
        }
        if (table.getIsOccupied() != null && table.getIsOccupied() != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bàn không ở trạng thái trống");
        }
    }

    private List<RestaurantTable> lockAndValidateTables(List<Integer> requestedIds, int guestCount,
                                                          LocalDate date, LocalTime time, int duration,
                                                          Long currentReservationId, Integer requiredAreaId) {
        // NOTE: Khóa bi quan các bàn theo thứ tự ID rồi kiểm tra lại tồn tại, khu vực, xung đột và sức chứa.
        if (requestedIds == null || requestedIds.isEmpty() || requestedIds.size() > MAX_COMBINED_TABLES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cần chọn từ 1 đến 4 bàn");
        }
        LinkedHashSet<Integer> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Danh sách bàn ghép bị trùng");
        }
        List<Integer> orderedIds = uniqueIds.stream().sorted().toList();
        List<RestaurantTable> locked = tableRepository.findLockedByIdIn(orderedIds);
        if (locked.size() != orderedIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Có bàn không tồn tại");
        }
        for (RestaurantTable table : locked) {
            validateTableAvailable(table);
            if (requiredAreaId != null && !requiredAreaId.equals(table.getAreaId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mọi bàn ghép phải thuộc khu vực đã chọn");
            }
            if (hasConflict(table.getId(), date, time, duration, currentReservationId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Một trong các bàn ghép đã có lượt đặt trùng khung giờ");
            }
        }
        int totalCapacity = locked.stream().mapToInt(this::maxCapacity).sum();
        if (totalCapacity < guestCount) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Tổng sức chứa các bàn chưa đủ cho số lượng khách");
        }
        TableArea area = resolveArea(requiredAreaId, locked.getFirst());
        areaReadinessService.requireBookingReady(area);
        return locked;
    }

    private void setTableAssignments(Reservation reservation, List<RestaurantTable> tables, Integer primaryTableId) {
        reservation.getTableAssignments().clear();
        for (RestaurantTable table : tables) {
            ReservationTableAssignment assignment = new ReservationTableAssignment();
            assignment.setReservation(reservation);
            assignment.setTable(table);
            assignment.setPrimary(table.getId().equals(primaryTableId));
            reservation.getTableAssignments().add(assignment);
        }
    }

    private List<RestaurantTable> assignedTables(Reservation reservation) {
        if (reservation.getTableAssignments() != null && !reservation.getTableAssignments().isEmpty()) {
            return reservation.getTableAssignments().stream()
                    .map(ReservationTableAssignment::getTable)
                    .sorted(Comparator.comparing(RestaurantTable::getId))
                    .toList();
        }
        return reservation.getTable() == null ? List.of() : List.of(reservation.getTable());
    }

    private Set<Integer> assignedTableIds(Reservation reservation) {
        return assignedTables(reservation).stream().map(RestaurantTable::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Integer> requestedTableIds(ReservationActionRequest request, Reservation reservation) {
        if (request != null && request.getTableIds() != null && !request.getTableIds().isEmpty()) {
            List<Integer> ids = new ArrayList<>(request.getTableIds());
            if (request.getTableId() != null && !ids.contains(request.getTableId())) ids.addFirst(request.getTableId());
            return ids;
        }
        if (request != null && request.getTableId() != null) return List.of(request.getTableId());
        return new ArrayList<>(assignedTableIds(reservation));
    }

    private int maxCapacity(RestaurantTable table) {
        return table.getMaxCapacity() != null ? table.getMaxCapacity()
                : (table.getCapacity() == null ? 0 : table.getCapacity());
    }

    private Price calculatePrice(RestaurantTable table, TableArea area) {
        // Normal dining areas are free. VIP/private rooms use the separately
        // managed AreaPricing room fee; legacy area.basePrice is intentionally ignored.
        BigDecimal roomFee = area != null
                && AreaType.PRIVATE_ROOM.equals(area.getAreaType())
                && area.getPricing() != null
                && Boolean.TRUE.equals(area.getPricing().getActive())
                && area.getPricing().getRoomFee() != null
                ? area.getPricing().getRoomFee()
                : BigDecimal.ZERO;
        return new Price(roomFee, BigDecimal.ZERO, BigDecimal.ZERO);
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
        Voucher voucher = (markAsUsed
                ? voucherRepository.findLockedByCode(code)
                : voucherRepository.findByCode(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Mã giảm giá không tồn tại"));
        String currentUsername = currentUsernameOrNull();
        voucherLifecycleService.validateForUse(voucher, currentUsername);
        BigDecimal discount = originalTotal
                .multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .min(originalTotal)
                .max(BigDecimal.ZERO);
        BigDecimal totalAfterDiscount = originalTotal.subtract(discount).setScale(0, RoundingMode.HALF_UP);
        if (markAsUsed) {
            voucherLifecycleService.redeemLocked(voucher, currentUsername);
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
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy món ăn"));
        if (Boolean.FALSE.equals(product.getStatus()) || Boolean.FALSE.equals(product.getAvailable())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Món ăn không còn khả dụng để đặt trước");
        }
        BigDecimal unitPrice = (product.getPrice() == null ? BigDecimal.ZERO : product.getPrice())
                .setScale(0, RoundingMode.HALF_UP);
        ReservationPreorderItem item = new ReservationPreorderItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getImage());
        item.setCategoryName(product.getCategory() == null ? null : product.getCategory().getName());
        item.setUnitPrice(unitPrice);
        item.setQuantity(request.getQuantity());
        item.setNote(null);
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
        response.setKitchenOrderId(reservation.getKitchenOrderId());
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
        response.setOrderNote(reservation.getOrderNote());
        response.setReservationStatus(reservation.getReservationStatus());
        response.setOriginalTotalAmount(reservation.getTotalAmount());
        response.setDiscountAmount(BigDecimal.ZERO);
        response.setTotalAmount(reservation.getTotalAmount());
        response.setTableAmount(reservation.getTableAmount());
        response.setFoodAmount(reservation.getFoodAmount());
        response.setDepositRate(reservation.getDepositRate());
        response.setDepositAmount(reservation.getDepositAmount());
        response.setPaidAmount(reservation.getPaidAmount());
        response.setAmountDueNow(calculateAmountDueNow(reservation));
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
            response.setReceiptEmailStatus(reservation.getReceiptEmailStatus());
            response.setReceiptEmailSentAt(reservation.getReceiptEmailSentAt());
            response.setReceiptEmailError(reservation.getReceiptEmailError());
            response.setContactStatus(reservation.getContactStatus());
            response.setContactCallNote(reservation.getContactCallNote());
            response.setContactCalledAt(reservation.getContactCalledAt());
            response.setContactCalledBy(reservation.getContactCalledBy());
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
        response.setTables(assignedTables(reservation).stream()
                .map(table -> toReservationTableResponse(table, reservation.getTable() != null
                        && table.getId().equals(reservation.getTable().getId())))
                .toList());
        if (includeInternal) {
            response.setManagerNote(reservation.getManagerNote());
            response.setHistory(historyRepository.findByReservationIdOrderByChangedAtAsc(reservation.getId()).stream()
                    .map(h -> h.getChangedAt() + " - " + h.getNewStatus() + " - " + (h.getNote() == null ? "" : h.getNote()))
                    .toList());
        }
        return response;
    }

    private BigDecimal calculateAmountDueNow(Reservation reservation) {
        BigDecimal paid = reservation.getPaidAmount() == null
                ? BigDecimal.ZERO
                : reservation.getPaidAmount();
        PaymentOption option = reservation.getPaymentOption();
        if (option == null || PaymentOption.PAY_AT_RESTAURANT.equals(option)) {
            return BigDecimal.ZERO;
        }
        BigDecimal required = PaymentOption.FULL.equals(option)
                ? reservation.getTotalAmount()
                : reservation.getDepositAmount();
        if (required == null) {
            return BigDecimal.ZERO;
        }
        return required.subtract(paid).max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    private ReservationTableResponse toReservationTableResponse(RestaurantTable table, boolean primary) {
        ReservationTableResponse response = new ReservationTableResponse();
        response.setTableId(table.getId());
        response.setTableName(table.getName());
        response.setFloor(table.getFloor());
        response.setCapacity(maxCapacity(table));
        response.setImageUrl(table.getImageUrl());
        response.setPrimary(primary);
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
        Integer resolvedId = areaId != null ? areaId : (table == null ? null : table.getAreaId());
        if (resolvedId == null) return null;
        return areaRepository.findById(resolvedId).orElse(null);
    }

    private boolean isTableInBookingReadyArea(RestaurantTable table) {
        if (table == null || table.getAreaId() == null) return false;
        return areaRepository.findById(table.getAreaId())
                .map(area -> areaReadinessService.evaluate(area).bookingReady())
                .orElse(false);
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
        String code;
        do {
            code = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
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

    private void requireIdempotencyLock(String resource) {
        int result = applicationLockService.acquireExclusive(resource, 10_000);
        if (result < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Yêu cầu trùng đang được xử lý, vui lòng thử lại");
        }
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
                normalized.tableIds().stream().map(String::valueOf).sorted().reduce((a, b) -> a + "," + b).orElse(""),
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

    private String eventFingerprint(EventBookingRequest request) {
        String preorder = request.preorderItems() == null ? "" : request.preorderItems().stream()
                .map(item -> item.getProductId() + ":" + item.getQuantity() + ":" + trimToNull(item.getNote()))
                .sorted()
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
        String source = String.join("|",
                request.customerName().trim(),
                String.valueOf(normalizePhone(request.customerPhone())),
                String.valueOf(trimToNull(request.customerEmail())),
                String.valueOf(request.areaId()),
                String.valueOf(request.eventType()),
                request.reservationDate(),
                request.arrivalTime(),
                String.valueOf(request.durationHours()),
                String.valueOf(request.guestCount()),
                String.valueOf(Boolean.TRUE.equals(request.decorationRequired())),
                String.valueOf(Boolean.TRUE.equals(request.mcRequired())),
                String.valueOf(trimToNull(request.eventNote())),
                String.valueOf(Boolean.TRUE.equals(request.preorderEnabled())),
                preorder);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo fingerprint đặt sự kiện", e);
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
                                         int guestCount, Integer tableId, List<Integer> tableIds) {
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
