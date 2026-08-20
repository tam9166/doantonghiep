-- V043: Convert timekeeping columns from Date to LocalDate/LocalTime
-- P2: Use Asia/Ho_Chi_Minh timezone consistently

-- timekeeping.work_date: DATE → date (Java LocalDate maps to SQL DATE)
-- timekeeping.check_in_time: DATETIME2 → time (Java LocalTime maps to SQL TIME)
-- timekeeping.check_out_time: DATETIME2 → time (Java LocalTime maps to SQL TIME)

-- SQL Server doesn't support direct type change for datetime→time when data exists.
-- Using ALTER TABLE with NULLIF to handle existing data.

-- Step 1: Add new columns
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[timekeeping]') AND name = N'check_in_time_local')
BEGIN
    ALTER TABLE [dbo].[timekeeping] ADD [check_in_time_local] time NULL;
    ALTER TABLE [dbo].[timekeeping] ADD [check_out_time_local] time NULL;
    ALTER TABLE [dbo].[timekeeping] ADD [work_date_local] date NULL;

    -- Step 2: Migrate existing data (convert from DATETIME2 to TIME/DATE)
    -- Dynamic SQL prevents SQL Server from binding the new columns before
    -- the preceding ALTER TABLE statements have executed in this batch.
    EXEC(N'
        UPDATE [dbo].[timekeeping]
        SET [check_in_time_local] = CAST([check_in_time] AS time),
            [check_out_time_local] = CAST([check_out_time] AS time),
            [work_date_local] = CAST([work_date] AS date)
        WHERE [work_date] IS NOT NULL;
    ');

    -- Step 3: Drop old columns
    ALTER TABLE [dbo].[timekeeping] DROP COLUMN [check_in_time];
    ALTER TABLE [dbo].[timekeeping] DROP COLUMN [check_out_time];
    ALTER TABLE [dbo].[timekeeping] DROP COLUMN [work_date];

    -- Step 4: Rename new columns to original names
    EXEC sp_rename N'[dbo].[timekeeping].[check_in_time_local]', N'check_in_time', N'COLUMN';
    EXEC sp_rename N'[dbo].[timekeeping].[check_out_time_local]', N'check_out_time', N'COLUMN';
    EXEC sp_rename N'[dbo].[timekeeping].[work_date_local]', N'work_date', N'COLUMN';
END;
