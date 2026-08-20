package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;

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
import jakarta.persistence.Version;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "ingredient_batches")
public class IngredientBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne
    @JoinColumn(name = "import_invoice_id")
    private ImportInvoice importInvoice;

    @Column(precision = 19, scale = 4)
    private BigDecimal quantity; // Số lượng còn lại trong lô này

    @Column(name = "import_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date importDate;

    @Column(name = "expiration_date")
    private Date expirationDate;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice; // Đơn giá lúc nhập
}
