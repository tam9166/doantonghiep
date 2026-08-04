package poly.edu.quanlynhahang.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DepositPolicyResponse {
    private String policyCode;
    private String nameVi;
    private String nameEn;
    private String policyType;
    private BigDecimal percentageRate;
    private BigDecimal fixedAmount;
    private BigDecimal amountPerGuest;
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private BigDecimal depositAmount;
    private String formula;
    private String explanation;
}
