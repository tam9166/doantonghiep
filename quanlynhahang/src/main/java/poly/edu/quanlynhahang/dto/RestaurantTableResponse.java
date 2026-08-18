package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.math.BigDecimal;

public record RestaurantTableResponse(
        Integer id,
        String name,
        String floor,
        Integer isOccupied,
        Boolean hasView,
        String reservedTime,
        Integer capacity,
        String viewType,
        Integer minCapacity,
        Integer maxCapacity,
        Integer seatCount,
        BigDecimal reservationPrice,
        Integer areaId,
        String positionDescription,
        Boolean windowSeat,
        Boolean privateRoom,
        Boolean childFriendly,
        Boolean active,
        String imageUrl,
        Integer displayOrder,
        String notes) {

    public static RestaurantTableResponse from(RestaurantTable table) {
        return new RestaurantTableResponse(
                table.getId(), table.getName(), table.getFloor(), table.getIsOccupied(), table.getHasView(),
                table.getReservedTime(), table.getCapacity(), table.getViewType(), table.getMinCapacity(),
                table.getMaxCapacity(), table.getSeatCount(), table.getReservationPrice(), table.getAreaId(),
                table.getPositionDescription(), table.getWindowSeat(), table.getPrivateRoom(),
                table.getChildFriendly(), table.getActive(), table.getImageUrl(), table.getDisplayOrder(), table.getNotes());
    }
}
