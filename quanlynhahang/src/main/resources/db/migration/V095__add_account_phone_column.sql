-- Keep the deployed schema compatible with the Account.phone mapping.
-- V001 defines this column for clean installs, but older databases may have
-- been created from a schema snapshot that predates that definition.
IF COL_LENGTH('dbo.Accounts', 'phone') IS NULL
BEGIN
    ALTER TABLE dbo.Accounts ADD phone VARCHAR(20) NULL;
END;
