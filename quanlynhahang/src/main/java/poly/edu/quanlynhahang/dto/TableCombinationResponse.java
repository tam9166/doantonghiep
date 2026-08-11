package poly.edu.quanlynhahang.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TableCombinationResponse {
    private boolean available;
    private boolean combinationRequired;
    private List<ReservationTableResponse> tables;
    private Integer totalCapacity;
    private BigDecimal totalReservationPrice;
    private List<String> reasons;
}
