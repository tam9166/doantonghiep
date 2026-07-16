package poly.edu.quanlynhahang.entity;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "points_ledger", uniqueConstraints = {
        @UniqueConstraint(name = "uk_points_ledger_event_key", columnNames = "event_key")
})
public class PointsLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "username", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 40, nullable = false)
    private PointsEventType eventType;

    @Column(name = "event_key", length = 120, nullable = false, unique = true)
    private String eventKey;

    @Column(nullable = false)
    private Integer delta;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(columnDefinition = "nvarchar(300)")
    private String reason;

    @Column(name = "reference_event_key", length = 120)
    private String referenceEventKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
