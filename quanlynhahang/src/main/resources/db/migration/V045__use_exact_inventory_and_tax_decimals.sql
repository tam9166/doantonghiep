ALTER TABLE dbo.ingredients ALTER COLUMN quantity DECIMAL(19, 4) NULL;
ALTER TABLE dbo.ingredients ALTER COLUMN min_stock DECIMAL(19, 4) NULL;
ALTER TABLE dbo.ingredient_batches ALTER COLUMN quantity DECIMAL(19, 4) NULL;
ALTER TABLE dbo.recipes ALTER COLUMN amount_required DECIMAL(19, 4) NULL;

DECLARE @products_tax_default SYSNAME;
DECLARE @drop_constraint_sql NVARCHAR(500);
SELECT @products_tax_default = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID('dbo.products') AND c.name = 'tax_rate';
IF @products_tax_default IS NOT NULL
BEGIN
    SET @drop_constraint_sql = N'ALTER TABLE dbo.products DROP CONSTRAINT '
            + QUOTENAME(@products_tax_default);
    EXEC sys.sp_executesql @drop_constraint_sql;
END;

DECLARE @order_details_tax_default SYSNAME;
SELECT @order_details_tax_default = dc.name
FROM sys.default_constraints dc
JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
WHERE dc.parent_object_id = OBJECT_ID('dbo.order_details') AND c.name = 'tax_rate';
IF @order_details_tax_default IS NOT NULL
BEGIN
    SET @drop_constraint_sql = N'ALTER TABLE dbo.order_details DROP CONSTRAINT '
            + QUOTENAME(@order_details_tax_default);
    EXEC sys.sp_executesql @drop_constraint_sql;
END;

ALTER TABLE dbo.products ALTER COLUMN tax_rate DECIMAL(5, 2) NULL;
ALTER TABLE dbo.order_details ALTER COLUMN tax_rate DECIMAL(5, 2) NULL;
ALTER TABLE dbo.products ADD CONSTRAINT DF_products_tax_rate_exact DEFAULT (8.00) FOR tax_rate;
ALTER TABLE dbo.order_details ADD CONSTRAINT DF_order_details_tax_rate_exact DEFAULT (8.00) FOR tax_rate;
