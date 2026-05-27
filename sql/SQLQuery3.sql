-- Xóa bảng cũ (nếu đang tồn tại) để làm sạch
DROP TABLE IF EXISTS recipes;
DROP TABLE IF EXISTS ingredients;

-- 1. Tạo lại bảng Nguyên liệu (chuẩn INT)
CREATE TABLE ingredients (
    id INT PRIMARY KEY IDENTITY(1,1), 
    name NVARCHAR(255) NOT NULL,
    quantity FLOAT DEFAULT 0, 
    unit NVARCHAR(50) 
);

-- 2. Tạo lại bảng Công thức (chuẩn INT)
CREATE TABLE recipes (
    id INT PRIMARY KEY IDENTITY(1,1),
    product_id INT, 
    ingredient_id INT, 
    amount_required FLOAT, 
    CONSTRAINT FK_Recipe_Product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT FK_Recipe_Ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

-- Tạo tài khoản cho BẾP
INSERT INTO accounts (username, password, email, role) 
VALUES ('bep1', '123', 'bep01@fpoly.edu.vn', 'ROLE_KITCHEN');

-- Tạo tài khoản cho PHỤC VỤ
INSERT INTO accounts (username, password, email, role) 
VALUES ('pv1', '123', 'phucvu01@fpoly.edu.vn', 'ROLE_WAITER');

-- Thêm Tài khoản (có điền email)
INSERT INTO accounts (username, password, email) 
VALUES ('bep01', '123456', 'bep01@fpoly.edu.vn');

INSERT INTO accounts (username, password, email) 
VALUES ('phucvu01', '123456', 'phucvu01@fpoly.edu.vn');

-- (Bạn tự map ID trong bảng account_roles như tin nhắn trước nhé)

UPDATE accounts 
SET password = '123' 
WHERE username IN ('bep1', 'pv1');

SELECT username, password, role FROM accounts WHERE username = 'bep1';

UPDATE Roles SET name = 'KITCHEN' WHERE name = 'ROLE_KITCHEN';
UPDATE Roles SET name = 'WAITER' WHERE name = 'ROLE_WAITER';

-- 1. Thêm nguyên liệu vào kho
INSERT INTO ingredients (name, quantity, unit) VALUES (N'Thịt bò thăn', 10.0, 'Kg');

-- 2. Thiết lập công thức: 
-- Món Bò BEEFSTEAK (ID=1) cần 0.3kg Thịt bò
INSERT INTO recipes (product_id, ingredient_id, amount_required) VALUES (1, 1, 0.3);

-- Món Bò Fuji (ID=2) cần 0.25kg Thịt bò
INSERT INTO recipes (product_id, ingredient_id, amount_required) VALUES (2, 1, 0.25);

-- 1. Đảm bảo đã có Role KITCHEN trong bảng Roles (ID giả sử là 2)
-- 2. Đảm bảo bep1 đã có trong bảng Accounts
-- 3. Chèn vào bảng Authorities để nối User và Role
INSERT INTO Authorities (username, role_id) 
SELECT 'bep1', id FROM Roles WHERE name = 'KITCHEN';

-- 1. Xóa các quyền thừa/sai trước đó của pv1 (nếu có)
DELETE FROM Authorities WHERE username = 'pv1';

-- 2. Chèn quyền WAITER chính xác vào bảng Authorities
-- Lưu ý: Tên quyền trong bảng Roles phải là 'WAITER' (không có chữ ROLE_ ở đầu)
INSERT INTO Authorities (username, role_id) 
SELECT 'pv1', id FROM Roles WHERE name = 'WAITER';

UPDATE Roles SET name = 'WAITER' WHERE name = 'ROLE_WAITER' OR name = 'waiter';

UPDATE accounts SET password = '123' WHERE username = 'bep1';
UPDATE accounts SET password = '123' WHERE username = 'pv1';
UPDATE accounts SET password = '123' WHERE username = 'admin';

UPDATE Roles SET name = 'KITCHEN' WHERE name = 'ROLE_KITCHEN' OR name = 'bep';

-- Đổi cái tên quyền kỳ lạ kia thành KITCHEN
UPDATE Roles SET name = 'KITCHEN' WHERE name = 'FACTOR_PASSWORD';

-- Tiện thể đảm bảo Admin và Waiter cũng đúng tên
UPDATE Roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN' OR name = 'admin';
UPDATE Roles SET name = 'WAITER' WHERE name = 'ROLE_WAITER' OR name = 'waiter';

-- 1. Bắt buộc SQL Server phải chọn đúng DB của bạn
USE RestaurantDB;
GO

-- 2. Sửa lại tên quyền cho chuẩn
UPDATE Roles SET name = 'KITCHEN' WHERE name = 'FACTOR_PASSWORD';
UPDATE Roles SET name = 'ADMIN' WHERE name = 'ROLE_ADMIN' OR name = 'admin';
UPDATE Roles SET name = 'WAITER' WHERE name = 'ROLE_WAITER' OR name = 'waiter';

-- 3. Xem lại thành quả xem đã đổi thành KITCHEN chưa nhé
SELECT * FROM Roles;

USE RestaurantDB;
GO

USE RestaurantDB;
GO

-- 1. Xóa sạch mọi quyền hiện tại của bep1 để tránh bị lưu đè
DELETE FROM Authorities WHERE username = 'bep1';

-- 2. Đổi tên quyền trong bảng Roles thành KITCHEN (Đã sửa lỗi)
UPDATE Roles SET name = 'KITCHEN' WHERE name = 'FACTOR_PASSWORD';

-- 3. Cấp lại quyền KITCHEN cho bep1
INSERT INTO Authorities (username, role_id) 
SELECT 'bep1', id FROM Roles WHERE name = 'KITCHEN';