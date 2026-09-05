/*
    Close legacy operational records that predate the current business day.
    No rows are deleted. Terminal cancellations/no-shows/refunds and future
    reservations are intentionally preserved for audit and upcoming service.
*/
SET XACT_ABORT ON;

DECLARE @business_now DATETIME2 = CONVERT(
    DATETIME2,
    SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time'
);
DECLARE @business_today DATE = CONVERT(DATE, @business_now);

BEGIN TRANSACTION;

-- Existing status codes: 0 pending, 1 cooking, 2 ready, 5 scheduled,
-- 6 partially ready, 7 served.  Status 3/4 are already terminal.
UPDATE dbo.Orders
SET status = 4
WHERE status IN (0, 1, 2, 5, 6, 7)
  AND (
       (scheduled_at IS NOT NULL AND CONVERT(DATE, scheduled_at) < @business_today)
    OR (scheduled_at IS NULL AND create_date IS NOT NULL AND CONVERT(DATE, create_date) < @business_today)
    OR (scheduled_at IS NULL AND create_date IS NULL AND order_code LIKE N'ORD-LEGACY-%')
  );

-- Normalize only historical completed invoices whose legacy payment fields
-- still say UNPAID.  The migration actor/timestamp keeps this repair auditable.
UPDATE dbo.Orders
SET is_paid = 1,
    payment_status = 'PAID',
    paid_amount = COALESCE(total_amount, 0),
    remaining_amount = 0,
    payment_confirmed_by = COALESCE(payment_confirmed_by, 'MIGRATION_V102_LEGACY_CLEANUP'),
    payment_confirmed_at = COALESCE(payment_confirmed_at, @business_now)
WHERE status = 4
  AND payment_status = 'UNPAID'
  AND (
       (scheduled_at IS NOT NULL AND CONVERT(DATE, scheduled_at) < @business_today)
    OR (scheduled_at IS NULL AND create_date IS NOT NULL AND CONVERT(DATE, create_date) < @business_today)
    OR (scheduled_at IS NULL AND create_date IS NULL AND order_code LIKE N'ORD-LEGACY-%')
  );

-- Some early databases allowed UNPAID intents. Align only intents belonging to
-- the completed legacy invoices above; pending/refund evidence is not changed.
UPDATE intent
SET intent.status = 'PAID',
    intent.paid_amount = intent.amount,
    intent.remaining_amount = 0,
    intent.paid_at = COALESCE(intent.paid_at, @business_now),
    intent.confirmed_by = COALESCE(intent.confirmed_by, 'MIGRATION_V102_LEGACY_CLEANUP'),
    intent.updated_at = @business_now
FROM dbo.payment_intents intent
JOIN dbo.Orders legacy_order ON legacy_order.id = intent.order_id
WHERE legacy_order.status = 4
  AND legacy_order.payment_status = 'PAID'
  AND legacy_order.payment_confirmed_by = 'MIGRATION_V102_LEGACY_CLEANUP'
  AND intent.status = 'UNPAID';

-- Preserve terminal audit outcomes; only stale active reservations are closed.
UPDATE dbo.reservations
SET reservation_status = 'COMPLETED',
    updated_at = @business_now
WHERE reservation_date < @business_today
  AND reservation_status IN (
      'PENDING', 'WAITING_TABLE_ASSIGNMENT', 'CONFIRMED', 'DEPOSIT_REQUIRED',
      'DEPOSIT_PENDING', 'DEPOSIT_PAID', 'FULLY_PAID', 'CHECKED_IN', 'IN_SERVICE'
  );

-- Release stale occupied/reserved/cleaning tables only when neither an active
-- order nor a current/future reservation still owns the table. Merged tables
-- (state 5) are deliberately untouched.
UPDATE table_record
SET is_occupied = 0,
    reserved_time = NULL
FROM dbo.restaurant_table table_record
WHERE table_record.is_occupied IN (1, 2, 3)
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.Orders active_order
      WHERE active_order.table_id = table_record.id
        AND active_order.status NOT IN (3, 4)
  )
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.reservations active_reservation
      WHERE (active_reservation.table_id = table_record.id OR EXISTS (
              SELECT 1
              FROM dbo.reservation_tables assignment
              WHERE assignment.reservation_id = active_reservation.id
                AND assignment.table_id = table_record.id
            ))
        AND active_reservation.reservation_date >= @business_today
        AND active_reservation.reservation_status NOT IN (
            'REJECTED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'NO_SHOW'
        )
  );

COMMIT TRANSACTION;
