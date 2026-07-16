IF COL_LENGTH(N'dbo.payment_intents', N'aggregate_type') IS NULL
    ALTER TABLE dbo.payment_intents ADD aggregate_type VARCHAR(30) NOT NULL
        CONSTRAINT df_payment_intent_aggregate_type DEFAULT 'RESERVATION';

IF COL_LENGTH(N'dbo.payment_intents', N'aggregate_id') IS NULL
BEGIN
    ALTER TABLE dbo.payment_intents ADD aggregate_id BIGINT NULL;
    EXEC(N'UPDATE dbo.payment_intents SET aggregate_id = reservation_id WHERE aggregate_id IS NULL');
    EXEC(N'ALTER TABLE dbo.payment_intents ALTER COLUMN aggregate_id BIGINT NOT NULL');
END;

IF COL_LENGTH(N'dbo.payment_intents', N'aggregate_code') IS NULL
BEGIN
    ALTER TABLE dbo.payment_intents ADD aggregate_code VARCHAR(40) NULL;
    EXEC(N'UPDATE pi SET aggregate_code = r.reservation_code
           FROM dbo.payment_intents pi
           INNER JOIN dbo.reservations r ON r.id = pi.reservation_id');
    EXEC(N'ALTER TABLE dbo.payment_intents ALTER COLUMN aggregate_code VARCHAR(40) NOT NULL');
END;

IF COL_LENGTH(N'dbo.payment_intents', N'purpose') IS NULL
BEGIN
    ALTER TABLE dbo.payment_intents ADD purpose VARCHAR(30) NULL;
    EXEC(N'UPDATE dbo.payment_intents SET purpose = payment_option WHERE purpose IS NULL');
    EXEC(N'ALTER TABLE dbo.payment_intents ALTER COLUMN purpose VARCHAR(30) NOT NULL');
END;

IF COL_LENGTH(N'dbo.payment_intents', N'currency') IS NULL
    ALTER TABLE dbo.payment_intents ADD currency VARCHAR(3) NOT NULL
        CONSTRAINT df_payment_intent_currency DEFAULT 'VND';

IF COL_LENGTH(N'dbo.payment_intents', N'paid_amount') IS NULL
    ALTER TABLE dbo.payment_intents ADD paid_amount DECIMAL(18,0) NOT NULL
        CONSTRAINT df_payment_intent_paid_amount DEFAULT 0;

IF COL_LENGTH(N'dbo.payment_intents', N'remaining_amount') IS NULL
BEGIN
    ALTER TABLE dbo.payment_intents ADD remaining_amount DECIMAL(18,0) NOT NULL
        CONSTRAINT df_payment_intent_remaining_amount DEFAULT 0;
    EXEC(N'UPDATE dbo.payment_intents
           SET paid_amount = CASE WHEN status = ''PAID'' THEN amount ELSE 0 END,
               remaining_amount = CASE WHEN status = ''PAID'' THEN 0 ELSE amount END');
END;

IF COL_LENGTH(N'dbo.payment_intents', N'bank_bin') IS NULL
    ALTER TABLE dbo.payment_intents ADD bank_bin VARCHAR(20) NULL;

IF COL_LENGTH(N'dbo.payment_intents', N'qr_provider') IS NULL
    ALTER TABLE dbo.payment_intents ADD qr_provider VARCHAR(30) NOT NULL
        CONSTRAINT df_payment_intent_qr_provider DEFAULT 'VIETQR';

IF COL_LENGTH(N'dbo.payment_intents', N'replaced_by_id') IS NULL
    ALTER TABLE dbo.payment_intents ADD replaced_by_id BIGINT NULL;

IF COL_LENGTH(N'dbo.payment_intents', N'idempotency_key') IS NULL
    ALTER TABLE dbo.payment_intents ADD idempotency_key VARCHAR(100) NULL;

IF COL_LENGTH(N'dbo.payment_intents', N'request_hash') IS NULL
    ALTER TABLE dbo.payment_intents ADD request_hash VARCHAR(64) NULL;

IF COL_LENGTH(N'dbo.payment_intents', N'created_by') IS NULL
    ALTER TABLE dbo.payment_intents ADD created_by VARCHAR(80) NULL;

IF COL_LENGTH(N'dbo.payment_intents', N'updated_at') IS NULL
    ALTER TABLE dbo.payment_intents ADD updated_at DATETIME2 NOT NULL
        CONSTRAINT df_payment_intent_updated_at DEFAULT SYSUTCDATETIME();

IF COL_LENGTH(N'dbo.payment_intents', N'version') IS NULL
    ALTER TABLE dbo.payment_intents ADD version BIGINT NOT NULL
        CONSTRAINT df_payment_intent_version DEFAULT 0;

;WITH duplicate_active AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY reservation_id, payment_option
               ORDER BY created_at DESC, id DESC
           ) AS row_number
    FROM dbo.payment_intents
    WHERE status = 'PENDING'
)
UPDATE pi SET status = 'EXPIRED'
FROM dbo.payment_intents pi
INNER JOIN duplicate_active duplicate ON duplicate.id = pi.id
WHERE duplicate.row_number > 1;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.payment_intents')
      AND name = N'uk_payment_intent_idempotency_key'
)
    EXEC(N'CREATE UNIQUE INDEX uk_payment_intent_idempotency_key
           ON dbo.payment_intents(idempotency_key)
           WHERE idempotency_key IS NOT NULL');

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'dbo.payment_intents')
      AND name = N'uk_payment_intent_active_purpose'
)
    CREATE UNIQUE INDEX uk_payment_intent_active_purpose
        ON dbo.payment_intents(reservation_id, payment_option)
        WHERE status = 'PENDING';
