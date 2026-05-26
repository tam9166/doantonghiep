USE RestaurantDB;
GO

-- =========================================================
-- NẠP ĐƠN GIÁ NGẪU NHIÊN CHO TẤT CẢ NGUYÊN LIỆU
-- (Dành cho tính năng Phân tích Tài chính & AI)
-- =========================================================

-- Cập nhật unit_price cho tất cả nguyên liệu đang có giá = 0 hoặc NULL
-- Mỗi nguyên liệu sẽ được gán giá từ 5,000đ đến 300,000đ (theo bội số 500đ)

UPDATE ingredients
SET unit_price = (ABS(CHECKSUM(NEWID())) % 590 + 10) * 500
WHERE unit_price IS NULL OR unit_price = 0;
GO

-- Kiểm tra kết quả: Xem đơn giá mới
SELECT id, name, unit, quantity, unit_price 
FROM ingredients 
ORDER BY id;
GO
