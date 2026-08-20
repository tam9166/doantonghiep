package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "ingredients")
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "nvarchar(200)")
    private String name;

    @Column(precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(columnDefinition = "nvarchar(50)")
    private String unit;

    @Column(name = "min_stock", precision = 19, scale = 4)
    private BigDecimal minStock = new BigDecimal("5.0000");

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    private String image;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays = 30; // Mặc định 30 ngày
}
