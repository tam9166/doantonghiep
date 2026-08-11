package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import poly.edu.quanlynhahang.entity.PaymentOption;

import java.util.List;

@Data
public class ReservationRequest {
    @NotBlank @Size(max = 100)
    private String customerName;
    @NotBlank @Pattern(regexp = "^[0-9+() -]{8,20}$")
    private String customerPhone;
    @Email @Size(max = 254)
    private String customerEmail;
    @Size(max = 500)
    private String contactNote;
    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String reservationDate;
    @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    private String arrivalTime;
    @NotNull @Min(30) @Max(480)
    private Integer expectedDurationMinutes;
    private Boolean lateDiningConfirmed;
    @NotNull @Min(1) @Max(50)
    private Integer guestCount;
    @Size(max = 100)
    private String occasion;
    @Size(max = 500)
    private String specialRequest;
    @Size(max = 100)
    private String seatingPreference;
    @Positive
    private Integer areaId;
    @NotNull @Positive
    private Integer tableId;
    @Size(max = 4)
    private List<@NotNull @Positive Integer> tableIds;
    private Boolean preorderEnabled;
    @Valid @Size(max = 30)
    private List<PreorderItemRequest> preorderItems;
    private PaymentOption paymentOption = PaymentOption.DEPOSIT_50;
    @Size(max = 30)
    private String voucherCode;
}
