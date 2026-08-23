package poly.edu.quanlynhahang.entity;

import java.util.Date;

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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "reservation_contact_logs")
public class ReservationContactLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "staff_username", length = 80, nullable = false)
    private String staffUsername;

    @Column(name = "contact_type", length = 30, nullable = false)
    private String contactType;

    @Enumerated(EnumType.STRING)
    @Column(length = 40, nullable = false)
    private ContactStatus result;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "contacted_at", nullable = false)
    private Date contactedAt;

    @Column(columnDefinition = "nvarchar(1000)")
    private String note;
}
