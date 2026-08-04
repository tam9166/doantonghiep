package poly.edu.quanlynhahang.dto;

import java.math.BigDecimal;

import poly.edu.quanlynhahang.entity.TableLayout;

/** Stable public projection used by the administration floor-plan editor. */
public record TableLayoutResponse(
        Integer tableId,
        Integer areaId,
        String floorName,
        BigDecimal xPosition,
        BigDecimal yPosition,
        BigDecimal width,
        BigDecimal height,
        String shape,
        BigDecimal rotation) {

    public static TableLayoutResponse from(TableLayout layout) {
        return new TableLayoutResponse(
                layout.getTableId(), layout.getAreaId(), layout.getFloorName(),
                layout.getXPosition(), layout.getYPosition(), layout.getWidth(),
                layout.getHeight(), layout.getShape(), layout.getRotation());
    }
}
