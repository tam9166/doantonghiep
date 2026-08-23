IF OBJECT_ID('dbo.timekeeping', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.timekeeping', 'total_hours') IS NULL
        EXEC(N'ALTER TABLE dbo.timekeeping ADD total_hours DECIMAL(5,2) NOT NULL CONSTRAINT DF_timekeeping_total_hours_v058 DEFAULT (0)');

    EXEC(N'UPDATE dbo.timekeeping
           SET total_hours = CAST((CASE
               WHEN check_in_time IS NULL OR check_out_time IS NULL THEN 0
               WHEN DATEDIFF(MINUTE, check_in_time, check_out_time) < 0
                   THEN DATEDIFF(MINUTE, check_in_time, check_out_time) + 1440
               ELSE DATEDIFF(MINUTE, check_in_time, check_out_time)
           END) / 60.0 AS DECIMAL(5,2))');

    EXEC(N'IF EXISTS (
               SELECT 1 FROM dbo.timekeeping
               GROUP BY username, work_date HAVING COUNT(*) > 1
           ) THROW 51000, ''Cannot enforce attendance integrity: duplicate username/work_date records exist.'', 1');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.timekeeping') AND name = 'UX_timekeeping_username_work_date')
        EXEC(N'CREATE UNIQUE INDEX UX_timekeeping_username_work_date ON dbo.timekeeping(username, work_date)');
END;
