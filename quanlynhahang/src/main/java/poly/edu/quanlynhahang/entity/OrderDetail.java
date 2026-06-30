package poly.edu.quanlynhahang.entity;

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

    private Double price;

    @jakarta.persistence.Column(name = "tax_rate", columnDefinition = "FLOAT DEFAULT 8.0")
    private Double taxRate = 8.0;

    @jakarta.persistence.Column(name = "tax_amount", columnDefinition = "FLOAT DEFAULT 0.0")
    private Double taxAmount = 0.0;
    
    private Integer quantity;

    @jakarta.persistence.Column(columnDefinition = "int default 0")
    private Integer status = 0; // 0: Chờ nấu, 1: Đã nấu xong, 2: Đã phục vụ

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