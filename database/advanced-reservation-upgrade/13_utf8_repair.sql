-- Do not automatically rewrite ambiguous broken text.
-- This script creates a manual repair queue and inserts suspicious rows once.

IF OBJECT_ID('dbo.utf8_manual_repair_queue', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.utf8_manual_repair_queue (
        id bigint IDENTITY(1,1) NOT NULL PRIMARY KEY,
        source_table varchar(100) NOT NULL,
        source_column varchar(100) NOT NULL,
        source_id varchar(80) NOT NULL,
        bad_value nvarchar(max) NULL,
        suggested_value nvarchar(max) NULL,
        status varchar(30) NOT NULL CONSTRAINT DF_utf8_repair_status DEFAULT('PENDING'),
        created_at datetime2 NOT NULL CONSTRAINT DF_utf8_repair_created DEFAULT SYSUTCDATETIME()
    );
END;

INSERT INTO dbo.utf8_manual_repair_queue (source_table, source_column, source_id, bad_value, suggested_value)
SELECT 'reservations', 'customer_name', CAST(r.id AS varchar(80)), r.customer_name, NULL
FROM dbo.reservations r
WHERE (
       r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
    OR r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Ã%'
    OR r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Â%'
    OR r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%'
    OR r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%'
    OR r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%'
    OR r.customer_name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%'
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.utf8_manual_repair_queue q
    WHERE q.source_table = 'reservations'
      AND q.source_column = 'customer_name'
      AND q.source_id = CAST(r.id AS varchar(80))
      AND q.status = 'PENDING'
);

INSERT INTO dbo.utf8_manual_repair_queue (source_table, source_column, source_id, bad_value, suggested_value)
SELECT 'products', 'name', CAST(p.id AS varchar(80)), p.name, NULL
FROM dbo.products p
WHERE (
       p.name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
    OR p.name COLLATE Latin1_General_100_BIN2 LIKE N'%Ã%'
    OR p.name COLLATE Latin1_General_100_BIN2 LIKE N'%Â%'
    OR p.name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%'
    OR p.name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%'
    OR p.name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%'
    OR p.name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%'
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.utf8_manual_repair_queue q
    WHERE q.source_table = 'products'
      AND q.source_column = 'name'
      AND q.source_id = CAST(p.id AS varchar(80))
      AND q.status = 'PENDING'
);

INSERT INTO dbo.utf8_manual_repair_queue (source_table, source_column, source_id, bad_value, suggested_value)
SELECT 'categories', 'name', CAST(c.id AS varchar(80)), c.name, NULL
FROM dbo.categories c
WHERE (
       c.name COLLATE Latin1_General_100_BIN2 LIKE N'%�%'
    OR c.name COLLATE Latin1_General_100_BIN2 LIKE N'%Ã%'
    OR c.name COLLATE Latin1_General_100_BIN2 LIKE N'%Â%'
    OR c.name COLLATE Latin1_General_100_BIN2 LIKE N'%áº%'
    OR c.name COLLATE Latin1_General_100_BIN2 LIKE N'%á»%'
    OR c.name COLLATE Latin1_General_100_BIN2 LIKE N'%Ä%'
    OR c.name COLLATE Latin1_General_100_BIN2 LIKE N'%Æ%'
)
AND NOT EXISTS (
    SELECT 1 FROM dbo.utf8_manual_repair_queue q
    WHERE q.source_table = 'categories'
      AND q.source_column = 'name'
      AND q.source_id = CAST(c.id AS varchar(80))
      AND q.status = 'PENDING'
);

SELECT source_table, source_column, source_id, bad_value, status, created_at
FROM dbo.utf8_manual_repair_queue
WHERE status = 'PENDING'
ORDER BY created_at DESC, id DESC;
