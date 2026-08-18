package poly.edu.quanlynhahang.dto;

import poly.edu.quanlynhahang.repository.HotMenuItemProjection;

import java.math.BigDecimal;

public record HotMenuItemResponse(Integer productId, String name, String image, BigDecimal price,
                                  Integer sold7Days, Integer sold30Days, Integer sold90Days,
                                  Double weightedScore) {
    public static HotMenuItemResponse from(HotMenuItemProjection item) {
        return new HotMenuItemResponse(item.getProductId(), item.getName(), item.getImage(), item.getPrice(),
                item.getSold7Days(), item.getSold30Days(), item.getSold90Days(), item.getWeightedScore());
    }
}
