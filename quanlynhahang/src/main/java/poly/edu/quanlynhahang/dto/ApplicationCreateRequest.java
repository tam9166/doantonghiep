package poly.edu.quanlynhahang.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationCreateRequest(
        @NotBlank @Size(max = 200) String fullname,
        @NotBlank @Size(max = 20) @Pattern(regexp = "^(?:\\+84|0)[0-9 .-]{8,14}$") String phone,
        @Email @Size(max = 100) String email,
        @Size(max = 50_000) String message,
        @NotNull @Positive Integer postId) {
}
