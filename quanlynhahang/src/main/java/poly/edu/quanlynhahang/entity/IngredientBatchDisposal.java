package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "ingredient_batch_disposals")
public class IngredientBatchDisposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private IngredientBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "lot_code", nullable = false, length = 50)
    private String lotCode;

    @Column(name = "quantity_disposed", nullable = false, precision = 19, scale = 4)
    private BigDecimal quantityDisposed;

    @Column(name = "expiry_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;

    @Column(name = "disposal_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date disposalDate;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "confirmed_by", nullable = false, length = 100)
    private String confirmedBy;
}
