package poly.edu.quanlynhahang.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentWebhookRequest {
    private String providerTransactionId;
    private String paymentCode;
    private String transferContent;
    private BigDecimal amount;
    private String accountNumber;
    private String bankCode;
    private String paidAt;
    private String rawPayload;
}
