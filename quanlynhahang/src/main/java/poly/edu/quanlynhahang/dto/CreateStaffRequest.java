package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Size(min = 4, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
        String username,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 100) String fullname,
        @NotBlank @Email @Size(max = 100) String email,
        @Size(max = 50) String shift,
        @Size(max = 100) String assignedArea,
        @DecimalMin(value = "0.01") java.math.BigDecimal shiftRate) {
}
