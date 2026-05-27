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
import lombok.Data;

@Data
@Entity
@Table(name = "timekeeping")
public class Timekeeping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "username", nullable = false)
    private Account account;

    @Temporal(TemporalType.DATE)
    @Column(name = "work_date", nullable = false)
    private Date workDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "check_in_time")
    private Date checkInTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "check_out_time")
    private Date checkOutTime;

    // Trạng thái: "Đúng giờ", "Đi trễ", "Về sớm", "Hoàn thành"
    @Column(columnDefinition = "nvarchar(50)")
    private String status;
}
