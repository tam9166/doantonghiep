package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MergeTablesRequest(
        @NotBlank @Size(max = 50) String fromTable,
        @NotBlank @Size(max = 50) String toTable
) {
}
