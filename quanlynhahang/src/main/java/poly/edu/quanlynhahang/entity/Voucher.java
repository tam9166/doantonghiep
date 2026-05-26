package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    private Integer discountPercent; // 5, 10, 15, ...

    private Boolean isUsed = false;

    private Date createDate;

    // Mã voucher này thuộc về ai? (null nếu là mã public, nhưng ở đây dùng riêng lẻ cho LuckyWheel)
    @ManyToOne
    @JoinColumn(name = "account_username")
    private Account account;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public Boolean getIsUsed() { return isUsed; }
    public void setIsUsed(Boolean isUsed) { this.isUsed = isUsed; }

    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
}
