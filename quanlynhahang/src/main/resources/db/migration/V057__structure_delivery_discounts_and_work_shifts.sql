-- Keep delivery identity, discount components, and work-shift snapshots queryable.
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.Orders', 'recipient_name') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD recipient_name NVARCHAR(100) NULL');
    IF COL_LENGTH('dbo.Orders', 'recipient_phone') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD recipient_phone VARCHAR(20) NULL');
    IF COL_LENGTH('dbo.Orders', 'delivery_address') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD delivery_address NVARCHAR(500) NULL');
    IF COL_LENGTH('dbo.Orders', 'delivery_note') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD delivery_note NVARCHAR(500) NULL');

    IF COL_LENGTH('dbo.Orders', 'original_subtotal') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD original_subtotal DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_original_subtotal DEFAULT (0)');
    IF COL_LENGTH('dbo.Orders', 'membership_discount') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD membership_discount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_membership_discount DEFAULT (0)');
    IF COL_LENGTH('dbo.Orders', 'voucher_discount') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD voucher_discount DECIMAL(18,2) NOT NULL CONSTRAINT DF_Orders_voucher_discount DEFAULT (0)');

    IF COL_LENGTH('dbo.Orders', 'sub_total') IS NOT NULL
        EXEC(N'UPDATE dbo.Orders
               SET original_subtotal = CASE WHEN original_subtotal = 0 THEN COALESCE(sub_total, 0) ELSE original_subtotal END');
    IF COL_LENGTH('dbo.Orders', 'order_type') IS NOT NULL
        EXEC(N'UPDATE dbo.Orders
               SET delivery_address = CASE WHEN order_type = ''DELIVERY'' AND delivery_address IS NULL THEN address ELSE delivery_address END');
END;

IF OBJECT_ID('dbo.work_schedules', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.work_schedules', 'shift_name') IS NULL
        EXEC(N'ALTER TABLE dbo.work_schedules ADD shift_name NVARCHAR(50) NULL');
    IF COL_LENGTH('dbo.work_schedules', 'start_time') IS NULL
        EXEC(N'ALTER TABLE dbo.work_schedules ADD start_time TIME(0) NULL');
    IF COL_LENGTH('dbo.work_schedules', 'end_time') IS NULL
        EXEC(N'ALTER TABLE dbo.work_schedules ADD end_time TIME(0) NULL');
    IF COL_LENGTH('dbo.work_schedules', 'status') IS NULL
        EXEC(N'ALTER TABLE dbo.work_schedules ADD status VARCHAR(20) NULL');
    IF COL_LENGTH('dbo.work_schedules', 'note') IS NULL
        EXEC(N'ALTER TABLE dbo.work_schedules ADD note NVARCHAR(500) NULL');

    EXEC(N'UPDATE dbo.work_schedules
           SET shift_name = COALESCE(NULLIF(LTRIM(RTRIM(shift_name)), ''''), shift),
               start_time = COALESCE(start_time, CASE shift WHEN N''Sáng'' THEN CAST(''06:00'' AS TIME) WHEN N''Chiều'' THEN CAST(''14:00'' AS TIME) WHEN N''Tối'' THEN CAST(''22:00'' AS TIME) ELSE CAST(''09:00'' AS TIME) END),
               end_time = COALESCE(end_time, CASE shift WHEN N''Sáng'' THEN CAST(''14:00'' AS TIME) WHEN N''Chiều'' THEN CAST(''22:00'' AS TIME) WHEN N''Tối'' THEN CAST(''06:00'' AS TIME) ELSE CAST(''17:00'' AS TIME) END),
               status = COALESCE(NULLIF(LTRIM(RTRIM(status)), ''''), ''SCHEDULED'')');

    EXEC(N'ALTER TABLE dbo.work_schedules ALTER COLUMN shift_name NVARCHAR(50) NOT NULL');
    EXEC(N'ALTER TABLE dbo.work_schedules ALTER COLUMN start_time TIME(0) NOT NULL');
    EXEC(N'ALTER TABLE dbo.work_schedules ALTER COLUMN end_time TIME(0) NOT NULL');
    EXEC(N'ALTER TABLE dbo.work_schedules ALTER COLUMN status VARCHAR(20) NOT NULL');
END;
