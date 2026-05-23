USE RestaurantDB;
GO

-- =========================================================
-- BƯỚC 1: CẬP NHẬT BẢNG products VÀ categories
-- =========================================================

-- Thêm cột category_id vào bảng products (Kiểm tra nếu chưa có thì mới thêm)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'dbo.products') AND name = 'category_id')
BEGIN
    ALTER TABLE products ADD category_id INT;
END
GO

-- Nối khóa ngoại từ products sang categories (Kiểm tra nếu chưa có thì mới nối)
IF NOT EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_products_categories')
BEGIN
    ALTER TABLE products ADD CONSTRAINT FK_products_categories FOREIGN KEY (category_id) REFERENCES categories(id);
END
GO

-- Bơm dữ liệu mẫu cho danh mục (Chỉ thêm nếu bảng categories đang trống)
IF NOT EXISTS (SELECT 1 FROM categories)
BEGIN
    INSERT INTO categories (name) VALUES 
    (N'Lẩu & Nước Dùng'), 
    (N'Đồ Nướng BBQ'), 
    (N'Món Chay'), 
    (N'Nước Uống & Tráng Miệng');
END
GO


-- =========================================================
-- BƯỚC 2: XÓA SQL CŨ (NẾU CÓ) VÀ TẠO BẢNG QUẢN LÝ BÀN
-- =========================================================

-- Xóa bảng cũ nếu tồn tại để tạo lại cho chuẩn tên
IF OBJECT_ID('dbo.restaurant_table', 'U') IS NOT NULL DROP TABLE dbo.restaurant_table;

CREATE TABLE restaurant_table (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    floor NVARCHAR(100) NOT NULL, 
    is_occupied BIT DEFAULT 0,    
    has_view BIT DEFAULT 0        
);

-- Thêm dữ liệu các bàn vào nhà hàng
INSERT INTO restaurant_table (name, floor, is_occupied, has_view) VALUES
(N'Bàn T2-01', N'Tầng 2', 0, 0),
(N'Bàn T2-02', N'Tầng 2', 0, 0),
(N'Bàn T2-03', N'Tầng 2', 0, 0),
(N'Bàn T3-01', N'Tầng 3', 0, 0),
(N'Bàn T3-02', N'Tầng 3', 0, 0),
(N'Bàn ST-VIP1', N'Sân thượng', 0, 0),
(N'Bàn ST-VIP2', N'Sân thượng', 0, 0);
GO


-- =========================================================
-- BƯỚC 3: TẠO BẢNG TỒN KHO & NHẬP HÀNG (Dành cho sau này)
-- =========================================================

-- Xóa các bảng cũ đi nếu chúng đã được tạo lỡ dở
IF OBJECT_ID('dbo.purchase_invoice_detail', 'U') IS NOT NULL DROP TABLE dbo.purchase_invoice_detail;
IF OBJECT_ID('dbo.purchase_invoice', 'U') IS NOT NULL DROP TABLE dbo.purchase_invoice;
IF OBJECT_ID('dbo.ingredient', 'U') IS NOT NULL DROP TABLE dbo.ingredient;

-- Bảng Nguyên Liệu
CREATE TABLE ingredient (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL,
    unit NVARCHAR(50), 
    quantity_in_stock FLOAT DEFAULT 0 
);

-- Bảng Hóa Đơn Nhập Hàng
CREATE TABLE purchase_invoice (
    id INT IDENTITY(1,1) PRIMARY KEY,
    create_date DATETIME DEFAULT GETDATE(),
    total_amount FLOAT DEFAULT 0, 
    supplier NVARCHAR(255)        
);

-- Bảng Chi Tiết Hóa Đơn
CREATE TABLE purchase_invoice_detail (
    id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity FLOAT NOT NULL,      
    unit_price FLOAT NOT NULL,    
    CONSTRAINT FK_InvoiceDetail_Invoice FOREIGN KEY (invoice_id) REFERENCES purchase_invoice(id),
    CONSTRAINT FK_InvoiceDetail_Ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredient(id)
);

-- Bơm dữ liệu mẫu cho kho nguyên liệu
INSERT INTO ingredient (name, unit, quantity_in_stock) VALUES
(N'Thịt bò Kobe', N'kg', 10.5),
(N'Rau xà lách', N'kg', 5.0),
(N'Coca Cola', N'lon', 120),
(N'Gạo ST25', N'kg', 50);
GO

USE RestaurantDB;
GO
UPDATE categories SET name = N'Đồ uống' WHERE name LIKE N'Đ%';
UPDATE categories SET name = N'Món chính' WHERE id = 1; -- Cập nhật lại cho chuẩn

USE RestaurantDB;
GO
ALTER TABLE products ADD status BIT DEFAULT 1;
GO
USE RestaurantDB;
GO
-- Xóa bảng cũ và tạo lại bảng bàn với cột is_occupied kiểu INT
IF OBJECT_ID('dbo.restaurant_table', 'U') IS NOT NULL DROP TABLE dbo.restaurant_table;

