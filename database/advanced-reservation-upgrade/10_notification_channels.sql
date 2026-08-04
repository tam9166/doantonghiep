BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.notification_channels', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.notification_channels (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            channel_code varchar(40) NOT NULL,
            name_vi nvarchar(100) NOT NULL,
            provider varchar(80) NULL,
            enabled bit NOT NULL CONSTRAINT DF_notification_channels_enabled DEFAULT(0),
            config_json nvarchar(max) NULL,
            created_at datetime2 NOT NULL CONSTRAINT DF_notification_channels_created DEFAULT SYSUTCDATETIME()
        );
    END;

    IF OBJECT_ID('dbo.notification_templates', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.notification_templates (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            template_code varchar(80) NOT NULL,
            locale varchar(10) NOT NULL,
            title nvarchar(200) NOT NULL,
            body nvarchar(max) NOT NULL,
            enabled bit NOT NULL CONSTRAINT DF_notification_templates_enabled DEFAULT(1)
        );
    END;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
