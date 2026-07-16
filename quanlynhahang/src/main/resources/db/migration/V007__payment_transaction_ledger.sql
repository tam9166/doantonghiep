IF COL_LENGTH(N'dbo.reservations', N'paid_amount') IS NULL
    ALTER TABLE dbo.reservations ADD paid_amount DECIMAL(18,0) NOT NULL
        CONSTRAINT df_reservation_paid_amount DEFAULT 0;

EXEC(N'UPDATE r
       SET paid_amount = paid.total_paid,
           remaining_amount = CASE WHEN r.total_amount > paid.total_paid
                                   THEN r.total_amount - paid.total_paid ELSE 0 END,
           payment_status = CASE WHEN paid.total_paid > r.total_amount THEN ''OVERPAID''
                                 WHEN paid.total_paid = r.total_amount THEN ''PAID''
                                 WHEN paid.total_paid > 0 THEN ''PARTIALLY_PAID''
                                 ELSE ''UNPAID'' END
       FROM dbo.reservations r
       CROSS APPLY (
           SELECT COALESCE(SUM(CASE WHEN pi.status = ''PAID'' THEN pi.amount ELSE 0 END), 0) AS total_paid
           FROM dbo.payment_intents pi
           WHERE pi.reservation_id = r.id
       ) paid');

IF OBJECT_ID(N'dbo.payment_transactions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.payment_transactions (
        id BIGINT IDENTITY(1,1) NOT NULL,
        payment_intent_id BIGINT NULL,
        aggregate_type VARCHAR(30) NULL,
        aggregate_id BIGINT NULL,
        provider VARCHAR(40) NOT NULL,
        provider_transaction_id VARCHAR(120) NOT NULL,
        amount DECIMAL(18,0) NOT NULL,
        direction VARCHAR(20) NOT NULL,
        status VARCHAR(30) NOT NULL,
        raw_reference NVARCHAR(200) NULL,
        payload_hash VARCHAR(64) NULL,
        received_at DATETIME2 NOT NULL CONSTRAINT df_payment_transaction_received_at DEFAULT SYSUTCDATETIME(),
        created_at DATETIME2 NOT NULL CONSTRAINT df_payment_transaction_created_at DEFAULT SYSUTCDATETIME(),
        CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
        CONSTRAINT uk_payment_transaction_provider_id UNIQUE (provider_transaction_id),
        CONSTRAINT fk_payment_transaction_intent FOREIGN KEY (payment_intent_id)
            REFERENCES dbo.payment_intents(id)
    );

    CREATE INDEX ix_payment_transaction_aggregate
        ON dbo.payment_transactions(aggregate_type, aggregate_id, status);
    CREATE INDEX ix_payment_transaction_intent
        ON dbo.payment_transactions(payment_intent_id, status);
END;
