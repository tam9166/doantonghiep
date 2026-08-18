IF OBJECT_ID('restaurant_settings', 'U') IS NULL
BEGIN
    CREATE TABLE restaurant_settings (
        setting_key varchar(80) NOT NULL PRIMARY KEY,
        setting_value nvarchar(1000) NOT NULL,
        description nvarchar(500) NULL,
        version bigint NOT NULL CONSTRAINT DF_restaurant_settings_version DEFAULT 0
    );
END;

IF NOT EXISTS (SELECT 1 FROM restaurant_settings WHERE setting_key = 'large_party_threshold')
    INSERT INTO restaurant_settings(setting_key, setting_value, description)
    VALUES ('large_party_threshold', '10', N'Số khách tối thiểu cần nhân viên bố trí/ghép bàn');

IF NOT EXISTS (SELECT 1 FROM restaurant_settings WHERE setting_key = 'restaurant_max_capacity')
    INSERT INTO restaurant_settings(setting_key, setting_value, description)
    VALUES ('restaurant_max_capacity', '200', N'Sức chứa tối đa của nhà hàng trong cùng khung giờ');

IF COL_LENGTH('restaurant_table', 'display_order') IS NULL
    ALTER TABLE restaurant_table ADD display_order int NOT NULL CONSTRAINT DF_restaurant_table_display_order DEFAULT 0;

IF COL_LENGTH('restaurant_table', 'notes') IS NULL
    ALTER TABLE restaurant_table ADD notes nvarchar(500) NULL;

EXEC(N'UPDATE restaurant_table SET display_order = id WHERE display_order = 0');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_restaurant_table_auto_assignment'
               AND object_id = OBJECT_ID('restaurant_table'))
    EXEC(N'CREATE INDEX IX_restaurant_table_auto_assignment
           ON restaurant_table(area_id, is_active, max_capacity, display_order)');
