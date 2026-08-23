package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import poly.edu.quanlynhahang.entity.OrderPaymentOption;
import poly.edu.quanlynhahang.entity.OrderType;

@Data
public class OrderRequest {
    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String recipientName;

    @Size(max = 20)
    private String recipientPhone;

    @Size(max = 500)
    private String deliveryAddress;

    @Size(max = 500)
    private String deliveryNote;

    @Size(max = 100)
    private String voucherCode;

    @Deprecated(forRemoval = false)
    private BigDecimal deposit;

    /** Type of order: DINE_IN (at table via QR session), TAKEAWAY, DELIVERY. Required for checkout. */
    private OrderType orderType;

    /** Table ID for DINE_IN orders - required when orderType=DINE_IN */
    private Integer tableId;

    private OrderPaymentOption paymentOption;

    @Valid
    @NotEmpty(message = "Giỏ hàng không được để trống")
    private List<OrderDetailRequest> items;
}
