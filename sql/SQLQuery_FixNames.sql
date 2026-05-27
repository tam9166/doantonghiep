USE RestaurantDB;
GO

-- Sửa lại tên nguyên liệu bị lỗi font
UPDATE ingredients SET name = N'Thịt bò thăn' WHERE id = 1;
UPDATE ingredients SET name = N'Mì Ý', unit = N'g' WHERE id = 2;
UPDATE ingredients SET name = N'Tôm' WHERE id = 3;
UPDATE ingredients SET name = N'Mực' WHERE id = 4;
UPDATE ingredients SET name = N'Sốt cà chua' WHERE id = 5;
UPDATE ingredients SET name = N'Cá hồi Na Uy' WHERE id = 6;
UPDATE ingredients SET name = N'Xà lách' WHERE id = 7;
UPDATE ingredients SET name = N'Cà chua bi' WHERE id = 8;
UPDATE ingredients SET name = N'Sốt mè rang' WHERE id = 9;
UPDATE ingredients SET name = N'Trà đen' WHERE id = 10;
UPDATE ingredients SET name = N'Đào ngâm' WHERE id = 11;
UPDATE ingredients SET name = N'Cam tươi', unit = N'quả' WHERE id = 12;
UPDATE ingredients SET name = N'Sả tươi', unit = N'nhánh' WHERE id = 13;
UPDATE ingredients SET name = N'Khoai tây' WHERE id = 14;
UPDATE ingredients SET name = N'Sốt tiêu đen' WHERE id = 15;
GO

-- Kiểm tra
SELECT id, name, unit, unit_price FROM ingredients ORDER BY id;
GO
