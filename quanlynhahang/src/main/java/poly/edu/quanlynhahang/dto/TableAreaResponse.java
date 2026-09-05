package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.service.TableAreaReadinessService;
import java.util.List;

/** Stable API projection for table areas. */
public record TableAreaResponse(
        Integer id,
        String nameVi,
        String nameEn,
        String descriptionVi,
        String descriptionEn,
        String imageUrl,
        List<String> gallery,
        BigDecimal basePrice,
        BigDecimal roomFee,
        BigDecimal minimumSpend,
        Integer capacity,
        String status,
        AreaType areaType,
        Integer minGuestCount,
        Integer maxGuestCount,
        Integer minBookingHours,
        BigDecimal hourlyRate,
        BigDecimal packagePrice,
        Integer maxTables,
        Integer defaultGuestsPerTable,
        List<String> suitableEventTypes,
        Boolean bookingReady,
        String bookingReadyReason,
        Integer usableTableCount,
        Integer totalTableCapacity,
        Integer availableCapacity,
        Boolean sufficientCapacity) {

    public static TableAreaResponse from(TableArea area) {
        return from(area, null);
    }

    public static TableAreaResponse from(TableArea area, TableAreaReadinessService.Readiness readiness) {
        return from(area, readiness, null);
    }

    public static TableAreaResponse from(TableArea area, TableAreaReadinessService.Readiness readiness,
                                         poly.edu.quanlynhahang.service.RestaurantCapacityService.CapacitySnapshot capacity) {
        return new TableAreaResponse(
                area.getId(), area.getNameVi(), area.getNameEn(), area.getDescriptionVi(), area.getDescriptionEn(),
                area.getImageUrl(), area.getGallery(), BigDecimal.ZERO,
                area.getPricing() == null ? BigDecimal.ZERO : area.getPricing().getRoomFee(),
                area.getPricing() == null ? BigDecimal.ZERO : area.getPricing().getMinimumSpend(),
                area.getCapacity(), area.getStatus(), area.getAreaType(),
                area.getMinGuestCount(), area.getMaxGuestCount(), area.getMinBookingHours(), area.getHourlyRate(), area.getPackagePrice(),
                area.getMaxTables(), area.getDefaultGuestsPerTable(), area.getSuitableEventTypes(),
                readiness == null ? null : readiness.bookingReady(),
                readiness == null ? null : readiness.reason(),
                readiness == null ? null : readiness.usableTableCount(),
                readiness == null ? null : readiness.totalTableCapacity(),
                capacity == null ? area.getCapacity() : capacity.remainingCapacity(),
                capacity == null ? null : capacity.available());
    }
}
