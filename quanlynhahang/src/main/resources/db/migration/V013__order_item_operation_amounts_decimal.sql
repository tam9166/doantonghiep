ALTER TABLE dbo.order_item_operations ALTER COLUMN sub_total DECIMAL(18,2) NOT NULL;
ALTER TABLE dbo.order_item_operations ALTER COLUMN tax_amount DECIMAL(18,2) NOT NULL;
ALTER TABLE dbo.order_item_operations ALTER COLUMN total_amount DECIMAL(18,2) NOT NULL;
