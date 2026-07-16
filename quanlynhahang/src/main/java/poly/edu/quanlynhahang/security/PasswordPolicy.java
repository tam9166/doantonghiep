package poly.edu.quanlynhahang.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

public final class PasswordPolicy {
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "1234567890", "password123", "qwerty12345", "admin12345",
            "restaurant123", "nhahang123", "matkhau123");

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.length() < 10 || password.length() > 72) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Mật khẩu phải có từ 10 đến 72 ký tự");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Mật khẩu quá phổ biến, vui lòng chọn mật khẩu khác");
        }
    }
}
