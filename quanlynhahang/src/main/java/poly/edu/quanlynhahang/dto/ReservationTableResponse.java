package poly.edu.quanlynhahang.dto;

import lombok.Data;

@Data
public class ReservationTableResponse {
    private Integer tableId;
    private String tableName;
    private String floor;
    private Integer capacity;
    private String imageUrl;
    private boolean primary;
}
