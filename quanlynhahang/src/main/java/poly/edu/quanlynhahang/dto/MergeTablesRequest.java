package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MergeTablesRequest(
        @NotNull @Positive Integer fromTableId,
        @NotNull @Positive Integer toTableId
) {
}
