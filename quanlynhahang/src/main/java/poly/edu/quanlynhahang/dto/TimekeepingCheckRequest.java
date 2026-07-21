package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TimekeepingCheckRequest(
        @NotBlank
        @Pattern(regexp = "IN|OUT", message = "type must be IN or OUT")
        String type) {
}
