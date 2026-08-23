package poly.edu.quanlynhahang.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.PaymentTransaction;
import poly.edu.quanlynhahang.entity.PaymentTransactionStatus;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    boolean existsByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId);
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);

    List<PaymentTransaction> findByPaymentIntentIdAndStatus(
            Long paymentIntentId, PaymentTransactionStatus status);

    List<PaymentTransaction> findByAggregateTypeAndAggregateIdAndStatus(
            String aggregateType, Long aggregateId, PaymentTransactionStatus status);
}
