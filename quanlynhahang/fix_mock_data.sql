USE RestaurantDB;
GO

-- 1. Fix encoding for 'Hoàn thành'
UPDATE timekeeping 
SET status = NCHAR(72) + NCHAR(111) + NCHAR(224) + NCHAR(110) + NCHAR(32) + NCHAR(116) + NCHAR(104) + NCHAR(224) + NCHAR(110) + NCHAR(104);

-- 2. Insert mock data for Thu Ngan ('tn')
DELETE FROM work_schedules WHERE work_date >= '2026-05-01' AND work_date <= '2026-05-31' AND username = 'tn';
DELETE FROM timekeeping WHERE work_date >= '2026-05-01' AND work_date <= '2026-05-31' AND username = 'tn';

DECLARE @date DATE = '2026-05-01';
WHILE @date <= '2026-05-31'
BEGIN
    INSERT INTO work_schedules (username, work_date, shift) VALUES ('tn', @date, 'Sáng');
    INSERT INTO timekeeping (username, work_date, check_in_time, check_out_time, status) 
    VALUES ('tn', @date, DATEADD(hour, 6, CAST(@date AS DATETIME)), DATEADD(hour, 14, CAST(@date AS DATETIME)), NCHAR(72) + NCHAR(111) + NCHAR(224) + NCHAR(110) + NCHAR(32) + NCHAR(116) + NCHAR(104) + NCHAR(224) + NCHAR(110) + NCHAR(104));
    
    SET @date = DATEADD(day, 1, @date);
END
GO
