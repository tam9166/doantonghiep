package poly.edu.quanlynhahang.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {
    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String voucherCode;

    @Deprecated(forRemoval = false)
    private Double deposit;

    @Valid
    @NotEmpty(message = "Giỏ hàng không được để trống")
    private List<OrderDetailRequest> items;
}
