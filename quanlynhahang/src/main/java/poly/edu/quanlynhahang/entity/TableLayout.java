package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "table_layouts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableLayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_id", nullable = false)
    private Integer tableId;

    @Column(name = "area_id")
    private Integer areaId;

    @Column(name = "floor_name", columnDefinition = "nvarchar(80)")
    private String floorName;

    @Column(name = "x_position", precision = 10, scale = 2)
    private BigDecimal xPosition = BigDecimal.ZERO;

    @Column(name = "y_position", precision = 10, scale = 2)
    private BigDecimal yPosition = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal width = BigDecimal.valueOf(170);

    @Column(precision = 10, scale = 2)
    private BigDecimal height = BigDecimal.valueOf(130);

    @Column(length = 30)
    private String shape = "RECTANGLE";

    @Column(precision = 10, scale = 2)
    private BigDecimal rotation = BigDecimal.ZERO;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "updated_at")
    private Date updatedAt = new Date();
}
