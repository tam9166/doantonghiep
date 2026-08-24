IF OBJECT_ID('area_pricing', 'U') IS NULL
BEGIN
    CREATE TABLE area_pricing (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        area_id INT NOT NULL,
        room_fee DECIMAL(18,0) NOT NULL CONSTRAINT DF_area_pricing_room_fee DEFAULT 0,
        minimum_spend DECIMAL(18,0) NOT NULL CONSTRAINT DF_area_pricing_minimum_spend DEFAULT 0,
        active BIT NOT NULL CONSTRAINT DF_area_pricing_active DEFAULT 1,
        CONSTRAINT UQ_area_pricing_area UNIQUE (area_id),
        CONSTRAINT FK_area_pricing_area FOREIGN KEY (area_id) REFERENCES table_areas(id)
    );
END;

IF OBJECT_ID('table_areas', 'U') IS NOT NULL
BEGIN
    INSERT INTO area_pricing(area_id, room_fee, minimum_spend, active)
    SELECT a.id, COALESCE(a.base_price, 0), 0, 1
      FROM table_areas a
     WHERE a.area_type = 'PRIVATE_ROOM'
       AND NOT EXISTS (SELECT 1 FROM area_pricing p WHERE p.area_id = a.id);

    UPDATE table_areas SET base_price = 0 WHERE base_price IS NULL OR base_price <> 0;
END;
