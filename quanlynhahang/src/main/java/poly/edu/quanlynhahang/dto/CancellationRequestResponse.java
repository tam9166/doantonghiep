package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import poly.edu.quanlynhahang.entity.CancellationRequestStatus;

public record CancellationRequestResponse(
        Long id,
        String requestCode,
        Long reservationId,
        String reservationCode,
        String customerName,
        String customerPhone,
        String customerEmail,
        LocalDate reservationDate,
        LocalTime arrivalTime,
        Integer guestCount,
        BigDecimal depositAmount,
        BigDecimal paidDepositAmount,
        Date requestedAt,
        BigDecimal hoursBeforeReservation,
        BigDecimal refundRate,
        BigDecimal expectedRefundAmount,
        String reason,
        CancellationRequestStatus status,
        Long refundTransactionId,
        String processedBy,
        Date processedAt,
        String processingNote) {
}
