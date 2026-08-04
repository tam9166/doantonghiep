-- Preserve Vietnamese floor labels instead of replacing accented characters with '?'.
IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON c.user_type_id = t.user_type_id
    WHERE c.object_id = OBJECT_ID('dbo.restaurant_table')
      AND c.name = 'floor'
      AND t.name = 'varchar'
)
BEGIN
    ALTER TABLE dbo.restaurant_table ALTER COLUMN floor NVARCHAR(255) NULL;
END;

UPDATE dbo.restaurant_table
SET floor = NCHAR(84) + NCHAR(7847) + N'ng 1'
WHERE floor = N'T?ng 1';

UPDATE dbo.restaurant_table
SET floor = NCHAR(84) + NCHAR(7847) + N'ng 2'
WHERE floor = N'T?ng 2';

UPDATE dbo.restaurant_table
SET floor = N'Ngo' + NCHAR(224) + N'i tr' + NCHAR(7901) + N'i'
WHERE floor = N'Ngo' + NCHAR(224) + N'i tr?i';
