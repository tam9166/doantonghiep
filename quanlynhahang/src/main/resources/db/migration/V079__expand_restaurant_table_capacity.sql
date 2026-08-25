DECLARE @IndoorAreaId INT = (SELECT TOP 1 id FROM dbo.table_areas
    WHERE name_en LIKE N'%Banquet%' OR name_en LIKE N'%Indoor%' OR name_vi LIKE N'%Trong nhà%' ORDER BY id);
DECLARE @GardenAreaId INT = (SELECT TOP 1 id FROM dbo.table_areas
    WHERE name_en LIKE N'%Rooftop%' OR name_en LIKE N'%Garden%' OR name_vi LIKE N'%Sân vườn%' OR name_vi LIKE N'%Sân thượng%' ORDER BY id);
DECLARE @VipAreaId INT = (SELECT TOP 1 id FROM dbo.table_areas
    WHERE name_en LIKE N'%Private%' OR name_en LIKE N'%VIP%' OR name_vi LIKE N'%VIP%' ORDER BY id);

IF @IndoorAreaId IS NULL OR @GardenAreaId IS NULL OR @VipAreaId IS NULL
    THROW 51000, 'V079 requires the canonical indoor, garden and VIP table areas.', 1;

UPDATE dbo.table_areas
SET name_vi = CASE id WHEN @IndoorAreaId THEN N'Khu trong nhà' WHEN @VipAreaId THEN N'Phòng riêng / VIP' WHEN @GardenAreaId THEN N'Sân vườn / Ngoài trời' END,
    name_en = CASE id WHEN @IndoorAreaId THEN N'Indoor Dining' WHEN @VipAreaId THEN N'Private / VIP' WHEN @GardenAreaId THEN N'Garden / Outdoor' END,
    capacity = CASE id WHEN @IndoorAreaId THEN 100 WHEN @VipAreaId THEN 50 WHEN @GardenAreaId THEN 70 END,
    updated_at = SYSDATETIME()
WHERE id IN (@IndoorAreaId, @VipAreaId, @GardenAreaId);

-- Normalize the original twenty business tables so blank and legacy databases
-- reach the same area totals without changing any table primary key or order link.
DECLARE @CanonicalTables TABLE (
    table_number INT NOT NULL,
    business_name NVARCHAR(20) NOT NULL,
    floor NVARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    area_id INT NOT NULL
);

INSERT INTO @CanonicalTables(table_number, business_name, floor, capacity, area_id)
VALUES
    (1, N'B01', N'Tầng 1', 4, @IndoorAreaId), (2, N'B02', N'Tầng 1', 4, @IndoorAreaId),
    (3, N'B03', N'Tầng 1', 4, @IndoorAreaId), (4, N'B04', N'Tầng 1', 4, @IndoorAreaId),
    (5, N'B05', N'Tầng 1', 4, @IndoorAreaId), (6, N'B06', N'Tầng 1', 4, @IndoorAreaId),
    (7, N'B07', N'Tầng 1', 4, @IndoorAreaId), (8, N'B08', N'Tầng 1', 4, @IndoorAreaId),
    (9, N'B09', N'Tầng 1', 4, @IndoorAreaId), (10, N'B10', N'Tầng 1', 4, @IndoorAreaId),
    (11, N'B11', N'Phòng VIP', 8, @VipAreaId), (12, N'B12', N'Phòng VIP', 8, @VipAreaId),
    (13, N'B13', N'Phòng VIP', 8, @VipAreaId), (14, N'B14', N'Phòng VIP', 8, @VipAreaId),
    (15, N'B15', N'Ngoài trời', 4, @GardenAreaId), (16, N'B16', N'Ngoài trời', 4, @GardenAreaId),
    (17, N'B17', N'Ngoài trời', 4, @GardenAreaId), (18, N'B18', N'Ngoài trời', 4, @GardenAreaId),
    (19, N'B19', N'Tầng 2', 6, @IndoorAreaId), (20, N'B20', N'Tầng 2', 8, @IndoorAreaId);

