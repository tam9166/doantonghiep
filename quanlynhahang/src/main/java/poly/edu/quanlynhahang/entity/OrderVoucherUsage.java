package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "order_voucher_usage", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"voucher_id", "order_id"})
})
public class OrderVoucherUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", nullable = false)
    private Long voucherId;

    @Column(name = "voucher_code", length = 50, nullable = false)
    private String voucherCode;

    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    @Column(name = "account_username", length = 80)
    private String accountUsername;

    @Column(name = "discount_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "original_amount", precision = 18, scale = 0, nullable = false)
    private BigDecimal originalAmount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "used_at", nullable = false)
    private Date usedAt = new Date();
}