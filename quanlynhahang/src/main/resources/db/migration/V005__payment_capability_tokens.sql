IF COL_LENGTH(N'dbo.reservations', N'created_by') IS NULL
    ALTER TABLE dbo.reservations ADD created_by VARCHAR(80) NULL;

IF COL_LENGTH(N'dbo.reservations', N'payment_capability_token_hash') IS NULL
    ALTER TABLE dbo.reservations ADD payment_capability_token_hash VARCHAR(64) NULL;

IF COL_LENGTH(N'dbo.reservations', N'payment_capability_scope') IS NULL
    ALTER TABLE dbo.reservations ADD payment_capability_scope VARCHAR(30) NULL;

IF COL_LENGTH(N'dbo.reservations', N'payment_capability_expires_at') IS NULL
    ALTER TABLE dbo.reservations ADD payment_capability_expires_at DATETIME2 NULL;

IF COL_LENGTH(N'dbo.reservations', N'payment_capability_revoked') IS NULL
BEGIN
    ALTER TABLE dbo.reservations
        ADD payment_capability_revoked BIT NOT NULL
            CONSTRAINT df_reservation_payment_capability_revoked DEFAULT 0;
END;

IF COL_LENGTH(N'dbo.payment_intents', N'capability_token_hash') IS NULL
    ALTER TABLE dbo.payment_intents ADD capability_token_hash VARCHAR(64) NULL;
