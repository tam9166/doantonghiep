package poly.edu.quanlynhahang.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.PaymentWebhookRequest;
import poly.edu.quanlynhahang.dto.PaymentWebhookResponse;
import poly.edu.quanlynhahang.entity.PaymentWebhookLog;
import poly.edu.quanlynhahang.repository.PaymentWebhookLogRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

class PaymentWebhookServiceTest {

    private final PaymentProvider provider = mock(PaymentProvider.class);
    private final PaymentWebhookLogRepository webhookLogRepository = mock(PaymentWebhookLogRepository.class);
    private final PaymentLedgerService paymentLedgerService = mock(PaymentLedgerService.class);
    private final PaymentWebhookService service = new PaymentWebhookService(
            List.of(provider),
            webhookLogRepository,
            paymentLedgerService,
            "webhook-secret",
            "919112006789",
            300);

    @Test
    void confirmsPaymentAndStoresProcessedWebhookLog() {
        PaymentWebhookRequest request = validRequest();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String payload = "{\"providerTransactionId\":\"BANK-001\"}";
        when(provider.supports("bank")).thenReturn(true);
        when(provider.providerCode()).thenReturn("bank");
        when(provider.verify(timestamp + "." + payload, "valid-signature", "webhook-secret")).thenReturn(true);
        when(provider.resolvePaymentCode(request)).thenReturn("PAY-MV-001");
        when(webhookLogRepository.findByProviderAndProviderTransactionId("bank", "BANK-001"))
                .thenReturn(Optional.empty());
        when(paymentLedgerService.recordCredit(
                eq("bank"), eq("BANK-001"), eq("PAY-MV-001"),
                eq(request.getTransferContent()), eq(request.getAmount()),
                eq(request.getAccountNumber()), any()))
                .thenReturn(new PaymentLedgerResult("PAYMENT_PAID", "PAY-MV-001"));

        PaymentWebhookResponse response = service.handle("bank", request, "valid-signature", timestamp, payload);

        assertEquals("PAYMENT_PAID", response.getCode());
        assertEquals("PAY-MV-001", response.getMessage());

        ArgumentCaptor<PaymentWebhookLog> logCaptor = ArgumentCaptor.forClass(PaymentWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        PaymentWebhookLog savedLog = logCaptor.getValue();
        assertEquals("PROCESSED", savedLog.getStatus());
        assertEquals("PAY-MV-001", savedLog.getPaymentCode());
    }

    @Test
    void rejectsExpiredTimestampBeforeSavingOrConfirmingPayment() {
        PaymentWebhookRequest request = validRequest();
        String expiredTimestamp = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());

        when(provider.supports("bank")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.handle("bank", request, "signature", expiredTimestamp, "{}"));

        assertEquals("401 UNAUTHORIZED \"PAYMENT_WEBHOOK_TIMESTAMP_EXPIRED\"", ex.getMessage());
        verify(webhookLogRepository, never()).save(any());
        verify(paymentLedgerService, never()).recordCredit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void duplicateProcessedWebhookReturnsIdempotentResponse() {
        PaymentWebhookRequest request = validRequest();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        when(provider.supports("bank")).thenReturn(true);
        when(provider.providerCode()).thenReturn("bank");
        when(provider.verify(any(), eq("signature"), eq("webhook-secret"))).thenReturn(true);
        PaymentWebhookLog existing = new PaymentWebhookLog();
        existing.setStatus("PROCESSED");
        existing.setPaymentCode("PAY-MV-001");
        when(webhookLogRepository.findByProviderAndProviderTransactionId("bank", "BANK-001"))
                .thenReturn(Optional.of(existing));

        PaymentWebhookResponse response = service.handle("bank", request, "signature", timestamp, "{}");

        assertEquals("PAYMENT_ALREADY_PROCESSED", response.getCode());
        verify(paymentLedgerService, never()).recordCredit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void storesRejectedLogWhenSignatureIsInvalid() {
        PaymentWebhookRequest request = validRequest();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        when(provider.supports("bank")).thenReturn(true);
        when(provider.providerCode()).thenReturn("bank");
        when(provider.verify(any(), eq("bad-signature"), eq("webhook-secret"))).thenReturn(false);
        when(webhookLogRepository.findByProviderAndProviderTransactionId("bank", "BANK-001"))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.handle("bank", request, "bad-signature", timestamp, "{}"));

        assertEquals("401 UNAUTHORIZED \"PAYMENT_WEBHOOK_INVALID_SIGNATURE\"", ex.getMessage());
        ArgumentCaptor<PaymentWebhookLog> logCaptor = ArgumentCaptor.forClass(PaymentWebhookLog.class);
        verify(webhookLogRepository).save(logCaptor.capture());
        assertEquals("REJECTED", logCaptor.getValue().getStatus());
        verify(paymentLedgerService, never()).recordCredit(any(), any(), any(), any(), any(), any(), any());
    }

    private PaymentWebhookRequest validRequest() {
        PaymentWebhookRequest request = new PaymentWebhookRequest();
        request.setProviderTransactionId("BANK-001");
        request.setPaymentCode("PAY-MV-001");
        request.setTransferContent("TT MV001 ABC12345");
        request.setAmount(new BigDecimal("100000"));
        request.setAccountNumber("919112006789");
        return request;
    }
}
