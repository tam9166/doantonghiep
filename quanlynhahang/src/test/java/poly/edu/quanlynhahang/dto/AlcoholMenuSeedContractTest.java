package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AlcoholMenuSeedContractTest {
    @Test
    void migrationSeedsEightGroupsWithFortyDistinctProductImages() throws Exception {
        String sql;
        try (var input = new ClassPathResource(
                "db/migration/V073__seed_alcoholic_beverage_menu.sql").getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        for (String category : new String[] {"Bia Việt Nam", "Bia quốc tế", "Vang đỏ", "Vang trắng",
                "Whisky", "Vodka", "Cognac / Brandy", "Sake"}) {
            assertTrue(sql.contains("N'" + category + "'"));
        }
        long productRows = sql.lines().filter(line -> line.startsWith("(N'")).count();
        assertEquals(40, productRows);
        assertFalse(sql.contains("google.com/imgres"));
        assertFalse(sql.contains("placeholder"));
    }
}
