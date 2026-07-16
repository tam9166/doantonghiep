package poly.edu.quanlynhahang.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "Accounts") // Hoá ra tên bảng của bạn có chữ "s", bảo sao lúc nãy SSMS báo lỗi không tìm thấy bảng Account! 😂
public class Account {
    @Id
    @Column(length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    // 🌟 ĐÃ SỬA: Ép cứng kiểu nvarchar(100) cho SQL Server để lưu Tiếng Việt
    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String fullname;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(columnDefinition = "int default 0")
    private Integer points = 0;

    @Column(name = "membership_tier", columnDefinition = "nvarchar(50) default 'Đồng'")
    private String membershipTier = "Đồng";

    @Column(name = "shift", columnDefinition = "nvarchar(50)")
    private String shift;

    @Column(name = "assigned_area", columnDefinition = "nvarchar(100)")
    private String assignedArea;

    @Column(name = "token_version", nullable = false)
    private Long tokenVersion = 0L;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @JsonIgnore
    @OneToMany(mappedBy = "account", fetch = FetchType.EAGER)
    private List<Authority> authorities;
}
