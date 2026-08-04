SELECT 'payment_webhook_logs' AS object_name, CASE WHEN OBJECT_ID('dbo.payment_webhook_logs', 'U') IS NULL THEN 0 ELSE 1 END AS exists_flag;
SELECT 'table_layouts' AS object_name, CASE WHEN OBJECT_ID('dbo.table_layouts', 'U') IS NULL THEN 0 ELSE 1 END AS exists_flag;
SELECT 'deposit_policies' AS object_name, CASE WHEN OBJECT_ID('dbo.deposit_policies', 'U') IS NULL THEN 0 ELSE 1 END AS exists_flag;
SELECT 'reservation_voucher_usages' AS object_name, CASE WHEN OBJECT_ID('dbo.reservation_voucher_usages', 'U') IS NULL THEN 0 ELSE 1 END AS exists_flag;
SELECT 'reservation_reviews' AS object_name, CASE WHEN OBJECT_ID('dbo.reservation_reviews', 'U') IS NULL THEN 0 ELSE 1 END AS exists_flag;
SELECT 'notification_channels' AS object_name, CASE WHEN OBJECT_ID('dbo.notification_channels', 'U') IS NULL THEN 0 ELSE 1 END AS exists_flag;

SELECT name AS index_name, OBJECT_NAME(object_id) AS table_name
FROM sys.indexes
WHERE name IN ('UX_payment_webhook_provider_tx', 'UX_payment_intents_bank_tx', 'IX_reservations_table_date_status');
