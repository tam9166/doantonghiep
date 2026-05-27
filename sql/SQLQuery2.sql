SELECT * FROM restaurant_table

-- Bước 1: Nâng cấp cột lên NVARCHAR để chứa được tiếng Việt
ALTER TABLE restaurant_table ALTER COLUMN floor NVARCHAR(255);
ALTER TABLE restaurant_table ALTER COLUMN name NVARCHAR(255);

-- Bước 2: Cập nhật lại đúng tên tầng theo ID của bạn
UPDATE restaurant_table SET floor = N'Tầng 2' WHERE id IN (38, 39, 40);
UPDATE restaurant_table SET floor = N'Tầng 3' WHERE id IN (41, 42);
UPDATE restaurant_table SET floor = N'Sân thượng' WHERE id IN (43, 44);

-- Bước 1: Ép tất cả các đơn hàng hiện có thành Đã hoàn thành (Status = 2)
UPDATE orders SET status = 2;

-- Bước 2: Reset toàn bộ bàn về trạng thái Trống (0)
UPDATE restaurant_table SET is_occupied = 0, reserved_time = NULL;