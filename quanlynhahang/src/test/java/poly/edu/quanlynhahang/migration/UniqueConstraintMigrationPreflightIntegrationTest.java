package poly.edu.quanlynhahang.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;

class UniqueConstraintMigrationPreflightIntegrationTest {
    private static final String MASTER_URL =
            "jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true";

    @Test
    void validLegacyRowsMigrateAndReceiveUniqueIndexes() throws Exception {
        withLegacyDatabase(statement -> {
            statement.executeUpdate("INSERT INTO dbo.reservation_reviews(reservation_id) VALUES (1), (2)");
            statement.executeUpdate("INSERT INTO dbo.reservations(reservation_code) VALUES ('MV-1'), ('MV-2')");
        }, (flyway, statement) -> {
            assertEquals(14, flyway.migrate().migrationsExecuted);
            assertTrue(indexExists(statement, "reservation_reviews", "UX_reservation_reviews_reservation_id"));
            assertTrue(indexExists(statement, "reservations", "UX_reservations_reservation_code"));
            assertTrue(indexExists(statement, "reservations", "UX_reservations_idempotency_key"));
            assertTrue(indexExists(statement, "reservation_waitlist", "UX_waitlist_linked_reservation_code"));
        });
    }

    @Test
    void duplicateReviewReservationIdsFailBeforeUniqueMigration() throws Exception {
        withLegacyDatabase(statement -> {
            statement.executeUpdate("INSERT INTO dbo.reservation_reviews(reservation_id) VALUES (7), (7)");
        }, (flyway, statement) -> {
            FlywayException error = assertThrows(FlywayException.class, flyway::migrate);
            assertTrue(rootMessage(error).contains("duplicate reservation_id"));
        });
    }

    @Test
    void duplicateReservationCodesFailBeforeUniqueMigration() throws Exception {
        withLegacyDatabase(statement -> {
            statement.executeUpdate("INSERT INTO dbo.reservations(reservation_code) VALUES ('MV-DUP'), ('MV-DUP')");
        }, (flyway, statement) -> {
            FlywayException error = assertThrows(FlywayException.class, flyway::migrate);
            assertTrue(rootMessage(error).contains("duplicate reservation_code"));
        });
    }

    @Test
    void duplicateWaitlistReservationLinksFailBeforeUniqueIndex() throws Exception {
        withLegacyDatabase(statement -> statement.executeUpdate("""
                INSERT INTO dbo.reservation_waitlist(linked_reservation_code)
                VALUES ('MV-LINKED-DUP'), ('MV-LINKED-DUP')
                """), (flyway, statement) -> {
            FlywayException error = assertThrows(FlywayException.class, flyway::migrate);
            assertTrue(rootMessage(error).contains("duplicate linked_reservation_code"));
        });
    }

    private void withLegacyDatabase(SqlSetup setup, MigrationAssertion assertion) throws Exception {
        String username = requiredEnvironment("DB_USERNAME");
        String password = requiredEnvironment("DB_PASSWORD");
        String databaseName = "CodexPreflight_" + UUID.randomUUID().toString().replace("-", "");
        String targetUrl = "jdbc:sqlserver://localhost:1433;databaseName=" + databaseName
                + ";encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true";

        try (Connection master = DriverManager.getConnection(MASTER_URL, username, password);
             Statement statement = master.createStatement()) {
            statement.execute("CREATE DATABASE [" + databaseName + "]");
        }

        try {
            try (Connection target = DriverManager.getConnection(targetUrl, username, password);
                 Statement statement = target.createStatement()) {
                statement.execute("CREATE TABLE dbo.reservation_reviews (id BIGINT IDENTITY PRIMARY KEY, reservation_id BIGINT NULL)");
                statement.execute("CREATE TABLE dbo.reservations (id BIGINT IDENTITY PRIMARY KEY, reservation_code NVARCHAR(30) NULL)");
                statement.execute("CREATE TABLE dbo.refund_transactions ("
                        + "id BIGINT IDENTITY PRIMARY KEY, order_id INT NULL, created_at DATETIME2 NOT NULL DEFAULT GETDATE())");
                statement.execute("CREATE TABLE dbo.Orders ("
                        + "id BIGINT IDENTITY PRIMARY KEY, address NVARCHAR(500) NULL, status INT NOT NULL DEFAULT 0)");
                statement.execute("CREATE TABLE dbo.ingredients (id BIGINT IDENTITY PRIMARY KEY)");
                statement.execute("CREATE TABLE dbo.reservation_waitlist ("
                        + "id BIGINT IDENTITY PRIMARY KEY, linked_reservation_code VARCHAR(30) NULL)");
                setup.run(statement);

                Flyway flyway = Flyway.configure()
                        .dataSource(targetUrl, username, password)
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .baselineVersion("45")
                        .load();
                assertion.run(flyway, statement);
            }
        } finally {
            try (Connection master = DriverManager.getConnection(MASTER_URL, username, password);
                 Statement statement = master.createStatement()) {
                statement.execute("ALTER DATABASE [" + databaseName + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                statement.execute("DROP DATABASE [" + databaseName + "]");
            }
        }
    }

    private boolean indexExists(Statement statement, String tableName, String indexName) throws Exception {
        try (ResultSet result = statement.executeQuery("""
                SELECT COUNT(*) FROM sys.indexes
                 WHERE object_id = OBJECT_ID(N'dbo.%s') AND name = N'%s'
                """.formatted(tableName, indexName))) {
            return result.next() && result.getInt(1) == 1;
        }
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return String.valueOf(current.getMessage());
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name.toUpperCase(Locale.ROOT) + " is required for SQL Server migration tests");
        }
        return value;
    }

    @FunctionalInterface
    private interface SqlSetup {
        void run(Statement statement) throws Exception;
    }

    @FunctionalInterface
    private interface MigrationAssertion {
        void run(Flyway flyway, Statement statement) throws Exception;
    }
}
