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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Data
@Entity
@Table(name = "reservation_waitlist")
public class ReservationWaitlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "waitlist_code", length = 30, nullable = false, unique = true)
    private String waitlistCode;

    @Column(name = "customer_name", columnDefinition = "nvarchar(150)", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 20, nullable = false)
    private String customerPhone;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "preferred_start_time", nullable = false)
    private LocalTime preferredStartTime;

    @Column(name = "preferred_end_time", nullable = false)
    private LocalTime preferredEndTime;

    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private TableArea area;

    @Column(name = "seating_preference", columnDefinition = "nvarchar(255)")
    private String seatingPreference;

    @Column(name = "special_request", columnDefinition = "nvarchar(500)")
    private String specialRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private WaitlistStatus status = WaitlistStatus.WAITING;

    @Column(name = "linked_reservation_code", length = 30)
    private String linkedReservationCode;

    @Column(name = "manager_note", columnDefinition = "nvarchar(500)")
    private String managerNote;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "contacted_at")
    private Date contactedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt = new Date();
}
