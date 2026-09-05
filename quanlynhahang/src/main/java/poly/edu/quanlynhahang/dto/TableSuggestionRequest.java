package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class TableSuggestionRequest {
    @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")
    private String reservationDate;
    @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$")
    private String arrivalTime;
    @NotNull @Min(30) @Max(480)
    private Integer durationMinutes;
    @NotNull @Min(1) @Max(200)
    private Integer guestCount;
    @Positive
    private Integer areaId;
    @Size(max = 100)
    private String seatingPreference;
    @Pattern(regexp = "^[0-9+() -]{8,20}$")
    private String customerPhone;
}
