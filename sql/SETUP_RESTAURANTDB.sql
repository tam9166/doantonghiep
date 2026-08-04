/*
   File cai dat SQL Server duy nhat cho project Quan Ly Nha Hang.

   Cach dung tren may moi:
   1. Mo SQL Server Management Studio, ket noi SQL Server.
   2. Mo file nay va Execute, hoac chay:
      sqlcmd -S localhost -E -i .\sql\SETUP_RESTAURANTDB.sql
   3. Cau hinh DB_URL, DB_USERNAME, DB_PASSWORD va JWT_SECRET.
   4. Khoi dong QuanlynhahangApplication. Flyway se tu dong ap dung V003-V025.

   Luu y: Script tao/lam moi database RestaurantDB va du lieu mau. Khong chay
   tren database dang co du lieu can giu lai.
*/

/* ===== BEGIN 01_create_schema.sql ===== */

SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO

USE master;
GO

IF DB_ID(N'RestaurantDB') IS NULL
BEGIN
    CREATE DATABASE RestaurantDB;
END;
GO

USE RestaurantDB;
GO

/* Drop child tables first so this script can be rerun from a clean slate. */
DROP TABLE IF EXISTS dbo.InventoryTransactions;
DROP TABLE IF EXISTS dbo.ImportInvoiceDetails;
DROP TABLE IF EXISTS dbo.ingredient_batches;
DROP TABLE IF EXISTS dbo.import_invoices;
DROP TABLE IF EXISTS dbo.timekeeping;
DROP TABLE IF EXISTS dbo.work_schedules;
DROP TABLE IF EXISTS dbo.order_details;
DROP TABLE IF EXISTS dbo.Reviews;
DROP TABLE IF EXISTS dbo.Orders;
DROP TABLE IF EXISTS dbo.recipes;
DROP TABLE IF EXISTS dbo.ingredients;
DROP TABLE IF EXISTS dbo.vouchers;
DROP TABLE IF EXISTS dbo.Applications;
DROP TABLE IF EXISTS dbo.Posts;
DROP TABLE IF EXISTS dbo.Products;
DROP TABLE IF EXISTS dbo.Categories;
DROP TABLE IF EXISTS dbo.Authorities;
DROP TABLE IF EXISTS dbo.Roles;
DROP TABLE IF EXISTS dbo.restaurant_table;
DROP TABLE IF EXISTS dbo.Accounts;
GO

CREATE TABLE dbo.Accounts (
    username        VARCHAR(50)    NOT NULL,
    password        VARCHAR(255)   NOT NULL,
    fullname        NVARCHAR(200)  NOT NULL,
    email           VARCHAR(150)   NOT NULL,
    phone           VARCHAR(20)    NULL,
    photo           VARCHAR(500)   NULL,
    enabled         BIT            NOT NULL CONSTRAINT DF_Accounts_enabled DEFAULT (1),
    total_spent     DECIMAL(18,2)  NOT NULL CONSTRAINT DF_Accounts_total_spent DEFAULT (0),
    loyalty_points  INT            NOT NULL CONSTRAINT DF_Accounts_loyalty_points DEFAULT (0),
    tier            VARCHAR(20)    NOT NULL CONSTRAINT DF_Accounts_tier DEFAULT ('BRONZE'),
    points          INT            NOT NULL CONSTRAINT DF_Accounts_points DEFAULT (0),
    membership_tier NVARCHAR(50)   NOT NULL CONSTRAINT DF_Accounts_membership_tier DEFAULT (N'BRONZE'),
    created_at      DATETIME2(0)   NOT NULL CONSTRAINT DF_Accounts_created_at DEFAULT (SYSDATETIME()),
    updated_at      DATETIME2(0)   NOT NULL CONSTRAINT DF_Accounts_updated_at DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_Accounts PRIMARY KEY (username),
    CONSTRAINT UQ_Accounts_email UNIQUE (email),
    CONSTRAINT CK_Accounts_total_spent_non_negative CHECK (total_spent >= 0),
    CONSTRAINT CK_Accounts_loyalty_points_non_negative CHECK (loyalty_points >= 0),
    CONSTRAINT CK_Accounts_points_non_negative CHECK (points >= 0),
    CONSTRAINT CK_Accounts_tier CHECK (tier IN ('BRONZE','SILVER','GOLD','DIAMOND'))
);
GO

CREATE TABLE dbo.Roles (
    id   INT IDENTITY(1,1) NOT NULL,
    name VARCHAR(50)       NOT NULL,
    CONSTRAINT PK_Roles PRIMARY KEY (id),
    CONSTRAINT UQ_Roles_name UNIQUE (name)
);
GO

CREATE TABLE dbo.Authorities (
    id       INT IDENTITY(1,1) NOT NULL,
    username VARCHAR(50)       NOT NULL,
    role_id  INT               NOT NULL,
    CONSTRAINT PK_Authorities PRIMARY KEY (id),
    CONSTRAINT UQ_Authorities_username_role UNIQUE (username, role_id),
    CONSTRAINT FK_Authorities_Accounts FOREIGN KEY (username)
        REFERENCES dbo.Accounts(username) ON DELETE CASCADE,
    CONSTRAINT FK_Authorities_Roles FOREIGN KEY (role_id)
        REFERENCES dbo.Roles(id) ON DELETE CASCADE
);
GO

CREATE TABLE dbo.Categories (
    id          INT IDENTITY(1,1) NOT NULL,
    name        NVARCHAR(100)     NOT NULL,
    description NVARCHAR(500)     NULL,
    active      BIT               NOT NULL CONSTRAINT DF_Categories_active DEFAULT (1),
    sort_order  INT               NOT NULL CONSTRAINT DF_Categories_sort_order DEFAULT (0),
    CONSTRAINT PK_Categories PRIMARY KEY (id),
    CONSTRAINT UQ_Categories_name UNIQUE (name)
);
GO

CREATE TABLE dbo.Products (
    id               INT IDENTITY(1,1) NOT NULL,
    name             NVARCHAR(200)     NOT NULL,
    price            DECIMAL(18,2)     NOT NULL,
    cost_price       DECIMAL(18,2)     NOT NULL CONSTRAINT DF_Products_cost_price DEFAULT (0),
    tax_rate         DECIMAL(5,2)      NOT NULL CONSTRAINT DF_Products_tax_rate DEFAULT (8),
    sold_count       INT               NOT NULL CONSTRAINT DF_Products_sold_count DEFAULT (0),
    average_rating   DECIMAL(3,2)      NOT NULL CONSTRAINT DF_Products_average_rating DEFAULT (0),
    preparation_time INT               NOT NULL CONSTRAINT DF_Products_preparation_time DEFAULT (10),
    image            VARCHAR(500)      NULL,
    description      NVARCHAR(MAX)     NULL,
    create_date      DATETIME2(0)      NOT NULL CONSTRAINT DF_Products_create_date DEFAULT (SYSDATETIME()),
    available        BIT               NOT NULL CONSTRAINT DF_Products_available DEFAULT (1),
    status           BIT               NOT NULL CONSTRAINT DF_Products_status DEFAULT (1),
    status_name      VARCHAR(20)       NOT NULL CONSTRAINT DF_Products_status_name DEFAULT ('AVAILABLE'),
    category_id      INT               NULL,
    CONSTRAINT PK_Products PRIMARY KEY (id),
    CONSTRAINT FK_Products_Categories FOREIGN KEY (category_id) REFERENCES dbo.Categories(id),
    CONSTRAINT CK_Products_price_non_negative CHECK (price >= 0),
    CONSTRAINT CK_Products_cost_price_non_negative CHECK (cost_price >= 0),
    CONSTRAINT CK_Products_tax_rate_range CHECK (tax_rate >= 0 AND tax_rate <= 100),
    CONSTRAINT CK_Products_sold_count_non_negative CHECK (sold_count >= 0),
    CONSTRAINT CK_Products_average_rating_range CHECK (average_rating >= 0 AND average_rating <= 5),
    CONSTRAINT CK_Products_preparation_time_positive CHECK (preparation_time > 0),
    CONSTRAINT CK_Products_status_name CHECK (status_name IN ('AVAILABLE','OUT_OF_STOCK','DISABLED'))
);
GO

CREATE TABLE dbo.ingredients (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    name            NVARCHAR(200)        NOT NULL,
    quantity        DECIMAL(18,3)        NOT NULL CONSTRAINT DF_ingredients_quantity DEFAULT (0),
    unit            NVARCHAR(50)         NOT NULL,
    min_stock       DECIMAL(18,3)        NOT NULL CONSTRAINT DF_ingredients_min_stock DEFAULT (0),
    unit_price      DECIMAL(18,2)        NOT NULL CONSTRAINT DF_ingredients_unit_price DEFAULT (0),
    image           VARCHAR(500)         NULL,
    shelf_life_days INT                  NOT NULL CONSTRAINT DF_ingredients_shelf_life_days DEFAULT (30),
    active          BIT                  NOT NULL CONSTRAINT DF_ingredients_active DEFAULT (1),
    CONSTRAINT PK_ingredients PRIMARY KEY (id),
    CONSTRAINT UQ_ingredients_name UNIQUE (name),
    CONSTRAINT CK_ingredients_quantity_non_negative CHECK (quantity >= 0),
    CONSTRAINT CK_ingredients_min_stock_non_negative CHECK (min_stock >= 0),
    CONSTRAINT CK_ingredients_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT CK_ingredients_shelf_life_days_positive CHECK (shelf_life_days > 0)
);
GO

