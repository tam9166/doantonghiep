package poly.edu.quanlynhahang.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phân công khu vực phục vụ cho nhân viên theo ca và ngày.
 * Mỗi record = 1 nhân viên được phân công phục vụ 1 tầng trong 1 ca của 1 ngày cụ thể.
 */
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_zone_assignments")
public class ServiceZoneAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "username", nullable = false)
    private Account account;

    // Tầng phục vụ: "Tầng 2 (Sảnh Tiệc)", "Tầng 3 (Phòng VIP)"...
    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String floor;

    // Ca làm: "Sáng", "Chiều", "Tối"
    @Column(nullable = false, columnDefinition = "nvarchar(50)")
    private String shift;

    // Ngày áp dụng
    @Temporal(TemporalType.DATE)
    @Column(name = "work_date", nullable = false)
    private Date workDate;
}
