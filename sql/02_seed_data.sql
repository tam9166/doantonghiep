SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

USE RestaurantDB;
GO

BEGIN TRANSACTION;

DECLARE @Password123 VARCHAR(255) = '$2a$10$5BZYd0dIfh5tsrkYljTxF.1dGgNJAHZto3e374.iz3aPoJp9tTZJS';

INSERT INTO dbo.Roles(name)
VALUES ('ADMIN'), ('MANAGER'), ('WAITER'), ('KITCHEN'), ('CASHIER'), ('CUSTOMER');

INSERT INTO dbo.Accounts(username, password, fullname, email, phone, enabled, total_spent, loyalty_points, tier, points, membership_tier)
VALUES
('admin',   @Password123, N'Quản trị hệ thống',  'admin@restaurant.local',   '0900000001', 1, 0,       0,    'DIAMOND', 0,    N'DIAMOND'),
('manager', @Password123, N'Quản lý nhà hàng',   'manager@restaurant.local', '0900000002', 1, 0,       0,    'DIAMOND', 0,    N'DIAMOND'),
('waiter',  @Password123, N'Nhân viên phục vụ',  'waiter@restaurant.local',  '0900000003', 1, 0,       0,    'BRONZE',  0,    N'BRONZE'),
('kitchen', @Password123, N'Nhân viên bếp',      'kitchen@restaurant.local', '0900000004', 1, 0,       0,    'BRONZE',  0,    N'BRONZE'),
('cashier', @Password123, N'Nhân viên thu ngân', 'cashier@restaurant.local', '0900000005', 1, 0,       0,    'BRONZE',  0,    N'BRONZE'),
('customer',@Password123, N'Khách hàng mẫu',     'customer@restaurant.local','0900000006', 1, 1250000, 1250, 'GOLD',    1250, N'GOLD');

INSERT INTO dbo.Authorities(username, role_id)
SELECT 'admin', id FROM dbo.Roles WHERE name = 'ADMIN'
UNION ALL SELECT 'manager', id FROM dbo.Roles WHERE name = 'MANAGER'
UNION ALL SELECT 'waiter', id FROM dbo.Roles WHERE name = 'WAITER'
UNION ALL SELECT 'kitchen', id FROM dbo.Roles WHERE name = 'KITCHEN'
UNION ALL SELECT 'cashier', id FROM dbo.Roles WHERE name = 'CASHIER'
UNION ALL SELECT 'customer', id FROM dbo.Roles WHERE name = 'CUSTOMER';

INSERT INTO dbo.Categories(name, description, sort_order)
VALUES
(N'Khai vị', N'Món ăn nhẹ mở đầu bữa ăn', 1),
(N'Món chính', N'Món ăn chính phục vụ tại bàn', 2),
(N'Lẩu', N'Lẩu dùng chung theo nhóm', 3),
(N'Nướng', N'Món nướng than và nướng sốt', 4),
(N'Cơm - Mì', N'Cơm, mì và món no', 5),
(N'Hải sản', N'Món hải sản tươi', 6),
(N'Tráng miệng', N'Bánh ngọt và trái cây', 7),
(N'Đồ uống', N'Nước ngọt, trà, cà phê', 8);

