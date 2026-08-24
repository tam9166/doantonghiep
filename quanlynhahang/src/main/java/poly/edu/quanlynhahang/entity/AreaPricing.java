package poly.edu.quanlynhahang.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@Table(name = "area_pricing")
public class AreaPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false, unique = true)
    @ToString.Exclude
    private TableArea area;

    @Column(name = "room_fee", precision = 18, scale = 0, nullable = false)
    private BigDecimal roomFee = BigDecimal.ZERO;

    @Column(name = "minimum_spend", precision = 18, scale = 0, nullable = false)
    private BigDecimal minimumSpend = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean active = true;
}
