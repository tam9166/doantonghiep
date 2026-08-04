package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "reservation_status_history")
public class ReservationStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private ReservationStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30, nullable = false)
    private ReservationStatus newStatus;

    @Column(name = "changed_by", length = 80)
    private String changedBy;

    @Column(columnDefinition = "nvarchar(500)")
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "changed_at")
    private Date changedAt = new Date();
}
