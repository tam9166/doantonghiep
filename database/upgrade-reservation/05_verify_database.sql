USE RestaurantDB;
GO

SELECT 'table_areas' AS object_name, COUNT(*) AS row_count FROM dbo.table_areas
UNION ALL
SELECT 'restaurant_table', COUNT(*) FROM dbo.restaurant_table
UNION ALL
SELECT 'reservations', COUNT(*) FROM dbo.reservations
UNION ALL
SELECT 'reservation_status_history', COUNT(*) FROM dbo.reservation_status_history;
GO

SELECT TOP 20 id, name, capacity, min_capacity, max_capacity, reservation_price, area_id, is_active
FROM dbo.restaurant_table
ORDER BY id;
GO

SELECT TOP 20 id, code, name_vi, name_en, base_price, capacity, status
FROM dbo.table_areas
ORDER BY display_order, id;
GO
