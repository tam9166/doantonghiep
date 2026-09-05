package poly.edu.quanlynhahang.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LegacyOperationalDataCleanupMigrationIntegrationTest {

    @Test
    void migrationClosesOldOperationsAndSynchronizesTerminalDetailsWithoutTouchingFutureOrActiveOrders()
            throws Exception {
        String username = requiredEnvironment("DB_USERNAME");
        String password = requiredEnvironment("DB_PASSWORD");
        String databaseName = "CodexLegacyCleanup_" + UUID.randomUUID().toString().replace("-", "");
        String masterUrl = "jdbc:sqlserver://localhost:1433;databaseName=master;encrypt=true;trustServerCertificate=true";
        String targetUrl = "jdbc:sqlserver://localhost:1433;databaseName=" + databaseName
                + ";encrypt=true;trustServerCertificate=true;sendStringParametersAsUnicode=true";

        try (Connection master = DriverManager.getConnection(masterUrl, username, password);
             Statement statement = master.createStatement()) {
            statement.execute("CREATE DATABASE [" + databaseName + "]");
        }

        try {
            Flyway throughV101 = Flyway.configure()
                    .dataSource(targetUrl, username, password)
                    .locations("classpath:db/migration")
                    .target("101")
                    .load();
            assertEquals(101, throughV101.migrate().migrationsExecuted);

            long tableId;
            long orderId;
            long completedDetailId;
            long reservationId;
            long futureTableId;
            long futureOrderId;
            long futureDetailId;
            long futureReservationId;
            long activeCookingOrderId;
            long activeCookingDetailId;
            long cancelledOrderId;
            long cancelledDetailId;
            long refundedOrderId;
            long refundedDetailId;
            LocalDate tomorrow = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1);
            try (Connection target = DriverManager.getConnection(targetUrl, username, password);
                 Statement statement = target.createStatement()) {
                tableId = number(statement, "SELECT TOP 1 id FROM dbo.restaurant_table ORDER BY id");
                long productId = number(statement, "SELECT TOP 1 id FROM dbo.Products ORDER BY id");
                futureTableId = number(statement,
                        "SELECT TOP 1 id FROM dbo.restaurant_table WHERE id <> " + tableId + " ORDER BY id");
                statement.executeUpdate("UPDATE dbo.restaurant_table SET is_occupied = 2, reserved_time = 'legacy test' WHERE id = " + tableId);
                statement.executeUpdate("UPDATE dbo.restaurant_table SET is_occupied = 2, reserved_time = 'future preorder test' WHERE id = " + futureTableId);
                statement.executeUpdate("""
                        INSERT INTO dbo.Orders
                            (order_code, order_type, table_id, status, create_date, is_paid,
                             payment_status, paid_amount, remaining_amount, total_amount)
                        VALUES
                            ('ORD-LEGACY-CLEANUP-TEST', 'DINE_IN', %d, 7, DATEADD(DAY, -2, SYSDATETIME()),
                             0, 'UNPAID', 0, 229000, 229000)
                        """.formatted(tableId));
                orderId = number(statement, "SELECT id FROM dbo.Orders WHERE order_code = 'ORD-LEGACY-CLEANUP-TEST'");
                statement.executeUpdate("""
                        INSERT INTO dbo.order_details
                            (order_id, product_id, price, quantity, status, queued_at)
                        VALUES (%d, %d, 100000, 1, 0, DATEADD(DAY, -2, SYSDATETIME()))
                        """.formatted(orderId, productId));
                completedDetailId = number(statement,
                        "SELECT id FROM dbo.order_details WHERE order_id = " + orderId);
                statement.executeUpdate("""
                        INSERT INTO dbo.reservations
                            (reservation_code, customer_name, customer_phone, reservation_date,
                             arrival_time, guest_count, table_id, reservation_status)
                        VALUES
                            ('MV-LEGACY-CLEANUP-TEST', N'Khách kiểm thử', '0900000000',
                             DATEADD(DAY, -2, CONVERT(date, SYSDATETIME())), '19:00', 2, %d, 'CHECKED_IN')
                        """.formatted(tableId));
                reservationId = number(statement,
                        "SELECT id FROM dbo.reservations WHERE reservation_code = 'MV-LEGACY-CLEANUP-TEST'");

                // Deliberately give the future preorder an old create_date. Its
                // scheduled service date is authoritative and V102 must preserve it.
                statement.executeUpdate("""
                        INSERT INTO dbo.Orders
                            (order_code, order_type, table_id, status, create_date, scheduled_at, is_paid,
                             payment_status, paid_amount, remaining_amount, total_amount)
                        VALUES
                            ('ORD-FUTURE-PREORDER-CLEANUP-TEST', 'DINE_IN', %d, 5,
                             DATEADD(DAY, -2, SYSDATETIME()), '%sT19:00:00',
                             0, 'UNPAID', 0, 350000, 350000)
                        """.formatted(futureTableId, tomorrow));
                futureOrderId = number(statement,
                        "SELECT id FROM dbo.Orders WHERE order_code = 'ORD-FUTURE-PREORDER-CLEANUP-TEST'");
                statement.executeUpdate("""
                        INSERT INTO dbo.order_details
                            (order_id, product_id, price, quantity, status, queued_at)
                        VALUES (%d, %d, 100000, 1, 0, SYSDATETIME())
                        """.formatted(futureOrderId, productId));
                futureDetailId = number(statement,
                        "SELECT id FROM dbo.order_details WHERE order_id = " + futureOrderId);
                statement.executeUpdate("""
                        INSERT INTO dbo.reservations
                            (reservation_code, customer_name, customer_phone, reservation_date,
                             arrival_time, guest_count, table_id, reservation_status, kitchen_order_id)
                        VALUES
                            ('MV-FUTURE-CLEANUP-TEST', N'Khách ngày mai', '0900000001',
                             '%s', '19:00', 2, %d, 'CONFIRMED', %d)
                        """.formatted(tomorrow, futureTableId, futureOrderId));
                futureReservationId = number(statement,
                        "SELECT id FROM dbo.reservations WHERE reservation_code = 'MV-FUTURE-CLEANUP-TEST'");

                statement.executeUpdate("""
                        INSERT INTO dbo.Orders
                            (order_code, order_type, status, create_date, is_paid,
                             payment_status, paid_amount, remaining_amount, total_amount)
                        VALUES
                            ('ORD-ACTIVE-COOKING-V103-TEST', 'TAKEAWAY', 1, SYSDATETIME(),
                             0, 'UNPAID', 0, 100000, 100000)
                        """);
                activeCookingOrderId = number(statement,
                        "SELECT id FROM dbo.Orders WHERE order_code = 'ORD-ACTIVE-COOKING-V103-TEST'");
                statement.executeUpdate("""
                        INSERT INTO dbo.order_details
                            (order_id, product_id, price, quantity, status, queued_at)
                        VALUES (%d, %d, 100000, 1, 0, SYSDATETIME())
                        """.formatted(activeCookingOrderId, productId));
                activeCookingDetailId = number(statement,
                        "SELECT id FROM dbo.order_details WHERE order_id = " + activeCookingOrderId);

                statement.executeUpdate("""
                        INSERT INTO dbo.Orders
                            (order_code, order_type, status, create_date, is_paid,
                             payment_status, paid_amount, remaining_amount, total_amount)
                        VALUES
                            ('ORD-CANCELLED-V103-TEST', 'TAKEAWAY', 3, SYSDATETIME(),
                             0, 'UNPAID', 0, 100000, 100000)
                        """);
                cancelledOrderId = number(statement,
                        "SELECT id FROM dbo.Orders WHERE order_code = 'ORD-CANCELLED-V103-TEST'");
                statement.executeUpdate("""
                        INSERT INTO dbo.order_details
                            (order_id, product_id, price, quantity, status, queued_at)
                        VALUES (%d, %d, 100000, 1, 1, SYSDATETIME())
                        """.formatted(cancelledOrderId, productId));
                cancelledDetailId = number(statement,
                        "SELECT id FROM dbo.order_details WHERE order_id = " + cancelledOrderId);

                statement.executeUpdate("""
                        INSERT INTO dbo.Orders
                            (order_code, order_type, status, create_date, is_paid,
                             payment_status, paid_amount, remaining_amount, total_amount)
                        VALUES
                            ('ORD-REFUNDED-V103-TEST', 'TAKEAWAY', 0, SYSDATETIME(),
                             0, 'REFUNDED', 100000, 0, 100000)
                        """);
                refundedOrderId = number(statement,
                        "SELECT id FROM dbo.Orders WHERE order_code = 'ORD-REFUNDED-V103-TEST'");
                statement.executeUpdate("""
                        INSERT INTO dbo.order_details
                            (order_id, product_id, price, quantity, status, queued_at)
                        VALUES (%d, %d, 100000, 1, 0, SYSDATETIME())
                        """.formatted(refundedOrderId, productId));
                refundedDetailId = number(statement,
                        "SELECT id FROM dbo.order_details WHERE order_id = " + refundedOrderId);
            }

            Flyway latest = Flyway.configure()
                    .dataSource(targetUrl, username, password)
                    .locations("classpath:db/migration")
                    .load();
            assertEquals(2, latest.migrate().migrationsExecuted);

            try (Connection target = DriverManager.getConnection(targetUrl, username, password);
                 Statement statement = target.createStatement()) {
                assertEquals(4, number(statement, "SELECT status FROM dbo.Orders WHERE id = " + orderId));
                assertEquals(1, number(statement, "SELECT is_paid FROM dbo.Orders WHERE id = " + orderId));
                assertEquals("PAID", text(statement, "SELECT payment_status FROM dbo.Orders WHERE id = " + orderId));
                assertEquals(229000, number(statement, "SELECT paid_amount FROM dbo.Orders WHERE id = " + orderId));
                assertEquals(0, number(statement, "SELECT remaining_amount FROM dbo.Orders WHERE id = " + orderId));
                assertEquals("COMPLETED", text(statement,
                        "SELECT reservation_status FROM dbo.reservations WHERE id = " + reservationId));
                assertEquals(0, number(statement,
                        "SELECT is_occupied FROM dbo.restaurant_table WHERE id = " + tableId));
                assertEquals(1, number(statement, "SELECT COUNT(*) FROM dbo.Orders WHERE id = " + orderId));
                assertEquals(1, number(statement, "SELECT COUNT(*) FROM dbo.reservations WHERE id = " + reservationId));
                assertEquals(2, number(statement,
                        "SELECT status FROM dbo.order_details WHERE id = " + completedDetailId));
                assertEquals(0, number(statement, """
                        SELECT COUNT(*)
                        FROM dbo.order_details
                        WHERE order_id = %d AND status IN (0, 1)
                        """.formatted(orderId)));

                assertEquals(5, number(statement, "SELECT status FROM dbo.Orders WHERE id = " + futureOrderId));
                assertEquals("UNPAID", text(statement,
                        "SELECT payment_status FROM dbo.Orders WHERE id = " + futureOrderId));
                assertEquals("CONFIRMED", text(statement,
                        "SELECT reservation_status FROM dbo.reservations WHERE id = " + futureReservationId));
                assertEquals(2, number(statement,
                        "SELECT is_occupied FROM dbo.restaurant_table WHERE id = " + futureTableId));
                assertEquals(1, number(statement,
                        "SELECT COUNT(*) FROM dbo.Orders WHERE id = " + futureOrderId));
                assertEquals(1, number(statement,
                        "SELECT COUNT(*) FROM dbo.reservations WHERE id = " + futureReservationId));
                assertEquals(0, number(statement,
                        "SELECT status FROM dbo.order_details WHERE id = " + futureDetailId));

                assertEquals(1, number(statement,
                        "SELECT status FROM dbo.Orders WHERE id = " + activeCookingOrderId));
                assertEquals(0, number(statement,
                        "SELECT status FROM dbo.order_details WHERE id = " + activeCookingDetailId));
                assertEquals(3, number(statement,
                        "SELECT status FROM dbo.order_details WHERE id = " + cancelledDetailId));
                assertEquals(2, number(statement,
                        "SELECT status FROM dbo.order_details WHERE id = " + refundedDetailId));
            }
        } finally {
            try (Connection master = DriverManager.getConnection(masterUrl, username, password);
                 Statement statement = master.createStatement()) {
                statement.execute("ALTER DATABASE [" + databaseName + "] SET SINGLE_USER WITH ROLLBACK IMMEDIATE");
                statement.execute("DROP DATABASE [" + databaseName + "]");
            }
        }
    }

    private long number(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
            return result.getLong(1);
        }
    }

    private String text(Statement statement, String sql) throws Exception {
        try (ResultSet result = statement.executeQuery(sql)) {
            result.next();
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
