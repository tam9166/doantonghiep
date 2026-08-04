-- P1: Reservation waitlist support.
-- Run after 19_reservation_idempotency.sql.

IF OBJECT_ID('dbo.reservation_waitlist', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_waitlist (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        waitlist_code VARCHAR(30) NOT NULL UNIQUE,
        customer_name NVARCHAR(150) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(150) NULL,
        reservation_date DATE NOT NULL,
        preferred_start_time TIME NOT NULL,
        preferred_end_time TIME NOT NULL,
        guest_count INT NOT NULL,
        area_id INT NULL,
        seating_preference NVARCHAR(255) NULL,
        special_request NVARCHAR(500) NULL,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_reservation_waitlist_status DEFAULT 'WAITING',
        linked_reservation_code VARCHAR(30) NULL,
        manager_note NVARCHAR(500) NULL,
        contacted_at DATETIME2 NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_waitlist_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT DF_reservation_waitlist_updated DEFAULT SYSUTCDATETIME()
    );
END;

IF OBJECT_ID('dbo.table_areas', 'U') IS NOT NULL
   AND COL_LENGTH('dbo.reservation_waitlist', 'area_id') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1 FROM sys.foreign_keys
       WHERE name = 'FK_reservation_waitlist_area'
         AND parent_object_id = OBJECT_ID('dbo.reservation_waitlist')
   )
BEGIN
    ALTER TABLE dbo.reservation_waitlist
    ADD CONSTRAINT FK_reservation_waitlist_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_reservation_waitlist_date_status'
      AND object_id = OBJECT_ID('dbo.reservation_waitlist')
)
BEGIN
    CREATE INDEX IX_reservation_waitlist_date_status
    ON dbo.reservation_waitlist(reservation_date, status, preferred_start_time);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_reservation_waitlist_phone'
      AND object_id = OBJECT_ID('dbo.reservation_waitlist')
)
BEGIN
    CREATE INDEX IX_reservation_waitlist_phone
    ON dbo.reservation_waitlist(customer_phone);
END;
