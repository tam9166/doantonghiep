package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.Valid;
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
public class ReservationQuoteRequest {
    @Positive
    private Integer areaId;
    @Positive
    private Integer tableId;
    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String reservationDate;
    @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    private String arrivalTime;
    @NotNull @Min(30) @Max(480)
    private Integer durationMinutes;
    private Boolean lateDiningConfirmed;
    @NotNull @Min(1) @Max(200)
    private Integer guestCount;
    @Valid @Size(max = 30)
    private List<PreorderItemRequest> preorderItems;
    private PaymentOption paymentOption = PaymentOption.DEPOSIT_50;
    @Size(max = 30)
    private String voucherCode;
}
