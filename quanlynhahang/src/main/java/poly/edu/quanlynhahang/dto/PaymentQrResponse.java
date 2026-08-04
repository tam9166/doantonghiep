package poly.edu.quanlynhahang.dto;

import lombok.Data;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentQrResponse {
    private String paymentCode;
    private BigDecimal amount;
    private PaymentOption paymentOption;
    private PaymentStatus status;
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private String transferContent;
    private String qrUrl;
    private Date expiresAt;
}
