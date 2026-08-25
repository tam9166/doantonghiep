package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.Voucher;

import java.util.Date;

public record VoucherResponse(
        Long id,
        String code,
        Integer discountPercent,
        Boolean isUsed,
        Date createDate,
        String accountUsername,
        Boolean active,
        Integer usageLimit,
        Integer usedCount,
        Date startAt,
        Date endAt,
        String status) {

    public static VoucherResponse from(Voucher voucher) {
        return new VoucherResponse(
                voucher.getId(),
                voucher.getCode(),
                voucher.getDiscountPercent(),
                voucher.getIsUsed(),
                voucher.getCreateDate(),
                voucher.getAccount() == null ? null : voucher.getAccount().getUsername(),
                voucher.getActive(), voucher.getUsageLimit(), voucher.getUsedCount(),
                voucher.getStartAt(), voucher.getEndAt(),
                poly.edu.quanlynhahang.service.VoucherLifecycleService.statusOf(voucher, new Date()));
    }
}
