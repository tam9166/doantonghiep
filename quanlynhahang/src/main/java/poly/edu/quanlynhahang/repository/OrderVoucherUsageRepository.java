package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.OrderVoucherUsage;

import java.util.List;
import java.util.Optional;

public interface OrderVoucherUsageRepository extends JpaRepository<OrderVoucherUsage, Long> {
    boolean existsByOrderId(Integer orderId);
    Optional<OrderVoucherUsage> findByVoucherIdAndOrderId(Long voucherId, Integer orderId);
    List<OrderVoucherUsage> findByVoucherId(Long voucherId);
    int countByVoucherId(Long voucherId);
}
