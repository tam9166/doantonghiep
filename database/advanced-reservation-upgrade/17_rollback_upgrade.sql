-- Rollback only objects introduced by this upgrade package.
BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.v_customer_reservation_history', 'V') IS NOT NULL DROP VIEW dbo.v_customer_reservation_history;
    IF OBJECT_ID('dbo.reservation_reviews', 'U') IS NOT NULL DROP TABLE dbo.reservation_reviews;
    IF OBJECT_ID('dbo.reservation_voucher_usages', 'U') IS NOT NULL DROP TABLE dbo.reservation_voucher_usages;
    IF OBJECT_ID('dbo.notification_templates', 'U') IS NOT NULL DROP TABLE dbo.notification_templates;
    IF OBJECT_ID('dbo.notification_channels', 'U') IS NOT NULL DROP TABLE dbo.notification_channels;
    IF OBJECT_ID('dbo.table_suggestion_logs', 'U') IS NOT NULL DROP TABLE dbo.table_suggestion_logs;
    IF OBJECT_ID('dbo.table_layouts', 'U') IS NOT NULL DROP TABLE dbo.table_layouts;
    IF OBJECT_ID('dbo.reservation_events', 'U') IS NOT NULL DROP TABLE dbo.reservation_events;
    IF OBJECT_ID('dbo.payment_webhook_logs', 'U') IS NOT NULL DROP TABLE dbo.payment_webhook_logs;
    IF OBJECT_ID('dbo.deposit_policies', 'U') IS NOT NULL DROP TABLE dbo.deposit_policies;
    IF OBJECT_ID('dbo.utf8_manual_repair_queue', 'U') IS NOT NULL DROP TABLE dbo.utf8_manual_repair_queue;

    IF COL_LENGTH('dbo.reservations', 'last_status_event_at') IS NOT NULL
        ALTER TABLE dbo.reservations DROP COLUMN last_status_event_at;
    IF COL_LENGTH('dbo.reservations', 'deposit_policy_snapshot') IS NOT NULL
        ALTER TABLE dbo.reservations DROP COLUMN deposit_policy_snapshot;
    IF COL_LENGTH('dbo.reservations', 'deposit_policy_code') IS NOT NULL
        ALTER TABLE dbo.reservations DROP COLUMN deposit_policy_code;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
