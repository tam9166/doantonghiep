IF OBJECT_ID(N'dbo.reservation_tables', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_tables (
        id BIGINT IDENTITY(1, 1) NOT NULL PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        table_id INT NOT NULL,
        is_primary BIT NOT NULL CONSTRAINT df_reservation_tables_is_primary DEFAULT 0,
        version BIGINT NOT NULL CONSTRAINT df_reservation_tables_version DEFAULT 0,
        CONSTRAINT uq_reservation_tables_reservation_table UNIQUE (reservation_id, table_id),
        CONSTRAINT fk_reservation_tables_reservation FOREIGN KEY (reservation_id)
            REFERENCES dbo.reservations(id) ON DELETE CASCADE,
        CONSTRAINT fk_reservation_tables_table FOREIGN KEY (table_id)
            REFERENCES dbo.restaurant_table(id)
    );
    CREATE INDEX ix_reservation_tables_table_id ON dbo.reservation_tables(table_id);
END
GO

-- Keep existing reservations compatible: their legacy table_id becomes the primary assignment.
INSERT INTO dbo.reservation_tables (reservation_id, table_id, is_primary, version)
SELECT r.id, r.table_id, 1, 0
FROM dbo.reservations r
WHERE r.table_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.reservation_tables rt
      WHERE rt.reservation_id = r.id AND rt.table_id = r.table_id
  );
GO
