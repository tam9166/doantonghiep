package poly.edu.quanlynhahang.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SplitTableRequest(
        @NotNull @Positive Integer fromTableId,
        @NotNull @Positive Integer toTableId,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Integer> detailIds
) {
}
