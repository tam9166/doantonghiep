package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.OrderItemOperation;

import java.util.Optional;

public interface OrderItemOperationRepository extends JpaRepository<OrderItemOperation, Long> {
    Optional<OrderItemOperation> findByOrderIdAndIdempotencyKey(Integer orderId, String idempotencyKey);
}
