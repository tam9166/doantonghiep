package poly.edu.quanlynhahang.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VoucherUpsertRequest(
        @Size(max = 100) String code,
        @NotNull @Min(1) @Max(100) Integer discountPercent,
        @Valid AccountReference account,
        Boolean active,
        @Min(1) Integer usageLimit,
        java.util.Date startAt,
        java.util.Date endAt) {

    public record AccountReference(@NotBlank @Size(max = 80) String username) {
    }
}
