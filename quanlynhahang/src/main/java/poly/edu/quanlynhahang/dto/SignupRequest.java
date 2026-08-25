package poly.edu.quanlynhahang.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank(message = "Tên đăng nhập là bắt buộc")
    @Size(min = 4, max = 50, message = "Tên đăng nhập phải có từ 4 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Tên đăng nhập chứa ký tự không hợp lệ")
    private String username;
    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 10, max = 72, message = "Mật khẩu phải có từ 10 đến 72 ký tự")
    private String password;
    @NotBlank(message = "Họ và tên là bắt buộc")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullname;
    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không đúng định dạng")
    @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    private String email;

    @AssertTrue(message = "Bạn phải đồng ý với Điều khoản sử dụng và Chính sách bảo mật")
    private boolean termsAccepted;
}
