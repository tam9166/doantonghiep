IF COL_LENGTH('dbo.import_invoices', 'source_request_id') IS NULL
BEGIN
    ALTER TABLE dbo.import_invoices ADD source_request_id VARCHAR(64) NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_import_invoices_source_request_id'
      AND object_id = OBJECT_ID('dbo.import_invoices')
)
BEGIN
    EXEC(N'CREATE UNIQUE INDEX UX_import_invoices_source_request_id
        ON dbo.import_invoices(source_request_id)
        WHERE source_request_id IS NOT NULL');
END;
