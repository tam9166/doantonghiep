package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import poly.edu.quanlynhahang.entity.AreaPricing;
import poly.edu.quanlynhahang.entity.AreaType;
import poly.edu.quanlynhahang.entity.TableArea;

class TableAreaPricingContractTest {
    @Test
    void responseIgnoresLegacyBasePriceAndExposesSeparatedPrivateRoomPricing() {
        TableArea area = new TableArea();
        area.setAreaType(AreaType.PRIVATE_ROOM);
        area.setBasePrice(new BigDecimal("999999"));
        AreaPricing pricing = new AreaPricing();
        pricing.setRoomFee(new BigDecimal("300000"));
        pricing.setMinimumSpend(new BigDecimal("2000000"));
        area.setPricing(pricing);

        TableAreaResponse response = TableAreaResponse.from(area);

        assertEquals(BigDecimal.ZERO, response.basePrice());
        assertEquals(new BigDecimal("300000"), response.roomFee());
        assertEquals(new BigDecimal("2000000"), response.minimumSpend());
    }
}
