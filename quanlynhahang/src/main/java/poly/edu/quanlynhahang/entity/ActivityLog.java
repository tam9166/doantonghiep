package poly.edu.quanlynhahang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String username;

    @Column(length = 50)
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "entity_type", length = 100)
    private String entityType; // Product, Ingredient, Order, ImportInvoice...

    @Column(name = "entity_id", length = 50)
    private String entityId;

    @Column(columnDefinition = "nvarchar(500)")
    private String description;

    @Column(name = "old_value", columnDefinition = "nvarchar(max)")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "nvarchar(max)")
    private String newValue;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp")
    private Date timestamp = new Date();
}
