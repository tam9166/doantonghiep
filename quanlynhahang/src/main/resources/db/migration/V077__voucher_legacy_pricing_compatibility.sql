IF COL_LENGTH('dbo.vouchers', 'voucher_name') IS NULL
    ALTER TABLE dbo.vouchers ADD voucher_name NVARCHAR(200) NULL;
IF COL_LENGTH('dbo.vouchers', 'discount_type') IS NULL
    ALTER TABLE dbo.vouchers ADD discount_type VARCHAR(10) NULL;
IF COL_LENGTH('dbo.vouchers', 'discount_value') IS NULL
    ALTER TABLE dbo.vouchers ADD discount_value DECIMAL(18,2) NULL;
GO

UPDATE dbo.vouchers
SET voucher_name = COALESCE(NULLIF(voucher_name, ''), code),
    discount_type = COALESCE(discount_type, 'PERCENT'),
    discount_value = COALESCE(discount_value, CONVERT(DECIMAL(18,2), discount_percent));
GO

EXEC('ALTER TABLE dbo.vouchers ALTER COLUMN voucher_name NVARCHAR(200) NOT NULL');
EXEC('ALTER TABLE dbo.vouchers ALTER COLUMN discount_type VARCHAR(10) NOT NULL');
EXEC('ALTER TABLE dbo.vouchers ALTER COLUMN discount_value DECIMAL(18,2) NOT NULL');
