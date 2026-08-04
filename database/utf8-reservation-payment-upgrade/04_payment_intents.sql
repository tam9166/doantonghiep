USE RestaurantDB;
GO

IF OBJECT_ID(N'dbo.payment_intents', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_intents (
        id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_payment_intents PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        payment_code VARCHAR(40) NOT NULL,
        payment_option VARCHAR(30) NOT NULL,
        status VARCHAR(30) NOT NULL CONSTRAINT DF_payment_intents_status DEFAULT 'PENDING',
        amount DECIMAL(18,0) NOT NULL,
        bank_code VARCHAR(20) NOT NULL,
        account_number VARCHAR(40) NOT NULL,
        account_holder NVARCHAR(150) NOT NULL,
        transfer_content NVARCHAR(120) NOT NULL,
        qr_url NVARCHAR(1000) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT DF_payment_intents_created_at DEFAULT SYSUTCDATETIME(),
        paid_at DATETIME2 NULL,
        confirmed_by VARCHAR(80) NULL,
        bank_transaction_code VARCHAR(80) NULL,
        note NVARCHAR(500) NULL
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_payment_intents_reservations')
BEGIN
    ALTER TABLE dbo.payment_intents
    ADD CONSTRAINT FK_payment_intents_reservations
    FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id);
END;
GO
