USE RestaurantDB;
GO

-- Sửa lỗi font chữ cho 'Sáng'
UPDATE work_schedules 
SET shift = NCHAR(83) + NCHAR(225) + NCHAR(110) + NCHAR(103)
WHERE shift LIKE 'S%';

-- Sửa lỗi font chữ cho 'Chiều'
UPDATE work_schedules 
SET shift = NCHAR(67) + NCHAR(104) + NCHAR(105) + NCHAR(7873) + NCHAR(117)
WHERE shift LIKE 'C%';

GO
