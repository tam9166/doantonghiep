package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "ImportInvoiceDetails")
public class ImportInvoiceDetail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invoice_id", nullable = false)
    private ImportInvoice invoice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;
    @Column(nullable = false, precision = 18, scale = 3) private BigDecimal quantity;
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2) private BigDecimal unitPrice;
    @Column(name = "expiry_date") @Temporal(TemporalType.DATE) private Date expiryDate;
    @Column(name = "total_price", nullable = false, precision = 18, scale = 2) private BigDecimal totalPrice;
}
