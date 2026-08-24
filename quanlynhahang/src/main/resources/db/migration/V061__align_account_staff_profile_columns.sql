SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.Accounts', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.Accounts', 'shift') IS NULL
        EXEC sp_executesql N'ALTER TABLE dbo.Accounts ADD shift NVARCHAR(50) NULL;';

    IF COL_LENGTH('dbo.Accounts', 'assigned_area') IS NULL
        EXEC sp_executesql N'ALTER TABLE dbo.Accounts ADD assigned_area NVARCHAR(100) NULL;';
END;
