IF OBJECT_ID(N'dbo.points_ledger', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.points_ledger (
        id BIGINT IDENTITY(1,1) NOT NULL,
        username VARCHAR(50) NOT NULL,
        event_type VARCHAR(40) NOT NULL,
        event_key VARCHAR(120) NOT NULL,
        delta INT NOT NULL,
        balance_after INT NOT NULL,
        reason NVARCHAR(300) NULL,
        reference_event_key VARCHAR(120) NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_points_ledger_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT pk_points_ledger PRIMARY KEY (id),
        CONSTRAINT uk_points_ledger_event_key UNIQUE (event_key),
        CONSTRAINT fk_points_ledger_account FOREIGN KEY (username) REFERENCES dbo.Accounts(username)
    );

    CREATE INDEX ix_points_ledger_account_created
        ON dbo.points_ledger(username, created_at DESC);
END;

IF OBJECT_ID(N'dbo.Reviews', N'U') IS NOT NULL
   AND NOT EXISTS (
       SELECT 1
       FROM sys.indexes
       WHERE object_id = OBJECT_ID(N'dbo.Reviews')
         AND name = N'uk_reviews_account_product'
   )
BEGIN
    IF EXISTS (
        SELECT 1
        FROM dbo.Reviews
        GROUP BY username, product_id
        HAVING COUNT(*) > 1
    )
    BEGIN
        THROW 51001, 'Duplicate account/product reviews must be reconciled before migration.', 1;
    END;

    CREATE UNIQUE INDEX uk_reviews_account_product
        ON dbo.Reviews(username, product_id)
        WHERE username IS NOT NULL AND product_id IS NOT NULL;
END;
