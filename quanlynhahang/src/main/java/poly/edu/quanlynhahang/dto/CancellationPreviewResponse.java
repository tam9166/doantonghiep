package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

public record CancellationPreviewResponse(
        String reservationCode,
        BigDecimal orderTotalAmount,
        BigDecimal paidDepositAmount,
        BigDecimal penaltyAmount,
        BigDecimal refundRate,
        BigDecimal expectedRefundAmount,
        BigDecimal hoursBeforeReservation,
        boolean eligible,
        String policyApplied,
        String message,
        String policyCode,
        String messageCode) {
}
