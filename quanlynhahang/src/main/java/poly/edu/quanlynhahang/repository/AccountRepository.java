package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import poly.edu.quanlynhahang.entity.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    @EntityGraph(attributePaths = {"authorities", "authorities.role"})
    @Query("select distinct a from Account a where a.username = :username")
    Optional<Account> findForAuthenticationByUsername(@Param("username") String username);

    @EntityGraph(attributePaths = {"authorities", "authorities.role"})
    @Query("select distinct a from Account a")
    List<Account> findAllWithAuthorities();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.username = :username")
    Optional<Account> findLockedByUsername(@Param("username") String username);
}
