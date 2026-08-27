package poly.edu.quanlynhahang.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BlankDatabaseMigrationIntegrationTest {

    @Test
    void officialMigrationsBuildAndSeedABlankDatabase() throws Exception {
        String username = requiredEnvironment("DB_USERNAME");
        String password = requiredEnvironment("DB_PASSWORD");
        String databaseName = "CodexBlank_" + UUID.randomUUID().toString().replace("-", "");
        String masterUrl = "jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true";
        String targetUrl = "jdbc:sqlserver://localhost:1433;databaseName=" + databaseName
                + ";encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true";

        try (Connection master = DriverManager.getConnection(masterUrl, username, password);
             Statement statement = master.createStatement()) {
            statement.execute("CREATE DATABASE [" + databaseName + "]");
        }

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(targetUrl, username, password)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .baselineVersion("2")
                    .load();
            assertEquals(80, flyway.migrate().migrationsExecuted);

            try (Connection target = DriverManager.getConnection(targetUrl, username, password);
                 Statement statement = target.createStatement()) {
                assertTrue(tableExists(statement, "Accounts"));
                assertTrue(tableExists(statement, "reservations"));
                assertTrue(tableExists(statement, "api_rate_limits"));
                assertTrue(tableExists(statement, "table_sessions"));
                assertTrue(tableExists(statement, "reservation_cancellation_requests"));
                assertTrue(tableExists(statement, "reservation_contact_logs"));
                assertTrue(tableExists(statement, "inventory_reservations"));
                assertTrue(tableExists(statement, "area_pricing"));
                assertTrue(tableExists(statement, "ingredient_batch_disposals"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'order_code'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'scheduled_at'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.order_details') AND name = 'cancelled_by'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'recipient_name'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'voucher_discount'"));
                assertEquals(3, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.work_schedules') AND name IN ('shift_name','start_time','end_time') AND is_nullable = 0"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.timekeeping') AND name = 'UX_timekeeping_username_work_date' AND is_unique = 1"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.reservation_waitlist') AND name = 'UX_waitlist_linked_reservation_code' AND is_unique = 1"));
                assertEquals(2, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Accounts') AND name IN ('shift','assigned_area')"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Accounts') AND name = 'shift_rate'"));
                assertEquals(5, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.vouchers') AND name IN ('active','usage_limit','used_count','start_date','end_date')"));
                assertEquals(6, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reservations') AND name IN ('preorder_enabled','table_amount','food_amount','payment_option','deposit_policy_code','deposit_policy_snapshot')"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reservations') AND name = 'order_note'"));
                assertEquals(7, count(statement,
                        "SELECT COUNT(*) FROM dbo.ai_faq_examples WHERE enabled = 1"));
                assertEquals(4, count(statement,
                        "SELECT COUNT(*) FROM sys.columns WHERE object_id = OBJECT_ID('dbo.reservations') AND name IN ('preorder_enabled','table_amount','food_amount','payment_option') AND is_nullable = 0"));
                assertEquals(20, count(statement,
                        "SELECT COUNT(*) FROM dbo.restaurant_table WHERE TRY_CONVERT(INT, SUBSTRING(name, 2, 10)) BETWEEN 1 AND 20 AND area_id IS NOT NULL"));
                assertEquals(8, count(statement,
                        "SELECT COUNT(*) FROM sys.tables WHERE name IN ('activity_logs','notifications','payment_webhook_logs','table_layouts','deposit_policies','reservation_preorder_items','reservation_voucher_usages','reservation_reviews')"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.views WHERE name = 'v_customer_reservation_history'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.payment_webhook_logs') AND name = 'UX_payment_webhook_provider_tx' AND is_unique = 1"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.payment_intents') AND name = 'uk_payment_intent_active_aggregate_purpose' AND is_unique = 1"));
                assertEquals(0, count(statement,
                        "SELECT COUNT(*) FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.payment_intents') AND name = 'uk_payment_intent_active_purpose'"));
                assertEquals(40, count(statement,
                        "SELECT COUNT(*) FROM dbo.Products WHERE alcohol_percentage IS NOT NULL AND volume_ml IS NOT NULL"));
                assertEquals(40, count(statement,
                        "SELECT COUNT(DISTINCT image) FROM dbo.Products WHERE alcohol_percentage IS NOT NULL"));
                assertEquals(0, count(statement,
                        "SELECT COUNT(*) FROM dbo.Products WHERE alcohol_percentage IS NOT NULL AND image LIKE '%google.com/imgres%'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id WHERE dc.parent_object_id = OBJECT_ID('dbo.order_details') AND c.name = 'unit_price'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.columns c JOIN sys.types t ON t.user_type_id = c.user_type_id WHERE c.object_id = OBJECT_ID('dbo.restaurant_table') AND c.name = 'reserved_time' AND t.name = 'varchar' AND c.max_length = 255"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.Orders') AND name = 'CK_Orders_status_legacy' AND definition LIKE '%(7)%'"));
                assertEquals(0, count(statement,
                        "SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.ingredient_batches') AND name IN ('CK_ingredient_batches_quantity_positive','CK_ingredient_batches_remaining_lte_quantity')"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.restaurant_table') AND name = 'CK_restaurant_table_is_occupied' AND definition LIKE '%(0)%' AND definition LIKE '%(1)%' AND definition LIKE '%(2)%' AND definition LIKE '%(3)%' AND definition LIKE '%(5)%'"));
                assertEquals(1, count(statement,
                        "SELECT COUNT(*) FROM dbo.restaurant_settings WHERE setting_key = 'min_profit_margin_percent' AND setting_value = '30.00'"));
                assertEquals(0, count(statement, """
                        SELECT COUNT(*)
                        FROM dbo.products product
                        JOIN (
                            SELECT recipe.product_id, SUM(recipe.amount_required * ingredient.unit_price) calculated_cost
                            FROM dbo.recipes recipe
                            JOIN dbo.ingredients ingredient ON ingredient.id = recipe.ingredient_id
                            GROUP BY recipe.product_id
                        ) economics ON economics.product_id = product.id
                        WHERE (product.status = 1 OR product.available = 1)
                          AND product.price <= economics.calculated_cost
                        """));
                assertEquals(40, count(statement, "SELECT COUNT(*) FROM dbo.restaurant_table"));
                assertEquals(3, count(statement, """
                        SELECT COUNT(*)
                        FROM dbo.table_areas area
                        JOIN (
                            SELECT area_id, SUM(capacity) total_capacity
                            FROM dbo.restaurant_table
                            WHERE is_active = 1
                            GROUP BY area_id
                        ) table_capacity ON table_capacity.area_id = area.id
                        WHERE (area.name_en = 'Indoor Dining' AND area.capacity = 100 AND table_capacity.total_capacity = 100)
                           OR (area.name_en = 'Private / VIP' AND area.capacity = 50 AND table_capacity.total_capacity = 50)
                           OR (area.name_en = 'Garden / Outdoor' AND area.capacity = 70 AND table_capacity.total_capacity = 70)
                        """));
                assertTrue(count(statement, "SELECT COUNT(*) FROM dbo.Accounts") >= 6);
                assertTrue(count(statement, "SELECT COUNT(*) FROM dbo.Products") >= 10);
                assertTrue(count(statement, "SELECT COUNT(*) FROM dbo.ingredients") >= 10);
                assertTrue(new BCryptPasswordEncoder().matches("123", stringValue(statement,
                        "SELECT password FROM dbo.Accounts WHERE username = 'customer'")));
            }
        } finally {
            try (Connection master = DriverManager.getConnection(masterUrl, username, password);
                 Statement statement = master.createStatement()) {
                statement.execute("ALTER DATABASE [" + databaseName + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                statement.execute("DROP DATABASE [" + databaseName + "]");
            }
        }
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet result = statement.executeQuery(
                "SELECT COUNT(*) FROM sys.tables WHERE name = '" + tableName + "'")) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private long count(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private String stringValue(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name.toUpperCase(Locale.ROOT) + " is required for SQL Server migration tests");
        }
        return value;
    }
}
