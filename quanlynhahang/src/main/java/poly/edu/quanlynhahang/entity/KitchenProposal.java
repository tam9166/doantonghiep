package poly.edu.quanlynhahang.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "kitchen_proposals")
public class KitchenProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_type", nullable = false, length = 20)
    private String proposalType;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "proposed_by", nullable = false, length = 50)
    private String proposedBy;

    @Column(name = "proposer_role", nullable = false, length = 30)
    private String proposerRole = "ROLE_KITCHEN";

    @Column(name = "payload", nullable = false, columnDefinition = "nvarchar(max)")
    private String payload;

    @Column(name = "reason", columnDefinition = "nvarchar(1000)")
    private String reason;

    @Column(name = "review_note", columnDefinition = "nvarchar(1000)")
    private String reviewNote;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "created_entity_type", length = 30)
    private String createdEntityType;

    @Column(name = "created_entity_id", length = 50)
    private String createdEntityId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
