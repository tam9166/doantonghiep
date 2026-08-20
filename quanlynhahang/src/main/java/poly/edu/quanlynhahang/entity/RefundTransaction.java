package poly.edu.quanlynhahang.entity;

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
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "refund_transactions")
public class RefundTransaction {

    public enum RefundStatus {
        PENDING,
        COMPLETED,
        FAILED
    }

    public enum RefundReason {
        CANCELLED_BY_CUSTOMER,
        CANCELLED_BY_RESTAURANT,
        NO_SHOW_POLICY,
        DUPLICATE_PAYMENT,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "payment_intent_id")
    private Long paymentIntentId;

    @Column(precision = 18, scale = 0, nullable = false)
    private BigDecimal amount;

    @Column(name = "forfeited_amount", precision = 18, scale = 0)
    private BigDecimal forfeitedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 30, nullable = false)
    private RefundReason reason;

    @Column(name = "reason_detail", columnDefinition = "nvarchar(500)")
    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(name = "processed_by", length = 80)
    private String processedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "processed_at")
    private Date processedAt;

    @Column(name = "failure_reason", columnDefinition = "nvarchar(500)")
    private String failureReason;

    @Column(name = "notes", columnDefinition = "nvarchar(1000)")
    private String notes;
}