package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ServiceZoneAssignmentRequest(@NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 80) String floor, @NotBlank @Size(max = 50) String shift,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$") String workDate) { }
