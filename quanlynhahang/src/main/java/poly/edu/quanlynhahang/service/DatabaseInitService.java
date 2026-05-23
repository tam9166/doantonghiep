package poly.edu.quanlynhahang.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import poly.edu.quanlynhahang.entity.Account;
import poly.edu.quanlynhahang.entity.Authority;
import poly.edu.quanlynhahang.entity.Role;
import poly.edu.quanlynhahang.repository.AccountRepository;
import poly.edu.quanlynhahang.repository.AuthorityRepository;
import poly.edu.quanlynhahang.repository.RoleRepository;

@Service
public class DatabaseInitService implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    // Thêm repository này (bạn nhớ tạo interface AuthorityRepository nhé)
    @Autowired
    private AuthorityRepository authorityRepository; 

    @Override
    public void run(String... args) throws Exception {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

       // ✅ ĐÃ SỬA: Thêm .orElse(null) vào cuối
Role adminRole = roleRepository.findByName("ADMIN").orElse(null);
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ADMIN");
            roleRepository.save(adminRole);
            System.out.println(">> Đã khởi tạo Role: ADMIN");
        }

        // Đổi tên kiểm tra thành "manager"
        if (!accountRepository.existsById("manager")) {
            Account admin = new Account();
            admin.setUsername("manager"); // Đổi username thành "manager"
            admin.setPassword(passwordEncoder.encode("123")); // Mã hóa mật khẩu "123"
            admin.setFullname("Quản lý Nhà hàng");
            admin.setEmail("manager@sofipos.vn");
            accountRepository.save(admin);
            
            Authority authority = new Authority();
            authority.setAccount(admin);
            authority.setRole(adminRole);
            authorityRepository.save(authority);

            System.out.println(">> Đã khởi tạo tài khoản manager (đã mã hóa)");
        }
    }
}