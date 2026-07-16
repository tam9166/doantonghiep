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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

@Data
@Entity
@Table(name = "Reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_account_product", columnNames = {"username", "product_id"})
})
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "username")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer rating; // 1 to 5 stars

    @Column(columnDefinition = "NVARCHAR(500)")
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_date")
    private Date createDate = new Date();
}
