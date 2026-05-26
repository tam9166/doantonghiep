-- ==========================================
-- SCRIPT TẠO CƠ SỞ DỮ LIỆU FPOLY RESTAURANT
-- ==========================================

USE master;
GO

-- 1. Tạo database nếu chưa tồn tại
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'QuanLyNhaHang')
BEGIN
    CREATE DATABASE QuanLyNhaHang;
END
GO

USE QuanLyNhaHang;
GO

-- ==========================================
-- XÓA BẢNG NẾU ĐÃ TỒN TẠI (Theo thứ tự khóa ngoại)
-- ==========================================
IF OBJECT_ID('Applications', 'U') IS NOT NULL DROP TABLE Applications;
IF OBJECT_ID('Posts', 'U') IS NOT NULL DROP TABLE Posts;
IF OBJECT_ID('Reviews', 'U') IS NOT NULL DROP TABLE Reviews;
IF OBJECT_ID('OrderDetails', 'U') IS NOT NULL DROP TABLE OrderDetails;
IF OBJECT_ID('Recipes', 'U') IS NOT NULL DROP TABLE Recipes;
IF OBJECT_ID('Ingredients', 'U') IS NOT NULL DROP TABLE Ingredients;
IF OBJECT_ID('Orders', 'U') IS NOT NULL DROP TABLE Orders;
IF OBJECT_ID('Products', 'U') IS NOT NULL DROP TABLE Products;
IF OBJECT_ID('Categories', 'U') IS NOT NULL DROP TABLE Categories;
IF OBJECT_ID('Vouchers', 'U') IS NOT NULL DROP TABLE Vouchers;
IF OBJECT_ID('Authorities', 'U') IS NOT NULL DROP TABLE Authorities;
IF OBJECT_ID('Roles', 'U') IS NOT NULL DROP TABLE Roles;
IF OBJECT_ID('Accounts', 'U') IS NOT NULL DROP TABLE Accounts;
IF OBJECT_ID('restaurant_table', 'U') IS NOT NULL DROP TABLE restaurant_table;
GO

-- ==========================================
-- 2. TẠO CÁC BẢNG (TABLES)
-- ==========================================

-- 2.1 Bảng Tài Khoản (Accounts)
CREATE TABLE Accounts (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    fullname NVARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    photo VARCHAR(255),
    total_spent FLOAT DEFAULT 0.0,
    loyalty_points INT DEFAULT 0,
    tier VARCHAR(20) DEFAULT 'BRONZE'
);
GO

-- 2.2 Bảng Phân Quyền (Roles & Authorities)
CREATE TABLE Roles (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);
GO

CREATE TABLE Authorities (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    role_id INT NOT NULL,
    CONSTRAINT FK_Authorities_Accounts FOREIGN KEY (username) REFERENCES Accounts(username) ON DELETE CASCADE,
    CONSTRAINT FK_Authorities_Roles FOREIGN KEY (role_id) REFERENCES Roles(id) ON DELETE CASCADE
);
GO

-- 2.3 Bảng Thực Đơn (Categories & Products)
CREATE TABLE Categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(200)
);
GO

CREATE TABLE Products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    price FLOAT NOT NULL,
    image VARCHAR(255),
    description NVARCHAR(MAX),
    create_date DATE DEFAULT GETDATE(),
    available BIT DEFAULT 1,
    status BIT DEFAULT 1,
    category_id INT,
    CONSTRAINT FK_Products_Categories FOREIGN KEY (category_id) REFERENCES Categories(id)
);
GO

-- 2.4 Bảng Kho Hàng & Công Thức (Ingredients & Recipes)
CREATE TABLE Ingredients (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    quantity FLOAT DEFAULT 0,
    unit NVARCHAR(50),
    min_stock FLOAT DEFAULT 10
);
GO

CREATE TABLE Recipes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    product_id INT,
    ingredient_id BIGINT,
    amount_required FLOAT,
    CONSTRAINT FK_Recipes_Products FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    CONSTRAINT FK_Recipes_Ingredients FOREIGN KEY (ingredient_id) REFERENCES Ingredients(id) ON DELETE CASCADE
);
GO

-- 2.5 Bảng Đơn Hàng (Orders & OrderDetails)
CREATE TABLE Orders (
    id INT IDENTITY(1,1) PRIMARY KEY,
    create_date DATETIME DEFAULT GETDATE(),
    address NVARCHAR(500),
    status INT DEFAULT 1,
    note NVARCHAR(MAX),
    voucher_code VARCHAR(50),
    username VARCHAR(50),
    CONSTRAINT FK_Orders_Accounts FOREIGN KEY (username) REFERENCES Accounts(username)
);
GO

CREATE TABLE OrderDetails (
    id INT IDENTITY(1,1) PRIMARY KEY,
    price FLOAT,
    quantity INT,
    product_id INT,
    order_id INT,
    CONSTRAINT FK_OrderDetails_Products FOREIGN KEY (product_id) REFERENCES Products(id),
    CONSTRAINT FK_OrderDetails_Orders FOREIGN KEY (order_id) REFERENCES Orders(id) ON DELETE CASCADE
);
GO

-- 2.6 Bảng Đánh Giá (Reviews)
CREATE TABLE Reviews (
    id INT IDENTITY(1,1) PRIMARY KEY,
    rating INT,
    comment NVARCHAR(500),
    create_date DATETIME DEFAULT GETDATE(),
    username VARCHAR(50),
    product_id INT,
    CONSTRAINT FK_Reviews_Accounts FOREIGN KEY (username) REFERENCES Accounts(username),
    CONSTRAINT FK_Reviews_Products FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
GO

-- 2.7 Bảng Sơ Đồ Bàn (RestaurantTable)
CREATE TABLE restaurant_table (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255),
    floor NVARCHAR(255),
    is_occupied INT DEFAULT 0,
    has_view BIT DEFAULT 0,
    reserved_time VARCHAR(255),
    capacity INT DEFAULT 4,
    view_type NVARCHAR(50)
);
GO

-- 2.8 Bảng Tin Tức & Ứng Tuyển (Posts & Applications)
CREATE TABLE Posts (
    id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(300) NOT NULL,
    content NVARCHAR(MAX),
    image VARCHAR(500),
    type VARCHAR(20) NOT NULL DEFAULT 'NEWS',
    likes INT DEFAULT 0,
    active BIT DEFAULT 1,
    create_date DATETIME DEFAULT GETDATE()
);
GO

CREATE TABLE Applications (
    id INT IDENTITY(1,1) PRIMARY KEY,
    fullname NVARCHAR(200) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    cv_url VARCHAR(255),
    status NVARCHAR(50),
    create_date DATETIME DEFAULT GETDATE(),
    post_id INT,
    CONSTRAINT FK_Applications_Posts FOREIGN KEY (post_id) REFERENCES Posts(id) ON DELETE CASCADE
);
GO

-- 2.9 Bảng Khuyến Mãi (Vouchers)
CREATE TABLE Vouchers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    discount_percent INT,
    is_used BIT DEFAULT 0,
    create_date DATETIME DEFAULT GETDATE(),
    account_username VARCHAR(50),
    CONSTRAINT FK_Vouchers_Accounts FOREIGN KEY (account_username) REFERENCES Accounts(username)
);
GO


PRINT N'Tạo Database và Schema thành công!';
