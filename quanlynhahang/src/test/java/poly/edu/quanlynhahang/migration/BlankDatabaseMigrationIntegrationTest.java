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
            assertEquals(59, flyway.migrate().migrationsExecuted);

            try (Connection target = DriverManager.getConnection(targetUrl, username, password);
                 Statement statement = target.createStatement()) {
                assertTrue(tableExists(statement, "Accounts"));
                assertTrue(tableExists(statement, "reservations"));
                assertTrue(tableExists(statement, "api_rate_limits"));
                assertTrue(tableExists(statement, "table_sessions"));
                assertTrue(tableExists(statement, "reservation_cancellation_requests"));
                assertTrue(tableExists(statement, "reservation_contact_logs"));
                assertTrue(tableExists(statement, "inventory_reservations"));
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
                assertTrue(count(statement, "SELECT COUNT(*) FROM dbo.Accounts") >= 6);
                assertTrue(count(statement, "SELECT COUNT(*) FROM dbo.Products") >= 10);
                assertTrue(count(statement, "SELECT COUNT(*) FROM dbo.ingredients") >= 10);
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

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name.toUpperCase(Locale.ROOT) + " is required for SQL Server migration tests");
        }
        return value;
    }
}
