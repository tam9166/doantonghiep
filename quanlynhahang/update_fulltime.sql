USE RestaurantDB;
GO

-- Cập nhật tất cả các ca làm hiện tại thành Full Time
UPDATE work_schedules 
SET shift = 'Full Time';

-- Cập nhật thời gian check-in thành 9h sáng và check-out thành 23h (11h đêm)
UPDATE timekeeping 
SET check_in_time = DATEADD(hour, 9, CAST(work_date AS DATETIME)),
    check_out_time = DATEADD(hour, 23, CAST(work_date AS DATETIME))
WHERE check_in_time IS NOT NULL;

GO
