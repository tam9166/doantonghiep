package poly.edu.quanlynhahang.service;

import poly.edu.quanlynhahang.dto.PaymentWebhookRequest;

public interface PaymentProvider {
    String providerCode();

    boolean supports(String provider);

    boolean verify(String payload, String signature, String secret);

    String resolvePaymentCode(PaymentWebhookRequest request);
}
