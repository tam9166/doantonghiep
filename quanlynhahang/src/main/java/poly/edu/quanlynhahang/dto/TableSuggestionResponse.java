package poly.edu.quanlynhahang.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TableSuggestionResponse {
    private Integer tableId;
    private String tableName;
    private Integer areaId;
    private String areaName;
    private Integer capacity;
    private Integer maxCapacity;
    private BigDecimal reservationPrice;
    private int score;
    private boolean best;
    private List<String> reasons;
    private String availabilityStatus;
}
