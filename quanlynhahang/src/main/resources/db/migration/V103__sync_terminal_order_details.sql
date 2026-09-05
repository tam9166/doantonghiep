/*
    Synchronize stale dish states with terminal parent orders.
    No rows are deleted. Future reservation preorders are excluded so their
    operational lifecycle remains available on the scheduled service date.

    Order status: 3 = CANCELLED, 4 = COMPLETED, 7 = SERVED.
    OrderDetail status: 0 = waiting/cooking, 1 = ready, 2 = served, 3 = cancelled.
    REFUNDED is represented by Orders.payment_status, not by Orders.status.
*/
SET XACT_ABORT ON;

DECLARE @business_now DATETIME2 = CONVERT(
    DATETIME2,
    SYSUTCDATETIME() AT TIME ZONE 'UTC' AT TIME ZONE 'SE Asia Standard Time'
);
DECLARE @business_today DATE = CONVERT(DATE, @business_now);

BEGIN TRANSACTION;

-- A cancelled order cannot retain a cook/serve queue item. Preserve the row
-- and its existing audit columns while making the terminal state explicit.
UPDATE detail
SET detail.status = 3
FROM dbo.order_details detail
JOIN dbo.Orders parent_order ON parent_order.id = detail.order_id
WHERE parent_order.status = 3
  AND detail.status <> 3;

-- Completed/served/refunded invoices must not leave waiting or ready dishes
-- visible in Kitchen/Waiter. Do not touch an anomalous future preorder: its
-- reservation service date remains authoritative until that date arrives.
UPDATE detail
SET detail.status = 2
FROM dbo.order_details detail
JOIN dbo.Orders parent_order ON parent_order.id = detail.order_id
WHERE detail.status IN (0, 1)
  AND (
       parent_order.status IN (4, 7)
    OR (parent_order.status <> 3 AND parent_order.payment_status = 'REFUNDED')
  )
  AND NOT EXISTS (
      SELECT 1
      FROM dbo.reservations future_reservation
      WHERE future_reservation.kitchen_order_id = parent_order.id
        AND future_reservation.reservation_date > @business_today
        AND future_reservation.reservation_status NOT IN (
            'REJECTED', 'COMPLETED', 'CANCELLED', 'EXPIRED', 'NO_SHOW'
        )
  );

COMMIT TRANSACTION;
