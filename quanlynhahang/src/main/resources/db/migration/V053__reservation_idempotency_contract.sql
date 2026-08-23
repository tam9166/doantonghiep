IF OBJECT_ID('dbo.reservations', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.reservations', 'idempotency_key') IS NULL
        EXEC(N'ALTER TABLE dbo.reservations ADD idempotency_key NVARCHAR(80) NULL');

    IF COL_LENGTH('dbo.reservations', 'request_fingerprint') IS NULL
        EXEC(N'ALTER TABLE dbo.reservations ADD request_fingerprint NVARCHAR(128) NULL');

    EXEC(N'IF EXISTS (
              SELECT idempotency_key FROM dbo.reservations
              WHERE idempotency_key IS NOT NULL
              GROUP BY idempotency_key HAVING COUNT(*) > 1
          ) THROW 51000, ''Cannot enforce reservation idempotency: duplicate idempotency_key values exist.'', 1');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.reservations') AND name = 'UX_reservations_idempotency_key')
        EXEC(N'CREATE UNIQUE INDEX UX_reservations_idempotency_key
               ON dbo.reservations(idempotency_key) WHERE idempotency_key IS NOT NULL');
END;
