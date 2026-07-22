package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Voucher;

import java.util.Date;

public record VoucherResponse(
        Long id,
        String code,
        Integer discountPercent,
        Boolean isUsed,
        Date createDate,
        String accountUsername) {

    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getDiscountPercent(),
                voucher.getIsUsed(),
                voucher.getCreateDate(),
                voucher.getAccount() == null ? null : voucher.getAccount().getUsername());
    }
}
