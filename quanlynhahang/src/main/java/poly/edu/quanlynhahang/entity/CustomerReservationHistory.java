package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Immutable
@Table(name = "v_customer_reservation_history")
@Data
public class CustomerReservationHistory {
    @Id
    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "latest_customer_name")
    private String latestCustomerName;

    @Column(name = "reservation_count")
    private Long reservationCount;

    @Column(name = "completed_count")
    private Long completedCount;

    @Column(name = "cancelled_count")
    private Long cancelledCount;

    @Column(name = "no_show_count")
    private Long noShowCount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "total_deposit_amount")
    private BigDecimal totalDepositAmount;

    @Column(name = "last_reservation_at")
    private Date lastReservationAt;
}
