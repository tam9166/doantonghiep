IF OBJECT_ID(N'dbo.restaurant_table', N'U') IS NOT NULL
   AND EXISTS (
       SELECT 1
       FROM sys.columns c
       JOIN sys.types t ON t.user_type_id = c.user_type_id
       WHERE c.object_id = OBJECT_ID(N'dbo.restaurant_table')
         AND c.name = N'reserved_time'
         AND t.name <> N'varchar'
   )
BEGIN
    ALTER TABLE dbo.restaurant_table ALTER COLUMN reserved_time VARCHAR(255) NULL;
END;
