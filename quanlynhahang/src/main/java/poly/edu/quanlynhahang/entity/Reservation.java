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
import jakarta.persistence.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@Entity
@Table(name = "reservations")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "reservation_code", length = 30, nullable = false, unique = true)
    private String reservationCode;

    @Column(name = "idempotency_key", length = 80, unique = true)
    private String idempotencyKey;

    @Column(name = "request_fingerprint", length = 128)
    private String requestFingerprint;

    /** The single kitchen order created from this reservation's preorder, if any. */
    @Column(name = "kitchen_order_id", unique = true)
    private Integer kitchenOrderId;

    @Column(name = "created_by", length = 80)
    private String createdBy;

    @Column(name = "payment_capability_token_hash", length = 64)
    private String paymentCapabilityTokenHash;

    @Column(name = "payment_capability_scope", length = 80)
    private String paymentCapabilityScope;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "payment_capability_expires_at")
    private Date paymentCapabilityExpiresAt;

    @Column(name = "payment_capability_revoked", nullable = false)
    private Boolean paymentCapabilityRevoked = false;

    @Column(name = "customer_name", columnDefinition = "nvarchar(150)", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 20, nullable = false)
    private String customerPhone;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "contact_note", columnDefinition = "nvarchar(500)")
    private String contactNote;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "arrival_time", nullable = false)
    private LocalTime arrivalTime;

    @Column(name = "expected_duration_minutes", nullable = false)
    private Integer expectedDurationMinutes = 120;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @Column(columnDefinition = "nvarchar(80)")
    private String occasion;

    @Column(name = "special_request", columnDefinition = "nvarchar(500)")
    private String specialRequest;

    @Column(name = "seating_preference", columnDefinition = "nvarchar(255)")
    private String seatingPreference;

    @Column(name = "preorder_enabled", nullable = false)
    private Boolean preorderEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private TableArea area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_status", length = 30, nullable = false)
    private ReservationStatus reservationStatus = ReservationStatus.PENDING;

    @Column(name = "total_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "table_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal tableAmount = BigDecimal.ZERO;

    @Column(name = "food_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal foodAmount = BigDecimal.ZERO;

    @Column(name = "deposit_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal depositRate = new BigDecimal("0.50");

    @Column(name = "deposit_policy_code", length = 40)
    private String depositPolicyCode;

    @Column(name = "deposit_policy_snapshot", columnDefinition = "nvarchar(max)")
    private String depositPolicySnapshot;

    @Column(name = "deposit_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", length = 30, nullable = false)
    private DepositStatus depositStatus = DepositStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option", length = 30, nullable = false)
    private PaymentOption paymentOption = PaymentOption.DEPOSIT_50;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "manager_note", columnDefinition = "nvarchar(500)")
    private String managerNote;

    @Column(name = "confirmed_by", length = 80)
    private String confirmedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "confirmed_at")
    private Date confirmedAt;

    @Column(name = "rejected_reason", columnDefinition = "nvarchar(500)")
    private String rejectedReason;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt = new Date();
}
