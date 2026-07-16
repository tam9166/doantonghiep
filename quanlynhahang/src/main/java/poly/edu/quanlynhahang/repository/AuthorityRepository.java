package poly.edu.quanlynhahang.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Authority;

import java.util.Collection;
import java.util.List;

public interface AuthorityRepository extends JpaRepository<Authority, Integer> {
    List<Authority> findByAccountUsername(String username);
    long countByRoleNameIn(Collection<String> roleNames);
}
