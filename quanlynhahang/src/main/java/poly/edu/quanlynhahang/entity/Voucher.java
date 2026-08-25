package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.math.BigDecimal;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(unique = true, nullable = false)
    private String code;

    private Integer discountPercent; // 5, 10, 15, ...

    @Column(name = "voucher_name", nullable = false)
    private String voucherName;

    @Column(name = "discount_type", nullable = false, length = 10)
    private String discountType = "PERCENT";

    @Column(name = "discount_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal discountValue;

    private Boolean isUsed = false;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    private Date startAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endAt;

    private Date createDate;

    // Mã voucher này thuộc về ai? (null nếu là mã public, nhưng ở đây dùng riêng lẻ cho LuckyWheel)
    @ManyToOne
    @JoinColumn(name = "account_username")
    private Account account;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    @PrePersist
    @PreUpdate
    private void synchronizeLegacyPricingColumns() {
        if (voucherName == null || voucherName.isBlank()) voucherName = code;
        discountType = "PERCENT";
        discountValue = discountPercent == null ? BigDecimal.ZERO : BigDecimal.valueOf(discountPercent);
    }

    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Date getStartAt() { return startAt; }
    public void setStartAt(Date startAt) { this.startAt = startAt; }

    public Date getEndAt() { return endAt; }
    public void setEndAt(Date endAt) { this.endAt = endAt; }

    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}
