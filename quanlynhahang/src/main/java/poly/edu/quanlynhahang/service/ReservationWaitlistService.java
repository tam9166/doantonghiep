package poly.edu.quanlynhahang.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.WaitlistActionRequest;
import poly.edu.quanlynhahang.dto.WaitlistRequest;
import poly.edu.quanlynhahang.dto.WaitlistResponse;
import poly.edu.quanlynhahang.entity.ReservationWaitlist;
import poly.edu.quanlynhahang.entity.Reservation;
import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.entity.WaitlistStatus;
import poly.edu.quanlynhahang.repository.ReservationWaitlistRepository;
import poly.edu.quanlynhahang.repository.ReservationRepository;
import poly.edu.quanlynhahang.repository.TableAreaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReservationWaitlistService {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_TEXT_PATTERN = Pattern.compile("(?i)<\\s*script|javascript:|onerror\\s*=|onload\\s*=");
    private final ReservationWaitlistRepository waitlistRepository;
    private final ReservationRepository reservationRepository;
    private final TableAreaRepository areaRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final RestaurantBusinessHoursService businessHoursService;

    public ReservationWaitlistService(ReservationWaitlistRepository waitlistRepository,
                                      ReservationRepository reservationRepository,
                                      TableAreaRepository areaRepository,
                                      NotificationService notificationService,
                                      ActivityLogService activityLogService,
                                      RestaurantBusinessHoursService businessHoursService) {
        this.waitlistRepository = waitlistRepository;
        this.reservationRepository = reservationRepository;
        this.areaRepository = areaRepository;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.businessHoursService = businessHoursService;
    }

    @Transactional
    public WaitlistResponse create(WaitlistRequest request) {
        // NOTE: Yêu cầu chờ được chuẩn hóa, lưu trạng thái WAITING rồi mới thông báo cho quản lý.
        NormalizedWaitlist normalized = normalize(request);
        ReservationWaitlist entry = new ReservationWaitlist();
        entry.setWaitlistCode(generateCode(normalized.date()));
        entry.setCustomerName(normalized.customerName());
        entry.setCustomerPhone(normalized.customerPhone());
        entry.setCustomerEmail(normalized.customerEmail());
        entry.setReservationDate(normalized.date());
        entry.setPreferredStartTime(normalized.startTime());
        entry.setPreferredEndTime(normalized.endTime());
        entry.setGuestCount(normalized.guestCount());
        entry.setArea(normalized.area());
        entry.setSeatingPreference(trimToNull(request.getSeatingPreference()));
        entry.setSpecialRequest(limit(trimToNull(request.getSpecialRequest()), 500));
        entry.setOverflowReason("GROUP_TOO_LARGE".equals(request.getOverflowReason()) ? "GROUP_TOO_LARGE" : null);
        entry.setStatus(WaitlistStatus.WAITING);

        ReservationWaitlist saved = waitlistRepository.save(entry);
        notificationService.createNotification(
                "WAITLIST_NEW",
                "Khách vào danh sách chờ",
                saved.getWaitlistCode() + " - " + saved.getCustomerName() + " (" + saved.getGuestCount() + " khách)",
                "ROLE_MANAGER",
                "warning",
                "reservation-waitlist",
                String.valueOf(saved.getId()));
        activityLogService.log("CREATE", "ReservationWaitlist", String.valueOf(saved.getId()),
                "Tạo yêu cầu danh sách chờ " + saved.getWaitlistCode());
        return toResponse(saved, false);
    }

    @Transactional(readOnly = true)
    public WaitlistResponse getPublic(String code, String phone) {
        String normalizedPhone = normalizePhone(required(phone, "Vui lòng nhập số điện thoại"));
        ReservationWaitlist entry = waitlistRepository.findByWaitlistCodeAndCustomerPhone(code, normalizedPhone)
                .orElseThrow(this::notFound);
        return toResponse(entry, false);
    }

    @Transactional(readOnly = true)
    public List<WaitlistResponse> getAdminList() {
        return waitlistRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(entry -> toResponse(entry, true))
                .toList();
    }

    @Transactional
    public WaitlistResponse contact(Long id, WaitlistActionRequest request) {
        // NOTE: Chỉ yêu cầu đang chờ hoặc đã liên hệ mới được cập nhật lần liên hệ tiếp theo.
        ReservationWaitlist entry = find(id);
        ensureWaitingOrContacted(entry);
        entry.setStatus(WaitlistStatus.CONTACTED);
        entry.setContactedAt(new Date());
        entry.setManagerNote(limit(trimToNull(request == null ? null : request.getNote()), 500));
        entry.setUpdatedAt(new Date());
        activityLogService.log("UPDATE", "ReservationWaitlist", String.valueOf(entry.getId()),
                "Liên hệ danh sách chờ " + entry.getWaitlistCode());
        return toResponse(waitlistRepository.save(entry), true);
    }

    @Transactional
    public WaitlistResponse convert(Long id, WaitlistActionRequest request) {
        // NOTE: Chuyển đổi chỉ liên kết với đặt bàn đã tồn tại và khớp khách, ngày, số người, khu vực.
        ReservationWaitlist entry = find(id);
        ensureWaitingOrContacted(entry);
        String reservationCode = limit(trimToNull(
                request == null ? null : request.getLinkedReservationCode()), 30);
        if (reservationCode == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng nhập mã đặt bàn đã tạo cho khách");
        }
        Reservation reservation = reservationRepository.findLockedByReservationCode(reservationCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy mã đặt bàn liên kết"));
        validateLinkedReservation(entry, reservation);
        waitlistRepository.findByLinkedReservationCode(reservationCode)
                .filter(existing -> !existing.getId().equals(entry.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Đặt bàn này đã được liên kết với một yêu cầu chờ khác");
                });
        entry.setStatus(WaitlistStatus.CONVERTED);
        entry.setLinkedReservationCode(reservationCode);
        entry.setManagerNote(limit(trimToNull(request == null ? null : request.getNote()), 500));
        entry.setUpdatedAt(new Date());
        activityLogService.log("UPDATE", "ReservationWaitlist", String.valueOf(entry.getId()),
                "Chuyển danh sách chờ thành đặt bàn " + entry.getWaitlistCode());
        return toResponse(waitlistRepository.save(entry), true);
    }

    private void validateLinkedReservation(ReservationWaitlist entry, Reservation reservation) {
        boolean sameCustomer = entry.getCustomerPhone().equals(reservation.getCustomerPhone());
        boolean sameDate = entry.getReservationDate().equals(reservation.getReservationDate());
        boolean sameGuests = entry.getGuestCount().equals(reservation.getGuestCount());
        boolean sameArea = entry.getArea() == null
                || (reservation.getArea() != null && entry.getArea().getId().equals(reservation.getArea().getId()));
        if (!sameCustomer || !sameDate || !sameGuests || !sameArea) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Đặt bàn không khớp khách, ngày, số người hoặc khu vực của yêu cầu chờ");
        }
    }

    @Transactional
    public WaitlistResponse cancel(Long id, WaitlistActionRequest request) {
        // NOTE: Hủy giữ nguyên bản ghi và ghi chú quản lý để bảo toàn dấu vết nghiệp vụ.
        ReservationWaitlist entry = find(id);
        ensureWaitingOrContacted(entry);
        entry.setStatus(WaitlistStatus.CANCELLED);
        entry.setManagerNote(limit(trimToNull(request == null ? null : request.getNote()), 500));
        entry.setUpdatedAt(new Date());
        activityLogService.log("UPDATE", "ReservationWaitlist", String.valueOf(entry.getId()),
                "Hủy danh sách chờ " + entry.getWaitlistCode());
        return toResponse(waitlistRepository.save(entry), true);
    }

    private ReservationWaitlist find(Long id) {
        return waitlistRepository.findLockedById(id).orElseThrow(() -> notFound());
    }

    private void ensureWaitingOrContacted(ReservationWaitlist entry) {
        if (entry.getStatus() != WaitlistStatus.WAITING && entry.getStatus() != WaitlistStatus.CONTACTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yêu cầu danh sách chờ đã kết thúc");
        }
    }

    private NormalizedWaitlist normalize(WaitlistRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Dữ liệu danh sách chờ không hợp lệ");
        }
        String name = limit(required(request.getCustomerName(), "Vui lòng nhập họ tên"), 150);
        String phone = normalizePhone(required(request.getCustomerPhone(), "Vui lòng nhập số điện thoại"));
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số điện thoại Việt Nam không hợp lệ");
        }
        String email = trimToNull(request.getCustomerEmail());
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Email không hợp lệ");
        }
        rejectUnsafe(name, request.getSpecialRequest(), request.getSeatingPreference());
        LocalDate date = parseDate(request.getReservationDate());
        LocalTime start = parseTime(request.getPreferredStartTime());
        LocalTime end = request.getPreferredEndTime() == null || request.getPreferredEndTime().isBlank()
                ? start.plusHours(2)
                : parseTime(request.getPreferredEndTime());
        if (date.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày đặt bàn không được ở quá khứ");
        }
        if (!businessHoursService.isServiceWindow(start, end)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Khung giờ chờ phải nằm trong giờ mở cửa " + businessHoursService.getFormattedHours());
        }
        int guests = request.getGuestCount() == null ? 0 : request.getGuestCount();
        if (guests < 1 || guests > 30) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Số khách không hợp lệ");
        }
        TableArea area = null;
        if (request.getAreaId() != null) {
            area = areaRepository.findById(request.getAreaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khu vực"));
        }
        return new NormalizedWaitlist(name, phone, email, date, start, end, guests, area);
    }

    private String generateCode(LocalDate date) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = "WL" + date.toString().replace("-", "") + "-"
                    + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
            if (waitlistRepository.findByWaitlistCode(code).isEmpty()) return code;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể cấp mã danh sách chờ, vui lòng thử lại");
    }

    private WaitlistResponse toResponse(ReservationWaitlist entry, boolean includePrivate) {
        WaitlistResponse response = new WaitlistResponse();
        response.setId(entry.getId());
        response.setWaitlistCode(entry.getWaitlistCode());
        response.setCustomerName(entry.getCustomerName());
        response.setCustomerPhone(includePrivate ? entry.getCustomerPhone() : maskPhone(entry.getCustomerPhone()));
        response.setCustomerEmail(includePrivate ? entry.getCustomerEmail() : null);
        response.setReservationDate(entry.getReservationDate().toString());
        response.setPreferredStartTime(entry.getPreferredStartTime().toString());
        response.setPreferredEndTime(entry.getPreferredEndTime().toString());
        response.setGuestCount(entry.getGuestCount());
        response.setAreaId(entry.getArea() == null ? null : entry.getArea().getId());
        response.setAreaName(entry.getArea() == null ? null : entry.getArea().getNameVi());
        response.setSeatingPreference(entry.getSeatingPreference());
        response.setSpecialRequest(entry.getSpecialRequest());
        response.setStatus(entry.getStatus());
        response.setLinkedReservationCode(entry.getLinkedReservationCode());
        response.setManagerNote(includePrivate ? entry.getManagerNote() : null);
        response.setContactedAt(entry.getContactedAt());
        response.setCreatedAt(entry.getCreatedAt());
        response.setUpdatedAt(entry.getUpdatedAt());
        return response;
    }

    private String required(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return trimmed;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(required(value, "Vui lòng chọn ngày"));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ngày đặt bàn không hợp lệ");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(required(value, "Vui lòng chọn giờ"));
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Giờ đặt bàn không hợp lệ");
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("\\s+", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void rejectUnsafe(String... values) {
        for (String value : values) {
            if (value != null && UNSAFE_TEXT_PATTERN.matcher(value).find()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Nội dung không hợp lệ");
            }
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return phone.substring(0, Math.min(4, phone.length())) + "****" + phone.substring(phone.length() - 2);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu danh sách chờ");
    }

    private record NormalizedWaitlist(String customerName,
                                      String customerPhone,
                                      String customerEmail,
                                      LocalDate date,
                                      LocalTime startTime,
                                      LocalTime endTime,
                                      Integer guestCount,
                                      TableArea area) {
    }
}
