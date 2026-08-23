package poly.edu.quanlynhahang.entity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "timekeeping")
public class Timekeeping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "username", nullable = false)
    private Account account;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "total_hours", precision = 5, scale = 2, nullable = false)
    private BigDecimal totalHours = BigDecimal.ZERO;

    // Trạng thái: "Đúng giờ", "Đi trễ", "Về sớm", "Hoàn thành"
    @Column(columnDefinition = "nvarchar(50)")
    private String status;
}
