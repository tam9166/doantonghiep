IF COL_LENGTH('dbo.Accounts', 'token_version') IS NULL
BEGIN
    ALTER TABLE dbo.Accounts ADD token_version BIGINT NOT NULL
        CONSTRAINT df_accounts_token_version DEFAULT 0;
END;

IF COL_LENGTH('dbo.Accounts', 'enabled') IS NULL
BEGIN
    ALTER TABLE dbo.Accounts ADD enabled BIT NOT NULL
        CONSTRAINT df_accounts_enabled DEFAULT 1;
END;
