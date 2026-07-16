package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.ReservationStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public record PublicReservationResponse(
        String reservationCode,
        String customerName,
        String customerPhone,
        String customerEmail,
        String reservationDate,
        String arrivalTime,
        Integer expectedDurationMinutes,
        Integer guestCount,
        String occasion,
        String specialRequest,
        String seatingPreference,
        Boolean preorderEnabled,
        Integer areaId,
        String areaName,
        Integer tableId,
        String tableName,
        String tableFloor,
        ReservationStatus reservationStatus,
        BigDecimal originalTotalAmount,
        String voucherCode,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal tableAmount,
        BigDecimal foodAmount,
        BigDecimal depositRate,
        BigDecimal depositAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        DepositStatus depositStatus,
        PaymentOption paymentOption,
        PaymentStatus paymentStatus,
        List<PreorderItemResponse> preorderItems,
        String rejectedReason,
        Date confirmedAt,
        Date createdAt) {

    public static PublicReservationResponse from(ReservationResponse response) {
        return new PublicReservationResponse(
                response.getReservationCode(),
                response.getCustomerName(),
                maskPhone(response.getCustomerPhone()),
                maskEmail(response.getCustomerEmail()),
                response.getReservationDate(),
                response.getArrivalTime(),
                response.getExpectedDurationMinutes(),
                response.getGuestCount(),
                response.getOccasion(),
                response.getSpecialRequest(),
                response.getSeatingPreference(),
                response.getPreorderEnabled(),
                response.getAreaId(),
                response.getAreaName(),
                response.getTableId(),
                response.getTableName(),
                response.getTableFloor(),
                response.getReservationStatus(),
                response.getOriginalTotalAmount(),
                response.getVoucherCode(),
                response.getDiscountAmount(),
                response.getTotalAmount(),
                response.getTableAmount(),
                response.getFoodAmount(),
                response.getDepositRate(),
                response.getDepositAmount(),
                response.getPaidAmount(),
                response.getRemainingAmount(),
                response.getDepositStatus(),
                response.getPaymentOption(),
                response.getPaymentStatus(),
                response.getPreorderItems(),
                response.getRejectedReason(),
                response.getConfirmedAt(),
                response.getCreatedAt());
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return "***" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.substring(0, 1) + "***" + email.substring(at);
    }
}