CREATE TABLE dbo.recipes (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    product_id      INT                  NOT NULL,
    ingredient_id   BIGINT               NOT NULL,
    amount_required DECIMAL(18,3)        NOT NULL,
    CONSTRAINT PK_recipes PRIMARY KEY (id),
    CONSTRAINT UQ_recipes_product_ingredient UNIQUE (product_id, ingredient_id),
    CONSTRAINT FK_recipes_Products FOREIGN KEY (product_id) REFERENCES dbo.Products(id) ON DELETE CASCADE,
    CONSTRAINT FK_recipes_ingredients FOREIGN KEY (ingredient_id) REFERENCES dbo.ingredients(id),
    CONSTRAINT CK_recipes_amount_required_positive CHECK (amount_required > 0)
);
GO

CREATE TABLE dbo.restaurant_table (
    id            INT IDENTITY(1,1) NOT NULL,
    name          NVARCHAR(100)     NOT NULL,
    floor         NVARCHAR(100)     NULL,
    is_occupied   INT               NOT NULL CONSTRAINT DF_restaurant_table_is_occupied DEFAULT (0),
    status        VARCHAR(20)       NOT NULL CONSTRAINT DF_restaurant_table_status DEFAULT ('AVAILABLE'),
    has_view      BIT               NOT NULL CONSTRAINT DF_restaurant_table_has_view DEFAULT (0),
    reserved_time DATETIME2(0)      NULL,
    capacity      INT               NOT NULL CONSTRAINT DF_restaurant_table_capacity DEFAULT (4),
    view_type     NVARCHAR(50)      NULL,
    CONSTRAINT PK_restaurant_table PRIMARY KEY (id),
    CONSTRAINT UQ_restaurant_table_name UNIQUE (name),
    CONSTRAINT CK_restaurant_table_status CHECK (status IN ('AVAILABLE','OCCUPIED','RESERVED','CLEANING','DISABLED')),
    CONSTRAINT CK_restaurant_table_capacity_positive CHECK (capacity > 0),
    CONSTRAINT CK_restaurant_table_is_occupied CHECK (is_occupied IN (0,1))
);
GO

CREATE TABLE dbo.vouchers (
    id                  BIGINT IDENTITY(1,1) NOT NULL,
    code                VARCHAR(50)          NOT NULL,
    voucher_name        NVARCHAR(200)        NOT NULL,
    discount_type       VARCHAR(10)          NOT NULL,
    discount_value      DECIMAL(18,2)        NOT NULL,
    discount_percent    INT                  NULL,
    max_discount_amount DECIMAL(18,2)        NULL,
    min_order_amount    DECIMAL(18,2)        NOT NULL CONSTRAINT DF_vouchers_min_order_amount DEFAULT (0),
    start_date          DATETIME2(0)         NOT NULL,
    end_date            DATETIME2(0)         NOT NULL,
    usage_limit         INT                  NULL,
    used_count          INT                  NOT NULL CONSTRAINT DF_vouchers_used_count DEFAULT (0),
    active              BIT                  NOT NULL CONSTRAINT DF_vouchers_active DEFAULT (1),
    is_used             BIT                  NOT NULL CONSTRAINT DF_vouchers_is_used DEFAULT (0),
    create_date         DATETIME2(0)         NOT NULL CONSTRAINT DF_vouchers_create_date DEFAULT (SYSDATETIME()),
    account_username    VARCHAR(50)          NULL,
    CONSTRAINT PK_vouchers PRIMARY KEY (id),
    CONSTRAINT UQ_vouchers_code UNIQUE (code),
    CONSTRAINT FK_vouchers_Accounts FOREIGN KEY (account_username) REFERENCES dbo.Accounts(username),
    CONSTRAINT CK_vouchers_discount_type CHECK (discount_type IN ('PERCENT','AMOUNT')),
    CONSTRAINT CK_vouchers_discount_value_positive CHECK (discount_value > 0),
    CONSTRAINT CK_vouchers_discount_percent_range CHECK (discount_percent IS NULL OR (discount_percent > 0 AND discount_percent <= 100)),
    CONSTRAINT CK_vouchers_max_discount_non_negative CHECK (max_discount_amount IS NULL OR max_discount_amount >= 0),
    CONSTRAINT CK_vouchers_min_order_non_negative CHECK (min_order_amount >= 0),
    CONSTRAINT CK_vouchers_usage_limit_positive CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT CK_vouchers_used_count_non_negative CHECK (used_count >= 0),
    CONSTRAINT CK_vouchers_date_range CHECK (end_date >= start_date)
);
GO

CREATE TABLE dbo.import_invoices (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    invoice_code  VARCHAR(50)          NOT NULL,
    supplier      NVARCHAR(255)        NOT NULL,
    supplier_name NVARCHAR(255)        NOT NULL,
    import_date   DATETIME2(0)         NOT NULL CONSTRAINT DF_import_invoices_import_date DEFAULT (SYSDATETIME()),
    total_amount  DECIMAL(18,2)        NOT NULL CONSTRAINT DF_import_invoices_total_amount DEFAULT (0),
    created_by    VARCHAR(50)          NULL,
    note          NVARCHAR(MAX)        NULL,
    CONSTRAINT PK_import_invoices PRIMARY KEY (id),
    CONSTRAINT UQ_import_invoices_invoice_code UNIQUE (invoice_code),
    CONSTRAINT FK_import_invoices_Accounts FOREIGN KEY (created_by) REFERENCES dbo.Accounts(username),
    CONSTRAINT CK_import_invoices_total_amount_non_negative CHECK (total_amount >= 0)
);
GO

CREATE TABLE dbo.ingredient_batches (
    id                 BIGINT IDENTITY(1,1) NOT NULL,
    ingredient_id      BIGINT               NOT NULL,
    import_invoice_id  BIGINT               NULL,
    batch_code         VARCHAR(50)          NOT NULL CONSTRAINT DF_ingredient_batches_batch_code DEFAULT ('AUTO-' + CONVERT(VARCHAR(36), NEWID())),
    quantity           DECIMAL(18,3)        NOT NULL,
    remaining_quantity DECIMAL(18,3)        NOT NULL CONSTRAINT DF_ingredient_batches_remaining_quantity DEFAULT (0),
    import_date        DATETIME2(0)         NOT NULL CONSTRAINT DF_ingredient_batches_import_date DEFAULT (SYSDATETIME()),
    expiration_date    DATE                 NULL,
    expiry_date        DATE                 NULL,
    unit_price         DECIMAL(18,2)        NOT NULL,
    supplier_name      NVARCHAR(255)        NULL,
    note               NVARCHAR(MAX)        NULL,
    CONSTRAINT PK_ingredient_batches PRIMARY KEY (id),
    CONSTRAINT UQ_ingredient_batches_batch_code UNIQUE (batch_code),
    CONSTRAINT FK_ingredient_batches_ingredients FOREIGN KEY (ingredient_id) REFERENCES dbo.ingredients(id),
    CONSTRAINT FK_ingredient_batches_import_invoices FOREIGN KEY (import_invoice_id) REFERENCES dbo.import_invoices(id),
    CONSTRAINT CK_ingredient_batches_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_ingredient_batches_remaining_non_negative CHECK (remaining_quantity >= 0),
    CONSTRAINT CK_ingredient_batches_remaining_lte_quantity CHECK (remaining_quantity <= quantity),
    CONSTRAINT CK_ingredient_batches_unit_price_non_negative CHECK (unit_price >= 0)
);
GO

CREATE TRIGGER dbo.TR_ingredient_batches_initialize_remaining_quantity
ON dbo.ingredient_batches
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE batch
    SET remaining_quantity = inserted.quantity
    FROM dbo.ingredient_batches batch
    INNER JOIN inserted ON inserted.id = batch.id
    WHERE inserted.batch_code LIKE 'AUTO-%'
      AND inserted.remaining_quantity = 0;
END;
GO

CREATE TABLE dbo.ImportInvoiceDetails (
    id            BIGINT IDENTITY(1,1) NOT NULL,
    invoice_id    BIGINT               NOT NULL,
    ingredient_id BIGINT               NOT NULL,
    quantity      DECIMAL(18,3)        NOT NULL,
    unit_price    DECIMAL(18,2)        NOT NULL,
    expiry_date   DATE                 NULL,
    total_price   DECIMAL(18,2)        NOT NULL,
    CONSTRAINT PK_ImportInvoiceDetails PRIMARY KEY (id),
    CONSTRAINT FK_ImportInvoiceDetails_import_invoices FOREIGN KEY (invoice_id) REFERENCES dbo.import_invoices(id) ON DELETE CASCADE,
    CONSTRAINT FK_ImportInvoiceDetails_ingredients FOREIGN KEY (ingredient_id) REFERENCES dbo.ingredients(id),
    CONSTRAINT CK_ImportInvoiceDetails_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_ImportInvoiceDetails_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT CK_ImportInvoiceDetails_total_price_non_negative CHECK (total_price >= 0)
);
GO

