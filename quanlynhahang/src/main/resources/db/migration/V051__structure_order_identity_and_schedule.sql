-- Persist order identity and scheduled activation data instead of parsing address text.
-- SQL Server compiles a batch before newly added columns become visible, so statements
-- that reference those columns are intentionally executed dynamically.
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.Orders', 'order_code') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD order_code NVARCHAR(40) NULL');

    EXEC(N'UPDATE dbo.Orders
           SET order_code = CONCAT(''ORD-LEGACY-'', RIGHT(REPLICATE(''0'', 10) + CAST(id AS VARCHAR(10)), 10))
           WHERE order_code IS NULL OR LTRIM(RTRIM(order_code)) = ''''');

    IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'order_code' AND is_nullable = 1)
        EXEC(N'ALTER TABLE dbo.Orders ALTER COLUMN order_code NVARCHAR(40) NOT NULL');

    EXEC(N'IF EXISTS (SELECT 1 FROM (SELECT order_code FROM dbo.Orders GROUP BY order_code HAVING COUNT(*) > 1) duplicates)
             THROW 51000, ''Cannot enforce order identity: duplicate order_code values exist.'', 1');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'UX_Orders_order_code')
        EXEC(N'CREATE UNIQUE INDEX UX_Orders_order_code ON dbo.Orders(order_code)');

    IF COL_LENGTH('dbo.Orders', 'scheduled_at') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD scheduled_at DATETIME2 NULL');

    EXEC(N'UPDATE dbo.Orders
           SET scheduled_at = TRY_CONVERT(DATETIME2,
               SUBSTRING(address, CHARINDEX(N''ngày '', address) + 5, 10) + N''T''
               + SUBSTRING(address, CHARINDEX(N''Lúc:'', address) + 5, 5) + N'':00'')
           WHERE status = 5 AND scheduled_at IS NULL
             AND address LIKE N''%Lúc:%ngày ____-__-__%''');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.Orders') AND name = 'IX_Orders_scheduled_activation')
        EXEC(N'CREATE INDEX IX_Orders_scheduled_activation ON dbo.Orders(status, scheduled_at) WHERE scheduled_at IS NOT NULL');
END;

IF OBJECT_ID('dbo.refund_transactions', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.refund_transactions') AND name = 'IX_refund_transactions_order')
    EXEC(N'CREATE INDEX IX_refund_transactions_order ON dbo.refund_transactions(order_id, created_at DESC)');