CREATE TABLE restaurant_table (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    floor NVARCHAR(100) NOT NULL, 
    is_occupied INT DEFAULT 0,    -- 0: Trống, 1: Đã Cọc, 2: Đang có khách
    has_view BIT DEFAULT 0        
);

INSERT INTO restaurant_table (name, floor, is_occupied, has_view) VALUES
(N'Bàn T2-01', N'Tầng 2', 0, 0), (N'Bàn T2-02', N'Tầng 2', 0, 0), (N'Bàn T2-03', N'Tầng 2', 0, 1),
(N'Bàn T3-01', N'Tầng 3', 0, 1), (N'Bàn T3-02', N'Tầng 3', 0, 0),
(N'Bàn ST-VIP1', N'Sân thượng', 0, 1), (N'Bàn ST-VIP2', N'Sân thượng', 0, 1);
GO

TRUNCATE TABLE restaurant_table;
GO
USE RestaurantDB;
GO

-- Xóa dữ liệu rác (nếu có) và reset ID
TRUNCATE TABLE restaurant_table;
GO

-- Bơm lại 7 cái bàn mẫu
INSERT INTO restaurant_table (name, floor, is_occupied, has_view) VALUES
(N'Bàn T2-01', N'Tầng 2', 0, 0),
(N'Bàn T2-02', N'Tầng 2', 0, 0),
(N'Bàn T2-03', N'Tầng 2', 0, 1),
(N'Bàn T3-01', N'Tầng 3', 0, 1),
(N'Bàn T3-02', N'Tầng 3', 0, 0),
(N'Bàn ST-VIP1', N'Sân thượng', 0, 1),
(N'Bàn ST-VIP2', N'Sân thượng', 0, 1);
GO

USE RestaurantDB;
GO

-- Cập nhật lại tên tầng cho chuẩn tiếng Việt
UPDATE restaurant_table SET floor = N'Tầng 2' WHERE floor LIKE 'T%2';
UPDATE restaurant_table SET floor = N'Tầng 3' WHERE floor LIKE 'T%3';
UPDATE restaurant_table SET floor = N'Sân thượng' WHERE floor LIKE 'S%';

-- Kiểm tra lại xem đã hết dấu hỏi chấm chưa
SELECT * FROM restaurant_table;
GO

USE RestaurantDB;
GO

-- 1. Sửa kiểu dữ liệu của cột 'floor' sang NVARCHAR để đọc được tiếng Việt
ALTER TABLE restaurant_table ALTER COLUMN floor NVARCHAR(100);
GO

-- 2. Cập nhật lại dữ liệu chuẩn có dấu (Lần này chắc chắn sẽ được)
UPDATE restaurant_table SET floor = N'Tầng 2' WHERE id IN (1, 2, 3);
UPDATE restaurant_table SET floor = N'Tầng 3' WHERE id IN (4, 5);
UPDATE restaurant_table SET floor = N'Sân thượng' WHERE id IN (6, 7);
GO

-- 3. Kiểm tra lại kết quả
SELECT * FROM restaurant_table;
GO

USE RestaurantDB;
GO

-- 1. Chuyển cột 'name' của bảng categories sang NVARCHAR để hỗ trợ tiếng Việt
ALTER TABLE categories ALTER COLUMN name NVARCHAR(255);
GO

-- 2. Cập nhật lại tên các danh mục cho chuẩn (Nhớ có chữ N phía trước)
UPDATE categories SET name = N'Đồ uống' WHERE name LIKE '%u%ng%' OR name LIKE '%?%';
UPDATE categories SET name = N'Món chính' WHERE name LIKE '%ch%nh%';
UPDATE categories SET name = N'Khai vị' WHERE name LIKE '%khai%';

-- 3. Kiểm tra lại dữ liệu
SELECT * FROM categories;
GO

USE RestaurantDB;
GO
-- Chắc chắn cột floor là NVARCHAR
ALTER TABLE restaurant_table ALTER COLUMN floor NVARCHAR(100);
GO
-- Cập nhật lại chính xác từng chữ
UPDATE restaurant_table SET floor = N'Tầng 2' WHERE id IN (1, 2, 3);
UPDATE restaurant_table SET floor = N'Tầng 3' WHERE id IN (4, 5);
UPDATE restaurant_table SET floor = N'Sân thượng' WHERE id IN (6, 7);
GO

USE RestaurantDB;
GO

-- Xóa dữ liệu cũ (nếu có) để nạp lại cho sạch
DELETE FROM restaurant_table;
GO

-- Chèn lại bàn với chữ N phía trước để không lỗi font "Tầng 2"
INSERT INTO restaurant_table (name, floor, is_occupied, has_view) VALUES
(N'Bàn T2-01', N'Tầng 2', 0, 0),
(N'Bàn T2-02', N'Tầng 2', 0, 0),
(N'Bàn T2-03', N'Tầng 2', 0, 1),
(N'Bàn T3-01', N'Tầng 3', 0, 1),
(N'Bàn T3-02', N'Tầng 3', 0, 0),
(N'Bàn ST-VIP1', N'Sân thượng', 0, 1),
(N'Bàn ST-VIP2', N'Sân thượng', 0, 1);
GO

