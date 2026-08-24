/*
 * Align the legacy numeric table-state constraint with the states used by the
 * operational UI and services: available, reserved, occupied, cleaning, merged.
 */
DECLARE @constraintName sysname;
DECLARE @dropSql nvarchar(max);

SELECT TOP (1) @constraintName = cc.name
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID(N'dbo.restaurant_table')
  AND cc.definition LIKE N'%is_occupied%';

IF @constraintName IS NOT NULL
BEGIN
    SET @dropSql = N'ALTER TABLE dbo.restaurant_table DROP CONSTRAINT ' + QUOTENAME(@constraintName) + N';';
    EXEC sp_executesql @dropSql;
END;

ALTER TABLE dbo.restaurant_table WITH CHECK
    ADD CONSTRAINT CK_restaurant_table_is_occupied
    CHECK (is_occupied IN (0, 1, 2, 3, 5));

ALTER TABLE dbo.restaurant_table CHECK CONSTRAINT CK_restaurant_table_is_occupied;
