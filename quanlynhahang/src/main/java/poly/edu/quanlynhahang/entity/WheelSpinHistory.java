package poly.edu.quanlynhahang.entity;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "wheel_spin_history", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wheel_spin_account_date", columnNames = {"username", "spin_date"})
})
public class WheelSpinHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "username", nullable = false)
    private Account account;

    @Column(name = "spin_date", nullable = false)
    private LocalDate spinDate;

    @Column(name = "reward_type", length = 30, nullable = false)
    private String rewardType;

    @Column(name = "reward_value", nullable = false)
    private Integer rewardValue;

    @Column(name = "voucher_code", length = 80)
    private String voucherCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
