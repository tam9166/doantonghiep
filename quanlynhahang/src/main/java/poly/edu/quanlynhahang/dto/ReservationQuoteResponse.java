package poly.edu.quanlynhahang.dto;

import lombok.Data;
import poly.edu.quanlynhahang.entity.PaymentOption;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReservationQuoteResponse {
    private BigDecimal tableAmount;
    private BigDecimal foodAmount;
    private BigDecimal originalTotalAmount;
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private PaymentOption paymentOption;
    private BigDecimal paymentRate;
    private BigDecimal payableNow;
    private BigDecimal remainingAmount;
    private DepositPolicyResponse depositPolicy;
    private List<PreorderItemResponse> preorderItems;
}
