IF COL_LENGTH('dbo.Accounts', 'must_change_password') IS NULL
BEGIN
    ALTER TABLE dbo.Accounts ADD must_change_password BIT NOT NULL
        CONSTRAINT df_accounts_must_change_password DEFAULT 0;
END;
