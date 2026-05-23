package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "OrderDetails")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Double price;
    
    private Integer quantity;

    // Khóa ngoại biết chi tiết này là của món ăn nào
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // Khóa ngoại biết chi tiết này thuộc về hóa đơn nào
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}