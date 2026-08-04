package poly.edu.quanlynhahang.dto;

import lombok.Data;
import poly.edu.quanlynhahang.entity.PaymentOption;

@Data
public class PaymentQrRequest {
    private String reservationCode;
    private PaymentOption paymentOption;
}
