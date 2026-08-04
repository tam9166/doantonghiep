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

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "reservation_preorder_items")
public class ReservationPreorderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_name", columnDefinition = "nvarchar(200)", nullable = false)
    private String productName;

    @Column(name = "product_image", columnDefinition = "nvarchar(255)")
    private String productImage;

    @Column(name = "category_name", columnDefinition = "nvarchar(150)")
    private String categoryName;

    @Column(name = "unit_price", precision = 18, scale = 0, nullable = false)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "nvarchar(300)")
    private String note;

    @Column(name = "line_total", precision = 18, scale = 0, nullable = false)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private PreorderItemStatus status = PreorderItemStatus.REQUESTED;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();
}
