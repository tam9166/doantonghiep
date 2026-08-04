package poly.edu.quanlynhahang.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import poly.edu.quanlynhahang.dto.PaymentWebhookRequest;
import poly.edu.quanlynhahang.dto.PaymentWebhookResponse;
import poly.edu.quanlynhahang.service.PaymentWebhookService;
import tools.jackson.databind.ObjectMapper;

@RestController
public class PaymentWebhookController {
    private final PaymentWebhookService paymentWebhookService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService, ObjectMapper objectMapper) {
        this.paymentWebhookService = paymentWebhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/webhooks/payments/{provider}")
    public PaymentWebhookResponse paymentWebhook(@PathVariable String provider,
                                                 @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
                                                 @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
                                                 @RequestBody String payload) throws Exception {
        PaymentWebhookRequest request = objectMapper.readValue(payload, PaymentWebhookRequest.class);
        return paymentWebhookService.handle(provider, request, signature, timestamp, payload);
    }
}
