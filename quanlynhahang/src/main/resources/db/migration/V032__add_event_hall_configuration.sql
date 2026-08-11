ALTER TABLE dbo.table_areas ADD area_type VARCHAR(30) NOT NULL CONSTRAINT df_table_areas_area_type DEFAULT 'DINING';
ALTER TABLE dbo.table_areas ADD min_guest_count INT NOT NULL CONSTRAINT df_table_areas_min_guest_count DEFAULT 1;
ALTER TABLE dbo.table_areas ADD max_guest_count INT NOT NULL CONSTRAINT df_table_areas_max_guest_count DEFAULT 1000;
ALTER TABLE dbo.table_areas ADD min_booking_hours INT NOT NULL CONSTRAINT df_table_areas_min_booking_hours DEFAULT 2;
ALTER TABLE dbo.table_areas ADD hourly_rate DECIMAL(18,0) NOT NULL CONSTRAINT df_table_areas_hourly_rate DEFAULT 0;
ALTER TABLE dbo.table_areas ADD package_price DECIMAL(18,0) NOT NULL CONSTRAINT df_table_areas_package_price DEFAULT 0;
GO
