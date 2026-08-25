IF COL_LENGTH('dbo.vouchers', 'active') IS NULL
    ALTER TABLE dbo.vouchers ADD active BIT NOT NULL CONSTRAINT DF_vouchers_active DEFAULT 1;
IF COL_LENGTH('dbo.vouchers', 'usage_limit') IS NULL
    ALTER TABLE dbo.vouchers ADD usage_limit INT NULL;
IF COL_LENGTH('dbo.vouchers', 'used_count') IS NULL
    ALTER TABLE dbo.vouchers ADD used_count INT NOT NULL CONSTRAINT DF_vouchers_used_count DEFAULT 0;
IF COL_LENGTH('dbo.vouchers', 'start_date') IS NULL
    ALTER TABLE dbo.vouchers ADD start_date DATETIME2 NULL;
ELSE
    EXEC('ALTER TABLE dbo.vouchers ALTER COLUMN start_date DATETIME2 NULL');
IF COL_LENGTH('dbo.vouchers', 'end_date') IS NULL
    ALTER TABLE dbo.vouchers ADD end_date DATETIME2 NULL;
ELSE
    EXEC('ALTER TABLE dbo.vouchers ALTER COLUMN end_date DATETIME2 NULL');
GO

UPDATE dbo.vouchers
SET usage_limit = COALESCE(usage_limit, 1),
    used_count = CASE WHEN is_used = 1 THEN 1 ELSE COALESCE(used_count, 0) END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name IN ('CK_vouchers_usage_limit', 'CK_vouchers_usage_limit_positive'))
    ALTER TABLE dbo.vouchers ADD CONSTRAINT CK_vouchers_usage_limit CHECK (usage_limit IS NULL OR usage_limit > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name IN ('CK_vouchers_used_count', 'CK_vouchers_used_count_non_negative'))
    ALTER TABLE dbo.vouchers ADD CONSTRAINT CK_vouchers_used_count CHECK (used_count >= 0 AND (usage_limit IS NULL OR used_count <= usage_limit));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_vouchers_used_not_over_limit')
    ALTER TABLE dbo.vouchers ADD CONSTRAINT CK_vouchers_used_not_over_limit CHECK (usage_limit IS NULL OR used_count <= usage_limit);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name IN ('CK_vouchers_time_window', 'CK_vouchers_date_range'))
    ALTER TABLE dbo.vouchers ADD CONSTRAINT CK_vouchers_time_window CHECK (start_date IS NULL OR end_date IS NULL OR start_date < end_date);
