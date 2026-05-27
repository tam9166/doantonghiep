USE RestaurantDB;
GO

-- Sửa lại lỗi lệch múi giờ (UTC+7)
-- Để FE hiện 09:00 sáng, DB cần lưu 02:00 sáng (UTC)
-- Để FE hiện 23:00 đêm, DB cần lưu 16:00 chiều (UTC)
UPDATE timekeeping 
SET check_in_time = DATEADD(hour, 2, CAST(work_date AS DATETIME)),
    check_out_time = DATEADD(hour, 16, CAST(work_date AS DATETIME))
WHERE check_in_time IS NOT NULL;

GO
