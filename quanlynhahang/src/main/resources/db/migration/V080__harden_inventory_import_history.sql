SET NOCOUNT ON;

-- Keep the legacy schema compatible while making the invoice code contract explicit.
IF OBJECT_ID(N'dbo.import_invoices', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.import_invoices', N'invoice_code') IS NULL
        ALTER TABLE dbo.import_invoices ADD invoice_code VARCHAR(50) NULL;

    EXEC sp_executesql N'UPDATE dbo.import_invoices
       SET invoice_code = ''IMP-'' + CONVERT(VARCHAR(8), COALESCE(import_date, SYSDATETIME()), 112)
                         + ''-'' + RIGHT(''000000'' + CONVERT(VARCHAR(6), id), 6)
     WHERE invoice_code IS NULL OR LTRIM(RTRIM(invoice_code)) = ''''';

    EXEC sp_executesql N'ALTER TABLE dbo.import_invoices ALTER COLUMN invoice_code VARCHAR(50) NOT NULL';
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_import_invoices_invoice_code'
                   AND object_id = OBJECT_ID(N'dbo.import_invoices'))
        EXEC sp_executesql N'CREATE UNIQUE INDEX UQ_import_invoices_invoice_code ON dbo.import_invoices(invoice_code)';
END;

IF OBJECT_ID(N'dbo.ImportInvoiceDetails', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ImportInvoiceDetails_invoice'
                   AND object_id = OBJECT_ID(N'dbo.ImportInvoiceDetails'))
    CREATE INDEX IX_ImportInvoiceDetails_invoice ON dbo.ImportInvoiceDetails(invoice_id);