CREATE TABLE dbo.Orders (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    create_date     DATETIME2(0)      NOT NULL CONSTRAINT DF_Orders_create_date DEFAULT (SYSDATETIME()),
    username        VARCHAR(50)       NULL,
    table_id        INT               NULL,
    order_type      VARCHAR(20)       NOT NULL CONSTRAINT DF_Orders_order_type DEFAULT ('DINE_IN'),
    status          INT               NOT NULL CONSTRAINT DF_Orders_status DEFAULT (0),
    status_name     VARCHAR(20)       NOT NULL CONSTRAINT DF_Orders_status_name DEFAULT ('PENDING'),
    customer_name   NVARCHAR(200)     NULL,
    customer_phone  VARCHAR(20)       NULL,
    reservation_time DATETIME2(0)     NULL,
    address         NVARCHAR(500)     NULL,
    note            NVARCHAR(MAX)     NULL,
    voucher_code    VARCHAR(50)       NULL,
    sub_total       DECIMAL(18,2)     NOT NULL CONSTRAINT DF_Orders_sub_total DEFAULT (0),
    discount_amount DECIMAL(18,2)     NOT NULL CONSTRAINT DF_Orders_discount_amount DEFAULT (0),
    tax_amount      DECIMAL(18,2)     NOT NULL CONSTRAINT DF_Orders_tax_amount DEFAULT (0),
    total_amount    DECIMAL(18,2)     NOT NULL CONSTRAINT DF_Orders_total_amount DEFAULT (0),
    deposit         DECIMAL(18,2)     NOT NULL CONSTRAINT DF_Orders_deposit DEFAULT (0),
    is_paid         BIT               NOT NULL CONSTRAINT DF_Orders_is_paid DEFAULT (0),
    payment_method  VARCHAR(30)       NULL,
    payment_time    DATETIME2(0)      NULL,
    created_by      VARCHAR(50)       NULL,
    CONSTRAINT PK_Orders PRIMARY KEY (id),
    CONSTRAINT FK_Orders_Accounts FOREIGN KEY (username) REFERENCES dbo.Accounts(username),
    CONSTRAINT FK_Orders_created_by_Accounts FOREIGN KEY (created_by) REFERENCES dbo.Accounts(username),
    CONSTRAINT FK_Orders_restaurant_table FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id),
    CONSTRAINT FK_Orders_vouchers FOREIGN KEY (voucher_code) REFERENCES dbo.vouchers(code),
    CONSTRAINT CK_Orders_order_type CHECK (order_type IN ('DINE_IN','TAKE_AWAY','DELIVERY','RESERVATION')),
    CONSTRAINT CK_Orders_status_legacy CHECK (status IN (0,1,2,3,4,5,6)),
    CONSTRAINT CK_Orders_status_name CHECK (status_name IN ('PENDING','COOKING','READY','SERVED','PAID','CANCELLED','RESERVED')),
    CONSTRAINT CK_Orders_money_non_negative CHECK (sub_total >= 0 AND discount_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0 AND deposit >= 0)
);
GO

CREATE TABLE dbo.order_details (
    id            INT IDENTITY(1,1) NOT NULL,
    order_id      BIGINT            NOT NULL,
    product_id    INT               NOT NULL,
    price         DECIMAL(18,2)     NOT NULL CONSTRAINT DF_OrderDetails_price DEFAULT (0),
    unit_price    DECIMAL(18,2)     NOT NULL,
    quantity      INT               NOT NULL,
    line_subtotal DECIMAL(18,2)     NOT NULL CONSTRAINT DF_OrderDetails_line_subtotal DEFAULT (0),
    tax_rate      DECIMAL(5,2)      NOT NULL CONSTRAINT DF_OrderDetails_tax_rate DEFAULT (8),
    tax_amount    DECIMAL(18,2)     NOT NULL CONSTRAINT DF_OrderDetails_tax_amount DEFAULT (0),
    line_total    DECIMAL(18,2)     NOT NULL CONSTRAINT DF_OrderDetails_line_total DEFAULT (0),
    status        INT               NOT NULL CONSTRAINT DF_OrderDetails_status DEFAULT (0),
    status_name   VARCHAR(20)       NOT NULL CONSTRAINT DF_OrderDetails_status_name DEFAULT ('PENDING'),
    note          NVARCHAR(500)     NULL,
    CONSTRAINT PK_OrderDetails PRIMARY KEY (id),
    CONSTRAINT FK_OrderDetails_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetails_Products FOREIGN KEY (product_id) REFERENCES dbo.Products(id),
    CONSTRAINT CK_OrderDetails_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_OrderDetails_money_non_negative CHECK (price >= 0 AND unit_price >= 0 AND line_subtotal >= 0 AND tax_amount >= 0 AND line_total >= 0),
    CONSTRAINT CK_OrderDetails_tax_rate_range CHECK (tax_rate >= 0 AND tax_rate <= 100),
    CONSTRAINT CK_OrderDetails_status_legacy CHECK (status IN (0,1,2,3,4)),
    CONSTRAINT CK_OrderDetails_status_name CHECK (status_name IN ('PENDING','COOKING','READY','SERVED','CANCELLED'))
);
GO

