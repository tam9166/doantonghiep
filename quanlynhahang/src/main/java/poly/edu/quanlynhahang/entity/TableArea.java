package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "table_areas")
public class TableArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name_vi", columnDefinition = "nvarchar(150)", nullable = false)
    private String nameVi;

    @Column(name = "name_en", columnDefinition = "nvarchar(150)")
    private String nameEn;

    @Column(name = "description_vi", columnDefinition = "nvarchar(500)")
    private String descriptionVi;

    @Column(name = "description_en", columnDefinition = "nvarchar(500)")
    private String descriptionEn;

    @Column(name = "image_url", columnDefinition = "nvarchar(500)")
    private String imageUrl;

    @Column(name = "base_price", precision = 18, scale = 0)
    private BigDecimal basePrice = BigDecimal.ZERO;

    private Integer capacity = 0;

    @Column(length = 30)
    private String status = "ACTIVE";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at")
    private Date updatedAt = new Date();
}
