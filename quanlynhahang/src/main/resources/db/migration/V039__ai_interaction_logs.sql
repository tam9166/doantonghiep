IF OBJECT_ID('ai_interaction_logs','U') IS NULL
BEGIN
 CREATE TABLE ai_interaction_logs(id BIGINT IDENTITY(1,1) PRIMARY KEY,session_id VARCHAR(80),request_type VARCHAR(40),question NVARCHAR(4000),response NVARCHAR(MAX),source VARCHAR(40),helpful BIT NULL,feedback_comment NVARCHAR(1000),created_at DATETIME2 NOT NULL CONSTRAINT df_ai_log_created DEFAULT SYSUTCDATETIME());
 CREATE INDEX ix_ai_logs_created ON ai_interaction_logs(created_at DESC);
END;
