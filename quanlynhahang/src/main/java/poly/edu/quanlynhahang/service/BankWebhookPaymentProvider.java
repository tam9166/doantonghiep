package poly.edu.quanlynhahang.service;

import org.springframework.stereotype.Component;
import poly.edu.quanlynhahang.dto.PaymentWebhookRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class BankWebhookPaymentProvider implements PaymentProvider {
    @Override
    public String providerCode() {
        return "bank";
    }

    @Override
    public boolean supports(String provider) {
        return provider != null && ("bank".equalsIgnoreCase(provider) || "mbbank".equalsIgnoreCase(provider));
    }

    @Override
    public boolean verify(String payload, String signature, String secret) {
        if (payload == null || signature == null || signature.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public String resolvePaymentCode(PaymentWebhookRequest request) {
        if (request == null) {
            return null;
        }
        if (request.getPaymentCode() != null && !request.getPaymentCode().isBlank()) {
            return request.getPaymentCode().trim();
        }
        String content = request.getTransferContent();
        if (content == null) {
            return null;
        }
        for (String part : content.split("\\s+")) {
            if (part.startsWith("PAY-")) {
                return part.trim();
            }
        }
        return null;
    }
}