INSERT INTO dbo.Products(name, price, cost_price, tax_rate, sold_count, average_rating, preparation_time, image, description, available, status, status_name, category_id)
VALUES
(N'Gỏi cuốn tôm thịt', 45000, 22000, 8, 120, 4.5, 8,  'goi-cuon.jpg', N'Gỏi cuốn tươi dùng cùng nước chấm đậu phộng', 1, 1, 'AVAILABLE', 1),
(N'Chả giò hải sản', 69000, 33000, 8, 98, 4.4, 12, 'cha-gio-hai-san.jpg', N'Chả giò giòn nhân hải sản', 1, 1, 'AVAILABLE', 1),
(N'Salad bò áp chảo', 79000, 42000, 8, 65, 4.3, 10, 'salad-bo.jpg', N'Salad rau xanh và bò áp chảo', 1, 1, 'AVAILABLE', 1),
(N'Súp cua', 55000, 26000, 8, 80, 4.2, 10, 'sup-cua.jpg', N'Súp cua trứng cút nóng', 1, 1, 'AVAILABLE', 1),
(N'Bò lúc lắc khoai tây', 159000, 84000, 8, 140, 4.7, 18, 'bo-luc-lac.jpg', N'Bò mềm xào sốt tiêu đen', 1, 1, 'AVAILABLE', 2),
(N'Gà nướng mật ong', 139000, 69000, 8, 130, 4.6, 22, 'ga-nuong-mat-ong.jpg', N'Gà nướng mật ong da giòn', 1, 1, 'AVAILABLE', 2),
(N'Sườn non sốt BBQ', 179000, 92000, 8, 90, 4.5, 25, 'suon-bbq.jpg', N'Sườn non nướng sốt BBQ', 1, 1, 'AVAILABLE', 2),
(N'Cá hồi sốt chanh dây', 189000, 112000, 8, 75, 4.6, 20, 'ca-hoi.jpg', N'Cá hồi áp chảo sốt chanh dây', 1, 1, 'AVAILABLE', 2),
(N'Lẩu thái hải sản', 299000, 175000, 8, 88, 4.8, 25, 'lau-thai.jpg', N'Lẩu thái chua cay hải sản', 1, 1, 'AVAILABLE', 3),
(N'Lẩu bò nhúng dấm', 279000, 155000, 8, 70, 4.4, 24, 'lau-bo.jpg', N'Lẩu bò chua thanh', 1, 1, 'AVAILABLE', 3),
(N'Lẩu gà lá é', 249000, 132000, 8, 64, 4.3, 24, 'lau-ga-la-e.jpg', N'Lẩu gà lá é thơm nhẹ', 1, 1, 'AVAILABLE', 3),
(N'Lẩu nấm chay', 219000, 105000, 8, 35, 4.2, 22, 'lau-nam.jpg', N'Lẩu nấm thanh đạm', 1, 1, 'AVAILABLE', 3),
(N'Ba chỉ bò nướng', 169000, 89000, 8, 110, 4.6, 16, 'ba-chi-bo.jpg', N'Ba chỉ bò ướp sốt nướng', 1, 1, 'AVAILABLE', 4),
(N'Sườn cây nướng muối ớt', 189000, 98000, 8, 92, 4.5, 20, 'suon-cay.jpg', N'Sườn cây cay nhẹ', 1, 1, 'AVAILABLE', 4),
(N'Mực nướng sa tế', 179000, 105000, 8, 84, 4.4, 18, 'muc-nuong.jpg', N'Mực nướng sa tế thơm cay', 1, 1, 'AVAILABLE', 4),
(N'Đậu hũ nướng giấy bạc', 89000, 41000, 8, 46, 4.1, 15, 'dau-hu-nuong.jpg', N'Đậu hũ non sốt nấm', 1, 1, 'AVAILABLE', 4),
(N'Cơm chiên hải sản', 99000, 48000, 8, 150, 4.3, 12, 'com-chien-hai-san.jpg', N'Cơm chiên tôm mực', 1, 1, 'AVAILABLE', 5),
(N'Mì xào bò', 89000, 42000, 8, 135, 4.2, 12, 'mi-xao-bo.jpg', N'Mì xào bò rau củ', 1, 1, 'AVAILABLE', 5),
(N'Cơm gà xối mỡ', 79000, 36000, 8, 120, 4.1, 14, 'com-ga.jpg', N'Cơm gà xối mỡ da giòn', 1, 1, 'AVAILABLE', 5),
(N'Miến cua tay cầm', 129000, 72000, 8, 50, 4.4, 16, 'mien-cua.jpg', N'Miến cua tay cầm nóng', 1, 1, 'AVAILABLE', 5),
(N'Tôm sú hấp dừa', 219000, 135000, 8, 60, 4.7, 18, 'tom-hap-dua.jpg', N'Tôm sú hấp nước dừa', 1, 1, 'AVAILABLE', 6),
(N'Cua rang me', 329000, 230000, 8, 35, 4.8, 25, 'cua-rang-me.jpg', N'Cua thịt rang sốt me', 1, 1, 'AVAILABLE', 6),
(N'Nghêu hấp sả', 99000, 52000, 8, 72, 4.2, 12, 'ngheu-hap-sa.jpg', N'Nghêu hấp sả ớt', 1, 1, 'AVAILABLE', 6),
(N'Sò điệp nướng phô mai', 159000, 91000, 8, 48, 4.5, 16, 'so-diep.jpg', N'Sò điệp nướng phô mai béo', 1, 1, 'AVAILABLE', 6),
(N'Panna cotta dâu', 49000, 22000, 8, 90, 4.3, 6, 'panna-cotta.jpg', N'Panna cotta sốt dâu', 1, 1, 'AVAILABLE', 7),
(N'Chè khúc bạch', 45000, 18000, 8, 100, 4.2, 5, 'che-khuc-bach.jpg', N'Chè khúc bạch thanh mát', 1, 1, 'AVAILABLE', 7),
(N'Trái cây theo mùa', 59000, 25000, 8, 55, 4.1, 5, 'trai-cay.jpg', N'Trái cây tươi cắt sẵn', 1, 1, 'AVAILABLE', 7),
(N'Trà đào cam sả', 45000, 14000, 8, 180, 4.5, 5, 'tra-dao.jpg', N'Trà đào cam sả mát lạnh', 1, 1, 'AVAILABLE', 8),
(N'Cà phê sữa đá', 35000, 10000, 8, 210, 4.4, 5, 'ca-phe-sua-da.jpg', N'Cà phê phin sữa đá', 1, 1, 'AVAILABLE', 8),
(N'Nước ép cam', 49000, 19000, 8, 95, 4.3, 5, 'nuoc-ep-cam.jpg', N'Nước ép cam tươi', 1, 1, 'AVAILABLE', 8);

