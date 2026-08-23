IF OBJECT_ID('dbo.order_details', 'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.order_details', 'cancelled_by') IS NULL
        ALTER TABLE dbo.order_details ADD cancelled_by NVARCHAR(80) NULL;

    IF NOT EXISTS (SELECT 1 FROM sys.indexes
                   WHERE object_id = OBJECT_ID('dbo.order_details')
                     AND name = 'IX_order_details_order_status')
        CREATE INDEX IX_order_details_order_status
            ON dbo.order_details(order_id, status);
END;
