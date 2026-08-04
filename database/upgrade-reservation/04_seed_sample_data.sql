USE RestaurantDB;
GO

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

MERGE dbo.table_areas AS target
USING (VALUES
    ('INDOOR', N'Khu trong nhà', N'Indoor dining', N'Không gian mát mẻ, phù hợp gia đình và tiếp khách.', N'Comfortable indoor area for families and business meals.', N'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1200&q=80', 100000, 40, 1),
    ('GARDEN', N'Sân vườn', N'Garden view', N'Không gian mở, gần cây xanh và ánh sáng tự nhiên.', N'Open garden seating with natural light.', N'https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=1200&q=80', 150000, 30, 2),
    ('VIP', N'Phòng riêng', N'Private room', N'Không gian riêng tư cho sinh nhật, tiếp khách và sự kiện nhỏ.', N'Private room for birthdays, meetings and small events.', N'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1200&q=80', 300000, 20, 3)
) AS src(code, name_vi, name_en, description_vi, description_en, image_url, base_price, capacity, display_order)
ON target.code = src.code
WHEN MATCHED THEN
    UPDATE SET name_vi = src.name_vi, name_en = src.name_en, description_vi = src.description_vi,
               description_en = src.description_en, image_url = src.image_url, base_price = src.base_price,
               capacity = src.capacity, display_order = src.display_order, status = 'ACTIVE', updated_at = SYSUTCDATETIME()
WHEN NOT MATCHED THEN
    INSERT (code, name_vi, name_en, description_vi, description_en, image_url, base_price, capacity, status, display_order)
    VALUES (src.code, src.name_vi, src.name_en, src.description_vi, src.description_en, src.image_url, src.base_price, src.capacity, 'ACTIVE', src.display_order);
GO

IF OBJECT_ID(N'dbo.dining_tables', N'U') IS NOT NULL
BEGIN
    SET IDENTITY_INSERT dbo.restaurant_table ON;

    INSERT INTO dbo.restaurant_table (
        id, name, floor, is_occupied, has_view, reserved_time, capacity, view_type,
        min_capacity, max_capacity, seat_count, reservation_price, area_id,
        position_description, is_window_seat, is_private_room, is_child_friendly, is_active, image_url
    )
    SELECT
        CONVERT(INT, dt.id),
        CONVERT(VARCHAR(255), dt.code),
        CONVERT(VARCHAR(255), dt.area),
        CASE
            WHEN dt.status = N'TRONG' THEN 0
            WHEN dt.status = N'DA_DAT' THEN 1
            WHEN dt.status = N'CAN_DON' THEN 3
            ELSE 2
        END,
        CASE WHEN dt.area LIKE N'%VIP%' OR dt.area LIKE N'%Ngoài%' THEN 1 ELSE 0 END,
        CONVERT(VARCHAR(255), dt.status),
        COALESCE(dt.seats, 4),
        CONVERT(NVARCHAR(50), dt.area),
        CASE WHEN COALESCE(dt.seats, 4) <= 2 THEN 1 WHEN COALESCE(dt.seats, 4) <= 4 THEN 2 WHEN COALESCE(dt.seats, 4) <= 6 THEN 4 ELSE 6 END,
        COALESCE(dt.seats, 4),
        COALESCE(dt.seats, 4),
        COALESCE(dt.seats, 4) * 100000,
        NULL,
        CONVERT(NVARCHAR(255), dt.area),
        CASE WHEN dt.area LIKE N'%Ngoài%' THEN 1 ELSE 0 END,
        CASE WHEN dt.area LIKE N'%VIP%' THEN 1 ELSE 0 END,
        1,
        1,
        NULL
    FROM dbo.dining_tables dt
    WHERE NOT EXISTS (SELECT 1 FROM dbo.restaurant_table rt WHERE rt.id = CONVERT(INT, dt.id));

    SET IDENTITY_INSERT dbo.restaurant_table OFF;
END;
GO

;WITH mapped AS (
    SELECT rt.id,
           CASE
               WHEN LOWER(COALESCE(rt.floor, '')) LIKE '%vip%' THEN 'VIP'
               WHEN LOWER(COALESCE(rt.floor, '')) LIKE '%sân%' OR LOWER(COALESCE(rt.floor, '')) LIKE '%ngoài%' OR LOWER(COALESCE(rt.floor, '')) LIKE '%garden%' OR LOWER(COALESCE(rt.floor, '')) LIKE '%outdoor%' THEN 'GARDEN'
               ELSE 'INDOOR'
           END AS area_code
    FROM dbo.restaurant_table rt
)
UPDATE rt
SET area_id = ta.id,
    image_url = COALESCE(rt.image_url, ta.image_url),
    reservation_price = COALESCE(NULLIF(rt.reservation_price, 0), ta.base_price + COALESCE(rt.capacity, 4) * 50000),
    position_description = COALESCE(rt.position_description, ta.name_vi)
FROM dbo.restaurant_table rt
JOIN mapped m ON m.id = rt.id
JOIN dbo.table_areas ta ON ta.code = m.area_code;
GO
