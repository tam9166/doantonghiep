package poly.edu.quanlynhahang.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.ReservationActionRequest;
import poly.edu.quanlynhahang.dto.ReservationContactUpdateRequest;
import poly.edu.quanlynhahang.dto.ReservationLookupRequest;
import poly.edu.quanlynhahang.dto.ReservationQuoteRequest;
import poly.edu.quanlynhahang.dto.ReservationRequest;
import poly.edu.quanlynhahang.dto.TableSuggestionRequest;
import poly.edu.quanlynhahang.dto.EventBookingRequest;
import poly.edu.quanlynhahang.service.ReservationService;
@RestController
public class ReservationController {
    private final ReservationService reservationService;
    private final poly.edu.quanlynhahang.service.ReservationReceiptService receiptService;

    public ReservationController(ReservationService reservationService,
                                 poly.edu.quanlynhahang.service.ReservationReceiptService receiptService) {
        this.reservationService = reservationService;
        this.receiptService = receiptService;
    }

    @PostMapping("/api/reservations")
    public ResponseEntity<?> create(@Valid @RequestBody ReservationRequest request,
                                    @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request, idempotencyKey));
    }
    @PostMapping("/api/event-bookings")
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventBookingRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createEventBooking(request)); }

    @PostMapping("/api/reservations/quote")
    public ResponseEntity<?> quote(@Valid @RequestBody ReservationQuoteRequest request) {
        return ResponseEntity.ok(reservationService.quote(request));
    }

    @PostMapping("/api/reservations/table-suggestions")
    public ResponseEntity<?> tableSuggestions(@Valid @RequestBody TableSuggestionRequest request) {
        return ResponseEntity.ok(reservationService.suggestTables(request));
    }

    @PostMapping("/api/reservations/table-combinations")
    public ResponseEntity<?> tableCombinations(@Valid @RequestBody TableSuggestionRequest request) {
        return ResponseEntity.ok(reservationService.suggestTableCombo(request));
    }

    /**
     * Tra cứu booking công khai: BẮT BUỘC code + phone trong body POST.
     * Không cho phép tra cứu bằng code đơn thuần hoặc phone/email đơn thuần.
     */
    @PostMapping("/api/reservations/lookup")
    public ResponseEntity<?> lookupPublic(@Valid @RequestBody ReservationLookupRequest request) {
        return ResponseEntity.ok(reservationService.lookupPublicReservation(
                request.reservationCode(), request.customerPhone()));
    }

    @GetMapping("/api/admin/reservations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAdminList() {
        return ResponseEntity.ok(reservationService.getAdminReservations());
    }

    @GetMapping("/api/reservations/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyHistory() {
        return ResponseEntity.ok(reservationService.getReservationsForUser(
                SecurityContextHolder.getContext().getAuthentication().getName()));
    }

    @GetMapping("/api/admin/reservations/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAdminDetail(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getAdminReservation(id));
    }

    @GetMapping("/api/admin/reservations/{id}/assignment-options")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> assignmentOptions(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getAssignmentOptions(id));
    }

    @PatchMapping("/api/admin/reservations/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> confirm(@PathVariable Long id, @Valid @RequestBody(required = false) ReservationActionRequest request) {
        return ResponseEntity.ok(reservationService.confirm(id, request));
    }

    @PatchMapping("/api/admin/reservations/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> reject(@PathVariable Long id, @Valid @RequestBody ReservationActionRequest request) {
        return ResponseEntity.ok(reservationService.reject(id, request));
    }

    @PatchMapping("/api/admin/reservations/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) ReservationActionRequest request) {
        return ResponseEntity.ok(reservationService.cancel(id, request));
    }

    @PatchMapping("/api/admin/reservations/{id}/deposit")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> markDepositPaid(@PathVariable Long id, @Valid @RequestBody(required = false) ReservationActionRequest request) {
        return ResponseEntity.ok(reservationService.markDepositPaid(id, request));
    }

    @PostMapping("/api/admin/reservations/{id}/resend-receipt")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> resendReceipt(@PathVariable Long id) {
        receiptService.resend(id);
        return ResponseEntity.accepted().body(java.util.Map.of("message", "Đã xếp lịch gửi lại biên nhận"));
    }

    @PatchMapping("/api/admin/reservations/{id}/contact-status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> updateContactStatus(@PathVariable Long id,
                                                  @Valid @RequestBody ReservationContactUpdateRequest request) {
        return ResponseEntity.ok(reservationService.updateContactStatus(id, request));
    }

    @PatchMapping("/api/admin/reservations/{id}/check-in")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> checkIn(@PathVariable Long id, @Valid @RequestBody(required = false) ReservationActionRequest request) {
        return ResponseEntity.ok(reservationService.checkIn(id, request));
    }

    @GetMapping("/api/tables/available")
    public ResponseEntity<?> available(@RequestParam String date,
                                       @RequestParam String time,
                                       @RequestParam(required = false) Integer durationMinutes,
                                       @RequestParam Integer guestCount,
                                       @RequestParam(required = false) Integer areaId) {
        return ResponseEntity.ok(reservationService.findAvailableTables(date, time, durationMinutes, guestCount, areaId));
    }
}
