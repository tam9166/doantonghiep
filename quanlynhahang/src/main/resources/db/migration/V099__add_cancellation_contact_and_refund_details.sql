-- Store the customer's preferred contact channel and conditional refund destination.
-- All columns are nullable so existing cancellation requests remain valid.
IF COL_LENGTH('dbo.reservation_cancellation_requests', 'contact_method') IS NULL
    ALTER TABLE dbo.reservation_cancellation_requests ADD contact_method varchar(20) NULL;
IF COL_LENGTH('dbo.reservation_cancellation_requests', 'refund_bank_name') IS NULL
    ALTER TABLE dbo.reservation_cancellation_requests ADD refund_bank_name varchar(120) NULL;
IF COL_LENGTH('dbo.reservation_cancellation_requests', 'refund_account_number') IS NULL
    ALTER TABLE dbo.reservation_cancellation_requests ADD refund_account_number varchar(40) NULL;
IF COL_LENGTH('dbo.reservation_cancellation_requests', 'refund_account_holder') IS NULL
    ALTER TABLE dbo.reservation_cancellation_requests ADD refund_account_holder nvarchar(150) NULL;