INSERT INTO dbo.ingredients(name, quantity, unit, min_stock, unit_price, image, shelf_life_days, active)
VALUES
(N'Thịt bò', 35.000, N'kg', 5.000, 220000, 'thit-bo.jpg', 7, 1),
(N'Thịt gà', 40.000, N'kg', 6.000, 85000, 'thit-ga.jpg', 5, 1),
(N'Sườn heo', 28.000, N'kg', 5.000, 145000, 'suon-heo.jpg', 5, 1),
(N'Cá hồi', 18.000, N'kg', 3.000, 320000, 'ca-hoi.jpg', 4, 1),
(N'Tôm sú', 24.000, N'kg', 4.000, 260000, 'tom-su.jpg', 3, 1),
(N'Cua thịt', 15.000, N'kg', 2.000, 420000, 'cua-thit.jpg', 3, 1),
(N'Mực lá', 20.000, N'kg', 3.000, 230000, 'muc-la.jpg', 3, 1),
(N'Nghêu', 30.000, N'kg', 5.000, 65000, 'ngheu.jpg', 2, 1),
(N'Rau xà lách', 12.000, N'kg', 2.000, 35000, 'xa-lach.jpg', 2, 1),
(N'Cà chua', 15.000, N'kg', 2.000, 28000, 'ca-chua.jpg', 4, 1),
(N'Khoai tây', 30.000, N'kg', 5.000, 25000, 'khoai-tay.jpg', 15, 1),
(N'Nấm tổng hợp', 16.000, N'kg', 3.000, 90000, 'nam.jpg', 4, 1),
(N'Đậu hũ non', 18.000, N'kg', 3.000, 42000, 'dau-hu.jpg', 4, 1),
(N'Gạo thơm', 80.000, N'kg', 20.000, 22000, 'gao.jpg', 90, 1),
(N'Mì trứng', 25.000, N'kg', 5.000, 55000, 'mi-trung.jpg', 30, 1),
(N'Nước mắm', 20.000, N'lít', 4.000, 45000, 'nuoc-mam.jpg', 180, 1),
(N'Sốt BBQ', 12.000, N'lít', 2.000, 75000, 'sot-bbq.jpg', 90, 1),
(N'Sa tế', 10.000, N'kg', 2.000, 65000, 'sa-te.jpg', 120, 1),
(N'Trà đen', 8.000, N'kg', 1.000, 120000, 'tra-den.jpg', 180, 1),
(N'Cam tươi', 35.000, N'kg', 6.000, 38000, 'cam-tuoi.jpg', 7, 1);

INSERT INTO dbo.recipes(product_id, ingredient_id, amount_required)
VALUES
(5, 1, 0.250), (5, 11, 0.150),
(6, 2, 0.350), (7, 3, 0.400),
(8, 4, 0.250), (9, 5, 0.300),
(15, 7, 0.250), (17, 14, 0.200),
(18, 1, 0.180), (28, 19, 0.030);

