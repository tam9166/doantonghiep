package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import poly.edu.quanlynhahang.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.username = :username")
    Optional<Account> findLockedByUsername(@Param("username") String username);
}
