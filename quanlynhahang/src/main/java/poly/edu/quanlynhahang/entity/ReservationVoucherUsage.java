package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "reservation_voucher_usages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationVoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "voucher_code", nullable = false, length = 60)
    private String voucherCode;

    @Column(name = "discount_scope", nullable = false, length = 40)
    private String discountScope = "RESERVATION_TOTAL";

    @Column(name = "discount_amount", precision = 18, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "snapshot_json", columnDefinition = "nvarchar(max)")
    private String snapshotJson;

    @Column(name = "created_at")
    private Date createdAt = new Date();
}
