package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class PreorderItemRequest {
    @NotNull
    @Positive
    private Integer productId;

    @NotNull
    @Min(1)
    @Max(30)
    private Integer quantity;

    @Size(max = 300)
    private String note;
}
