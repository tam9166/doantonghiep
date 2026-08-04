IF COL_LENGTH('dbo.order_details', 'note') IS NULL
    ALTER TABLE dbo.order_details ADD note NVARCHAR(500) NULL;

IF COL_LENGTH('dbo.order_details', 'allergy_note') IS NULL
    ALTER TABLE dbo.order_details ADD allergy_note NVARCHAR(500) NULL;

IF COL_LENGTH('dbo.order_details', 'priority') IS NULL
    ALTER TABLE dbo.order_details ADD priority INT NOT NULL CONSTRAINT DF_order_details_priority DEFAULT 0;

IF COL_LENGTH('dbo.order_details', 'queued_at') IS NULL
    ALTER TABLE dbo.order_details ADD queued_at DATETIME2 NULL;

IF COL_LENGTH('dbo.order_details', 'started_at') IS NULL
    ALTER TABLE dbo.order_details ADD started_at DATETIME2 NULL;

IF COL_LENGTH('dbo.order_details', 'completed_at') IS NULL
    ALTER TABLE dbo.order_details ADD completed_at DATETIME2 NULL;

IF COL_LENGTH('dbo.order_details', 'cancelled_at') IS NULL
    ALTER TABLE dbo.order_details ADD cancelled_at DATETIME2 NULL;

IF COL_LENGTH('dbo.order_details', 'cancel_reason') IS NULL
    ALTER TABLE dbo.order_details ADD cancel_reason NVARCHAR(500) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_order_details_status_queued_at'
               AND object_id = OBJECT_ID('dbo.order_details'))
    CREATE INDEX IX_order_details_status_queued_at ON dbo.order_details(status, queued_at);
