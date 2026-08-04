package poly.edu.quanlynhahang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KitchenDishCancelRequest(
        @NotBlank(message = "Cần nêu lý do hủy món")
        @Size(max = 500, message = "Lý do hủy món không được vượt quá 500 ký tự")
        String reason) {
}
