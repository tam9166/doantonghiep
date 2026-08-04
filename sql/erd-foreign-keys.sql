/* Run in SQL Server Management Studio after selecting RestaurantDB.
   The result is an ERD-ready list of all foreign-key relationships. */
SELECT
    fk.name AS foreign_key_name,
    SCHEMA_NAME(parent_table.schema_id) AS schema_name,
    parent_table.name AS child_table,
    parent_column.name AS child_column,
    referenced_table.name AS parent_table,
    referenced_column.name AS parent_column
FROM sys.foreign_keys AS fk
JOIN sys.foreign_key_columns AS fkc ON fkc.constraint_object_id = fk.object_id
JOIN sys.tables AS parent_table ON parent_table.object_id = fkc.parent_object_id
JOIN sys.columns AS parent_column
    ON parent_column.object_id = fkc.parent_object_id AND parent_column.column_id = fkc.parent_column_id
JOIN sys.tables AS referenced_table ON referenced_table.object_id = fkc.referenced_object_id
JOIN sys.columns AS referenced_column
    ON referenced_column.object_id = fkc.referenced_object_id AND referenced_column.column_id = fkc.referenced_column_id
ORDER BY child_table, fk.name, fkc.constraint_column_id;
