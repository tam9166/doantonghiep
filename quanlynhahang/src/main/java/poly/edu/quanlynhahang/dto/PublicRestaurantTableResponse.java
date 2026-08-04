package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.RestaurantTable;

public record PublicRestaurantTableResponse(
        Integer id,
        String name,
        String floor,
        Integer isOccupied,
        Boolean hasView,
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
        String imageUrl) {

    public static PublicRestaurantTableResponse from(RestaurantTable table) {
        return new PublicRestaurantTableResponse(
                table.getId(), table.getName(), table.getFloor(), table.getIsOccupied(), table.getHasView(),
                table.getCapacity(), table.getViewType(), table.getMinCapacity(), table.getMaxCapacity(),
                table.getSeatCount(), table.getReservationPrice(), table.getAreaId(), table.getPositionDescription(),
                table.getWindowSeat(), table.getPrivateRoom(), table.getChildFriendly(), table.getActive(),
                table.getImageUrl());
    }
}
