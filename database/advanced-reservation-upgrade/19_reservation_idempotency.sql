SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF COL_LENGTH('dbo.reservations', 'idempotency_key') IS NULL
    BEGIN
        ALTER TABLE dbo.reservations
            ADD idempotency_key varchar(80) NULL;
    END;

    IF COL_LENGTH('dbo.reservations', 'request_fingerprint') IS NULL
    BEGIN
        ALTER TABLE dbo.reservations
            ADD request_fingerprint varchar(128) NULL;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'UX_reservations_idempotency_key'
          AND object_id = OBJECT_ID('dbo.reservations')
    )
    BEGIN
        EXEC(N'CREATE UNIQUE INDEX UX_reservations_idempotency_key
            ON dbo.reservations(idempotency_key)
            WHERE idempotency_key IS NOT NULL;');
    END;

    COMMIT TRANSACTION;
    PRINT N'Done: reservation idempotency columns and unique index are ready.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