DECLARE @TableNo INT = 1;
WHILE @TableNo <= 20
BEGIN
    INSERT INTO dbo.restaurant_table(name, floor, is_occupied, status, has_view, reserved_time, capacity, view_type)
    VALUES (
        CONCAT(N'Bàn ', FORMAT(@TableNo, '00')),
        CASE WHEN @TableNo <= 10 THEN N'Tầng 1' ELSE N'Tầng 2' END,
        0,
        'AVAILABLE',
        CASE WHEN @TableNo IN (5, 10, 15, 20) THEN 1 ELSE 0 END,
        NULL,
        CASE WHEN @TableNo % 5 = 0 THEN 8 WHEN @TableNo % 3 = 0 THEN 6 ELSE 4 END,
        CASE WHEN @TableNo IN (5, 10, 15, 20) THEN N'Cửa sổ' ELSE N'Tiêu chuẩn' END
    );
    SET @TableNo += 1;
END;

INSERT INTO dbo.vouchers(code, voucher_name, discount_type, discount_value, discount_percent, max_discount_amount, min_order_amount, start_date, end_date, usage_limit, used_count, active, is_used, account_username)
VALUES
('WELCOME10', N'Chào mừng khách mới', 'PERCENT', 10, 10, 50000, 200000, SYSDATETIME(), DATEADD(day, 60, SYSDATETIME()), 200, 0, 1, 0, NULL),
('VIP15', N'Ưu đãi khách VIP', 'PERCENT', 15, 15, 120000, 500000, SYSDATETIME(), DATEADD(day, 90, SYSDATETIME()), 100, 0, 1, 0, NULL),
('FAMILY50', N'Giảm 50K nhóm gia đình', 'AMOUNT', 50000, NULL, 50000, 350000, SYSDATETIME(), DATEADD(day, 60, SYSDATETIME()), 150, 0, 1, 0, NULL),
('SEAFOOD80', N'Ưu đãi hải sản', 'AMOUNT', 80000, NULL, 80000, 700000, SYSDATETIME(), DATEADD(day, 45, SYSDATETIME()), 80, 0, 1, 0, NULL),
('LUNCH5', N'Giảm giờ trưa', 'PERCENT', 5, 5, 30000, 150000, SYSDATETIME(), DATEADD(day, 30, SYSDATETIME()), 300, 0, 1, 0, NULL),
('DINNER12', N'Giảm buổi tối', 'PERCENT', 12, 12, 90000, 450000, SYSDATETIME(), DATEADD(day, 30, SYSDATETIME()), 120, 0, 1, 0, NULL),
('CUSTOMER20', N'Voucher riêng khách mẫu', 'PERCENT', 20, 20, 150000, 600000, SYSDATETIME(), DATEADD(day, 90, SYSDATETIME()), 1, 0, 1, 0, 'customer'),
('HOTPOT70', N'Ưu đãi món lẩu', 'AMOUNT', 70000, NULL, 70000, 500000, SYSDATETIME(), DATEADD(day, 40, SYSDATETIME()), 70, 0, 1, 0, NULL),
('DRINK25', N'Giảm đồ uống', 'PERCENT', 25, 25, 40000, 120000, SYSDATETIME(), DATEADD(day, 25, SYSDATETIME()), 100, 0, 1, 0, NULL),
('BIRTHDAY100', N'Sinh nhật khách hàng', 'AMOUNT', 100000, NULL, 100000, 800000, SYSDATETIME(), DATEADD(day, 365, SYSDATETIME()), 50, 0, 1, 0, NULL);

INSERT INTO dbo.Posts(title, content, image, type)
VALUES
(N'Ưu đãi cuối tuần', N'Nhà hàng áp dụng nhiều voucher cho nhóm gia đình.', 'post-weekend.jpg', 'NEWS'),
(N'Tuyển phục vụ ca tối', N'Cần tuyển phục vụ bán thời gian ca tối.', 'post-hiring-waiter.jpg', 'RECRUITMENT'),
(N'Món mới tháng này', N'Ra mắt set hải sản nướng phô mai.', 'post-new-menu.jpg', 'NEWS');

COMMIT TRANSACTION;
GO

PRINT N'02_seed_data.sql completed. Sample password for all accounts: 123 (BCrypt).';
GO
