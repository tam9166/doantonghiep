package poly.edu.quanlynhahang.entity;

import java.util.Date;

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

    @Column(nullable = false)
    private Double price;

    @Column(length = 255)
    private String image;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Temporal(TemporalType.DATE)
    @Column(name = "create_date")
    private Date createDate = new Date();

    private Boolean available = true; // Trạng thái: Còn hàng (true) hoặc Hết hàng (false)

    @jakarta.persistence.Transient
    private Double averageRating;

    @jakarta.persistence.Transient
    private Double costPrice;

    // Khóa ngoại liên kết với Category
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    
}