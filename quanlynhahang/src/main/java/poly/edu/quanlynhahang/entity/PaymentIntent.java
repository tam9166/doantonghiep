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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
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
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "payment_code", length = 40, nullable = false, unique = true)
    private String paymentCode;

    @Column(name = "capability_token_hash", length = 64)
    private String capabilityTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option", length = 30, nullable = false)
    private PaymentOption paymentOption;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(precision = 18, scale = 0, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "bank_code", length = 20, nullable = false)
    private String bankCode;

    @Column(name = "account_number", length = 40, nullable = false)
    private String accountNumber;

    @Column(name = "account_holder", columnDefinition = "nvarchar(150)", nullable = false)
    private String accountHolder;

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
    @Column(name = "paid_at")
    private Date paidAt;

    @Column(name = "confirmed_by", length = 80)
    private String confirmedBy;

    @Column(name = "bank_transaction_code", length = 80)
    private String bankTransactionCode;

    @Column(columnDefinition = "nvarchar(500)")
    private String note;
}
