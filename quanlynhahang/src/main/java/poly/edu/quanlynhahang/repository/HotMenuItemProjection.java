package poly.edu.quanlynhahang.repository;

import java.math.BigDecimal;

public interface HotMenuItemProjection {
    Integer getProductId();
    String getName();
    String getImage();
    BigDecimal getPrice();
    Integer getSold7Days();
    Integer getSold30Days();
    Integer getSold90Days();
    Double getWeightedScore();
}
