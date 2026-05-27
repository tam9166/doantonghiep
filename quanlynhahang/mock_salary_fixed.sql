USE RestaurantDB;
GO

DELETE FROM work_schedules WHERE work_date >= '2026-05-01' AND work_date <= '2026-05-31' AND username IN ('bep1', 'pv1', 'admin', 'manager');
DELETE FROM timekeeping WHERE work_date >= '2026-05-01' AND work_date <= '2026-05-31' AND username IN ('bep1', 'pv1', 'admin', 'manager');

DECLARE @date DATE = '2026-05-01';
WHILE @date <= '2026-05-31'
BEGIN
    -- Insert for bep1
    INSERT INTO work_schedules (username, work_date, shift) VALUES ('bep1', @date, 'Sáng');
    INSERT INTO timekeeping (username, work_date, check_in_time, check_out_time, status) 
    VALUES ('bep1', @date, DATEADD(hour, 6, CAST(@date AS DATETIME)), DATEADD(hour, 14, CAST(@date AS DATETIME)), N'Hoàn thành');
    
    -- Insert for pv1
    INSERT INTO work_schedules (username, work_date, shift) VALUES ('pv1', @date, 'Chiều');
    INSERT INTO timekeeping (username, work_date, check_in_time, check_out_time, status) 
    VALUES ('pv1', @date, DATEADD(hour, 14, CAST(@date AS DATETIME)), DATEADD(hour, 22, CAST(@date AS DATETIME)), N'Hoàn thành');
    
    -- Insert for manager
    INSERT INTO work_schedules (username, work_date, shift) VALUES ('manager', @date, 'Sáng');
    INSERT INTO timekeeping (username, work_date, check_in_time, check_out_time, status) 
    VALUES ('manager', @date, DATEADD(hour, 6, CAST(@date AS DATETIME)), DATEADD(hour, 14, CAST(@date AS DATETIME)), N'Hoàn thành');

    SET @date = DATEADD(day, 1, @date);
END
GO
