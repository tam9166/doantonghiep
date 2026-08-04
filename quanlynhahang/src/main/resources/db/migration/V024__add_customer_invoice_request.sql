IF COL_LENGTH('dbo.Orders', 'invoice_requested') IS NULL
    ALTER TABLE dbo.Orders ADD invoice_requested BIT NOT NULL
        CONSTRAINT DF_Orders_invoice_requested DEFAULT 0;

IF COL_LENGTH('dbo.Orders', 'invoice_requested_at') IS NULL
    ALTER TABLE dbo.Orders ADD invoice_requested_at DATETIME2 NULL;

IF COL_LENGTH('dbo.Orders', 'invoice_email') IS NULL
    ALTER TABLE dbo.Orders ADD invoice_email NVARCHAR(100) NULL;
