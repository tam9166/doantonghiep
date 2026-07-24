/*
 * Orders created from the current dine-in flow use restaurant_table.
 * The legacy FK pointed to dining_tables, causing a constraint violation
 * whenever checkout assigned the selected table to an order.
 */
IF EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_orders_table' AND parent_object_id = OBJECT_ID('dbo.Orders'))
BEGIN
    ALTER TABLE dbo.Orders DROP CONSTRAINT fk_orders_table;
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'table_id' AND system_type_id <> TYPE_ID('int')
)
BEGIN
    ALTER TABLE dbo.Orders ALTER COLUMN table_id INT NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'fk_orders_restaurant_table' AND parent_object_id = OBJECT_ID('dbo.Orders'))
BEGIN
    ALTER TABLE dbo.Orders
        ADD CONSTRAINT fk_orders_restaurant_table
        FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id);
END;
GO
