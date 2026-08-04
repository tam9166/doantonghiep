package poly.edu.quanlynhahang.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.WaitlistActionRequest;
import poly.edu.quanlynhahang.dto.WaitlistRequest;
import poly.edu.quanlynhahang.service.ReservationWaitlistService;

@RestController
public class ReservationWaitlistController {
    private final ReservationWaitlistService waitlistService;

    public ReservationWaitlistController(ReservationWaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping("/api/reservation-waitlist")
    public ResponseEntity<?> create(@RequestBody WaitlistRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(waitlistService.create(request));
    }

    @GetMapping("/api/reservation-waitlist/{code}")
    public ResponseEntity<?> getPublic(@PathVariable String code,
                                       @RequestParam String phone) {
        return ResponseEntity.ok(waitlistService.getPublic(code, phone));
    }

    @GetMapping("/api/admin/reservation-waitlist")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> getAdminList() {
        return ResponseEntity.ok(waitlistService.getAdminList());
    }

    @PatchMapping("/api/admin/reservation-waitlist/{id}/contact")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> contact(@PathVariable Long id, @RequestBody(required = false) WaitlistActionRequest request) {
        return ResponseEntity.ok(waitlistService.contact(id, request));
    }

    @PatchMapping("/api/admin/reservation-waitlist/{id}/convert")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> convert(@PathVariable Long id, @RequestBody(required = false) WaitlistActionRequest request) {
        return ResponseEntity.ok(waitlistService.convert(id, request));
    }

    @PatchMapping("/api/admin/reservation-waitlist/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> cancel(@PathVariable Long id, @RequestBody(required = false) WaitlistActionRequest request) {
        return ResponseEntity.ok(waitlistService.cancel(id, request));
    }
}