CREATE TABLE dbo.Reviews (
    id          INT IDENTITY(1,1) NOT NULL,
    username    VARCHAR(50)       NOT NULL,
    product_id  INT               NOT NULL,
    order_id    BIGINT            NULL,
    rating      INT               NOT NULL,
    comment     NVARCHAR(500)     NULL,
    create_date DATETIME2(0)      NOT NULL CONSTRAINT DF_Reviews_create_date DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_Reviews PRIMARY KEY (id),
    CONSTRAINT FK_Reviews_Accounts FOREIGN KEY (username) REFERENCES dbo.Accounts(username),
    CONSTRAINT FK_Reviews_Products FOREIGN KEY (product_id) REFERENCES dbo.Products(id) ON DELETE CASCADE,
    CONSTRAINT FK_Reviews_Orders FOREIGN KEY (order_id) REFERENCES dbo.Orders(id),
    CONSTRAINT CK_Reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
GO

CREATE TABLE dbo.Posts (
    id          INT IDENTITY(1,1) NOT NULL,
    title       NVARCHAR(300)     NOT NULL,
    content     NVARCHAR(MAX)     NULL,
    image       VARCHAR(500)      NULL,
    type        VARCHAR(20)       NOT NULL CONSTRAINT DF_Posts_type DEFAULT ('NEWS'),
    likes       INT               NOT NULL CONSTRAINT DF_Posts_likes DEFAULT (0),
    active      BIT               NOT NULL CONSTRAINT DF_Posts_active DEFAULT (1),
    create_date DATETIME2(0)      NOT NULL CONSTRAINT DF_Posts_create_date DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_Posts PRIMARY KEY (id),
    CONSTRAINT CK_Posts_type CHECK (type IN ('NEWS','RECRUITMENT')),
    CONSTRAINT CK_Posts_likes_non_negative CHECK (likes >= 0)
);
GO

CREATE TABLE dbo.Applications (
    id          INT IDENTITY(1,1) NOT NULL,
    fullname    NVARCHAR(200)     NOT NULL,
    phone       VARCHAR(20)       NULL,
    email       VARCHAR(150)      NULL,
    message     NVARCHAR(MAX)     NULL,
    post_id     INT               NULL,
    postId      INT               NULL,
    cv_file     NVARCHAR(500)     NULL,
    status      VARCHAR(30)       NOT NULL CONSTRAINT DF_Applications_status DEFAULT ('NEW'),
    create_date DATETIME2(0)      NOT NULL CONSTRAINT DF_Applications_create_date DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_Applications PRIMARY KEY (id),
    CONSTRAINT FK_Applications_Posts FOREIGN KEY (post_id) REFERENCES dbo.Posts(id),
    CONSTRAINT CK_Applications_status CHECK (status IN ('NEW','REVIEWING','INTERVIEW','HIRED','REJECTED'))
);
GO

CREATE TABLE dbo.work_schedules (
    id         BIGINT IDENTITY(1,1) NOT NULL,
    username   VARCHAR(50)          NOT NULL,
    work_date  DATE                 NOT NULL,
    shift      NVARCHAR(50)         NOT NULL,
    shift_name NVARCHAR(50)         NOT NULL,
    start_time TIME(0)              NOT NULL,
    end_time   TIME(0)              NOT NULL,
    status     VARCHAR(20)          NOT NULL CONSTRAINT DF_work_schedules_status DEFAULT ('SCHEDULED'),
    note       NVARCHAR(500)        NULL,
    CONSTRAINT PK_work_schedules PRIMARY KEY (id),
    CONSTRAINT FK_work_schedules_Accounts FOREIGN KEY (username) REFERENCES dbo.Accounts(username),
    CONSTRAINT CK_work_schedules_status CHECK (status IN ('SCHEDULED','COMPLETED','ABSENT','CANCELLED'))
);
GO

CREATE TABLE dbo.timekeeping (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    username       VARCHAR(50)          NOT NULL,
    work_date      DATE                 NOT NULL,
    check_in_time  DATETIME2(0)         NULL,
    check_out_time DATETIME2(0)         NULL,
    total_hours    DECIMAL(5,2)         NOT NULL CONSTRAINT DF_timekeeping_total_hours DEFAULT (0),
    status         NVARCHAR(50)         NOT NULL CONSTRAINT DF_timekeeping_status DEFAULT (N'NOT_CHECKED_IN'),
    note           NVARCHAR(500)        NULL,
    CONSTRAINT PK_timekeeping PRIMARY KEY (id),
    CONSTRAINT FK_timekeeping_Accounts FOREIGN KEY (username) REFERENCES dbo.Accounts(username),
    CONSTRAINT CK_timekeeping_total_hours_non_negative CHECK (total_hours >= 0),
    CONSTRAINT CK_timekeeping_status CHECK (status IN (N'NOT_CHECKED_IN',N'WORKING',N'COMPLETED',N'LATE',N'ABSENT',N'ÄÃºng giá»',N'Äi trá»…',N'Vá» sá»›m',N'HoÃ n thÃ nh'))
);
GO

CREATE TABLE dbo.InventoryTransactions (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    ingredient_id    BIGINT               NOT NULL,
    batch_id         BIGINT               NULL,
    transaction_type VARCHAR(30)          NOT NULL,
    quantity         DECIMAL(18,3)        NOT NULL,
    before_quantity  DECIMAL(18,3)        NOT NULL,
    after_quantity   DECIMAL(18,3)        NOT NULL,
    reason           NVARCHAR(500)        NULL,
    created_by       VARCHAR(50)          NULL,
    created_at       DATETIME2(0)         NOT NULL CONSTRAINT DF_InventoryTransactions_created_at DEFAULT (SYSDATETIME()),
    CONSTRAINT PK_InventoryTransactions PRIMARY KEY (id),
    CONSTRAINT FK_InventoryTransactions_ingredients FOREIGN KEY (ingredient_id) REFERENCES dbo.ingredients(id),
    CONSTRAINT FK_InventoryTransactions_ingredient_batches FOREIGN KEY (batch_id) REFERENCES dbo.ingredient_batches(id),
    CONSTRAINT FK_InventoryTransactions_Accounts FOREIGN KEY (created_by) REFERENCES dbo.Accounts(username),
    CONSTRAINT CK_InventoryTransactions_type CHECK (transaction_type IN ('IMPORT','EXPORT_FOR_ORDER','ADJUSTMENT','EXPIRED','DAMAGED','CANCEL_ORDER_RESTORE')),
    CONSTRAINT CK_InventoryTransactions_quantity_positive CHECK (quantity > 0),
    CONSTRAINT CK_InventoryTransactions_quantities_non_negative CHECK (before_quantity >= 0 AND after_quantity >= 0)
);
GO

CREATE INDEX IX_Orders_username ON dbo.Orders(username);
CREATE INDEX IX_Orders_status ON dbo.Orders(status_name);
CREATE INDEX IX_Orders_create_date ON dbo.Orders(create_date);
CREATE INDEX IX_Orders_table_id ON dbo.Orders(table_id);
CREATE INDEX IX_OrderDetails_order_id ON dbo.order_details(order_id);
CREATE INDEX IX_OrderDetails_product_id ON dbo.order_details(product_id);
CREATE INDEX IX_OrderDetails_status ON dbo.order_details(status_name);
CREATE INDEX IX_Products_category_id ON dbo.Products(category_id);
CREATE INDEX IX_Reviews_product_id ON dbo.Reviews(product_id);
CREATE INDEX IX_recipes_product_id ON dbo.recipes(product_id);
CREATE INDEX IX_recipes_ingredient_id ON dbo.recipes(ingredient_id);
CREATE INDEX IX_ingredient_batches_ingredient_id ON dbo.ingredient_batches(ingredient_id);
CREATE INDEX IX_ingredient_batches_expiry_date ON dbo.ingredient_batches(expiry_date);
CREATE INDEX IX_vouchers_code ON dbo.vouchers(code);
CREATE INDEX IX_InventoryTransactions_ingredient_id ON dbo.InventoryTransactions(ingredient_id);
CREATE INDEX IX_InventoryTransactions_created_at ON dbo.InventoryTransactions(created_at);
GO

PRINT N'01_create_schema.sql completed for RestaurantDB.';
GO


/* ===== END 01_create_schema.sql ===== */


/* ===== BEGIN 02_seed_data.sql ===== */

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
('admin',   @Password123, N'Quáº£n trá»‹ há»‡ thá»‘ng',  'admin@restaurant.local',   '0900000001', 1, 0,       0,    'DIAMOND', 0,    N'DIAMOND'),
('manager', @Password123, N'Quáº£n lÃ½ nhÃ  hÃ ng',   'manager@restaurant.local', '0900000002', 1, 0,       0,    'DIAMOND', 0,    N'DIAMOND'),
('waiter',  @Password123, N'NhÃ¢n viÃªn phá»¥c vá»¥',  'waiter@restaurant.local',  '0900000003', 1, 0,       0,    'BRONZE',  0,    N'BRONZE'),
('kitchen', @Password123, N'NhÃ¢n viÃªn báº¿p',      'kitchen@restaurant.local', '0900000004', 1, 0,       0,    'BRONZE',  0,    N'BRONZE'),
('cashier', @Password123, N'NhÃ¢n viÃªn thu ngÃ¢n', 'cashier@restaurant.local', '0900000005', 1, 0,       0,    'BRONZE',  0,    N'BRONZE'),
('customer',@Password123, N'KhÃ¡ch hÃ ng máº«u',     'customer@restaurant.local','0900000006', 1, 1250000, 1250, 'GOLD',    1250, N'GOLD');

INSERT INTO dbo.Authorities(username, role_id)
SELECT 'admin', id FROM dbo.Roles WHERE name = 'ADMIN'
UNION ALL SELECT 'manager', id FROM dbo.Roles WHERE name = 'MANAGER'
UNION ALL SELECT 'waiter', id FROM dbo.Roles WHERE name = 'WAITER'
UNION ALL SELECT 'kitchen', id FROM dbo.Roles WHERE name = 'KITCHEN'
UNION ALL SELECT 'cashier', id FROM dbo.Roles WHERE name = 'CASHIER'
UNION ALL SELECT 'customer', id FROM dbo.Roles WHERE name = 'CUSTOMER';

INSERT INTO dbo.Categories(name, description, sort_order)
VALUES
(N'Khai vá»‹', N'MÃ³n Äƒn nháº¹ má»Ÿ Ä‘áº§u bá»¯a Äƒn', 1),
(N'MÃ³n chÃ­nh', N'MÃ³n Äƒn chÃ­nh phá»¥c vá»¥ táº¡i bÃ n', 2),
(N'Láº©u', N'Láº©u dÃ¹ng chung theo nhÃ³m', 3),
(N'NÆ°á»›ng', N'MÃ³n nÆ°á»›ng than vÃ  nÆ°á»›ng sá»‘t', 4),
(N'CÆ¡m - MÃ¬', N'CÆ¡m, mÃ¬ vÃ  mÃ³n no', 5),
(N'Háº£i sáº£n', N'MÃ³n háº£i sáº£n tÆ°Æ¡i', 6),
(N'TrÃ¡ng miá»‡ng', N'BÃ¡nh ngá»t vÃ  trÃ¡i cÃ¢y', 7),
(N'Äá»“ uá»‘ng', N'NÆ°á»›c ngá»t, trÃ , cÃ  phÃª', 8);

INSERT INTO dbo.Products(name, price, cost_price, tax_rate, sold_count, average_rating, preparation_time, image, description, available, status, status_name, category_id)
VALUES
(N'Gá»i cuá»‘n tÃ´m thá»‹t', 45000, 22000, 8, 120, 4.5, 8,  'goi-cuon.jpg', N'Gá»i cuá»‘n tÆ°Æ¡i dÃ¹ng cÃ¹ng nÆ°á»›c cháº¥m Ä‘áº­u phá»™ng', 1, 1, 'AVAILABLE', 1),
(N'Cháº£ giÃ² háº£i sáº£n', 69000, 33000, 8, 98, 4.4, 12, 'cha-gio-hai-san.jpg', N'Cháº£ giÃ² giÃ²n nhÃ¢n háº£i sáº£n', 1, 1, 'AVAILABLE', 1),
(N'Salad bÃ² Ã¡p cháº£o', 79000, 42000, 8, 65, 4.3, 10, 'salad-bo.jpg', N'Salad rau xanh vÃ  bÃ² Ã¡p cháº£o', 1, 1, 'AVAILABLE', 1),
(N'SÃºp cua', 55000, 26000, 8, 80, 4.2, 10, 'sup-cua.jpg', N'SÃºp cua trá»©ng cÃºt nÃ³ng', 1, 1, 'AVAILABLE', 1),
(N'BÃ² lÃºc láº¯c khoai tÃ¢y', 159000, 84000, 8, 140, 4.7, 18, 'bo-luc-lac.jpg', N'BÃ² má»m xÃ o sá»‘t tiÃªu Ä‘en', 1, 1, 'AVAILABLE', 2),
(N'GÃ  nÆ°á»›ng máº­t ong', 139000, 69000, 8, 130, 4.6, 22, 'ga-nuong-mat-ong.jpg', N'GÃ  nÆ°á»›ng máº­t ong da giÃ²n', 1, 1, 'AVAILABLE', 2),
(N'SÆ°á»n non sá»‘t BBQ', 179000, 92000, 8, 90, 4.5, 25, 'suon-bbq.jpg', N'SÆ°á»n non nÆ°á»›ng sá»‘t BBQ', 1, 1, 'AVAILABLE', 2),
(N'CÃ¡ há»“i sá»‘t chanh dÃ¢y', 189000, 112000, 8, 75, 4.6, 20, 'ca-hoi.jpg', N'CÃ¡ há»“i Ã¡p cháº£o sá»‘t chanh dÃ¢y', 1, 1, 'AVAILABLE', 2),
(N'Láº©u thÃ¡i háº£i sáº£n', 299000, 175000, 8, 88, 4.8, 25, 'lau-thai.jpg', N'Láº©u thÃ¡i chua cay háº£i sáº£n', 1, 1, 'AVAILABLE', 3),
(N'Láº©u bÃ² nhÃºng dáº¥m', 279000, 155000, 8, 70, 4.4, 24, 'lau-bo.jpg', N'Láº©u bÃ² chua thanh', 1, 1, 'AVAILABLE', 3),
(N'Láº©u gÃ  lÃ¡ Ã©', 249000, 132000, 8, 64, 4.3, 24, 'lau-ga-la-e.jpg', N'Láº©u gÃ  lÃ¡ Ã© thÆ¡m nháº¹', 1, 1, 'AVAILABLE', 3),
(N'Láº©u náº¥m chay', 219000, 105000, 8, 35, 4.2, 22, 'lau-nam.jpg', N'Láº©u náº¥m thanh Ä‘áº¡m', 1, 1, 'AVAILABLE', 3),
(N'Ba chá»‰ bÃ² nÆ°á»›ng', 169000, 89000, 8, 110, 4.6, 16, 'ba-chi-bo.jpg', N'Ba chá»‰ bÃ² Æ°á»›p sá»‘t nÆ°á»›ng', 1, 1, 'AVAILABLE', 4),
(N'SÆ°á»n cÃ¢y nÆ°á»›ng muá»‘i á»›t', 189000, 98000, 8, 92, 4.5, 20, 'suon-cay.jpg', N'SÆ°á»n cÃ¢y cay nháº¹', 1, 1, 'AVAILABLE', 4),
(N'Má»±c nÆ°á»›ng sa táº¿', 179000, 105000, 8, 84, 4.4, 18, 'muc-nuong.jpg', N'Má»±c nÆ°á»›ng sa táº¿ thÆ¡m cay', 1, 1, 'AVAILABLE', 4),
(N'Äáº­u hÅ© nÆ°á»›ng giáº¥y báº¡c', 89000, 41000, 8, 46, 4.1, 15, 'dau-hu-nuong.jpg', N'Äáº­u hÅ© non sá»‘t náº¥m', 1, 1, 'AVAILABLE', 4),
(N'CÆ¡m chiÃªn háº£i sáº£n', 99000, 48000, 8, 150, 4.3, 12, 'com-chien-hai-san.jpg', N'CÆ¡m chiÃªn tÃ´m má»±c', 1, 1, 'AVAILABLE', 5),
(N'MÃ¬ xÃ o bÃ²', 89000, 42000, 8, 135, 4.2, 12, 'mi-xao-bo.jpg', N'MÃ¬ xÃ o bÃ² rau cá»§', 1, 1, 'AVAILABLE', 5),
(N'CÆ¡m gÃ  xá»‘i má»¡', 79000, 36000, 8, 120, 4.1, 14, 'com-ga.jpg', N'CÆ¡m gÃ  xá»‘i má»¡ da giÃ²n', 1, 1, 'AVAILABLE', 5),
(N'Miáº¿n cua tay cáº§m', 129000, 72000, 8, 50, 4.4, 16, 'mien-cua.jpg', N'Miáº¿n cua tay cáº§m nÃ³ng', 1, 1, 'AVAILABLE', 5),
(N'TÃ´m sÃº háº¥p dá»«a', 219000, 135000, 8, 60, 4.7, 18, 'tom-hap-dua.jpg', N'TÃ´m sÃº háº¥p nÆ°á»›c dá»«a', 1, 1, 'AVAILABLE', 6),
(N'Cua rang me', 329000, 230000, 8, 35, 4.8, 25, 'cua-rang-me.jpg', N'Cua thá»‹t rang sá»‘t me', 1, 1, 'AVAILABLE', 6),
(N'NghÃªu háº¥p sáº£', 99000, 52000, 8, 72, 4.2, 12, 'ngheu-hap-sa.jpg', N'NghÃªu háº¥p sáº£ á»›t', 1, 1, 'AVAILABLE', 6),
(N'SÃ² Ä‘iá»‡p nÆ°á»›ng phÃ´ mai', 159000, 91000, 8, 48, 4.5, 16, 'so-diep.jpg', N'SÃ² Ä‘iá»‡p nÆ°á»›ng phÃ´ mai bÃ©o', 1, 1, 'AVAILABLE', 6),
(N'Panna cotta dÃ¢u', 49000, 22000, 8, 90, 4.3, 6, 'panna-cotta.jpg', N'Panna cotta sá»‘t dÃ¢u', 1, 1, 'AVAILABLE', 7),
(N'ChÃ¨ khÃºc báº¡ch', 45000, 18000, 8, 100, 4.2, 5, 'che-khuc-bach.jpg', N'ChÃ¨ khÃºc báº¡ch thanh mÃ¡t', 1, 1, 'AVAILABLE', 7),
(N'TrÃ¡i cÃ¢y theo mÃ¹a', 59000, 25000, 8, 55, 4.1, 5, 'trai-cay.jpg', N'TrÃ¡i cÃ¢y tÆ°Æ¡i cáº¯t sáºµn', 1, 1, 'AVAILABLE', 7),
(N'TrÃ  Ä‘Ã o cam sáº£', 45000, 14000, 8, 180, 4.5, 5, 'tra-dao.jpg', N'TrÃ  Ä‘Ã o cam sáº£ mÃ¡t láº¡nh', 1, 1, 'AVAILABLE', 8),
(N'CÃ  phÃª sá»¯a Ä‘Ã¡', 35000, 10000, 8, 210, 4.4, 5, 'ca-phe-sua-da.jpg', N'CÃ  phÃª phin sá»¯a Ä‘Ã¡', 1, 1, 'AVAILABLE', 8),
(N'NÆ°á»›c Ã©p cam', 49000, 19000, 8, 95, 4.3, 5, 'nuoc-ep-cam.jpg', N'NÆ°á»›c Ã©p cam tÆ°Æ¡i', 1, 1, 'AVAILABLE', 8);

INSERT INTO dbo.ingredients(name, quantity, unit, min_stock, unit_price, image, shelf_life_days, active)
VALUES
(N'Thá»‹t bÃ²', 35.000, N'kg', 5.000, 220000, 'thit-bo.jpg', 7, 1),
(N'Thá»‹t gÃ ', 40.000, N'kg', 6.000, 85000, 'thit-ga.jpg', 5, 1),
(N'SÆ°á»n heo', 28.000, N'kg', 5.000, 145000, 'suon-heo.jpg', 5, 1),
(N'CÃ¡ há»“i', 18.000, N'kg', 3.000, 320000, 'ca-hoi.jpg', 4, 1),
(N'TÃ´m sÃº', 24.000, N'kg', 4.000, 260000, 'tom-su.jpg', 3, 1),
(N'Cua thá»‹t', 15.000, N'kg', 2.000, 420000, 'cua-thit.jpg', 3, 1),
(N'Má»±c lÃ¡', 20.000, N'kg', 3.000, 230000, 'muc-la.jpg', 3, 1),
(N'NghÃªu', 30.000, N'kg', 5.000, 65000, 'ngheu.jpg', 2, 1),
(N'Rau xÃ  lÃ¡ch', 12.000, N'kg', 2.000, 35000, 'xa-lach.jpg', 2, 1),
(N'CÃ  chua', 15.000, N'kg', 2.000, 28000, 'ca-chua.jpg', 4, 1),
(N'Khoai tÃ¢y', 30.000, N'kg', 5.000, 25000, 'khoai-tay.jpg', 15, 1),
(N'Náº¥m tá»•ng há»£p', 16.000, N'kg', 3.000, 90000, 'nam.jpg', 4, 1),
(N'Äáº­u hÅ© non', 18.000, N'kg', 3.000, 42000, 'dau-hu.jpg', 4, 1),
(N'Gáº¡o thÆ¡m', 80.000, N'kg', 20.000, 22000, 'gao.jpg', 90, 1),
(N'MÃ¬ trá»©ng', 25.000, N'kg', 5.000, 55000, 'mi-trung.jpg', 30, 1),
(N'NÆ°á»›c máº¯m', 20.000, N'lÃ­t', 4.000, 45000, 'nuoc-mam.jpg', 180, 1),
(N'Sá»‘t BBQ', 12.000, N'lÃ­t', 2.000, 75000, 'sot-bbq.jpg', 90, 1),
(N'Sa táº¿', 10.000, N'kg', 2.000, 65000, 'sa-te.jpg', 120, 1),
(N'TrÃ  Ä‘en', 8.000, N'kg', 1.000, 120000, 'tra-den.jpg', 180, 1),
(N'Cam tÆ°Æ¡i', 35.000, N'kg', 6.000, 38000, 'cam-tuoi.jpg', 7, 1);

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
        CONCAT(N'BÃ n ', FORMAT(@TableNo, '00')),
        CASE WHEN @TableNo <= 10 THEN N'Táº§ng 1' ELSE N'Táº§ng 2' END,
        0,
        'AVAILABLE',
        CASE WHEN @TableNo IN (5, 10, 15, 20) THEN 1 ELSE 0 END,
        NULL,
        CASE WHEN @TableNo % 5 = 0 THEN 8 WHEN @TableNo % 3 = 0 THEN 6 ELSE 4 END,
        CASE WHEN @TableNo IN (5, 10, 15, 20) THEN N'Cá»­a sá»•' ELSE N'TiÃªu chuáº©n' END
    );
    SET @TableNo += 1;
