IF OBJECT_ID('ai_knowledge_sources', 'U') IS NULL
BEGIN
    CREATE TABLE ai_knowledge_sources (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        title NVARCHAR(200) NOT NULL,
        type VARCHAR(30) NOT NULL CONSTRAINT df_ai_source_type DEFAULT 'TEXT',
        content NVARCHAR(MAX) NOT NULL,
        enabled BIT NOT NULL CONSTRAINT df_ai_source_enabled DEFAULT 1,
        created_at DATETIME2 NOT NULL CONSTRAINT df_ai_source_created DEFAULT SYSUTCDATETIME(),
        updated_at DATETIME2 NOT NULL CONSTRAINT df_ai_source_updated DEFAULT SYSUTCDATETIME()
    );
END;
