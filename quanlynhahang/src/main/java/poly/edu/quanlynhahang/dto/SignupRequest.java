package poly.edu.quanlynhahang.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 4, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    private String username;
    @NotBlank
    @Size(min = 10, max = 72)
    private String password;
    @NotBlank
    @Size(max = 100)
    private String fullname;
    @NotBlank
    @Email
    @Size(max = 100)
    private String email;
}
