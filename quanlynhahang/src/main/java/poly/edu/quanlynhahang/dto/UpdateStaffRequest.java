package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        @Size(max = 72) String password,
        @Size(max = 100) String fullname,
        @Email @Size(max = 100) String email,
        @Size(max = 50) String shift,
        @Size(max = 100) String assignedArea,
        @DecimalMin(value = "0.01") java.math.BigDecimal shiftRate) {
}
