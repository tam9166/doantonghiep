package poly.edu.quanlynhahang.repository;

import java.util.Optional; // Nhớ thêm dòng import này

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.quanlynhahang.entity.Role;

// ĐÃ SỬA: String -> Integer cho khớp với private Integer id;
public interface RoleRepository extends JpaRepository<Role, Integer>{
    
    // ĐÃ SỬA: Thêm Optional để có thể dùng được hàm .ifPresent() bên Controller
    Optional<Role> findByName(String name);
}