IF COL_LENGTH('dbo.products', 'cost_price') IS NULL
BEGIN
    ALTER TABLE dbo.products ADD cost_price DECIMAL(18,2) NULL;
END;

DECLARE @dropOrderDetailTaxDefault NVARCHAR(MAX) = (
    SELECT 'ALTER TABLE dbo.order_details DROP CONSTRAINT ' + QUOTENAME(dc.name)
    FROM sys.default_constraints dc
    JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
    JOIN sys.tables t ON t.object_id = dc.parent_object_id
    WHERE t.name = 'order_details' AND c.name = 'tax_amount'
);
IF @dropOrderDetailTaxDefault IS NOT NULL EXEC sp_executesql @dropOrderDetailTaxDefault;

DECLARE @dropProductCostDefault NVARCHAR(MAX) = (
    SELECT 'ALTER TABLE dbo.products DROP CONSTRAINT ' + QUOTENAME(dc.name)
    FROM sys.default_constraints dc
    JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id
    JOIN sys.tables t ON t.object_id = dc.parent_object_id
    WHERE t.name = 'products' AND c.name = 'cost_price'
);
IF @dropProductCostDefault IS NOT NULL EXEC sp_executesql @dropProductCostDefault;

ALTER TABLE dbo.orders ALTER COLUMN sub_total DECIMAL(18,2) NULL;
ALTER TABLE dbo.orders ALTER COLUMN tax_amount DECIMAL(18,2) NULL;
ALTER TABLE dbo.orders ALTER COLUMN total_amount DECIMAL(18,2) NULL;
ALTER TABLE dbo.orders ALTER COLUMN deposit DECIMAL(18,2) NULL;
ALTER TABLE dbo.order_details ALTER COLUMN price DECIMAL(18,2) NULL;
ALTER TABLE dbo.order_details ALTER COLUMN tax_amount DECIMAL(18,2) NULL;
ALTER TABLE dbo.products ALTER COLUMN price DECIMAL(18,2) NOT NULL;
ALTER TABLE dbo.products ALTER COLUMN cost_price DECIMAL(18,2) NULL;
ALTER TABLE dbo.ingredients ALTER COLUMN unit_price DECIMAL(18,2) NULL;
ALTER TABLE dbo.ingredient_batches ALTER COLUMN unit_price DECIMAL(18,2) NULL;
ALTER TABLE dbo.import_invoices ALTER COLUMN total_amount DECIMAL(18,2) NULL;

ALTER TABLE dbo.order_details ADD CONSTRAINT DF_order_details_tax_amount DEFAULT (0) FOR tax_amount;
ALTER TABLE dbo.products ADD CONSTRAINT DF_products_cost_price DEFAULT (0) FOR cost_price;
