package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import poly.edu.quanlynhahang.entity.Voucher;
import java.util.Optional;
import java.util.List;

import jakarta.persistence.LockModeType;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v left join fetch v.account where v.code = :code")
    Optional<Voucher> findLockedByCode(@Param("code") String code);

    List<Voucher> findByAccountUsername(String username);
}
