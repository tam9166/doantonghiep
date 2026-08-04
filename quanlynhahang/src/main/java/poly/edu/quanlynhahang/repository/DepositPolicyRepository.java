package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import poly.edu.quanlynhahang.entity.DepositPolicy;

import java.util.List;
import java.util.Optional;

public interface DepositPolicyRepository extends JpaRepository<DepositPolicy, Long> {
    Optional<DepositPolicy> findByPolicyCode(String policyCode);

    List<DepositPolicy> findByActiveTrue();

    List<DepositPolicy> findAllByOrderByPriorityDescIdDesc();
}
