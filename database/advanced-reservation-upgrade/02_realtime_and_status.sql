BEGIN TRY
    BEGIN TRAN;

    IF COL_LENGTH('dbo.reservations', 'expected_duration_minutes') IS NULL
        ALTER TABLE dbo.reservations ADD expected_duration_minutes int NOT NULL CONSTRAINT DF_reservations_duration DEFAULT(120);

    IF COL_LENGTH('dbo.reservations', 'last_status_event_at') IS NULL
        ALTER TABLE dbo.reservations ADD last_status_event_at datetime2 NULL;

    IF OBJECT_ID('dbo.reservation_events', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.reservation_events (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            reservation_id bigint NOT NULL,
            reservation_code varchar(30) NOT NULL,
            event_type varchar(60) NOT NULL,
            old_status varchar(30) NULL,
            new_status varchar(30) NOT NULL,
            message nvarchar(500) NULL,
            created_at datetime2 NOT NULL CONSTRAINT DF_reservation_events_created DEFAULT SYSUTCDATETIME(),
            created_by varchar(80) NULL,
            CONSTRAINT FK_reservation_events_reservations FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
        );
    END;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
