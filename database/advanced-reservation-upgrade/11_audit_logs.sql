BEGIN TRY
    BEGIN TRAN;

    IF COL_LENGTH('dbo.activity_logs', 'role_name') IS NULL
        ALTER TABLE dbo.activity_logs ADD role_name varchar(80) NULL;
    IF COL_LENGTH('dbo.activity_logs', 'user_agent') IS NULL
        ALTER TABLE dbo.activity_logs ADD user_agent nvarchar(300) NULL;
    IF COL_LENGTH('dbo.activity_logs', 'reason') IS NULL
        ALTER TABLE dbo.activity_logs ADD reason nvarchar(500) NULL;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
