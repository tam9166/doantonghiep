package poly.edu.quanlynhahang.entity;

import java.util.Date;
import java.util.List;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "Orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    private Date createDate = new Date();

   // Sửa lại chỗ address thành thế này:
    @Column(columnDefinition = "nvarchar(500)")
    private String address;

    // Trạng thái đơn hàng: 0 - Chờ xác nhận, 1 - Đang làm món, 2 - Đã giao, 3 - Đã hủy
    private Integer status = 0;

    @Column(name = "sub_total", precision = 18, scale = 2)
    private BigDecimal subTotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "deposit", precision = 18, scale = 2)
    private BigDecimal deposit = BigDecimal.ZERO;

    @Column(name = "table_id")
    private Integer tableId;

    // Khóa ngoại liên kết với bảng Account (người đặt hàng)
    @ManyToOne
    @JoinColumn(name = "username")
    private Account account;

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;

    // Đánh dấu đơn hàng đã được thanh toán (đối với ăn tại quán)
    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_option", length = 30, nullable = false)
    private OrderPaymentOption paymentOption = OrderPaymentOption.PAY_AT_RESTAURANT;

    /** Type of order: DINE_IN (at table via QR session), TAKEAWAY, DELIVERY. */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", length = 20, nullable = false)
    private OrderType orderType = OrderType.TAKEAWAY;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 30, nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(name = "paid_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal remainingAmount = BigDecimal.ZERO;

    @Column(name = "payment_confirmed_by", length = 80)
    private String paymentConfirmedBy;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "payment_confirmed_at")
    private Date paymentConfirmedAt;

    @Column(name = "invoice_requested", nullable = false)
    private Boolean invoiceRequested = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invoice_requested_at")
    private Date invoiceRequestedAt;

    @Column(name = "invoice_email", length = 100)
    private String invoiceEmail;
}
