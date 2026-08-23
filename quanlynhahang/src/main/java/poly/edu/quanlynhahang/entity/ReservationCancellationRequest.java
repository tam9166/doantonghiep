package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;
import java.util.Date;

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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reservation_cancellation_requests")
public class ReservationCancellationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "request_code", length = 30, nullable = false, unique = true)
    private String requestCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(columnDefinition = "nvarchar(1000)")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private CancellationRequestStatus status = CancellationRequestStatus.PENDING;

    @Column(name = "matched_field_count", nullable = false)
    private Integer matchedFieldCount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "requested_at", nullable = false)
    private Date requestedAt;

    @Column(name = "hours_before_reservation", precision = 12, scale = 2, nullable = false)
    private BigDecimal hoursBeforeReservation;

    @Column(name = "refund_rate", precision = 5, scale = 4, nullable = false)
    private BigDecimal refundRate;

    @Column(name = "paid_deposit_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal paidDepositAmount;

    @Column(name = "expected_refund_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal expectedRefundAmount;

    @Column(name = "refund_transaction_id")
    private Long refundTransactionId;

    @Column(name = "processed_by", length = 80)
    private String processedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "processed_at")
    private Date processedAt;

    @Column(name = "processing_note", columnDefinition = "nvarchar(1000)")
    private String processingNote;
}
