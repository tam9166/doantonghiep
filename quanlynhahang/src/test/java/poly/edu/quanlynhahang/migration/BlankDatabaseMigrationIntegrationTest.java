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
            assertEquals(50, flyway.migrate().migrationsExecuted);

            try (Connection target = DriverManager.getConnection(targetUrl, username, password);
                 Statement statement = target.createStatement()) {
                assertTrue(tableExists(statement, "Accounts"));
                assertTrue(tableExists(statement, "reservations"));
                assertTrue(tableExists(statement, "api_rate_limits"));
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
