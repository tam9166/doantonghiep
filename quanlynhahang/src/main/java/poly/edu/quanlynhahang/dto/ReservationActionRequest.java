package poly.edu.quanlynhahang.dto;

import lombok.Data;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class ReservationActionRequest {
    @Size(max = 500)
    private String note;
    @Size(max = 500)
    private String reason;
    @Positive
    private Integer tableId;
    @Size(max = 4)
    private List<@NotNull @Positive Integer> tableIds;
    @Positive
    private Integer areaId;
}
