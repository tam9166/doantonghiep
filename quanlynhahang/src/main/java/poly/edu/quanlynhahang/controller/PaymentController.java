package poly.edu.quanlynhahang.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.PaymentQrRequest;
import poly.edu.quanlynhahang.dto.PaymentQrResponse;
import poly.edu.quanlynhahang.service.PaymentService;
@RestController
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/payments/qr")
    public PaymentQrResponse createQr(
            @RequestBody PaymentQrRequest request,
            @RequestHeader(value = "X-Payment-Capability", required = false) String capabilityToken) {
        return paymentService.createQr(request, capabilityToken);
    }

    @PostMapping("/api/payments/{paymentCode}/regenerate")
    public PaymentQrResponse regenerate(
            @PathVariable String paymentCode,
            @RequestHeader(value = "X-Payment-Capability", required = false) String capabilityToken) {
        return paymentService.regenerate(paymentCode, capabilityToken);
    }

    @PatchMapping("/api/admin/payments/{paymentCode}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or hasRole('CASHIER')")
    public PaymentQrResponse confirm(@PathVariable String paymentCode,
                                     @RequestParam(required = false) String bankTransactionCode,
                                     @RequestParam(required = false) String note) {
        return paymentService.confirm(paymentCode, bankTransactionCode, note);
    }
}
