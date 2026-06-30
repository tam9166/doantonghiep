package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String type; // LOW_STOCK, EXPIRING_BATCH, PURCHASE_REQUEST, APPROVED, SYSTEM

    @Column(columnDefinition = "nvarchar(500)")
    private String title;

    @Column(columnDefinition = "nvarchar(max)")
    private String message;

    @Column(name = "target_role", length = 50)
    private String targetRole; // ROLE_ADMIN, ROLE_MANAGER, ALL

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Column(name = "related_entity", length = 100)
    private String relatedEntity; // ingredient, batch, order

    @Column(name = "related_id", length = 50)
    private String relatedId;

    @Column(length = 20)
    private String severity; // critical, warning, info
}
