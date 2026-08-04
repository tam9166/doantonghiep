package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "payment_webhook_logs")
public class PaymentWebhookLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 40, nullable = false)
    private String provider;

    @Column(name = "provider_transaction_id", length = 120, nullable = false)
    private String providerTransactionId;

    @Column(name = "payment_code", length = 40)
    private String paymentCode;

    @Column(name = "transfer_content", columnDefinition = "nvarchar(200)")
    private String transferContent;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "account_number", length = 40)
    private String accountNumber;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid = false;

    @Column(length = 30, nullable = false)
    private String status;

    @Column(name = "raw_payload_hash", length = 128)
    private String rawPayloadHash;

    @Column(name = "failure_reason", columnDefinition = "nvarchar(500)")
    private String failureReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "received_at", nullable = false)
    private Date receivedAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "processed_at")
    private Date processedAt;
}
