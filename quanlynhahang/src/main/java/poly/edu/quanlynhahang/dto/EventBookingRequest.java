package poly.edu.quanlynhahang.dto;
import jakarta.validation.constraints.*;
import poly.edu.quanlynhahang.entity.EventType;
import java.util.List;
public record EventBookingRequest(
 @NotBlank @Size(max=100) String customerName, @NotBlank @Pattern(regexp="^[0-9+() -]{8,20}$") String customerPhone,
 @Email String customerEmail, @Positive Integer areaId, @NotNull EventType eventType,
 @NotBlank @Pattern(regexp="^\\d{4}-\\d{2}-\\d{2}$") String reservationDate,
 @NotBlank @Pattern(regexp="^([01]\\d|2[0-3]):[0-5]\\d$") String arrivalTime,
 @NotNull @Min(1) @Max(72) Integer durationHours, @NotNull @Min(1) @Max(10000) Integer guestCount,
 Boolean decorationRequired, Boolean mcRequired, @Size(max=500) String eventNote,
 Boolean preorderEnabled, List<@jakarta.validation.Valid PreorderItemRequest> preorderItems,
 Boolean lateDiningConfirmed) {}
