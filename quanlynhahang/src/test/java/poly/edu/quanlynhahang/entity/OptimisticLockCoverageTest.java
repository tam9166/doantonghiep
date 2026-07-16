package poly.edu.quanlynhahang.entity;

import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisticLockCoverageTest {

    @Test
    void mutableAggregatesDeclareJpaVersionField() throws Exception {
        for (Class<?> aggregate : List.of(
                Order.class,
                Reservation.class,
                RestaurantTable.class,
                Voucher.class,
                Account.class,
                IngredientBatch.class,
                PaymentIntent.class)) {
            assertNotNull(aggregate.getDeclaredField("version").getAnnotation(Version.class),
                    () -> aggregate.getSimpleName() + " must use @Version");
        }
    }

    @Test
    void migrationAddsVersionToEveryAggregateTable() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V011__optimistic_locking_versions.sql")) {
            assertNotNull(input);
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        for (String table : List.of(
                "orders", "reservations", "restaurant_table", "vouchers",
                "accounts", "ingredient_batches", "payment_intents")) {
            assertTrue(migration.contains("dbo." + table), () -> "Missing version migration for " + table);
        }
    }
}
