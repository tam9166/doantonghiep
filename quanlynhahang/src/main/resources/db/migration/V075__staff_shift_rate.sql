IF COL_LENGTH('dbo.Accounts', 'shift_rate') IS NULL
BEGIN
    ALTER TABLE dbo.Accounts ADD shift_rate DECIMAL(19, 2) NULL;
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints WHERE name = 'CK_accounts_shift_rate_positive'
)
BEGIN
    ALTER TABLE dbo.Accounts ADD CONSTRAINT CK_accounts_shift_rate_positive
        CHECK (shift_rate IS NULL OR shift_rate > 0);
END;
