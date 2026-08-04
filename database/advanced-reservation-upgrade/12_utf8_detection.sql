-- Detect records that look like broken UTF-8/mojibake.
-- This script only reports suspicious rows; it does not modify data.

SELECT 'reservations.customer_name' AS source_name, id, customer_name AS value
FROM dbo.reservations
WHERE customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
   OR customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Ã%'
   OR customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Â%'
   OR customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%'
   OR customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%'
   OR customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%'
   OR customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';

SELECT 'products.name' AS source_name, id, name AS value
FROM dbo.products
WHERE name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Ã%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Â%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';

SELECT 'categories.name' AS source_name, id, name AS value
FROM dbo.categories
WHERE name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Ã%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Â%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%'
   OR name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%';
