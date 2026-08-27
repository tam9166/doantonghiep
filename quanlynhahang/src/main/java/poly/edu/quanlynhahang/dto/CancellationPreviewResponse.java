package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

public record CancellationPreviewResponse(
        String reservationCode,
        BigDecimal paidDepositAmount,
        BigDecimal refundRate,
        BigDecimal expectedRefundAmount,
        BigDecimal hoursBeforeReservation,
        boolean eligible,
        String message) {
}
