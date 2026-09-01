package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @Size(max = 100) String fullname,
        @Email @Size(max = 100) String email,
        @Size(max = 20) String phone) {
}