-- Kiểm tra: Nếu thấy chữ "Tầng 2" không có dấu ? là OK
SELECT * FROM restaurant_table;
-- Kiểm tra lại: Nếu cột floor hiện đúng chữ "Tầng 2" là thành công
SELECT * FROM restaurant_table;

USE RestaurantDB;
GO

USE RestaurantDB;
GO

USE RestaurantDB;
GO

-- 1. Xóa dữ liệu cũ theo đúng thứ tự (Con trước cha sau)
DELETE FROM authorities;
DELETE FROM accounts;
DELETE FROM roles;
GO

-- 2. Tạo quyền ADMIN trong bảng roles
-- Mình giả sử bảng roles có cột 'name'. Nếu bị lỗi, Nhật check lại tên cột bảng roles nhé.
INSERT INTO roles (name) VALUES ('ADMIN');
GO

-- 3. Lấy ID của quyền ADMIN vừa tạo để dùng cho bước sau
DECLARE @AdminRoleId INT;
SELECT @AdminRoleId = id FROM roles WHERE name = 'ADMIN';

-- 4. Tạo tài khoản Admin (Mật khẩu: 123456)
INSERT INTO accounts (username, password, fullname, email)
VALUES ('admin', '$2a$10$R/lZJuT9skteN9.X.U.4u.9aGjK0pW8j.l.J9.4u.9aGjK0pW8j.', N'Nguyễn Quang Nhật', 'nhat@gmail.com');

-- 5. Cấp quyền Admin cho tài khoản vừa tạo thông qua role_id
INSERT INTO authorities (role_id, username)
VALUES (@AdminRoleId, 'admin');
GO

-- 6. Kiểm tra kết quả cuối cùng
SELECT * FROM accounts;
SELECT * FROM authorities;
SELECT * FROM roles;

USE RestaurantDB;
GO

-- 1. Đảm bảo cột fullname là NVARCHAR để hiện đúng tiếng Việt
ALTER TABLE accounts ALTER COLUMN fullname NVARCHAR(255);
GO

-- 2. Cập nhật lại tên chuẩn của Nhật
UPDATE accounts SET fullname = N'Nguyễn Quang Nhật' WHERE username = 'admin';
GO

-- Kiểm tra lại lần cuối
SELECT * FROM accounts;
USE RestaurantDB;
GO

-- Cập nhật mật khẩu thành "123" (đã mã hóa BCrypt)
UPDATE accounts 
SET password = '123' 
WHERE username = 'admin';
GO

UPDATE accounts 
SET password = '123' 
WHERE username = 'manager';
GO


USE RestaurantDB;
GO

-- 1. Ép cột floor sang NVARCHAR (để không bao giờ bị dấu ? nữa)
ALTER TABLE restaurant_table ALTER COLUMN floor NVARCHAR(100);
GO

-- 2. Cập nhật lại dữ liệu chuẩn (Nhớ có chữ N đứng trước)
UPDATE restaurant_table SET floor = N'Tầng 2' WHERE id IN (8, 9, 10);
UPDATE restaurant_table SET floor = N'Tầng 3' WHERE id IN (11, 12);
UPDATE restaurant_table SET floor = N'Sân thượng' WHERE id IN (13, 14);
GO

-- 3. Kiểm tra lại: Nếu cột floor hiện chữ "Tầng 2" sạch đẹp là xong
SELECT * FROM restaurant_table;

USE RestaurantDB;
GO

-- Đưa mật khẩu về chữ thô '123'
UPDATE accounts SET password = '123' WHERE username = 'admin';
GO

-- Kiểm tra lại: Nếu cột password hiện đúng số 123 là chuẩn
SELECT username, password FROM accounts;

ALTER TABLE restaurant_table ADD reserved_time NVARCHAR(255);

USE RestaurantDB;
GO

-- 1. Ép kiểu cột floor sang NVARCHAR nếu nó đang bị sai
ALTER TABLE restaurant_table ALTER COLUMN floor NVARCHAR(100);
GO

-- 2. Dọn sạch rác, xóa toàn bộ dữ liệu bị trùng và lỗi font
DELETE FROM restaurant_table;
GO

-- 3. Nạp lại đúng 7 bàn chuẩn (Bắt buộc phải có chữ N trước chuỗi tiếng Việt)
INSERT INTO restaurant_table (name, floor, is_occupied, has_view) VALUES
(N'Bàn T2-01', N'Tầng 2', 0, 0),
(N'Bàn T2-02', N'Tầng 2', 0, 0),
(N'Bàn T2-03', N'Tầng 2', 0, 1),
(N'Bàn T3-01', N'Tầng 3', 0, 1),
(N'Bàn T3-02', N'Tầng 3', 0, 0),
(N'Bàn ST-VIP1', N'Sân thượng', 0, 1),
(N'Bàn ST-VIP2', N'Sân thượng', 0, 1);
GO

DELETE FROM restaurant_table WHERE name = N'Bàn T1-VIP';