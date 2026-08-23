package poly.edu.quanlynhahang.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.quanlynhahang.dto.CancellationDecisionRequest;
import poly.edu.quanlynhahang.dto.CancellationRequestCreateRequest;
import poly.edu.quanlynhahang.dto.RefundCompletionRequest;
import poly.edu.quanlynhahang.service.ReservationCancellationService;

@RestController
@RequestMapping("/api")
public class ReservationCancellationController {
    private final ReservationCancellationService service;

    public ReservationCancellationController(ReservationCancellationService service) {
        this.service = service;
    }

    @PostMapping("/reservation-cancellations")
    public ResponseEntity<?> create(@Valid @RequestBody CancellationRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/admin/reservation-cancellations")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(service.list());
    }

    @PatchMapping("/admin/reservation-cancellations/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @Valid @RequestBody CancellationDecisionRequest request) {
        return ResponseEntity.ok(service.approve(id, request));
    }

    @PatchMapping("/admin/reservation-cancellations/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @Valid @RequestBody CancellationDecisionRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }

    @PatchMapping("/admin/reservation-cancellations/{id}/refund-complete")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CASHIER')")
    public ResponseEntity<?> completeRefund(@PathVariable Long id,
                                            @Valid @RequestBody RefundCompletionRequest request) {
        return ResponseEntity.ok(service.completeRefund(id, request));
    }
}
