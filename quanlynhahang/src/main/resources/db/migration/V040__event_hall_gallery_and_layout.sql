IF COL_LENGTH('table_areas','gallery_json') IS NULL ALTER TABLE table_areas ADD gallery_json NVARCHAR(MAX) NULL;
IF COL_LENGTH('table_areas','max_tables') IS NULL ALTER TABLE table_areas ADD max_tables INT NULL;
IF COL_LENGTH('table_areas','default_guests_per_table') IS NULL ALTER TABLE table_areas ADD default_guests_per_table INT NOT NULL CONSTRAINT df_area_guests_table DEFAULT 10;
IF COL_LENGTH('table_areas','suitable_event_types') IS NULL ALTER TABLE table_areas ADD suitable_event_types NVARCHAR(MAX) NULL;
