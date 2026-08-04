package poly.edu.quanlynhahang.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApplicationCreateRequest(
        @NotBlank @Size(max = 200) String fullname,
        @NotBlank @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        @Size(max = 50_000) String message,
        Integer postId) {
}
