package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderDetailRequest {
    @NotNull(message = "Sản phẩm không được để trống")
    private Integer productId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    @Max(value = 100, message = "Mỗi món không được vượt quá 100")
    private Integer quantity;

    @Size(max = 500, message = "Ghi chú món không được vượt quá 500 ký tự")
    private String note;

    @Size(max = 500, message = "Ghi chú dị ứng không được vượt quá 500 ký tự")
    private String allergyNote;

    @Min(value = 0, message = "Mức ưu tiên không hợp lệ")
    @Max(value = 10, message = "Mức ưu tiên không được vượt quá 10")
    private Integer priority = 0;
}
