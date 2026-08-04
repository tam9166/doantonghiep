USE RestaurantDB;
GO

SELECT 'reservations' AS object_name, COUNT(*) AS row_count FROM dbo.reservations
UNION ALL SELECT 'reservation_preorder_items', COUNT(*) FROM dbo.reservation_preorder_items
UNION ALL SELECT 'payment_intents', COUNT(*) FROM dbo.payment_intents;
GO

SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME IN ('reservations', 'reservation_preorder_items', 'payment_intents')
  AND COLUMN_NAME IN ('preorder_enabled', 'table_amount', 'food_amount', 'payment_option', 'payment_status', 'product_name', 'note', 'account_holder', 'transfer_content', 'qr_url')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
GO
