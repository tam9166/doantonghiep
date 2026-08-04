package poly.edu.quanlynhahang.entity;

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

import java.util.Date;

@Data
@Entity
@Table(name = "reservation_images")
public class ReservationImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "area_id")
    private TableArea area;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Column(name = "image_url", columnDefinition = "nvarchar(500)", nullable = false)
    private String imageUrl;

    @Column(name = "alt_text_vi", columnDefinition = "nvarchar(255)")
    private String altTextVi;

    @Column(name = "alt_text_en", columnDefinition = "nvarchar(255)")
    private String altTextEn;

    @Column(name = "is_primary")
    private Boolean primaryImage = false;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();
}
