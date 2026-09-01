package poly.edu.quanlynhahang.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProductImageAuditContractTest {
    @Test
    void legacyImageRepairUsesStableProductIdsAndSemanticApprovedSources() throws Exception {
        String sql;
        try (var input = new ClassPathResource(
                "db/migration/V083__repair_legacy_product_images.sql").getInputStream()) {
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("WHERE id = 3 AND status = 1"));
        assertTrue(sql.contains("Can_of_Coca_Cola"));
        assertTrue(sql.contains("WHERE id = 5 AND status = 1"));
        assertTrue(sql.contains("WHERE id = 6 AND status = 1"));
        assertTrue(sql.contains("WHERE id = 7 AND status = 1"));
        assertTrue(sql.contains("WHERE id BETWEEN 14 AND 21 AND status = 1"));
        assertFalse(sql.contains("1504674900247"));
        assertFalse(sql.contains("google.com/imgres"));

        String demoSql;
        try (var input = new ClassPathResource(
                "db/migration/V084__hide_demo_products_from_public_menu.sql").getInputStream()) {
            demoSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(demoSql.contains("id BETWEEN 14 AND 21"));
        assertTrue(demoSql.contains("name LIKE N'Demo %'"));
        assertTrue(demoSql.contains("status = 0"));
        assertTrue(demoSql.contains("available = 0"));

        String batchSql;
        try (var input = new ClassPathResource(
                "db/migration/V085__localize_verified_batch_one_product_images.sql").getInputStream()) {
            batchSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(batchSql.contains("/images/products/goi-cuon-tom-thit.jpg"));
        assertTrue(batchSql.contains("/images/products/mi-quang-dac-biet.jpg"));
        assertTrue(batchSql.contains("/images/products/nuoc-ep-dua-hau.jpg"));
        assertTrue(batchSql.contains("/images/products/che-khuc-bach.jpg"));
        assertFalse(batchSql.contains("product.name"));
        String correctionSql;
        try (var input = new ClassPathResource(
                "db/migration/V086__localize_batch_one_verified_food_images.sql").getInputStream()) {
            correctionSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(correctionSql.contains("WHERE id = 5 AND status = 1"));
        assertTrue(correctionSql.contains("/images/products/cha-gio-hai-san-v2.jpg"));
        assertTrue(correctionSql.contains("WHERE id = 6 AND status = 1"));
        assertTrue(correctionSql.contains("/images/products/lau-thai-hai-san-v2.jpg"));
        String semanticSql;
        try (var input = new ClassPathResource(
                "db/migration/V087__localize_batch_one_semantic_food_images.sql").getInputStream()) {
            semanticSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(semanticSql.contains("WHERE id = 9 AND status = 1"));
        assertTrue(semanticSql.contains("/images/products/com-ga-hoi-an-v2.jpg"));
        assertTrue(semanticSql.contains("WHERE id = 12 AND status = 1"));
        assertTrue(semanticSql.contains("/images/products/tra-dao-cam-sa-v2.jpg"));
        String licensedSql;
        try (var input = new ClassPathResource(
                "db/migration/V088__localize_batch_one_licensed_replacements.sql").getInputStream()) {
            licensedSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(licensedSql.contains("WHERE id = 4 AND status = 1"));
        assertTrue(licensedSql.contains("/images/products/goi-cuon-tom-thit-cc0.jpg"));
        assertTrue(licensedSql.contains("WHERE id = 7 AND status = 1"));
        assertTrue(licensedSql.contains("/images/products/bo-nuong-la-lot-cc-by.jpg"));
        assertTrue(licensedSql.contains("WHERE id = 10 AND status = 1"));
        assertTrue(licensedSql.contains("/images/products/mi-quang-dac-biet-cc-by.jpg"));
        assertTrue(licensedSql.contains("WHERE id = 11 AND status = 1"));
        assertTrue(licensedSql.contains("/images/products/nuoc-ep-dua-hau-cc0.jpg"));
        assertTrue(licensedSql.contains("WHERE id = 13 AND status = 1"));
        assertTrue(licensedSql.contains("/images/products/che-khuc-bach-cc-by-sa.jpg"));
        assertTrue(new ClassPathResource("static/images/products/goi-cuon-tom-thit.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/mi-quang-dac-biet.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/nuoc-ep-dua-hau.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/che-khuc-bach.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/cha-gio-hai-san-v2.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/lau-thai-hai-san-v2.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/com-ga-hoi-an-v2.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/tra-dao-cam-sa-v2.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/goi-cuon-tom-thit-cc0.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/bo-nuong-la-lot-cc-by.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/mi-quang-dac-biet-cc-by.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/nuoc-ep-dua-hau-cc0.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/che-khuc-bach-cc-by-sa.jpg").exists());

        String batchTwoSql;
        try (var input = new ClassPathResource(
                "db/migration/V089__localize_batch_two_verified_product_image.sql").getInputStream()) {
            batchTwoSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(batchTwoSql.contains("WHERE id = 79"));
        assertTrue(batchTwoSql.contains("/images/products/cha-ca-da-nang-nuong-la-chuoi.jpg"));
        assertTrue(new ClassPathResource(
                "static/images/products/cha-ca-da-nang-nuong-la-chuoi.jpg").exists());

        String batchThreeSql;
        try (var input = new ClassPathResource(
                "db/migration/V090__localize_batch_three_verified_product_image.sql").getInputStream()) {
            batchThreeSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(batchThreeSql.contains("WHERE id = 382"));
        assertTrue(batchThreeSql.contains("/images/products/saigon-special-cc-by-sa.jpg"));
        assertFalse(batchThreeSql.contains("product.name"));
        assertTrue(new ClassPathResource(
                "static/images/products/saigon-special-cc-by-sa.jpg").exists());

        String batchThreeIdentitySql;
        try (var input = new ClassPathResource(
                "db/migration/V091__align_batch_three_product_identity.sql").getInputStream()) {
            batchThreeIdentitySql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(batchThreeIdentitySql.contains("COUNT(*) FROM products WHERE name = N'Saigon Special'"));
        assertTrue(batchThreeIdentitySql.contains("WHERE id = @saigon_special_id"));
        assertTrue(batchThreeIdentitySql.contains("/images/products/saigon-special-cc-by-sa.jpg"));

        String batchFiveSql;
        try (var input = new ClassPathResource(
                "db/migration/V092__localize_batch_five_verified_product_images.sql").getInputStream()) {
            batchFiveSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(batchFiveSql.contains("name = N'Absolut'"));
        assertTrue(batchFiveSql.contains("/images/products/absolut-vodka-cc-by.jpg"));
        assertTrue(batchFiveSql.contains("name = N'Finlandia'"));
        assertTrue(batchFiveSql.contains("/images/products/finlandia-vodka-cc-by-sa.jpg"));
        assertTrue(batchFiveSql.contains("name = N'Hennessy VS'"));
        assertTrue(batchFiveSql.contains("/images/products/hennessy-vs-cognac-cc-by-sa.jpg"));
        assertFalse(batchFiveSql.contains("DBCC CHECKIDENT"));
        assertFalse(batchFiveSql.contains("IDENTITY_INSERT"));
        assertTrue(new ClassPathResource("static/images/products/absolut-vodka-cc-by.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/finlandia-vodka-cc-by-sa.jpg").exists());
        assertTrue(new ClassPathResource("static/images/products/hennessy-vs-cognac-cc-by-sa.jpg").exists());

        String finalClearanceSql;
        try (var input = new ClassPathResource(
                "db/migration/V093__finalize_verified_product_image_clearance.sql").getInputStream()) {
            finalClearanceSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String[][] finalClearanceProducts = {
                { "Bia 333", "bia-333-cc-by-sa.jpg" },
                { "Larue", "larue-beer-public-domain.jpg" },
                { "Corona Extra", "corona-extra-cc-by-sa.png" },
                { "Hoegaarden", "hoegaarden-original-cc0.jpg" },
                { "Casillero del Diablo Cabernet Sauvignon",
                        "casillero-del-diablo-cabernet-sauvignon-cc-by-sa.jpg" },
                { "Johnnie Walker Black Label", "johnnie-walker-black-label-cc-by-sa.jpg" },
                { "Chivas Regal 12", "chivas-regal-12-cc-by-sa.jpg" },
                { "Ballantine''s Finest", "ballantines-finest-cc-by-sa.jpg" },
                { "Jack Daniel''s Old No.7", "jack-daniels-old-no-7-cc0.jpg" },
                { "Jameson", "jameson-original-cc-by-sa.jpg" },
                { "Smirnoff Red", "smirnoff-red-cc-by.jpg" },
                { "Grey Goose", "grey-goose-cc-by.jpg" },
                { "Belvedere", "belvedere-vodka-cc-by-sa.jpg" },
                { "Rémy Martin VSOP", "remy-martin-vsop-cc-by.jpg" },
                { "Martell VSOP", "martell-vsop-cc0.jpg" },
                { "Courvoisier VSOP", "courvoisier-vsop-cc-by-sa.jpg" }
        };
        for (String[] product : finalClearanceProducts) {
            assertTrue(finalClearanceSql.contains("name = N'" + product[0] + "'"));
            assertTrue(finalClearanceSql.contains("/images/products/" + product[1]));
            assertTrue(new ClassPathResource("static/images/products/" + product[1]).exists());
        }
        assertFalse(finalClearanceSql.contains("DBCC CHECKIDENT"));
        assertFalse(finalClearanceSql.contains("IDENTITY_INSERT"));
        assertFalse(finalClearanceSql.contains("WHERE id ="));
        assertFalse(finalClearanceSql.contains("LIKE"));

        String duplicateClearanceSql;
        try (var input = new ClassPathResource(
                "db/migration/V094__remove_duplicate_mi_quang_product_image.sql").getInputStream()) {
            duplicateClearanceSql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(duplicateClearanceSql.contains("name = N'Mì Quảng Đà Nẵng chuẩn vị'"));
        assertTrue(duplicateClearanceSql.contains("/images/products/mi-quang-da-nang-cc-by-sa.jpg"));
        assertFalse(duplicateClearanceSql.contains("WHERE id ="));
        assertFalse(duplicateClearanceSql.contains("DBCC CHECKIDENT"));
        assertFalse(duplicateClearanceSql.contains("IDENTITY_INSERT"));
        assertTrue(new ClassPathResource(
                "static/images/products/mi-quang-da-nang-cc-by-sa.jpg").exists());
    }
}
