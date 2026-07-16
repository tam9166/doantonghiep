IF COL_LENGTH(N'dbo.Orders', N'payment_option') IS NULL
    ALTER TABLE dbo.Orders ADD payment_option VARCHAR(30) NOT NULL
        CONSTRAINT df_orders_payment_option DEFAULT 'PAY_AT_RESTAURANT';

IF COL_LENGTH(N'dbo.Orders', N'payment_status') IS NULL
    ALTER TABLE dbo.Orders ADD payment_status VARCHAR(30) NOT NULL
        CONSTRAINT df_orders_payment_status DEFAULT 'UNPAID';

IF COL_LENGTH(N'dbo.Orders', N'paid_amount') IS NULL
    ALTER TABLE dbo.Orders ADD paid_amount DECIMAL(18,0) NOT NULL
        CONSTRAINT df_orders_paid_amount DEFAULT 0;

IF COL_LENGTH(N'dbo.Orders', N'remaining_amount') IS NULL
BEGIN
    ALTER TABLE dbo.Orders ADD remaining_amount DECIMAL(18,0) NOT NULL
        CONSTRAINT df_orders_remaining_amount DEFAULT 0;
    EXEC(N'UPDATE dbo.Orders
           SET remaining_amount = CASE
               WHEN total_amount > 0 AND is_paid = 0 THEN total_amount
               ELSE 0 END');
END;

IF COL_LENGTH(N'dbo.Orders', N'payment_confirmed_by') IS NULL
    ALTER TABLE dbo.Orders ADD payment_confirmed_by VARCHAR(80) NULL;

IF COL_LENGTH(N'dbo.Orders', N'payment_confirmed_at') IS NULL
    ALTER TABLE dbo.Orders ADD payment_confirmed_at DATETIME2 NULL;

IF COL_LENGTH(N'dbo.payment_intents', N'order_id') IS NULL
BEGIN
    ALTER TABLE dbo.payment_intents ADD order_id BIGINT NULL;
    EXEC(N'ALTER TABLE dbo.payment_intents ADD CONSTRAINT fk_payment_intent_order
        FOREIGN KEY (order_id) REFERENCES dbo.Orders(id)');
END;

IF EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.payment_intents')
      AND name = N'reservation_id'
      AND is_nullable = 0
)
    ALTER TABLE dbo.payment_intents ALTER COLUMN reservation_id BIGINT NULL;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.payment_intents')
      AND name = N'uk_payment_intent_active_order_purpose'
)
    EXEC(N'CREATE UNIQUE INDEX uk_payment_intent_active_order_purpose
        ON dbo.payment_intents(order_id, payment_option)
        WHERE status = ''PENDING'' AND order_id IS NOT NULL');

IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.payment_intents')
      AND name = N'ck_payment_intent_single_aggregate'
)
    EXEC(N'ALTER TABLE dbo.payment_intents ADD CONSTRAINT ck_payment_intent_single_aggregate
        CHECK ((reservation_id IS NOT NULL AND order_id IS NULL)
            OR (reservation_id IS NULL AND order_id IS NOT NULL))');
