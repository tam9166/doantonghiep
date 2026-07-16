package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_intent_id")
    private Long paymentIntentId;

    @Column(name = "aggregate_type", length = 30)
    private String aggregateType;

    @Column(name = "aggregate_id")
    private Long aggregateId;

    @Column(length = 40, nullable = false)
    private String provider;

    @Column(name = "provider_transaction_id", length = 120, nullable = false, unique = true)
    private String providerTransactionId;

    @Column(precision = 18, scale = 0, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PaymentDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private PaymentTransactionStatus status;

    @Column(name = "raw_reference", columnDefinition = "nvarchar(200)")
    private String rawReference;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "received_at", nullable = false)
    private Date receivedAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();
}
