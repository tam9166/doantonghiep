package poly.edu.quanlynhahang.service;

import org.junit.jupiter.api.Test;
import poly.edu.quanlynhahang.dto.PaymentWebhookRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankWebhookPaymentProviderTest {
    private final BankWebhookPaymentProvider provider = new BankWebhookPaymentProvider();

    @Test
    void verifyAcceptsValidHmacSha256Signature() throws Exception {
        String payload = "{\"paymentCode\":\"PAY-MV-1\"}";
        String secret = "test-secret";

        assertTrue(provider.verify(payload, hmac(payload, secret), secret));
        assertFalse(provider.verify(payload, "bad-signature", secret));
    }

    @Test
    void resolvePaymentCodeFromExplicitFieldOrTransferContent() {
        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setPaymentCode("PAY-MV-20260703-001");
        assertEquals("PAY-MV-20260703-001", provider.resolvePaymentCode(request));

        request.setPaymentCode(null);
        request.setTransferContent("MV MV-20260703-001 PAY-MV-20260703-002");
        assertEquals("PAY-MV-20260703-002", provider.resolvePaymentCode(request));
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