END;

INSERT INTO dbo.vouchers(code, voucher_name, discount_type, discount_value, discount_percent, max_discount_amount, min_order_amount, start_date, end_date, usage_limit, used_count, active, is_used, account_username)
VALUES
('WELCOME10', N'ChÃ o má»«ng khÃ¡ch má»›i', 'PERCENT', 10, 10, 50000, 200000, SYSDATETIME(), DATEADD(day, 60, SYSDATETIME()), 200, 0, 1, 0, NULL),
('VIP15', N'Æ¯u Ä‘Ã£i khÃ¡ch VIP', 'PERCENT', 15, 15, 120000, 500000, SYSDATETIME(), DATEADD(day, 90, SYSDATETIME()), 100, 0, 1, 0, NULL),
('FAMILY50', N'Giáº£m 50K nhÃ³m gia Ä‘Ã¬nh', 'AMOUNT', 50000, NULL, 50000, 350000, SYSDATETIME(), DATEADD(day, 60, SYSDATETIME()), 150, 0, 1, 0, NULL),
('SEAFOOD80', N'Æ¯u Ä‘Ã£i háº£i sáº£n', 'AMOUNT', 80000, NULL, 80000, 700000, SYSDATETIME(), DATEADD(day, 45, SYSDATETIME()), 80, 0, 1, 0, NULL),
('LUNCH5', N'Giáº£m giá» trÆ°a', 'PERCENT', 5, 5, 30000, 150000, SYSDATETIME(), DATEADD(day, 30, SYSDATETIME()), 300, 0, 1, 0, NULL),
('DINNER12', N'Giáº£m buá»•i tá»‘i', 'PERCENT', 12, 12, 90000, 450000, SYSDATETIME(), DATEADD(day, 30, SYSDATETIME()), 120, 0, 1, 0, NULL),
('CUSTOMER20', N'Voucher riÃªng khÃ¡ch máº«u', 'PERCENT', 20, 20, 150000, 600000, SYSDATETIME(), DATEADD(day, 90, SYSDATETIME()), 1, 0, 1, 0, 'customer'),
('HOTPOT70', N'Æ¯u Ä‘Ã£i mÃ³n láº©u', 'AMOUNT', 70000, NULL, 70000, 500000, SYSDATETIME(), DATEADD(day, 40, SYSDATETIME()), 70, 0, 1, 0, NULL),
('DRINK25', N'Giáº£m Ä‘á»“ uá»‘ng', 'PERCENT', 25, 25, 40000, 120000, SYSDATETIME(), DATEADD(day, 25, SYSDATETIME()), 100, 0, 1, 0, NULL),
('BIRTHDAY100', N'Sinh nháº­t khÃ¡ch hÃ ng', 'AMOUNT', 100000, NULL, 100000, 800000, SYSDATETIME(), DATEADD(day, 365, SYSDATETIME()), 50, 0, 1, 0, NULL);

