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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
@Table(name = "Products")
public class Product {
    private Boolean status = true;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "NVARCHAR(200)", nullable = false)
    private String name;

    @Column(name = "name_vi", columnDefinition = "NVARCHAR(200)")
    private String nameVi;

    @Column(name = "name_en", columnDefinition = "NVARCHAR(200)")
    private String nameEn;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("8.00");

    @Column(length = 1000)
    private String image;

    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Column(name = "alcohol_percentage", precision = 5, scale = 2)
    private BigDecimal alcoholPercentage;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "description_vi", columnDefinition = "NVARCHAR(MAX)")
    private String descriptionVi;

    @Column(name = "description_en", columnDefinition = "NVARCHAR(MAX)")
    private String descriptionEn;

    @Temporal(TemporalType.DATE)
    @Column(name = "create_date")
    private Date createDate = new Date();

    private Boolean available = true; // Trạng thái: Còn hàng (true) hoặc Hết hàng (false)

    @jakarta.persistence.Transient
    private Double averageRating;

    @Column(name = "cost_price", precision = 18, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type", nullable = false, length = 20)
    private DietType dietType = DietType.MAN;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooking_method", nullable = false, length = 20)
    private CookingMethod cookingMethod = CookingMethod.KHAC;

    @Column(name = "spicy_level", nullable = false)
    private Integer spicyLevel = 0;

    @Column(name = "is_signature_dish", nullable = false)
    private Boolean isSignatureDish = false;

    // Khóa ngoại liên kết với Category
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
}
