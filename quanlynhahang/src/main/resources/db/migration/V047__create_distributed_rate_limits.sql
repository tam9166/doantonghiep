IF OBJECT_ID(N'dbo.api_rate_limits', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.api_rate_limits (
        bucket_key VARCHAR(220) NOT NULL,
        window_end_epoch BIGINT NOT NULL,
        request_count INT NOT NULL,
        updated_at DATETIME2 NOT NULL,
        CONSTRAINT pk_api_rate_limits PRIMARY KEY (bucket_key),
        CONSTRAINT ck_api_rate_limits_request_count CHECK (request_count > 0)
    );
    CREATE INDEX ix_api_rate_limits_updated_at ON dbo.api_rate_limits(updated_at);
END;
