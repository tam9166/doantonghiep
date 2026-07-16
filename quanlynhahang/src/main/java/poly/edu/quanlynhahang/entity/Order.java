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
import lombok.Data;

@Data
@Entity
@Table(name = "Orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    private Date createDate = new Date();

   // Sửa lại chỗ address thành thế này:
    @Column(columnDefinition = "nvarchar(500)")
    private String address;

    // Trạng thái đơn hàng: 0 - Chờ xác nhận, 1 - Đang làm món, 2 - Đã giao, 3 - Đã hủy
    private Integer status = 0;

    @Column(name = "sub_total")
    private Double subTotal = 0.0;

    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @Column(name = "deposit")
    private Double deposit = 0.0;

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
}