INSERT INTO dbo.Posts(title, content, image, type)
VALUES
(N'Æ¯u Ä‘Ã£i cuá»‘i tuáº§n', N'NhÃ  hÃ ng Ã¡p dá»¥ng nhiá»u voucher cho nhÃ³m gia Ä‘Ã¬nh.', 'post-weekend.jpg', 'NEWS'),
(N'Tuyá»ƒn phá»¥c vá»¥ ca tá»‘i', N'Cáº§n tuyá»ƒn phá»¥c vá»¥ bÃ¡n thá»i gian ca tá»‘i.', 'post-hiring-waiter.jpg', 'RECRUITMENT'),
(N'MÃ³n má»›i thÃ¡ng nÃ y', N'Ra máº¯t set háº£i sáº£n nÆ°á»›ng phÃ´ mai.', 'post-new-menu.jpg', 'NEWS');

COMMIT TRANSACTION;
GO

PRINT N'02_seed_data.sql completed. Sample password for all accounts: 123 (BCrypt).';
GO


/* ===== END 02_seed_data.sql ===== */


/* ===== BEGIN 03_demo_data.sql ===== */

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

/* 10 import invoices, details, ingredient batches and stock movement history. */
DECLARE @InvoiceNo INT = 1;
WHILE @InvoiceNo <= 10
BEGIN
    DECLARE @InvoiceCode VARCHAR(50) = CONCAT('PNK', FORMAT(@InvoiceNo, '000'));
    DECLARE @IngredientId BIGINT = ((@InvoiceNo - 1) % 20) + 1;
    DECLARE @Quantity DECIMAL(18,3) = 10 + @InvoiceNo;
    DECLARE @UnitPrice DECIMAL(18,2) = 25000 + (@InvoiceNo * 7500);
    DECLARE @Total DECIMAL(18,2) = @Quantity * @UnitPrice;
    DECLARE @Before DECIMAL(18,3) = (SELECT quantity FROM dbo.ingredients WHERE id = @IngredientId);
    DECLARE @After DECIMAL(18,3) = @Before + @Quantity;

    INSERT INTO dbo.import_invoices(invoice_code, supplier, supplier_name, import_date, total_amount, created_by, note)
    VALUES (@InvoiceCode, CONCAT(N'NhÃ  cung cáº¥p ', @InvoiceNo), CONCAT(N'NhÃ  cung cáº¥p ', @InvoiceNo),
            DATEADD(day, -@InvoiceNo, SYSDATETIME()), @Total, 'manager', N'Phiáº¿u nháº­p demo');

    DECLARE @InvoiceId BIGINT = SCOPE_IDENTITY();

    INSERT INTO dbo.ImportInvoiceDetails(invoice_id, ingredient_id, quantity, unit_price, expiry_date, total_price)
    VALUES (@InvoiceId, @IngredientId, @Quantity, @UnitPrice, DATEADD(day, 20 + @InvoiceNo, CAST(SYSDATETIME() AS DATE)), @Total);

    INSERT INTO dbo.ingredient_batches(ingredient_id, import_invoice_id, batch_code, quantity, remaining_quantity,
                                       import_date, expiration_date, expiry_date, unit_price, supplier_name, note)
    VALUES (@IngredientId, @InvoiceId, CONCAT('BATCH-', FORMAT(@InvoiceNo, '000')), @Quantity, @Quantity,
            DATEADD(day, -@InvoiceNo, SYSDATETIME()), DATEADD(day, 20 + @InvoiceNo, CAST(SYSDATETIME() AS DATE)),
            DATEADD(day, 20 + @InvoiceNo, CAST(SYSDATETIME() AS DATE)), @UnitPrice,
            CONCAT(N'NhÃ  cung cáº¥p ', @InvoiceNo), N'LÃ´ nháº­p demo');

    DECLARE @BatchId BIGINT = SCOPE_IDENTITY();

    UPDATE dbo.ingredients SET quantity = @After WHERE id = @IngredientId;

    INSERT INTO dbo.InventoryTransactions(ingredient_id, batch_id, transaction_type, quantity, before_quantity, after_quantity, reason, created_by)
    VALUES (@IngredientId, @BatchId, 'IMPORT', @Quantity, @Before, @After, CONCAT(N'Nháº­p kho tá»« ', @InvoiceCode), 'manager');

    SET @InvoiceNo += 1;
END;

/* 20 demo orders with two order details each. */
DECLARE @OrderNo INT = 1;
WHILE @OrderNo <= 20
BEGIN
    DECLARE @ProductA INT = ((@OrderNo - 1) % 30) + 1;
    DECLARE @ProductB INT = ((@OrderNo + 7) % 30) + 1;
    DECLARE @QtyA INT = CASE WHEN @OrderNo % 3 = 0 THEN 2 ELSE 1 END;
    DECLARE @QtyB INT = 1;
    DECLARE @PriceA DECIMAL(18,2) = (SELECT price FROM dbo.Products WHERE id = @ProductA);
    DECLARE @PriceB DECIMAL(18,2) = (SELECT price FROM dbo.Products WHERE id = @ProductB);
    DECLARE @SubTotal DECIMAL(18,2) = (@PriceA * @QtyA) + (@PriceB * @QtyB);
    DECLARE @Discount DECIMAL(18,2) = CASE WHEN @OrderNo % 5 = 0 THEN 50000 ELSE 0 END;
    DECLARE @Tax DECIMAL(18,2) = ROUND((@SubTotal - @Discount) * 0.08, 2);
    DECLARE @GrandTotal DECIMAL(18,2) = @SubTotal - @Discount + @Tax;
    DECLARE @StatusName VARCHAR(20) = CASE
        WHEN @OrderNo % 7 = 0 THEN 'CANCELLED'
        WHEN @OrderNo % 5 = 0 THEN 'PAID'
        WHEN @OrderNo % 4 = 0 THEN 'SERVED'
        WHEN @OrderNo % 3 = 0 THEN 'READY'
        WHEN @OrderNo % 2 = 0 THEN 'COOKING'
        ELSE 'PENDING'
    END;
    DECLARE @StatusInt INT = CASE @StatusName
        WHEN 'PENDING' THEN 0 WHEN 'COOKING' THEN 1 WHEN 'READY' THEN 2
        WHEN 'SERVED' THEN 3 WHEN 'PAID' THEN 4 WHEN 'CANCELLED' THEN 5 ELSE 6
    END;

    INSERT INTO dbo.Orders(create_date, username, table_id, order_type, status, status_name, customer_name, customer_phone,
                           reservation_time, address, note, voucher_code, sub_total, discount_amount, tax_amount,
                           total_amount, deposit, is_paid, payment_method, payment_time, created_by)
    VALUES (DATEADD(hour, -@OrderNo * 3, SYSDATETIME()), 'customer', ((@OrderNo - 1) % 20) + 1,
            CASE WHEN @OrderNo % 6 = 0 THEN 'TAKE_AWAY' WHEN @OrderNo % 9 = 0 THEN 'RESERVATION' ELSE 'DINE_IN' END,
            @StatusInt, @StatusName, N'KhÃ¡ch demo', CONCAT('09876543', FORMAT(@OrderNo, '00')),
            CASE WHEN @OrderNo % 9 = 0 THEN DATEADD(day, 1, SYSDATETIME()) ELSE NULL END,
            N'Äá»‹a chá»‰ demo', N'ÄÆ¡n hÃ ng demo',
            CASE WHEN @OrderNo % 5 = 0 THEN 'FAMILY50' ELSE NULL END,
            @SubTotal, @Discount, @Tax, @GrandTotal,
            CASE WHEN @OrderNo % 9 = 0 THEN 100000 ELSE 0 END,
            CASE WHEN @StatusName = 'PAID' THEN 1 ELSE 0 END,
            CASE WHEN @StatusName = 'PAID' THEN 'CASH' ELSE NULL END,
            CASE WHEN @StatusName = 'PAID' THEN SYSDATETIME() ELSE NULL END,
            CASE WHEN @OrderNo % 2 = 0 THEN 'waiter' ELSE 'manager' END);

    DECLARE @OrderId INT = SCOPE_IDENTITY();

    INSERT INTO dbo.order_details(order_id, product_id, price, unit_price, quantity, line_subtotal, tax_rate, tax_amount, line_total, status, status_name, note)
    VALUES
    (@OrderId, @ProductA, @PriceA, @PriceA, @QtyA, @PriceA * @QtyA, 8, ROUND(@PriceA * @QtyA * 0.08, 2),
     ROUND(@PriceA * @QtyA * 1.08, 2), CASE WHEN @StatusName = 'CANCELLED' THEN 4 ELSE @StatusInt END,
     CASE WHEN @StatusName = 'CANCELLED' THEN 'CANCELLED' ELSE 'SERVED' END, N'Chi tiáº¿t mÃ³n demo'),
    (@OrderId, @ProductB, @PriceB, @PriceB, @QtyB, @PriceB * @QtyB, 8, ROUND(@PriceB * @QtyB * 0.08, 2),
     ROUND(@PriceB * @QtyB * 1.08, 2), CASE WHEN @StatusName = 'CANCELLED' THEN 4 ELSE @StatusInt END,
     CASE WHEN @StatusName = 'CANCELLED' THEN 'CANCELLED' ELSE 'READY' END, N'Chi tiáº¿t mÃ³n demo');

    IF @StatusName IN ('SERVED','PAID')
    BEGIN
        INSERT INTO dbo.Reviews(username, product_id, order_id, rating, comment)
        VALUES ('customer', @ProductA, @OrderId, 4 + (@OrderNo % 2), N'MÃ³n Äƒn ngon, phá»¥c vá»¥ tá»‘t.');
    END;

    SET @OrderNo += 1;
