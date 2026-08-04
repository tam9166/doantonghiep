package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Data
public class ReservationActionRequest {
    @Size(max = 500)
    private String note;
    @Size(max = 500)
    private String reason;
    @Positive
    private Integer tableId;
    @Positive
    private Integer areaId;
}