UPDATE target
SET target.name = source.business_name,
    target.floor = source.floor,
    target.capacity = source.capacity,
    target.max_capacity = source.capacity,
    target.seat_count = source.capacity,
    target.area_id = source.area_id,
    target.display_order = source.table_number
FROM dbo.restaurant_table target
JOIN @CanonicalTables source
  ON target.name = source.business_name
  OR target.name = N'Bàn ' + RIGHT(N'0' + CONVERT(NVARCHAR(2), source.table_number), 2);

DECLARE @NewTables TABLE (
    name NVARCHAR(100) NOT NULL,
    floor NVARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    area_id INT NOT NULL,
    display_order INT NOT NULL,
    private_room BIT NOT NULL,
    has_view BIT NOT NULL,
    view_type NVARCHAR(50) NOT NULL
);

INSERT INTO @NewTables(name, floor, capacity, area_id, display_order, private_room, has_view, view_type)
VALUES
    (N'B21', N'Tầng 1', 2, @IndoorAreaId, 21, 0, 0, N'Tiêu chuẩn'),
    (N'B22', N'Tầng 1', 4, @IndoorAreaId, 22, 0, 0, N'Tiêu chuẩn'),
    (N'B23', N'Tầng 1', 4, @IndoorAreaId, 23, 0, 1, N'Cửa sổ'),
    (N'B24', N'Tầng 1', 6, @IndoorAreaId, 24, 0, 0, N'Tiêu chuẩn'),
    (N'B25', N'Tầng 2', 6, @IndoorAreaId, 25, 0, 0, N'Tiêu chuẩn'),
    (N'B26', N'Tầng 2', 8, @IndoorAreaId, 26, 0, 1, N'Cửa sổ'),
    (N'B27', N'Tầng 2', 8, @IndoorAreaId, 27, 0, 0, N'Tiêu chuẩn'),
    (N'B28', N'Tầng 2', 8, @IndoorAreaId, 28, 0, 0, N'Tiêu chuẩn'),
    (N'B29', N'Ngoài trời', 4, @GardenAreaId, 29, 0, 1, N'Sân vườn'),
    (N'B30', N'Ngoài trời', 4, @GardenAreaId, 30, 0, 1, N'Sân vườn'),
    (N'B31', N'Ngoài trời', 4, @GardenAreaId, 31, 0, 1, N'Sân vườn'),
    (N'B32', N'Ngoài trời', 6, @GardenAreaId, 32, 0, 1, N'Sân vườn'),
    (N'B33', N'Ngoài trời', 6, @GardenAreaId, 33, 0, 1, N'Sân vườn'),
    (N'B34', N'Ngoài trời', 6, @GardenAreaId, 34, 0, 1, N'Sân vườn'),
    (N'B35', N'Ngoài trời', 8, @GardenAreaId, 35, 0, 1, N'Sân vườn'),
    (N'B36', N'Ngoài trời', 8, @GardenAreaId, 36, 0, 1, N'Sân vườn'),
    (N'B37', N'Ngoài trời', 8, @GardenAreaId, 37, 0, 1, N'Sân vườn'),
    (N'VIP01', N'Phòng VIP', 4, @VipAreaId, 38, 1, 0, N'Phòng riêng'),
    (N'VIP02', N'Phòng VIP', 6, @VipAreaId, 39, 1, 0, N'Phòng riêng'),
    (N'VIP03', N'Phòng VIP', 8, @VipAreaId, 40, 1, 0, N'Phòng riêng');

INSERT INTO dbo.restaurant_table(
    name, floor, is_occupied, has_view, reserved_time, capacity, view_type,
    min_capacity, max_capacity, seat_count, reservation_price, area_id,
    position_description, is_window_seat, is_private_room, is_child_friendly,
    is_active, image_url, version, display_order, notes)
SELECT source.name, source.floor, 0, source.has_view, NULL, source.capacity, source.view_type,
       1, source.capacity, source.capacity, 0, source.area_id,
       NULL, source.has_view, source.private_room, 1,
       1, NULL, 0, source.display_order, N'Bổ sung theo năng lực phục vụ khu vực'
FROM @NewTables source
WHERE NOT EXISTS (SELECT 1 FROM dbo.restaurant_table target WHERE target.name = source.name);
GO
