package poly.edu.quanlynhahang.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import poly.edu.quanlynhahang.entity.RestaurantTable;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RestaurantTableUpsertRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String floor,
        Boolean hasView,
        @Min(1) @Max(100) Integer capacity,
        @Size(max = 50) String viewType,
        @Min(1) @Max(100) Integer minCapacity,
        @Min(1) @Max(100) Integer maxCapacity,
        @Min(1) @Max(100) Integer seatCount,
        @DecimalMin("0") BigDecimal reservationPrice,
        @Positive Integer areaId,
        @Size(max = 255) String positionDescription,
        Boolean windowSeat,
        Boolean privateRoom,
        Boolean childFriendly,
        Boolean active,
        @Size(max = 500) String imageUrl) {

    public RestaurantTable toNewEntity() {
        RestaurantTable table = new RestaurantTable();
        table.setIsOccupied(0);
        applyTo(table, true);
        return table;
    }

    public void applyTo(RestaurantTable table, boolean creating) {
        int normalizedCapacity = capacity == null ? 4 : capacity;
        table.setName(name.trim());
        table.setFloor(trimToNull(floor));
        table.setCapacity(normalizedCapacity);
        table.setMinCapacity(minCapacity == null ? 1 : minCapacity);
        table.setMaxCapacity(maxCapacity == null ? normalizedCapacity : maxCapacity);
        table.setSeatCount(seatCount == null ? table.getMaxCapacity() : seatCount);
        table.setReservationPrice(reservationPrice == null ? BigDecimal.ZERO : reservationPrice);
        table.setAreaId(areaId);
        table.setViewType(trimToNull(viewType));
        table.setPositionDescription(trimToNull(positionDescription));
        table.setImageUrl(trimToNull(imageUrl));
        table.setHasView(hasView == null ? Boolean.TRUE.equals(windowSeat) || viewType != null : hasView);
        table.setWindowSeat(windowSeat == null ? Boolean.TRUE.equals(table.getHasView()) : windowSeat);
        table.setPrivateRoom(privateRoom == null
                ? floor != null && floor.toLowerCase().contains("vip")
                : privateRoom);
        table.setChildFriendly(childFriendly == null || childFriendly);
        if (active != null || creating) {
            table.setActive(active == null || active);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
