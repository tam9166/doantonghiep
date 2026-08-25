SET NOCOUNT ON;

-- V006 keyed active intents by reservation_id. SQL Server treats all order
-- intents as the same NULL reservation key, so a second pending order QR
-- violated the unique index. Aggregate identity is the canonical key for both
-- reservations and orders.
IF OBJECT_ID(N'dbo.payment_intents', N'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.payment_intents')
          AND name = N'uk_payment_intent_active_purpose'
    )
        EXEC(N'DROP INDEX uk_payment_intent_active_purpose ON dbo.payment_intents');

    IF NOT EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE object_id = OBJECT_ID(N'dbo.payment_intents')
          AND name = N'uk_payment_intent_active_aggregate_purpose'
    )
        EXEC(N'CREATE UNIQUE INDEX uk_payment_intent_active_aggregate_purpose
            ON dbo.payment_intents(aggregate_type, aggregate_id, payment_option)
            WHERE status = ''PENDING''');
END;

IF OBJECT_ID(N'dbo.Orders', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH(N'dbo.Orders', N'checkout_idempotency_key') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD checkout_idempotency_key VARCHAR(100) NULL');

    IF COL_LENGTH(N'dbo.Orders', N'checkout_request_hash') IS NULL
        EXEC(N'ALTER TABLE dbo.Orders ADD checkout_request_hash VARCHAR(64) NULL');
END;

GO

IF OBJECT_ID(N'dbo.Orders', N'U') IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.Orders')
      AND name = N'ux_orders_checkout_idempotency_key'
)
    EXEC(N'CREATE UNIQUE INDEX ux_orders_checkout_idempotency_key
        ON dbo.Orders(checkout_idempotency_key)
        WHERE checkout_idempotency_key IS NOT NULL');
