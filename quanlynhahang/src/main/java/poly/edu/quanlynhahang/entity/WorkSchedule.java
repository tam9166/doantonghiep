package poly.edu.quanlynhahang.entity;

import java.util.Date;
import java.time.LocalTime;

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
@Table(name = "work_schedules")
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "username", nullable = false)
    private Account account;

    @Temporal(TemporalType.DATE)
    @Column(name = "work_date", nullable = false)
    private Date workDate;

    // Ca làm việc: "Sáng" (06:00-14:00), "Chiều" (14:00-22:00), "Tối" (22:00-06:00)
    @Column(nullable = false, columnDefinition = "nvarchar(50)")
    private String shift;

    @Column(name = "shift_name", nullable = false, columnDefinition = "nvarchar(50)")
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED";

    @Column(columnDefinition = "nvarchar(500)")
    private String note;

    public void applyShift(WorkShiftDefinition definition) {
        this.shift = definition.label();
        this.shiftName = definition.label();
        this.startTime = definition.startTime();
        this.endTime = definition.endTime();
    }
}
