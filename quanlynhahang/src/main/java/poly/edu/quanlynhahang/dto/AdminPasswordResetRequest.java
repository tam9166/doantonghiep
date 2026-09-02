package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Size;

public record AdminPasswordResetRequest(
        @Size(min = 10, max = 72) String password,
        Boolean generateTemporary) {
}
