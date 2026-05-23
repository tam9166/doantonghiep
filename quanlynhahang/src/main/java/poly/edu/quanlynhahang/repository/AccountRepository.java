package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Account;

public interface AccountRepository extends JpaRepository<Account, String> {
}