END;

/* Seven days of staff schedules and timekeeping. */
DECLARE @DayOffset INT = 0;
WHILE @DayOffset < 7
BEGIN
    DECLARE @WorkDate DATE = DATEADD(day, -@DayOffset, CAST(SYSDATETIME() AS DATE));

    INSERT INTO dbo.work_schedules(username, work_date, shift, shift_name, start_time, end_time, status, note)
    VALUES
    ('manager', @WorkDate, N'SÃ¡ng',  N'SÃ¡ng',  '06:00', '14:00', 'COMPLETED', N'Ca quáº£n lÃ½'),
    ('waiter',  @WorkDate, N'Chiá»u', N'Chiá»u', '14:00', '22:00', 'COMPLETED', N'Ca phá»¥c vá»¥'),
    ('kitchen', @WorkDate, N'SÃ¡ng',  N'SÃ¡ng',  '06:00', '14:00', 'COMPLETED', N'Ca báº¿p'),
    ('cashier', @WorkDate, N'Tá»‘i',   N'Tá»‘i',   '16:00', '23:00', CASE WHEN @DayOffset = 2 THEN 'ABSENT' ELSE 'COMPLETED' END, N'Ca thu ngÃ¢n');

    INSERT INTO dbo.timekeeping(username, work_date, check_in_time, check_out_time, total_hours, status, note)
    VALUES
    ('manager', @WorkDate, DATEADD(hour, 6, CAST(@WorkDate AS DATETIME2)), DATEADD(hour, 14, CAST(@WorkDate AS DATETIME2)), 8, N'COMPLETED', N'ÄÃºng giá»'),
    ('waiter',  @WorkDate, DATEADD(minute, 5, DATEADD(hour, 14, CAST(@WorkDate AS DATETIME2))), DATEADD(hour, 22, CAST(@WorkDate AS DATETIME2)), 7.92, N'LATE', N'Äi trá»… 5 phÃºt'),
    ('kitchen', @WorkDate, DATEADD(hour, 6, CAST(@WorkDate AS DATETIME2)), DATEADD(hour, 14, CAST(@WorkDate AS DATETIME2)), 8, N'COMPLETED', N'ÄÃºng giá»'),
    ('cashier', @WorkDate,
        CASE WHEN @DayOffset = 2 THEN NULL ELSE DATEADD(hour, 16, CAST(@WorkDate AS DATETIME2)) END,
        CASE WHEN @DayOffset = 2 THEN NULL ELSE DATEADD(hour, 23, CAST(@WorkDate AS DATETIME2)) END,
        CASE WHEN @DayOffset = 2 THEN 0 ELSE 7 END,
        CASE WHEN @DayOffset = 2 THEN N'ABSENT' ELSE N'COMPLETED' END,
        CASE WHEN @DayOffset = 2 THEN N'Nghá»‰ khÃ´ng phÃ©p' ELSE N'HoÃ n thÃ nh ca' END);

    SET @DayOffset += 1;
END;

INSERT INTO dbo.Applications(fullname, phone, email, message, post_id, postId, cv_file, status)
VALUES
(N'Nguyá»…n VÄƒn á»¨ng ViÃªn', '0912345678', 'ungvien@example.com', N'á»¨ng tuyá»ƒn vá»‹ trÃ­ phá»¥c vá»¥ ca tá»‘i.', 2, 2, N'uploads/cv-ung-vien.pdf', 'NEW');

COMMIT TRANSACTION;
GO

PRINT N'03_demo_data.sql completed.';
GO


/* ===== END 03_demo_data.sql ===== */


/* ===== BEGIN 04_upgrade_reservations.sql ===== */

-- Upgrade reservation and table management for Moc Vi.
-- Run on SQL Server database RestaurantDB before deploying the new backend.

IF OBJECT_ID('dbo.table_areas', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.table_areas (
        id INT IDENTITY(1,1) PRIMARY KEY,
        name_vi NVARCHAR(150) NOT NULL,
        name_en NVARCHAR(150) NULL,
        description_vi NVARCHAR(500) NULL,
        description_en NVARCHAR(500) NULL,
        image_url NVARCHAR(500) NULL,
        base_price DECIMAL(18,0) NOT NULL DEFAULT 0,
        capacity INT NOT NULL DEFAULT 0,
        status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END;

IF COL_LENGTH('dbo.restaurant_table', 'min_capacity') IS NULL
    ALTER TABLE dbo.restaurant_table ADD min_capacity INT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'max_capacity') IS NULL
    ALTER TABLE dbo.restaurant_table ADD max_capacity INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'seat_count') IS NULL
    ALTER TABLE dbo.restaurant_table ADD seat_count INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'reservation_price') IS NULL
    ALTER TABLE dbo.restaurant_table ADD reservation_price DECIMAL(18,0) NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'area_id') IS NULL
    ALTER TABLE dbo.restaurant_table ADD area_id INT NULL;
IF COL_LENGTH('dbo.restaurant_table', 'position_description') IS NULL
    ALTER TABLE dbo.restaurant_table ADD position_description NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.restaurant_table', 'is_window_seat') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_window_seat BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'is_private_room') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_private_room BIT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.restaurant_table', 'is_child_friendly') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_child_friendly BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'is_active') IS NULL
    ALTER TABLE dbo.restaurant_table ADD is_active BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.restaurant_table', 'image_url') IS NULL
    ALTER TABLE dbo.restaurant_table ADD image_url NVARCHAR(500) NULL;

IF OBJECT_ID('dbo.FK_restaurant_table_area', 'F') IS NULL
BEGIN
    ALTER TABLE dbo.restaurant_table
        ADD CONSTRAINT FK_restaurant_table_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id);
END;

-- SQL Server compiles a batch before executing ALTER TABLE statements. Split
-- here so the following update can safely reference the newly added columns.
GO

IF OBJECT_ID('dbo.reservations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservations (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        reservation_code VARCHAR(30) NOT NULL UNIQUE,
        customer_name NVARCHAR(150) NOT NULL,
        customer_phone VARCHAR(20) NOT NULL,
        customer_email VARCHAR(150) NULL,
        contact_note NVARCHAR(500) NULL,
        reservation_date DATE NOT NULL,
        arrival_time TIME NOT NULL,
        expected_duration_minutes INT NOT NULL DEFAULT 120,
        guest_count INT NOT NULL,
        occasion NVARCHAR(80) NULL,
        special_request NVARCHAR(500) NULL,
        seating_preference NVARCHAR(255) NULL,
        area_id INT NULL,
        table_id INT NULL,
        reservation_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
        total_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
        deposit_rate DECIMAL(5,2) NOT NULL DEFAULT 0.50,
        deposit_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
        remaining_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
        deposit_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
        manager_note NVARCHAR(500) NULL,
        confirmed_by VARCHAR(80) NULL,
        confirmed_at DATETIME2 NULL,
        rejected_reason NVARCHAR(500) NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_reservations_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id),
        CONSTRAINT FK_reservations_table FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id)
    );
END;

-- Required by the payment ledger migration that runs when Spring Boot starts.
IF COL_LENGTH('dbo.reservations', 'payment_status') IS NULL
    ALTER TABLE dbo.reservations ADD payment_status VARCHAR(30) NOT NULL
        CONSTRAINT df_setup_reservations_payment_status DEFAULT 'UNPAID';
GO

IF OBJECT_ID('dbo.reservation_images', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_images (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        area_id INT NULL,
        table_id INT NULL,
        image_url NVARCHAR(500) NOT NULL,
        alt_text_vi NVARCHAR(255) NULL,
        alt_text_en NVARCHAR(255) NULL,
        is_primary BIT NOT NULL DEFAULT 0,
        sort_order INT NOT NULL DEFAULT 0,
        created_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_reservation_images_area FOREIGN KEY (area_id) REFERENCES dbo.table_areas(id),
        CONSTRAINT FK_reservation_images_table FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id)
    );
END;

