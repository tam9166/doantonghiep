BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.table_suggestion_logs', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.table_suggestion_logs (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            reservation_code varchar(30) NULL,
            customer_phone varchar(20) NULL,
            guest_count int NOT NULL,
            requested_date date NOT NULL,
            requested_time time NOT NULL,
            table_id int NULL,
            score int NOT NULL,
            reasons nvarchar(1000) NULL,
            created_at datetime2 NOT NULL CONSTRAINT DF_table_suggestion_created DEFAULT SYSUTCDATETIME()
        );
    END;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
