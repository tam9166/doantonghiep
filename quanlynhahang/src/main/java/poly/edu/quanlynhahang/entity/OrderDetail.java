package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "OrderDetails")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(precision = 18, scale = 2)
    private BigDecimal price;

    @jakarta.persistence.Column(name = "tax_rate", columnDefinition = "FLOAT DEFAULT 8.0")
    private Double taxRate = 8.0;

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    private Integer quantity;

    @jakarta.persistence.Column(columnDefinition = "int default 0")
    private Integer status = 0; // 0: Chờ nấu, 1: Đã nấu xong, 2: Đã phục vụ

    @Column(name = "note", columnDefinition = "nvarchar(500)")
    private String note;

    @Column(name = "allergy_note", columnDefinition = "nvarchar(500)")
    private String allergyNote;

    @Column(nullable = false)
    private Integer priority = 0;

    @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    @Column(name = "queued_at")
    private Date queuedAt;

    @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    @Column(name = "started_at")
    private Date startedAt;

    @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    @Column(name = "completed_at")
    private Date completedAt;

    @jakarta.persistence.Temporal(jakarta.persistence.TemporalType.TIMESTAMP)
    @Column(name = "cancelled_at")
    private Date cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "nvarchar(500)")
    private String cancelReason;

    // Khóa ngoại biết chi tiết này là của món ăn nào
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // Khóa ngoại biết chi tiết này thuộc về hóa đơn nào
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
