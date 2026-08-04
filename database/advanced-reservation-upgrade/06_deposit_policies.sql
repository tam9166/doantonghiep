BEGIN TRY
    BEGIN TRAN;

    IF OBJECT_ID('dbo.deposit_policies', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.deposit_policies (
            id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
            policy_code varchar(40) NOT NULL,
            name_vi nvarchar(150) NOT NULL,
            name_en nvarchar(150) NULL,
            policy_type varchar(30) NOT NULL,
            percentage_rate decimal(5,2) NULL,
            fixed_amount decimal(18,2) NULL,
            amount_per_guest decimal(18,2) NULL,
            minimum_amount decimal(18,2) NULL,
            maximum_amount decimal(18,2) NULL,
            area_id int NULL,
            table_type varchar(40) NULL,
            day_of_week int NULL,
            holiday_id bigint NULL,
            start_time time NULL,
            end_time time NULL,
            minimum_guests int NULL,
            minimum_order_amount decimal(18,2) NULL,
            priority int NOT NULL CONSTRAINT DF_deposit_policies_priority DEFAULT(100),
            is_active bit NOT NULL CONSTRAINT DF_deposit_policies_active DEFAULT(1),
            effective_from date NULL,
            effective_to date NULL,
            created_at datetime2 NOT NULL CONSTRAINT DF_deposit_policies_created DEFAULT SYSUTCDATETIME(),
            updated_at datetime2 NULL
        );
    END;

    IF COL_LENGTH('dbo.reservations', 'deposit_policy_code') IS NULL
        ALTER TABLE dbo.reservations ADD deposit_policy_code varchar(40) NULL;
    IF COL_LENGTH('dbo.reservations', 'deposit_policy_snapshot') IS NULL
        ALTER TABLE dbo.reservations ADD deposit_policy_snapshot nvarchar(max) NULL;

    COMMIT;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK;
    THROW;
END CATCH;
