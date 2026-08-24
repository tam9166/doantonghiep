package poly.edu.quanlynhahang.dto;

import lombok.Data;
import poly.edu.quanlynhahang.entity.DepositStatus;
import poly.edu.quanlynhahang.entity.PaymentOption;
import poly.edu.quanlynhahang.entity.PaymentStatus;
import poly.edu.quanlynhahang.entity.ReservationStatus;
import poly.edu.quanlynhahang.entity.ContactStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class ReservationResponse {
    private Long id;
    private String reservationCode;
    private Integer kitchenOrderId;
    private String paymentCapabilityToken;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String reservationDate;
    private String arrivalTime;
    private Integer expectedDurationMinutes;
    private Integer guestCount;
    private String occasion;
    private String specialRequest;
    private String seatingPreference;
    private Boolean preorderEnabled;
    private String orderNote;
    private Integer areaId;
    private String areaName;
    private Integer tableId;
    private String tableName;
    private String tableFloor;
    private List<ReservationTableResponse> tables;
    private ReservationStatus reservationStatus;
    private BigDecimal originalTotalAmount;
    private String voucherCode;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal tableAmount;
    private BigDecimal foodAmount;
    private BigDecimal depositRate;
    private BigDecimal depositAmount;
    private BigDecimal paidAmount;
    private BigDecimal amountDueNow;
    private BigDecimal remainingAmount;
    private DepositStatus depositStatus;
    private PaymentOption paymentOption;
    private PaymentStatus paymentStatus;
    private List<PreorderItemResponse> preorderItems;
    private List<PaymentQrResponse> payments;
    private String managerNote;
    private String rejectedReason;
    private String receiptEmailStatus;
    private Date receiptEmailSentAt;
    private String receiptEmailError;
    private ContactStatus contactStatus;
    private String contactCallNote;
    private Date contactCalledAt;
    private String contactCalledBy;
    private Date confirmedAt;
    private Date createdAt;
    private List<String> history;
}
