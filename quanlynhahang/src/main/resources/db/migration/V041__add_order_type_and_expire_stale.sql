-- V041: Add order_type column and expire_time to reservations for P0 fixes
-- 1. Add order_type to Orders table
ALTER TABLE dbo.Orders ADD order_type NVARCHAR(20) NOT NULL DEFAULT 'TAKEAWAY';

-- 2. Add deposit_expires_at to Reservations for waiting expiration tracking
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Reservations') AND name = 'deposit_expires_at')
BEGIN
    ALTER TABLE dbo.Reservations ADD deposit_expires_at DATETIME NULL;
END

-- 3. Add contact_task_status to Reservations for post-deposit workflow
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('dbo.Reservations') AND name = 'contact_task_status')
BEGIN
    ALTER TABLE dbo.Reservations ADD contact_task_status NVARCHAR(40) NOT NULL DEFAULT 'PENDING';
END
