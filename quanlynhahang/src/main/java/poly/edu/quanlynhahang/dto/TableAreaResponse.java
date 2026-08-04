package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.TableArea;

/** Stable API projection for table areas. */
public record TableAreaResponse(
        Integer id,
        String nameVi,
        String nameEn,
        String descriptionVi,
        String descriptionEn,
        String imageUrl,
        BigDecimal basePrice,
        Integer capacity,
        String status) {

    public static TableAreaResponse from(TableArea area) {
        return new TableAreaResponse(
                area.getId(), area.getNameVi(), area.getNameEn(), area.getDescriptionVi(), area.getDescriptionEn(),
                area.getImageUrl(), area.getBasePrice(), area.getCapacity(), area.getStatus());
    }
}
