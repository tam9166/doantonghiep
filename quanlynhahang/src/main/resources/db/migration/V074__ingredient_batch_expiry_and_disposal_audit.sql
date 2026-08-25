SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.ingredient_batches', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.ingredient_batches', N'status') IS NULL
        EXEC(N'ALTER TABLE dbo.ingredient_batches ADD status VARCHAR(20) NOT NULL
            CONSTRAINT DF_ingredient_batches_status DEFAULT ''AVAILABLE''');

    EXEC(N'UPDATE dbo.ingredient_batches
        SET status = CASE
            WHEN expiration_date < GETDATE() THEN ''EXPIRED''
            ELSE ''AVAILABLE''
        END
        WHERE status <> ''DISPOSED''');
END;

IF OBJECT_ID(N'dbo.ingredient_batch_disposals', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.ingredient_batch_disposals (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        batch_id BIGINT NOT NULL,
        ingredient_id BIGINT NOT NULL,
        lot_code VARCHAR(50) NOT NULL,
        quantity_disposed DECIMAL(19,4) NOT NULL,
        expiry_date DATETIME2 NULL,
        disposal_date DATETIME2 NOT NULL,
        reason NVARCHAR(500) NOT NULL,
        confirmed_by NVARCHAR(100) NOT NULL,
        CONSTRAINT FK_batch_disposal_batch FOREIGN KEY (batch_id) REFERENCES dbo.ingredient_batches(id),
        CONSTRAINT FK_batch_disposal_ingredient FOREIGN KEY (ingredient_id) REFERENCES dbo.ingredients(id),
        CONSTRAINT CK_batch_disposal_quantity_positive CHECK (quantity_disposed > 0)
    );

    CREATE INDEX IX_batch_disposal_batch_date
        ON dbo.ingredient_batch_disposals(batch_id, disposal_date DESC);
END;
