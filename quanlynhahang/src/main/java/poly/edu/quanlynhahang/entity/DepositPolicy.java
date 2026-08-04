package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@Entity
@Table(name = "deposit_policies")
public class DepositPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_code", length = 40, nullable = false, unique = true)
    private String policyCode;

    @Column(name = "name_vi", columnDefinition = "nvarchar(150)", nullable = false)
    private String nameVi;

    @Column(name = "name_en", columnDefinition = "nvarchar(150)")
    private String nameEn;

    @Column(name = "policy_type", length = 30, nullable = false)
    private String policyType;

    @Column(name = "percentage_rate", precision = 5, scale = 2)
    private BigDecimal percentageRate;

    @Column(name = "fixed_amount", precision = 18, scale = 2)
    private BigDecimal fixedAmount;

    @Column(name = "amount_per_guest", precision = 18, scale = 2)
    private BigDecimal amountPerGuest;

    @Column(name = "minimum_amount", precision = 18, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", precision = 18, scale = 2)
    private BigDecimal maximumAmount;

    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "table_type", length = 40)
    private String tableType;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "minimum_guests")
    private Integer minimumGuests;

    @Column(name = "minimum_order_amount", precision = 18, scale = 2)
    private BigDecimal minimumOrderAmount;

    @Column(nullable = false)
    private Integer priority = 100;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "updated_at")
    private Date updatedAt;
}
