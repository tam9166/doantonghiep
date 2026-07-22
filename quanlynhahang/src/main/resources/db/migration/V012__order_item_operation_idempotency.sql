IF OBJECT_ID(N'dbo.order_item_operations', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.order_item_operations (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        order_id INT NOT NULL,
        idempotency_key VARCHAR(100) NOT NULL,
        request_hash VARCHAR(64) NOT NULL,
        added_items INT NOT NULL,
        sub_total FLOAT NOT NULL,
        tax_amount FLOAT NOT NULL,
        total_amount FLOAT NOT NULL,
        created_at DATETIME2 NOT NULL CONSTRAINT df_order_item_operations_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT uk_order_item_operations_key UNIQUE (order_id, idempotency_key)
    );
END;
