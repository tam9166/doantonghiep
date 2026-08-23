IF OBJECT_ID('dbo.restaurant_table', 'U') IS NOT NULL
   AND OBJECT_ID('dbo.table_sessions', 'U') IS NULL
BEGIN
    EXEC(N'CREATE TABLE dbo.table_sessions (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        token_hash CHAR(64) NOT NULL,
        table_id INT NOT NULL,
        created_at DATETIME2 NOT NULL,
        expires_at DATETIME2 NOT NULL,
        revoked_at DATETIME2 NULL,
        active BIT NOT NULL CONSTRAINT DF_table_sessions_active DEFAULT 1,
        CONSTRAINT FK_table_sessions_table FOREIGN KEY (table_id) REFERENCES dbo.restaurant_table(id),
        CONSTRAINT UX_table_sessions_token_hash UNIQUE (token_hash)
    )');
END;

IF OBJECT_ID('dbo.table_sessions', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.table_sessions') AND name = 'UX_table_sessions_one_active_per_table')
    EXEC(N'CREATE UNIQUE INDEX UX_table_sessions_one_active_per_table ON dbo.table_sessions(table_id) WHERE active = 1');

IF OBJECT_ID('dbo.table_sessions', 'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID('dbo.table_sessions') AND name = 'IX_table_sessions_expiry')
    EXEC(N'CREATE INDEX IX_table_sessions_expiry ON dbo.table_sessions(expires_at, active)');
