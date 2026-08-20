-- V042: Add refund_transactions and order_voucher_usage tables
-- Part of P0-X2: Refund support + Voucher usage tracking

-- 1. Refund transactions table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[refund_transactions]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[refund_transactions] (
        [id]              BIGINT IDENTITY(1,1) PRIMARY KEY,
        [reservation_id]  BIGINT NULL,
        [order_id]        INT NULL,
        [payment_intent_id] BIGINT NULL,
        [amount]          DECIMAL(18,0) NOT NULL,
        [forfeited_amount] DECIMAL(18,0) NULL,
        [reason]          NVARCHAR(30) NOT NULL,
        [reason_detail]   NVARCHAR(500) NULL,
        [status]          NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
        [processed_by]    NVARCHAR(80) NULL,
        [created_at]      DATETIME2 NOT NULL DEFAULT GETDATE(),
        [processed_at]    DATETIME2 NULL,
        [failure_reason]  NVARCHAR(500) NULL,
        [notes]           NVARCHAR(1000) NULL
    );
END;

-- 2. Order voucher usage table (separate from reservation_voucher_usage)
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[order_voucher_usage]') AND type in (N'U'))
BEGIN
    CREATE TABLE [dbo].[order_voucher_usage] (
        [id]               BIGINT IDENTITY(1,1) PRIMARY KEY,
        [voucher_id]       BIGINT NOT NULL,
        [voucher_code]     NVARCHAR(50) NOT NULL,
        [order_id]         INT NOT NULL,
        [account_username] NVARCHAR(80) NULL,
        [discount_amount]  DECIMAL(18,0) NOT NULL,
        [original_amount]  DECIMAL(18,0) NOT NULL,
        [used_at]          DATETIME2 NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [UQ_voucher_order] UNIQUE ([voucher_id], [order_id])
    );
END;

-- 3. Index for refund lookups
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_refund_transactions_reservation')
    CREATE INDEX [IX_refund_transactions_reservation] ON [dbo].[refund_transactions] ([reservation_id]);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_refund_transactions_status')
    CREATE INDEX [IX_refund_transactions_status] ON [dbo].[refund_transactions] ([status]);

-- 4. Index for order voucher usage
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_order_voucher_usage_voucher')
    CREATE INDEX [IX_order_voucher_usage_voucher] ON [dbo].[order_voucher_usage] ([voucher_id]);
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_order_voucher_usage_order')
    CREATE INDEX [IX_order_voucher_usage_order] ON [dbo].[order_voucher_usage] ([order_id]);