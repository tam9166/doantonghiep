package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "payment_intents")
public class PaymentIntent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "aggregate_type", length = 30, nullable = false)
    private String aggregateType = "RESERVATION";

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "aggregate_code", length = 40, nullable = false)
    private String aggregateCode;

    @Column(length = 30, nullable = false)
    private String purpose;

    @Column(name = "payment_code", length = 40, nullable = false, unique = true)
    private String paymentCode;

    @Column(name = "capability_token_hash", length = 64)
    private String capabilityTokenHash;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option", length = 30, nullable = false)
    private PaymentOption paymentOption;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(precision = 18, scale = 0, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    private String currency = "VND";

    @Column(name = "bank_code", length = 20, nullable = false)
    private String bankCode;

    @Column(name = "bank_bin", length = 20)
    private String bankBin;

    @Column(name = "account_number", length = 40, nullable = false)
    private String accountNumber;

    @Column(name = "account_holder", columnDefinition = "nvarchar(150)", nullable = false)
    private String accountHolder;

    @Column(name = "qr_provider", length = 30, nullable = false)
    private String qrProvider;

    @Column(name = "transfer_content", columnDefinition = "nvarchar(120)", nullable = false)
    private String transferContent;

    @Column(name = "qr_url", columnDefinition = "nvarchar(1000)", nullable = false)
    private String qrUrl;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Date updatedAt = new Date();

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "paid_at")
    private Date paidAt;

    @Column(name = "confirmed_by", length = 80)
    private String confirmedBy;

    @Column(name = "bank_transaction_code", length = 80)
    private String bankTransactionCode;

    @Column(columnDefinition = "nvarchar(500)")
    private String note;

    @PreUpdate
    void touchUpdatedAt() {
        updatedAt = new Date();
    }
}
