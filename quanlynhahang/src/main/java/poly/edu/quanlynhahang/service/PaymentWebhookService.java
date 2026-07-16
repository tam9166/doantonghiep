package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import poly.edu.quanlynhahang.dto.PaymentWebhookRequest;
import poly.edu.quanlynhahang.dto.PaymentWebhookResponse;
import poly.edu.quanlynhahang.entity.PaymentWebhookLog;
import poly.edu.quanlynhahang.repository.PaymentWebhookLogRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

@Service
public class PaymentWebhookService {
    private final List<PaymentProvider> providers;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final PaymentLedgerService paymentLedgerService;
    private final String webhookSecret;
    private final String accountNumber;
    private final long timestampToleranceSeconds;

    public PaymentWebhookService(List<PaymentProvider> providers,
                                 PaymentWebhookLogRepository webhookLogRepository,
                                 PaymentLedgerService paymentLedgerService,
                                 @Value("${restaurant.payment.webhook-secret:}") String webhookSecret,
                                 @Value("${restaurant.payment.account-number:}") String accountNumber,
                                 @Value("${restaurant.payment.webhook-timestamp-tolerance-seconds:300}") long timestampToleranceSeconds) {
        this.providers = providers;
        this.webhookLogRepository = webhookLogRepository;
        this.paymentLedgerService = paymentLedgerService;
        this.webhookSecret = webhookSecret;
        this.accountNumber = accountNumber;
        this.timestampToleranceSeconds = timestampToleranceSeconds;
    }

    @Transactional
    public PaymentWebhookResponse handle(String providerCode,
                                         PaymentWebhookRequest request,
                                         String signature,
                                         String timestamp,
                                         String rawPayload) {
        PaymentProvider provider = providers.stream()
                .filter(p -> p.supports(providerCode))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "PAYMENT_PROVIDER_UNSUPPORTED"));

        validateRequired(request);
        validateTimestamp(timestamp);
        boolean signatureValid = provider.verify(payloadForSignature(request, timestamp, rawPayload), signature, webhookSecret);
        PaymentWebhookLog log = createLog(provider.providerCode(), request, signatureValid, rawPayload);
        if (!signatureValid) {
            log.setStatus("REJECTED");
            log.setFailureReason("Invalid webhook signature");
            webhookLogRepository.save(log);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PAYMENT_WEBHOOK_INVALID_SIGNATURE");
        }
        if (!accountNumber.equals(request.getAccountNumber())) {
            log.setStatus("REJECTED");
            log.setFailureReason("Receiver account mismatch");
            webhookLogRepository.save(log);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_ACCOUNT_MISMATCH");
        }

        PaymentWebhookLog existingLog = webhookLogRepository
                .findByProviderAndProviderTransactionId(provider.providerCode(), request.getProviderTransactionId())
                .orElse(null);
        if (existingLog != null && ("PROCESSED".equals(existingLog.getStatus())
                || "MANUAL_REVIEW".equals(existingLog.getStatus()))) {
            return new PaymentWebhookResponse(
                    true,
                    "PAYMENT_ALREADY_PROCESSED",
                    existingLog.getPaymentCode());
        }

        String paymentCode = provider.resolvePaymentCode(request);
        if (paymentCode == null || paymentCode.isBlank()) {
            log.setStatus("MANUAL_REVIEW");
            log.setFailureReason("Payment code not found");
            webhookLogRepository.save(log);
            return new PaymentWebhookResponse(false, "PAYMENT_MANUAL_REVIEW", null);
        }

        PaymentLedgerResult result = paymentLedgerService.recordCredit(
                provider.providerCode(),
                request.getProviderTransactionId(),
                paymentCode,
                request.getTransferContent(),
                request.getAmount(),
                request.getAccountNumber(),
                sha256(rawPayload == null ? "" : rawPayload));
        log.setPaymentCode(paymentCode);
        log.setStatus("PAYMENT_MANUAL_REVIEW".equals(result.code()) ? "MANUAL_REVIEW" : "PROCESSED");
        log.setProcessedAt(new Date());
        webhookLogRepository.save(log);
        return new PaymentWebhookResponse(
                !"PAYMENT_MANUAL_REVIEW".equals(result.code()),
                result.code(),
                result.paymentCode());
    }

    private void validateTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PAYMENT_WEBHOOK_TIMESTAMP_REQUIRED");
        }
        try {
            Instant requestTime = parseTimestamp(timestamp.trim());
            long skew = Math.abs(Instant.now().getEpochSecond() - requestTime.getEpochSecond());
            if (skew > timestampToleranceSeconds) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PAYMENT_WEBHOOK_TIMESTAMP_EXPIRED");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "PAYMENT_WEBHOOK_TIMESTAMP_INVALID");
        }
    }

    private Instant parseTimestamp(String timestamp) {
        if (timestamp.matches("\\d+")) {
            long value = Long.parseLong(timestamp);
            return value > 9_999_999_999L ? Instant.ofEpochMilli(value) : Instant.ofEpochSecond(value);
        }
        return Instant.parse(timestamp);
    }

    private void validateRequired(PaymentWebhookRequest request) {
        if (request == null
                || request.getProviderTransactionId() == null || request.getProviderTransactionId().isBlank()
                || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_WEBHOOK_INVALID_PAYLOAD");
        }
    }

    private PaymentWebhookLog createLog(String provider, PaymentWebhookRequest request, boolean signatureValid, String rawPayload) {
        PaymentWebhookLog log = new PaymentWebhookLog();
        log.setProvider(provider);
        log.setProviderTransactionId(request.getProviderTransactionId());
        log.setPaymentCode(request.getPaymentCode());
        log.setTransferContent(mask(request.getTransferContent()));
        log.setAmount(request.getAmount());
        log.setAccountNumber(maskAccount(request.getAccountNumber()));
        log.setSignatureValid(signatureValid);
        log.setStatus("RECEIVED");
        log.setRawPayloadHash(sha256(rawPayload == null ? "" : rawPayload));
        return log;
    }

    private String payloadForSignature(PaymentWebhookRequest request, String timestamp, String rawPayload) {
        if (rawPayload != null && !rawPayload.isBlank()) {
            return timestamp + "." + rawPayload;
        }
        return timestamp + "." + request.getProviderTransactionId() + "|" + request.getPaymentCode() + "|"
                + request.getAmount() + "|" + request.getAccountNumber();
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return null;
        }
    }

    private String mask(String value) {
        if (value == null || value.length() <= 16) {
            return value;
        }
        return value.substring(0, 12) + "...";
    }

    private String maskAccount(String value) {
        if (value == null || value.length() < 6) {
            return value;
        }
        return "***" + value.substring(value.length() - 4);
    }
}
