package poly.edu.quanlynhahang.entity;

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

    private Double quantity = 0.0;

    @Column(columnDefinition = "nvarchar(50)")
    private String unit;

    @Column(name = "min_stock")
    private Double minStock = 5.0;

    @Column(name = "unit_price")
    private Double unitPrice = 0.0;

    private String image;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays = 30; // Mặc định 30 ngày
}