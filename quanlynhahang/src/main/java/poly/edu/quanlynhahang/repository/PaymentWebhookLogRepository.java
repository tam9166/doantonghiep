package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.PaymentWebhookLog;

import java.util.Optional;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, Long> {
    Optional<PaymentWebhookLog> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);
}
