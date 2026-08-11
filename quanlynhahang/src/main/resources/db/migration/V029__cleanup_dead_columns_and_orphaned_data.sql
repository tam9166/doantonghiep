/*
    V029__cleanup_dead_columns_and_orphaned_data.sql

    Don cac cot khong con duoc entity nao trong backend Java su dung
    (da doi chieu truc tiep voi source code, khong suy doan) va cac
    index/constraint di kem. An toan de chay lai nhieu lan (idempotent)
    - moi buoc deu kiem tra ton tai truoc khi thuc hien.

    Cac cot bi xoa trong migration nay va ly do:
      - Accounts.total_spent, Accounts.loyalty_points, Accounts.tier
        => Account.java chi dung 'points' va 'membership_tier', 3 cot
           nay khong duoc bat ky entity/service nao tham chieu.
      - ingredient_batches.expiry_date
        => IngredientBatch.java chi dung 'expiration_date'.
      - import_invoices.supplier_name
        => ImportInvoice.java chi dung 'supplier'.
      - Orders.status_name, order_details.status_name
        => khong co tham chieu nao trong toan bo backend Java (0 ket
           qua tim kiem), du dang co index rieng cho ca 2 cot.
      - restaurant_table.status
        => RestaurantTable.java chi dung 'is_occupied'.
      - Products.status_name
        => Product.java chi dung 'status' (bool) va 'available' (bool).
      - Applications.postId
        => cot trung lap voi 'post_id' do nham lan khi tao bang;
           Application.java chi co 1 field postId, Hibernate tu anh xa
           sang cot 'post_id' (snake_case), con cot 'postId' (camelCase,
           tach biet) la du thua.

    LUU Y: script nay KHONG dong den cac cot con dang duoc dung that
    nhung co thiet ke chua chuan (vi du Products.status/available,
    Accounts.membership_tier, timekeeping.status) - nhung thay doi do
    can sua ca code Java di kem, khong phu hop lam trong 1 migration
    thuan DDL don le. Xem de xuat rieng.
*/

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
BEGIN TRANSACTION;

-- ===================================================================
-- 1) Accounts: total_spent, loyalty_points, tier
-- ===================================================================
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Accounts_tier')
    ALTER TABLE dbo.Accounts DROP CONSTRAINT CK_Accounts_tier;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Accounts_loyalty_points_non_negative')
    ALTER TABLE dbo.Accounts DROP CONSTRAINT CK_Accounts_loyalty_points_non_negative;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Accounts_total_spent_non_negative')
    ALTER TABLE dbo.Accounts DROP CONSTRAINT CK_Accounts_total_spent_non_negative;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Accounts_tier')
    ALTER TABLE dbo.Accounts DROP CONSTRAINT DF_Accounts_tier;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Accounts_loyalty_points')
    ALTER TABLE dbo.Accounts DROP CONSTRAINT DF_Accounts_loyalty_points;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Accounts_total_spent')
    ALTER TABLE dbo.Accounts DROP CONSTRAINT DF_Accounts_total_spent;
IF COL_LENGTH('dbo.Accounts', 'tier') IS NOT NULL
    ALTER TABLE dbo.Accounts DROP COLUMN tier;
IF COL_LENGTH('dbo.Accounts', 'loyalty_points') IS NOT NULL
    ALTER TABLE dbo.Accounts DROP COLUMN loyalty_points;
IF COL_LENGTH('dbo.Accounts', 'total_spent') IS NOT NULL
    ALTER TABLE dbo.Accounts DROP COLUMN total_spent;

-- ===================================================================
-- 2) ingredient_batches.expiry_date (+ index rieng cua no)
-- ===================================================================
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ingredient_batches_expiry_date')
    DROP INDEX IX_ingredient_batches_expiry_date ON dbo.ingredient_batches;
IF COL_LENGTH('dbo.ingredient_batches', 'expiry_date') IS NOT NULL
    ALTER TABLE dbo.ingredient_batches DROP COLUMN expiry_date;

-- ===================================================================
-- 3) import_invoices.supplier_name
-- ===================================================================
IF COL_LENGTH('dbo.import_invoices', 'supplier_name') IS NOT NULL
    ALTER TABLE dbo.import_invoices DROP COLUMN supplier_name;

-- ===================================================================
-- 4) Orders.status_name (+ index rieng)
-- ===================================================================
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_Orders_status')
    DROP INDEX IX_Orders_status ON dbo.Orders;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Orders_status_name')
    ALTER TABLE dbo.Orders DROP CONSTRAINT CK_Orders_status_name;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Orders_status_name')
    ALTER TABLE dbo.Orders DROP CONSTRAINT DF_Orders_status_name;
IF COL_LENGTH('dbo.Orders', 'status_name') IS NOT NULL
    ALTER TABLE dbo.Orders DROP COLUMN status_name;

-- ===================================================================
-- 5) order_details.status_name (+ index rieng)
-- ===================================================================
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_OrderDetails_status')
    DROP INDEX IX_OrderDetails_status ON dbo.order_details;
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_OrderDetails_status_name')
    ALTER TABLE dbo.order_details DROP CONSTRAINT CK_OrderDetails_status_name;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_OrderDetails_status_name')
    ALTER TABLE dbo.order_details DROP CONSTRAINT DF_OrderDetails_status_name;
IF COL_LENGTH('dbo.order_details', 'status_name') IS NOT NULL
    ALTER TABLE dbo.order_details DROP COLUMN status_name;

-- ===================================================================
-- 6) restaurant_table.status (trung voi is_occupied)
-- ===================================================================
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_restaurant_table_status')
    ALTER TABLE dbo.restaurant_table DROP CONSTRAINT CK_restaurant_table_status;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_restaurant_table_status')
    ALTER TABLE dbo.restaurant_table DROP CONSTRAINT DF_restaurant_table_status;
IF COL_LENGTH('dbo.restaurant_table', 'status') IS NOT NULL
    ALTER TABLE dbo.restaurant_table DROP COLUMN status;

-- ===================================================================
-- 7) Products.status_name
-- ===================================================================
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_Products_status_name')
    ALTER TABLE dbo.Products DROP CONSTRAINT CK_Products_status_name;
IF EXISTS (SELECT 1 FROM sys.default_constraints WHERE name = 'DF_Products_status_name')
    ALTER TABLE dbo.Products DROP CONSTRAINT DF_Products_status_name;
IF COL_LENGTH('dbo.Products', 'status_name') IS NOT NULL
    ALTER TABLE dbo.Products DROP COLUMN status_name;

-- ===================================================================
-- 8) Applications.postId (cot trung lap voi post_id do nham lan)
-- ===================================================================
IF COL_LENGTH('dbo.Applications', 'postId') IS NOT NULL
    ALTER TABLE dbo.Applications DROP COLUMN postId;

COMMIT TRANSACTION;
PRINT N'V029: da don xong cac cot rac va index khong su dung.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