IF OBJECT_ID('dbo.reservation_status_history', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.reservation_status_history (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        reservation_id BIGINT NOT NULL,
        old_status VARCHAR(30) NULL,
        new_status VARCHAR(30) NOT NULL,
        changed_by VARCHAR(80) NULL,
        note NVARCHAR(500) NULL,
        changed_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_reservation_status_history_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_table_date_status')
    CREATE INDEX IX_reservations_table_date_status ON dbo.reservations(table_id, reservation_date, reservation_status);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_reservations_code_phone')
    CREATE INDEX IX_reservations_code_phone ON dbo.reservations(reservation_code, customer_phone);

IF NOT EXISTS (SELECT 1 FROM dbo.table_areas)
BEGIN
    INSERT INTO dbo.table_areas (name_vi, name_en, description_vi, description_en, image_url, base_price, capacity, status)
    VALUES
        (N'Táº§ng 2 - Sáº£nh tiá»‡c', 'Level 2 - Banquet Hall', N'KhÃ´ng gian rá»™ng cho gia Ä‘Ã¬nh vÃ  nhÃ³m Ä‘Ã´ng.', 'Spacious hall for families and groups.', '/images/areas/banquet.jpg', 800000, 150, 'ACTIVE'),
        (N'Táº§ng 3-5 - PhÃ²ng VIP', 'Level 3-5 - Private Rooms', N'PhÃ²ng riÃªng yÃªn tÄ©nh, phÃ¹ há»£p tiáº¿p khÃ¡ch vÃ  sinh nháº­t.', 'Quiet private rooms for business meals and birthdays.', '/images/areas/vip.jpg', 1200000, 90, 'ACTIVE'),
        (N'Táº§ng 6 - SÃ¢n thÆ°á»£ng', 'Level 6 - Rooftop', N'KhÃ´ng gian ngoÃ i trá»i vá»›i view phá»‘, sÃ´ng vÃ  sÃ¢n vÆ°á»n.', 'Outdoor rooftop with city, river and garden views.', '/images/areas/rooftop.jpg', 600000, 80, 'ACTIVE');
END;

UPDATE dbo.restaurant_table
SET
    max_capacity = ISNULL(max_capacity, ISNULL(capacity, 4)),
    seat_count = ISNULL(seat_count, ISNULL(capacity, 4)),
    reservation_price = CASE WHEN ISNULL(reservation_price, 0) = 0 THEN ISNULL(capacity, 4) * 100000 ELSE reservation_price END,
    is_private_room = CASE WHEN floor LIKE '%VIP%' THEN 1 ELSE ISNULL(is_private_room, 0) END,
    is_window_seat = CASE WHEN ISNULL(has_view, 0) = 1 THEN 1 ELSE ISNULL(is_window_seat, 0) END,
    is_child_friendly = ISNULL(is_child_friendly, 1),
    is_active = ISNULL(is_active, 1)
WHERE 1 = 1;


/* ===== END 04_upgrade_reservations.sql ===== */

/* ===== BEGIN compatibility for Flyway and analytics seed ===== */
IF OBJECT_ID(N'dbo.payment_intents', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_intents (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        reservation_id BIGINT NULL,
        payment_code VARCHAR(40) NOT NULL UNIQUE,
        payment_option VARCHAR(30) NOT NULL,
        status VARCHAR(30) NOT NULL,
        amount DECIMAL(18,0) NOT NULL,
        bank_code VARCHAR(20) NOT NULL,
        account_number VARCHAR(40) NOT NULL,
        account_holder NVARCHAR(150) NOT NULL,
        transfer_content NVARCHAR(120) NOT NULL,
        qr_url NVARCHAR(1000) NOT NULL,
        expires_at DATETIME2 NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_setup_payment_intents_created_at DEFAULT SYSUTCDATETIME(),
        paid_at DATETIME2 NULL,
        confirmed_by VARCHAR(80) NULL,
        bank_transaction_code VARCHAR(80) NULL,
        note NVARCHAR(500) NULL,
        CONSTRAINT fk_setup_payment_intents_reservation FOREIGN KEY (reservation_id) REFERENCES dbo.reservations(id)
    );
END;
IF COL_LENGTH(N'dbo.Orders', N'payment_option') IS NULL ALTER TABLE dbo.Orders ADD payment_option VARCHAR(30) NOT NULL CONSTRAINT df_setup_orders_payment_option DEFAULT 'PAY_AT_RESTAURANT';
IF COL_LENGTH(N'dbo.Orders', N'payment_status') IS NULL ALTER TABLE dbo.Orders ADD payment_status VARCHAR(30) NOT NULL CONSTRAINT df_setup_orders_payment_status DEFAULT 'UNPAID';
IF COL_LENGTH(N'dbo.Orders', N'paid_amount') IS NULL ALTER TABLE dbo.Orders ADD paid_amount DECIMAL(18,0) NOT NULL CONSTRAINT df_setup_orders_paid_amount DEFAULT 0;
IF COL_LENGTH(N'dbo.Orders', N'remaining_amount') IS NULL ALTER TABLE dbo.Orders ADD remaining_amount DECIMAL(18,0) NOT NULL CONSTRAINT df_setup_orders_remaining_amount DEFAULT 0;
IF COL_LENGTH(N'dbo.Orders', N'version') IS NULL ALTER TABLE dbo.Orders ADD version BIGINT NOT NULL CONSTRAINT df_setup_orders_version DEFAULT 0 WITH VALUES;
GO
/* ===== END compatibility for Flyway and analytics seed ===== */

/* ===== BEGIN 05_seed_analytics_demo.sql ===== */

/*
  Analytics demo data for FPoly Restaurant.
  Creates 28 paid/completed orders across the last seven days.
  Safe to execute repeatedly: the marker address prevents duplicates.
*/
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM dbo.orders WHERE address = N'[DEMO_ANALYTICS_2026]')
    BEGIN
        UPDATE dbo.products
        SET cost_price = CASE id
            WHEN 1 THEN 32000 WHEN 2 THEN 22000 WHEN 3 THEN 6000
            WHEN 4 THEN 26000 WHEN 5 THEN 37000 WHEN 6 THEN 145000
            WHEN 7 THEN 69000 WHEN 8 THEN 112000 WHEN 9 THEN 45000
            WHEN 10 THEN 42000
        END
        WHERE id BETWEEN 1 AND 10 AND (cost_price IS NULL OR cost_price = 0);

        DECLARE @dayOffset INT = 0;
        DECLARE @slot INT;
        DECLARE @productA INT;
        DECLARE @productB INT;
        DECLARE @productC INT;
        DECLARE @priceA DECIMAL(18, 2);
        DECLARE @priceB DECIMAL(18, 2);
        DECLARE @priceC DECIMAL(18, 2);
        DECLARE @qtyA INT;
        DECLARE @qtyB INT;
        DECLARE @qtyC INT;
        DECLARE @subTotal DECIMAL(18, 2);
        DECLARE @taxAmount DECIMAL(18, 2);
        DECLARE @totalAmount DECIMAL(18, 2);
        DECLARE @orderId INT;
        DECLARE @orderDate DATETIME2;

        WHILE @dayOffset < 7
        BEGIN
            SET @slot = 1;

            WHILE @slot <= 4
            BEGIN
                SET @productA = ((@dayOffset * 4 + @slot - 1) % 10) + 1;
                SET @productB = ((@dayOffset * 4 + @slot + 2) % 10) + 1;
                SET @productC = ((@dayOffset * 4 + @slot + 5) % 10) + 1;
                SET @qtyA = 2 + ((@dayOffset + @slot) % 3);
                SET @qtyB = 1 + ((@dayOffset + @slot) % 2);
                SET @qtyC = 1 + ((@dayOffset + @slot + 1) % 2);

                SELECT @priceA = price FROM dbo.products WHERE id = @productA;
                SELECT @priceB = price FROM dbo.products WHERE id = @productB;
                SELECT @priceC = price FROM dbo.products WHERE id = @productC;

                SET @subTotal = @priceA * @qtyA + @priceB * @qtyB + @priceC * @qtyC;
                SET @taxAmount = ROUND(@subTotal * 0.08, 0);
                SET @totalAmount = @subTotal + @taxAmount;
                SET @orderDate = DATEADD(hour, 10 + (@slot * 2), DATEADD(day, -@dayOffset, CAST(CAST(SYSDATETIME() AS date) AS datetime2)));

                INSERT INTO dbo.orders (
                    create_date, status, address, sub_total, tax_amount, total_amount,
                    is_paid, payment_option, payment_status, paid_amount, remaining_amount, version
                ) VALUES (
                    @orderDate, N'4', N'[DEMO_ANALYTICS_2026]', @subTotal, @taxAmount, @totalAmount,
                    1, 'PAY_AT_RESTAURANT', 'PAID', @totalAmount, 0, 0
                );

                SET @orderId = SCOPE_IDENTITY();

                INSERT INTO dbo.order_details (price, unit_price, quantity, status, tax_amount, tax_rate, line_subtotal, line_total, order_id, product_id)
                VALUES
                    (@priceA, @priceA, @qtyA, 2, ROUND(@priceA * @qtyA * 0.08, 0), 8.0, @priceA * @qtyA, @priceA * @qtyA * 1.08, @orderId, @productA),
                    (@priceB, @priceB, @qtyB, 2, ROUND(@priceB * @qtyB * 0.08, 0), 8.0, @priceB * @qtyB, @priceB * @qtyB * 1.08, @orderId, @productB),
                    (@priceC, @priceC, @qtyC, 2, ROUND(@priceC * @qtyC * 0.08, 0), 8.0, @priceC * @qtyC, @priceC * @qtyC * 1.08, @orderId, @productC);

                SET @slot += 1;
            END;

            SET @dayOffset += 1;
        END;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;


/* ===== END 05_seed_analytics_demo.sql ===== */
