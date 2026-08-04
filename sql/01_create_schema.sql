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

/* Auto-created batches use their received quantity as the initial stock. */
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
    CONSTRAINT CK_timekeeping_status CHECK (status IN (N'NOT_CHECKED_IN',N'WORKING',N'COMPLETED',N'LATE',N'ABSENT',N'Đúng giờ',N'Đi trễ',N'Về sớm',N'Hoàn thành'))
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
