package poly.edu.quanlynhahang.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AvailableTableResponse {
    private Integer id;
    private String name;
    private String floor;
    private Integer capacity;
    private Integer minCapacity;
    private Integer maxCapacity;
    private BigDecimal reservationPrice;
    private Integer areaId;
    private String areaName;
    private String viewType;
    private Boolean hasView;
    private Boolean windowSeat;
    private Boolean privateRoom;
    private Boolean childFriendly;
    private String positionDescription;
    private String imageUrl;
    private String availabilityStatus;
    private Integer fitScore;
    private String warning;
}
