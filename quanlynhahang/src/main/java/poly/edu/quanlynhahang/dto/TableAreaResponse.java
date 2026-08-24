package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.TableArea;
import poly.edu.quanlynhahang.entity.AreaType;
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
        List<String> suitableEventTypes) {

    public static TableAreaResponse from(TableArea area) {
        return new TableAreaResponse(
                area.getId(), area.getNameVi(), area.getNameEn(), area.getDescriptionVi(), area.getDescriptionEn(),
                area.getImageUrl(), area.getGallery(), BigDecimal.ZERO,
                area.getPricing() == null ? BigDecimal.ZERO : area.getPricing().getRoomFee(),
                area.getPricing() == null ? BigDecimal.ZERO : area.getPricing().getMinimumSpend(),
                area.getCapacity(), area.getStatus(), area.getAreaType(),
                area.getMinGuestCount(), area.getMaxGuestCount(), area.getMinBookingHours(), area.getHourlyRate(), area.getPackagePrice(),
                area.getMaxTables(), area.getDefaultGuestsPerTable(), area.getSuitableEventTypes());
    }
}
