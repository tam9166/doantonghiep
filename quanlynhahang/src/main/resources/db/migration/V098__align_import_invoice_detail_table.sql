-- Align legacy/missing import-invoice detail storage with Hibernate's canonical table name.
-- Some upgraded databases predate the V001 detail table, while clean databases still use
-- the original PascalCase name. Preserve all rows when the legacy table exists.
IF OBJECT_ID(N'dbo.import_invoice_details', N'U') IS NULL
BEGIN
    IF OBJECT_ID(N'dbo.ImportInvoiceDetails', N'U') IS NOT NULL
    BEGIN
        EXEC sp_rename N'dbo.ImportInvoiceDetails', N'import_invoice_details';
    END
    ELSE
    BEGIN
        EXEC(N'
            CREATE TABLE dbo.import_invoice_details (
                id            BIGINT IDENTITY(1,1) NOT NULL,
                invoice_id    BIGINT               NOT NULL,
                ingredient_id BIGINT               NOT NULL,
                quantity      DECIMAL(18,3)        NOT NULL,
                unit_price    DECIMAL(18,2)        NOT NULL,
                expiry_date   DATE                 NULL,
                total_price   DECIMAL(18,2)        NOT NULL,
                CONSTRAINT PK_import_invoice_details PRIMARY KEY (id),
                CONSTRAINT FK_import_invoice_details_invoice FOREIGN KEY (invoice_id)
                    REFERENCES dbo.import_invoices(id) ON DELETE CASCADE,
                CONSTRAINT FK_import_invoice_details_ingredient FOREIGN KEY (ingredient_id)
                    REFERENCES dbo.ingredients(id),
                CONSTRAINT CK_import_invoice_details_quantity CHECK (quantity > 0),
                CONSTRAINT CK_import_invoice_details_unit_price CHECK (unit_price >= 0),
                CONSTRAINT CK_import_invoice_details_total_price CHECK (total_price >= 0)
            );
        ');
    END
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'IX_import_invoice_details_invoice'
      AND object_id = OBJECT_ID(N'dbo.import_invoice_details')
)
BEGIN
    EXEC(N'CREATE INDEX IX_import_invoice_details_invoice
        ON dbo.import_invoice_details(invoice_id);');
END;
