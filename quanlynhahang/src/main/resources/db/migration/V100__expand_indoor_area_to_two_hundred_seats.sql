DECLARE @IndoorAreaId INT = (
    SELECT TOP 1 id
    FROM dbo.table_areas
    WHERE name_en = N'Indoor Dining' OR name_vi = N'Khu trong nhà'
    ORDER BY id
);

IF @IndoorAreaId IS NULL
    THROW 51000, 'V100 requires the canonical indoor dining area.', 1;

UPDATE dbo.table_areas
SET capacity = 200,
    updated_at = SYSDATETIME()
WHERE id = @IndoorAreaId;

DECLARE @IndoorTables TABLE (
    name NVARCHAR(100) NOT NULL,
    floor NVARCHAR(100) NOT NULL,
    capacity INT NOT NULL,
    display_order INT NOT NULL
);

INSERT INTO @IndoorTables(name, floor, capacity, display_order)
VALUES
    (N'B41', N'Tầng 1', 10, 41),
    (N'B42', N'Tầng 1', 10, 42),
    (N'B43', N'Tầng 1', 10, 43),
    (N'B44', N'Tầng 1', 10, 44),
    (N'B45', N'Tầng 1', 10, 45),
    (N'B46', N'Tầng 2', 10, 46),
    (N'B47', N'Tầng 2', 10, 47),
    (N'B48', N'Tầng 2', 10, 48),
    (N'B49', N'Tầng 2', 10, 49),
    (N'B50', N'Tầng 2', 10, 50);

INSERT INTO dbo.restaurant_table(
    name, floor, is_occupied, has_view, reserved_time, capacity, view_type,
    min_capacity, max_capacity, seat_count, reservation_price, area_id,
    position_description, is_window_seat, is_private_room, is_child_friendly,
    is_active, image_url, version, display_order, notes)
SELECT source.name, source.floor, 0, 0, NULL, source.capacity, N'Tiêu chuẩn',
       1, source.capacity, source.capacity, 0, @IndoorAreaId,
       N'Bàn nhóm bổ sung cho khu trong nhà', 0, 0, 1,
       1, NULL, 0, source.display_order, N'Bổ sung theo sức chứa 200 khách'
FROM @IndoorTables source
WHERE NOT EXISTS (
    SELECT 1 FROM dbo.restaurant_table target WHERE target.name = source.name
);
