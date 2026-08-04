SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

BEGIN TRY
    BEGIN TRAN;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_payment_webhook_provider_tx' AND object_id = OBJECT_ID('dbo.payment_webhook_logs'))
        CREATE UNIQUE INDEX UX_payment_webhook_provider_tx ON dbo.payment_webhook_logs(provider, provider_transaction_id);

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_payment_intents_bank_tx' AND object_id = OBJECT_ID('dbo.payment_intents'))
        CREATE UNIQUE INDEX UX_payment_intents_bank_tx ON dbo.payment_intents(bank_transaction_code) WHERE bank_transaction_code IS NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_table_date_status' AND object_id = OBJECT_ID('dbo.reservations'))
        CREATE INDEX IX_reservations_table_date_status ON dbo.reservations(table_id, reservation_date, reservation_status);

    IF OBJECT_ID('dbo.deposit_policies', 'U') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_deposit_policies_code' AND object_id = OBJECT_ID('dbo.deposit_policies'))
        CREATE UNIQUE INDEX UX_deposit_policies_code ON dbo.deposit_policies(policy_code);

    IF OBJECT_ID('dbo.notification_channels', 'U') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_notification_channels_code' AND object_id = OBJECT_ID('dbo.notification_channels'))
        CREATE UNIQUE INDEX UX_notification_channels_code ON dbo.notification_channels(channel_code);

    IF OBJECT_ID('dbo.notification_templates', 'U') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_notification_templates_code_locale' AND object_id = OBJECT_ID('dbo.notification_templates'))
        CREATE UNIQUE INDEX UX_notification_templates_code_locale ON dbo.notification_templates(template_code, locale);

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
