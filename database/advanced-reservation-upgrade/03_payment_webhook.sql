BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.payment_webhook_logs', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.payment_webhook_logs (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            provider varchar(40) NOT NULL,
            provider_transaction_id varchar(120) NOT NULL,
            payment_code varchar(40) NULL,
            transfer_content nvarchar(200) NULL,
            amount decimal(18,2) NULL,
            account_number varchar(40) NULL,
            signature_valid bit NOT NULL CONSTRAINT DF_payment_webhook_signature DEFAULT(0),
            status varchar(30) NOT NULL,
            raw_payload_hash varchar(128) NULL,
            failure_reason nvarchar(500) NULL,
            received_at datetime2 NOT NULL CONSTRAINT DF_payment_webhook_received DEFAULT SYSUTCDATETIME(),
            processed_at datetime2 NULL
        );
    END;

    IF COL_LENGTH('dbo.payment_intents', 'bank_transaction_code') IS NULL
        ALTER TABLE dbo.payment_intents ADD bank_transaction_code varchar(80) NULL;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
