IF COL_LENGTH('dbo.restaurant_table', 'merged_into_table_id') IS NULL
    EXEC(N'ALTER TABLE dbo.restaurant_table ADD merged_into_table_id INT NULL');

IF COL_LENGTH('dbo.restaurant_table', 'merged_at') IS NULL
    EXEC(N'ALTER TABLE dbo.restaurant_table ADD merged_at DATETIME2 NULL');

IF COL_LENGTH('dbo.restaurant_table', 'merged_by') IS NULL
    EXEC(N'ALTER TABLE dbo.restaurant_table ADD merged_by VARCHAR(50) NULL');

IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID('dbo.restaurant_table')
      AND name = 'FK_restaurant_table_merged_into'
)
    EXEC(N'ALTER TABLE dbo.restaurant_table
        ADD CONSTRAINT FK_restaurant_table_merged_into
        FOREIGN KEY (merged_into_table_id) REFERENCES dbo.restaurant_table(id)');

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID('dbo.restaurant_table')
      AND name = 'IX_restaurant_table_merged_into'
)
    EXEC(N'CREATE INDEX IX_restaurant_table_merged_into
        ON dbo.restaurant_table(merged_into_table_id)
        WHERE merged_into_table_id IS NOT NULL');
